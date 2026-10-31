package com.knot.redis.dto;

import java.io.Serializable;

public class TenantsDTO implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private String tenantID;

	private String tenantName;

	private String tenantShortCode;

	private String tenantRegion;

	public String getTenantID() {
		return tenantID;
	}

	public void setTenantID(String tenantID) {
		this.tenantID = tenantID;
	}

	public String getTenantName() {
		return tenantName;
	}

	public void setTenantName(String tenantName) {
		this.tenantName = tenantName;
	}

	public String getTenantShortCode() {
		return tenantShortCode;
	}

	public void setTenantShortCode(String tenantShortCode) {
		this.tenantShortCode = tenantShortCode;
	}

	public String getTenantRegion() {
		return tenantRegion;
	}

	public void setTenantRegion(String tenantRegion) {
		this.tenantRegion = tenantRegion;
	}

	

}