package com.db.awmd.challenge;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.service.AccountsService;
import com.db.awmd.challenge.service.TransactionService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * @author abhijit.patil on 05-01-2018
 *         <p>
 *         ${tags}
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@WebAppConfiguration
public class TransactionControllerTest {
	
	private MockMvc mockMvc;
	
	@Autowired
	private TransactionService transactionService;
	
	@Autowired
	private AccountsService accountsService;
	
	@Autowired
	private WebApplicationContext webApplicationContext;
	
	@Before
	public void prepareMockMvc() {
		this.mockMvc = webAppContextSetup(this.webApplicationContext).build();
		// Reset the existing accounts before each test.
		accountsService.getAccountsRepository().clearAccounts();
	}
	
	@Test
	public void transfer_valid() throws Exception {
		String fromAcc = "Id-123";
		String toAcc = "Id-124";
		String fromAccBalance = "100";
		String toAccBalance = "50";
		String amountToTransfer = "10.50";
		
		this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
				.content("{\"accountId\":\"Id-123\",\"balance\":" + fromAccBalance + "}")).andExpect(status().isCreated());
		this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
				.content("{\"accountId\":\"Id-124\",\"balance\":" + toAccBalance + "}")).andExpect(status().isCreated());
		
		this.mockMvc.perform(post("/v1/transactions/transfer?fromAccountId=" + fromAcc + "&toAccountId=" + toAcc +
				"&amountToTransfer=" + amountToTransfer).contentType(MediaType
				.APPLICATION_JSON)).andExpect(status().isOk());
		
		Account fromAccount = this.accountsService.getAccount(fromAcc);
		Account toAccount = this.accountsService.getAccount(toAcc);
		
		BigDecimal initialBalanceFromAcc = new BigDecimal(fromAccBalance);
		BigDecimal initialBalanceToAcc = new BigDecimal(toAccBalance);
		BigDecimal amtToTransfer = new BigDecimal(amountToTransfer);
		Assert.assertTrue("Balance in from account should change to (initial balance-transferred amount)",
				(initialBalanceFromAcc.subtract(amtToTransfer).compareTo(fromAccount.getBalance()) == 0));
		Assert.assertTrue("Balance in to account should change to (initial balance+transferred amount)",
				(initialBalanceToAcc.add(amtToTransfer).compareTo(toAccount.getBalance()) == 0));
	}
	
	@Test
	public void transfer_LowBalance() throws Exception {
		String fromAcc = "Id-125";
		String toAcc = "Id-126";
		String fromAccBalance = "100";
		String toAccBalance = "50";
		String amountToTransfer = "100.50";
		
		this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
				.content("{\"accountId\":\"Id-125\",\"balance\":" + fromAccBalance + "}")).andExpect(status()
				.isCreated());
		this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
				.content("{\"accountId\":\"Id-126\",\"balance\":" + toAccBalance + "}")).andExpect(status().isCreated
				());
		
		this.mockMvc.perform(post("/v1/transactions/transfer?fromAccountId=" + fromAcc + "&toAccountId=" + toAcc +
				"&amountToTransfer=" + amountToTransfer).contentType(MediaType
				.APPLICATION_JSON)).andExpect(status().isBadRequest());
	}
	
	@Test
	public void transfer_InvalidTransferAmount() throws Exception {
		String fromAcc = "Id-127";
		String toAcc = "Id-128";
		String fromAccBalance = "100";
		String toAccBalance = "50";
		String amountToTransfer = "-10.50";
		
		this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
				.content("{\"accountId\":\"Id-127\",\"balance\":" + fromAccBalance + "}")).andExpect(status()
				.isCreated());
		this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
				.content("{\"accountId\":\"Id-128\",\"balance\":" + toAccBalance + "}")).andExpect(status().isCreated
				());
		
		this.mockMvc.perform(post("/v1/transactions/transfer?fromAccountId=" + fromAcc + "&toAccountId=" + toAcc +
				"&amountToTransfer=" + amountToTransfer).contentType(MediaType
				.APPLICATION_JSON)).andExpect(status().isBadRequest());
	}
}
