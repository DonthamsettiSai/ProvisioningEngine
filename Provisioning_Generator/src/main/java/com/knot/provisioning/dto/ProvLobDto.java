package com.knot.provisioning.dto;

import java.io.Serializable;

public class ProvLobDto implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String lobID;

	private String lobName;
	
	private String lobCode;
	
	private String tenantID;
	
	private String partnerID;

	public String getLobID() {
		return lobID;
	}

	public void setLobID(String lobID) {
		this.lobID = lobID;
	}

	public String getLobName() {
		return lobName;
	}

	public void setLobName(String lobName) {
		this.lobName = lobName;
	}

	public String getLobCode() {
		return lobCode;
	}

	public void setLobCode(String lobCode) {
		this.lobCode = lobCode;
	}

	public String getTenantID() {
		return tenantID;
	}

	public void setTenantID(String tenantID) {
		this.tenantID = tenantID;
	}

	public String getPartnerID() {
		return partnerID;
	}

	public void setPartnerID(String partnerID) {
		this.partnerID = partnerID;
	}
	
	

}
