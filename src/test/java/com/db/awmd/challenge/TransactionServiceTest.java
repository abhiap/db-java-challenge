package com.db.awmd.challenge;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.exception.InvalidAmountException;
import com.db.awmd.challenge.exception.LowBalanceException;
import com.db.awmd.challenge.exception.ResourceNotFoundException;
import com.db.awmd.challenge.service.AccountsService;
import com.db.awmd.challenge.service.NotificationService;
import com.db.awmd.challenge.service.TransactionService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;

/**
 * @author abhijit.patil on 05-01-2018
 *         <p>
 *         ${tags}
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class TransactionServiceTest {
	
	private final static int NUM_THREADS = 15;
	@Autowired
	private TransactionService transactionService;
	@Autowired
	private AccountsService accountsService;
	@MockBean
	private NotificationService notificationService;
	private Account fromAccount;
	private Account toAccount;
	
	@Before
	public void init() {
		// Reset the existing accounts before each test.
		accountsService.getAccountsRepository().clearAccounts();
		
		// Create test accounts
		String uniqueId = "Id-" + System.currentTimeMillis();
		fromAccount = new Account(uniqueId);
		this.accountsService.createAccount(fromAccount);
		
		uniqueId = "Id-" + (System.currentTimeMillis() + 1);
		toAccount = new Account(uniqueId);
		this.accountsService.createAccount(toAccount);
	}
	
	@Test(expected = InvalidAmountException.class)
	public void transfer_FailsOnNegativeAmount() {
		BigDecimal amtToTransfer = new BigDecimal(-10);
		this.transactionService.transfer(fromAccount.getAccountId(), toAccount.getAccountId(), amtToTransfer);
	}
	
	@Test(expected = InvalidAmountException.class)
	public void transfer_FailsOnZeroAmount() {
		BigDecimal amtToTransfer = new BigDecimal(0);
		this.transactionService.transfer(fromAccount.getAccountId(), toAccount.getAccountId(), amtToTransfer);
	}
	
	@Test(expected = ResourceNotFoundException.class)
	public void transfer_FailsOnWrongFromAccount() {
		BigDecimal amtToTransfer = new BigDecimal(10);
		this.transactionService.transfer("id908", toAccount.getAccountId(), amtToTransfer);
	}
	
	@Test(expected = ResourceNotFoundException.class)
	public void transfer_FailsOnWrongToAccount() {
		BigDecimal amtToTransfer = new BigDecimal(10);
		this.transactionService.transfer(fromAccount.getAccountId(), "id909", amtToTransfer);
	}
	
	@Test
	public void transfer_Valid() {
		fromAccount.setBalance(new BigDecimal(100.00));
		toAccount.setBalance(new BigDecimal(10.00));
		BigDecimal initialBalanceFromAcc = fromAccount.getBalance();
		BigDecimal initialBalanceToAcc = toAccount.getBalance();
		BigDecimal amtToTransfer = new BigDecimal(10.50);
		
		// mocking notification service
		doNothing().when(notificationService).notifyAboutTransfer(any(Account.class), anyString());
		
		this.transactionService.transfer(fromAccount.getAccountId(), toAccount.getAccountId(), amtToTransfer);
		
		Account fAccount = this.accountsService.getAccount(fromAccount.getAccountId());
		Account tAccount = this.accountsService.getAccount(toAccount.getAccountId());
		
		Assert.assertTrue("Balance in from account should change to (initial balance-transferred amount)",
				(initialBalanceFromAcc.subtract(amtToTransfer).compareTo(fromAccount.getBalance()) == 0));
		Assert.assertTrue("Balance in to account should change to (initial balance+transferred amount)",
				(initialBalanceToAcc.add(amtToTransfer).compareTo(toAccount.getBalance()) == 0));
		
	}
	
	@Test
	public void transfer_FailsWhenAccountHasLessBalance() {
		fromAccount.setBalance(new BigDecimal(100.00));
		toAccount.setBalance(new BigDecimal(10.00));
		BigDecimal initialBalanceFromAcc = fromAccount.getBalance();
		BigDecimal initialBalanceToAcc = toAccount.getBalance();
		BigDecimal amtToTransfer = new BigDecimal(100.50);
		
		try {
			doNothing().when(notificationService).notifyAboutTransfer(any(Account.class), anyString());
			this.transactionService.transfer(fromAccount.getAccountId(), toAccount.getAccountId(), amtToTransfer);
			fail("Should have thrown Low Balance exception");
		} catch (Throwable lbe) {
			Assert.assertTrue((lbe instanceof LowBalanceException));
		}
		Account fAccount = this.accountsService.getAccount(fromAccount.getAccountId());
		Account tAccount = this.accountsService.getAccount(toAccount.getAccountId());
		
		Assert.assertTrue("Balance in from account should stay unchanged",
				(fAccount.getBalance().compareTo(new BigDecimal(100.00)) == 0));
		Assert.assertTrue("Balance in to account should stay unchanged",
				(tAccount.getBalance().compareTo(new BigDecimal(10.00)) == 0));
		
	}
	
	@Test
	public void transfer_Concurrent() throws InterruptedException, ExecutionException {
		
		BigDecimal initialBalanceFromAcc = new BigDecimal(100.00);
		BigDecimal initialBalanceToAcc = new BigDecimal(50.00);
		BigDecimal amtToTransfer1 = new BigDecimal(10.00);
		BigDecimal amtToTransfer2 = new BigDecimal(15.00);
		
		fromAccount.setBalance(initialBalanceFromAcc);
		toAccount.setBalance(initialBalanceToAcc);
		//test(5);
		for (int i = 0; i < NUM_THREADS; i++) {
			Runnable task = null;
			if (i % 2 == 1) {
				task = new Runnable() {
					@Override
					public void run() {
						transactionService.transfer(fromAccount.getAccountId(), toAccount.getAccountId(), amtToTransfer1);
					}
				};
			} else {
				task = new Runnable() {
					@Override
					public void run() {
						transactionService.transfer(toAccount.getAccountId(), fromAccount.getAccountId(),
								amtToTransfer2);
					}
				};
			}
			
			Thread t = new Thread(task);
			t.start();
			//t.join();
			Assert.assertTrue("Total balance in accounts at any time should be same as total balance at beginning.",
					(initialBalanceFromAcc.add(initialBalanceToAcc).compareTo(fromAccount.getBalance().add(toAccount
							.getBalance())) == 0));
			Assert.assertEquals(initialBalanceFromAcc.add(initialBalanceToAcc), fromAccount.getBalance().add(toAccount
					.getBalance()));
		}
		System.out.println("fromAccount = " + fromAccount.getBalance() + "\ttoAccount = " + toAccount.getBalance());
	}
	
	@Test
	public void transfer_ConcurrentWithThreeAccounts() throws InterruptedException, ExecutionException {
		
		BigDecimal initialBalanceAcc1 = new BigDecimal(100.00);
		BigDecimal initialBalanceAcc2 = new BigDecimal(50.00);
		BigDecimal initialBalanceAcc3 = new BigDecimal(200.00);
		BigDecimal amtToTransfer1 = new BigDecimal(10.00);
		BigDecimal amtToTransfer2 = new BigDecimal(20.00);
		BigDecimal amtToTransfer3 = new BigDecimal(30.00);
		
		fromAccount.setBalance(initialBalanceAcc1);
		toAccount.setBalance(initialBalanceAcc2);
		String uniqueId = "Id-" + (System.currentTimeMillis() + 2);
		Account account3 = new Account(uniqueId);
		this.accountsService.createAccount(account3);
		account3.setBalance(initialBalanceAcc3);
		
		for (int i = 0; i < NUM_THREADS; i++) {
			Runnable task = null;
			if (i % 3 == 1) {
				task = new Runnable() {
					@Override
					public void run() {
						transactionService.transfer(fromAccount.getAccountId(), toAccount.getAccountId(), amtToTransfer1);
					}
				};
			} else if (i % 2 == 2) {
				task = new Runnable() {
					@Override
					public void run() {
						transactionService.transfer(toAccount.getAccountId(), account3.getAccountId(), amtToTransfer2);
					}
				};
			} else {
				task = new Runnable() {
					@Override
					public void run() {
						transactionService.transfer(account3.getAccountId(), fromAccount.getAccountId(),
								amtToTransfer3);
					}
				};
			}
			
			Thread t = new Thread(task);
			t.start();
			//t.join();
			Assert.assertTrue("Total balance in accounts at any time should be same as total balance at beginning.",
					(initialBalanceAcc1.add(initialBalanceAcc2).add(initialBalanceAcc3).compareTo(fromAccount.getBalance().add
							(toAccount.getBalance()).add(account3.getBalance())) == 0));
			Assert.assertEquals(initialBalanceAcc1.add(initialBalanceAcc2).add(initialBalanceAcc3), fromAccount.getBalance().add
					(toAccount.getBalance()).add(account3.getBalance()));
		}
		System.out.println("fromAccount = " + fromAccount.getBalance() + "\ttoAccount = " + toAccount.getBalance() +
				"\taccount3 = " + account3.getBalance());
	}
	
	@Test
	public void transfer_ConcurrentWithTransferFromOneAcc() throws InterruptedException, ExecutionException {
		
		BigDecimal initialBalanceAcc1 = new BigDecimal(100.00);
		BigDecimal initialBalanceAcc2 = new BigDecimal(50.00);
		BigDecimal initialBalanceAcc3 = new BigDecimal(200.00);
		BigDecimal amtToTransfer1 = new BigDecimal(10.00);
		BigDecimal amtToTransfer2 = new BigDecimal(20.00);
		BigDecimal amtToTransfer3 = new BigDecimal(30.00);
		
		fromAccount.setBalance(initialBalanceAcc1);
		toAccount.setBalance(initialBalanceAcc2);
		String uniqueId = "Id-" + (System.currentTimeMillis() + 2);
		Account account3 = new Account(uniqueId);
		this.accountsService.createAccount(account3);
		account3.setBalance(initialBalanceAcc3);
		
		for (int i = 0; i < NUM_THREADS; i++) {
			Runnable task = null;
			if (i % 3 == 1) {
				task = new Runnable() {
					@Override
					public void run() {
						transactionService.transfer(fromAccount.getAccountId(), toAccount.getAccountId(), amtToTransfer1);
					}
				};
			} else if (i % 2 == 2) {
				task = new Runnable() {
					@Override
					public void run() {
						transactionService.transfer(fromAccount.getAccountId(), account3.getAccountId(), amtToTransfer2);
					}
				};
			} else {
				task = new Runnable() {
					@Override
					public void run() {
						transactionService.transfer(account3.getAccountId(), fromAccount.getAccountId(),
								amtToTransfer3);
					}
				};
			}
			
			Thread t = new Thread(task);
			t.start();
			//t.join();
			Assert.assertTrue("Total balance in accounts at any time should be same as total balance at beginning.",
					(initialBalanceAcc1.add(initialBalanceAcc2).add(initialBalanceAcc3).compareTo(fromAccount.getBalance().add
							(toAccount.getBalance()).add(account3.getBalance())) == 0));
			Assert.assertEquals(initialBalanceAcc1.add(initialBalanceAcc2).add(initialBalanceAcc3), fromAccount.getBalance().add
					(toAccount.getBalance()).add(account3.getBalance()));
		}
		System.out.println("fromAccount = " + fromAccount.getBalance() + "\ttoAccount = " + toAccount.getBalance() +
				"\taccount3 = " + account3.getBalance());
	}
}
