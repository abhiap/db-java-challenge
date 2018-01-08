package com.db.awmd.challenge.exception;

/**
 * @author abhijit.patil on 05-01-2018
 *         <p>
 *         ${tags}
 */
public class LowBalanceException extends RuntimeException {
	
	public LowBalanceException(String message){
		super(message);
	}
}
