package com.knot.provisioning.bo;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class ConfigBo implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "CONFIG_ID")
	private String configID;
	
	@Column(name = "TENANT_ID")
	private String tenantID;

	@Column(name = "EFFECTIVE_FROM")
	private String effectiveFrom;

	@Column(name = "CODE")
	private String code;

	@Column(name = "CONFIG_VALUE")
	private String value;

	

	/**
	 * @return the configID
	 */
	public String getConfigID() {
		return configID;
	}

	/**
	 * @param configID the configID to set
	 */
	public void setConfigID(String configID) {
		this.configID = configID;
	}

	/**
	 * @return the tenantID
	 */
	public String getTenantID() {
		return tenantID;
	}

	/**
	 * @param tenantID the tenantID to set
	 */
	public void setTenantID(String tenantID) {
		this.tenantID = tenantID;
	}

	/**
	 * @return the effectiveFrom
	 */
	public String getEffectiveFrom() {
		return effectiveFrom;
	}

	/**
	 * @param effectiveFrom the effectiveFrom to set
	 */
	public void setEffectiveFrom(String effectiveFrom) {
		this.effectiveFrom = effectiveFrom;
	}

	/**
	 * @return the code
	 */
	public String getCode() {
		return code;
	}

	/**
	 * @param code the code to set
	 */
	public void setCode(String code) {
		this.code = code;
	}

	/**
	 * @return the value
	 */
	public String getValue() {
		return value;
	}

	/**
	 * @param value the value to set
	 */
	public void setValue(String value) {
		this.value = value;
	}
}
