package com.rdkm.tdkservice.exception;

public class TDKServiceException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * This is a parameterized constructor that takes the exception message as an
	 * argument.
	 * 
	 * @param message
	 */
	public TDKServiceException(String message) {
		super(message);

	}

}
