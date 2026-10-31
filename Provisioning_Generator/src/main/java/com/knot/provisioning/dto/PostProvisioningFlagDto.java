package com.knot.provisioning.dto;

import java.io.Serializable;

public class PostProvisioningFlagDto implements Serializable{
	/**
	 * sai
	 */
	private static final long serialVersionUID = 1L;
	private Integer statusCode;
	private String isMNP;
	public Integer getStatusCode() {
		return statusCode;
	}
	public void setStatusCode(Integer statusCode) {
		this.statusCode = statusCode;
	}
	public String getIsMNP() {
		return isMNP;
	}
	public void setIsMNP(String isMNP) {
		this.isMNP = isMNP;
	}

	
}
