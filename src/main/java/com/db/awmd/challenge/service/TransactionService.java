package com.db.awmd.challenge.service;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.exception.InvalidAmountException;
import com.db.awmd.challenge.exception.LowBalanceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * @author abhijit.patil on 05-01-2018
 *         <p>
 *         ${tags}
 */
@Service
@Slf4j
public class TransactionService {
	
	private static final String SUCCESS = "SUCCESS";
	
	@Autowired
	private AccountsService accountsService;
	
	@Autowired
	private NotificationService notificationService;
	
	private Object lock = new Object();
	
	/**
	 * Transfers amount from sender account to receiver account in thread-safe manner
	 * @param fromAccountId
	 * @param toAccountId
	 * @param amountToTransfer
	 * @return
	 */
	public String transfer(String fromAccountId, String toAccountId, BigDecimal amountToTransfer) {
		log.info("Initiating transfer..thread: " + Thread.currentThread().getName() + "\tfromAccount = " + fromAccountId
				+ "\ttoAccount = " + toAccountId + "\tamt: " + amountToTransfer);
		//Req: The amount to transfer should always be a positive number.
		if (amountToTransfer == null || amountToTransfer.compareTo(new BigDecimal(0)) <= 0) {
			throw new InvalidAmountException("Transfer amount should be greater than 0.");
		}
		
		Account fromAccount = accountsService.getAccount(fromAccountId);
		Account toAccount = accountsService.getAccount(toAccountId);
		
		//Req: should never deadlock, should never result in corrupted account state, and should work efficiently for
		// multiple transfers happening at the same time
		Object firstLock = fromAccount.getAccountId().compareTo(toAccount.getAccountId()) > 0 ? fromAccount : toAccount;
		Object secondLock = fromAccount.getAccountId().compareTo(toAccount.getAccountId()) > 0 ? toAccount :
				fromAccount;
		//Acquiring locks in same order to avoid possible deadlock
		synchronized (firstLock) {
			synchronized (secondLock) {
				fromAccount = accountsService.getAccount(fromAccountId);
				toAccount = accountsService.getAccount(toAccountId);
				
				//Req: It should not be possible for an account to end up with negative balance
				if (fromAccount.getBalance().compareTo(amountToTransfer) < 0) {
					throw new LowBalanceException("Balance in from account is less than amount to be transferred. Overdraft " +
							"facility not supported");
				}
				fromAccount.setBalance(fromAccount.getBalance().subtract(amountToTransfer));
				toAccount.setBalance(toAccount.getBalance().add(amountToTransfer));
				
				log.info("fromAccount = " + fromAccount.getBalance() + "\ttoAccount = " + toAccount.getBalance
						() + "\tInt balance = " + fromAccount.getBalance().add(toAccount.getBalance()));
			}
		}
		
		//Req: Notifications to be sent to sender and receiver with account id and amount transferred
		notificationService.notifyAboutTransfer(fromAccount, "Amount " + amountToTransfer + " is transferred to " +
				"account " + toAccountId);
		notificationService.notifyAboutTransfer(toAccount, "Amount " + amountToTransfer + " is received from " +
				"account " + fromAccountId);
		
		return SUCCESS;
	}
	
//	public String transfer1(String fromAccountId, String toAccountId, BigDecimal amountToTransfer) {
//		log.info("Initiating transfer..thread: " + Thread.currentThread().getName() + "\tfromAccount = " + fromAccountId
//				+ "\ttoAccount = " + toAccountId + "\tamt: " + amountToTransfer);
//		//Req: The amount to transfer should always be a positive number.
//		if (amountToTransfer == null || amountToTransfer.compareTo(new BigDecimal(0)) <= 0) {
//			throw new InvalidAmountException("Transfer amount should be greater than 0.");
//		}
//
//		Account fromAccount = accountsService.getAccount(fromAccountId);
//		Account toAccount = accountsService.getAccount(toAccountId);
//		//Req: should never deadlock, should never result in corrupted account state, and should work efficiently for
//		// multiple transfers happening at the same time
//		synchronized (lock) {
//			fromAccount = accountsService.getAccount(fromAccountId);
//			toAccount = accountsService.getAccount(toAccountId);
//
//			//Req: It should not be possible for an account to end up with negative balance
//			if (fromAccount.getBalance().compareTo(amountToTransfer) < 0) {
//				throw new LowBalanceException("Balance in from account is less than amount to be transferred. Overdraft " +
//						"facility not supported");
//			}
//			fromAccount.setBalance(fromAccount.getBalance().subtract(amountToTransfer));
//			toAccount.setBalance(toAccount.getBalance().add(amountToTransfer));
//
//			log.info("thread: " + Thread.currentThread().getName() + "\tfromAccount = " + fromAccount
//					.getBalance() + "\ttoAccount = " + toAccount.getBalance
//					() + "\tInt balance = " + fromAccount.getBalance().add(toAccount.getBalance()));
//		}
//
//		//Req: Notifications to be sent to sender and receiver with account id and amount transferred
//		notificationService.notifyAboutTransfer(fromAccount, "Amount " + amountToTransfer + " is transferred to " +
//				"account " + toAccountId);
//		notificationService.notifyAboutTransfer(toAccount, "Amount " + amountToTransfer + " is received from " +
//				"account " + fromAccountId);
//
//		return SUCCESS;
//	}
}
