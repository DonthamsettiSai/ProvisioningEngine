package com.knot.provisioning.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * @author Sai D
 *
 */

public class ResponseDto<T> {

	/**
	 * 
	 */
	private String statusCode;

	private String statusMessage;
	
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private String id;

	private List<T> result;


	public String getStatusCode() {
		return statusCode;
	}

	public void setStatusCode(String statusCode) {
		this.statusCode = statusCode;
	}

	public String getStatusMessage() {
		return statusMessage;
	}

	public void setStatusMessage(String statusMessage) {
		this.statusMessage = statusMessage;
	}

	/**
	 * @return the result
	 */
	public List<T> getResult() {
		return result;
	}

	/**
	 * @param result the result to set
	 */
	public void setResult(List<T> result) {
		this.result = result;
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

}
