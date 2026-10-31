package com.knot.provisioning.utill;

/**
 * @author Sai D
 *
 */
public class ProvisionException extends RuntimeException{

	private static final long serialVersionUID = 1L;
	
	private String message;

	public ProvisionException(String message) {
		this.message = message;
	}

	/**
	 * @return the message
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * @param message the message to set
	 */
	public void setMessage(String message) {
		this.message = message;
	}
	
}
