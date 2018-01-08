package com.db.awmd.challenge.web;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.exception.InvalidAmountException;
import com.db.awmd.challenge.exception.LowBalanceException;
import com.db.awmd.challenge.exception.ResourceNotFoundException;
import com.db.awmd.challenge.service.TransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.math.BigDecimal;

/**
 * @author abhijit.patil on 05-01-2018
 *         <p>
 *         ${tags}
 */
@RestController
@RequestMapping("/v1/transactions")
@Slf4j
public class TransactionController {
	
	@Autowired
	private TransactionService transactionService;
	
	@PostMapping(value = "/transfer", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Object> transfer(@RequestParam (required = true) String fromAccountId, @RequestParam
			(required = true) String toAccountId, @RequestParam (required = true) BigDecimal amountToTransfer) {
		log.info("Transferring amount {} from account {} to account {}", fromAccountId, toAccountId, amountToTransfer);
		try {
			String result = transactionService.transfer(fromAccountId, toAccountId, amountToTransfer);
		} catch (ResourceNotFoundException | InvalidAmountException | LowBalanceException e){
			//TODO Can be replaced with exceptionhandler (controlleradvice)
			return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
		}
		log.info("Transfer successful");
		return new ResponseEntity<>(HttpStatus.OK);
	}
}
