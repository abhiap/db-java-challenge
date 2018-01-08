package com.db.awmd.challenge.service;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.exception.InvalidAmountException;
import com.db.awmd.challenge.exception.LowBalanceException;
import com.db.awmd.challenge.exception.ResourceNotFoundException;
import com.db.awmd.challenge.repository.AccountsRepository;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class AccountsService {

  @Getter
  private final AccountsRepository accountsRepository;

  @Autowired
  public AccountsService(AccountsRepository accountsRepository) {
    this.accountsRepository = accountsRepository;
  }

  public void createAccount(Account account) {
    this.accountsRepository.createAccount(account);
  }

  public Account getAccount(String accountId) {
  	Account account = this.accountsRepository.getAccount(accountId);
	  if (account == null) {
		  throw new ResourceNotFoundException("Account with id " + accountId + " not found");
	  }
	  return account;
  }
	
}
