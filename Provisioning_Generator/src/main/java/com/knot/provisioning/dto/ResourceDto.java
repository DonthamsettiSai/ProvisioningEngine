package com.knot.provisioning.dto;

public class ResourceDto {

	private String physicalResourceDtlsID;
	private String serialNumber;
	private String msisdn;

	public String getPhysicalResourceDtlsID() {
		return physicalResourceDtlsID;
	}

	public void setPhysicalResourceDtlsID(String physicalResourceDtlsID) {
		this.physicalResourceDtlsID = physicalResourceDtlsID;
	}

	public String getSerialNumber() {
		return serialNumber;
	}

	public void setSerialNumber(String serialNumber) {
		this.serialNumber = serialNumber;
	}

	public String getMsisdn() {
		return msisdn;
	}

	public void setMsisdn(String msisdn) {
		this.msisdn = msisdn;
	}

}
