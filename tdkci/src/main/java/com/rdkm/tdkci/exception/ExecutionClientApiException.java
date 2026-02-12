package com.rdkm.tdkci.exception;

public class ExecutionClientApiException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * This is a parameterized constructor that takes the exception message as an
	 * argument.
	 * 
	 * @param message
	 */

	public ExecutionClientApiException(String message) {
		super(message);
	}

	public ExecutionClientApiException(String message, Throwable cause) {
		super(message, cause);
	}

}
