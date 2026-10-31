package com.knot.provisioning.dto;

import java.io.Serializable;

public class OcsRPSPMapData implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String oobSPID;
	
	private String ocsSPID;
	
	private String oobSPName;
	
	private String oobRPID;
	
	private String oobServiceID;
	
	

	public String getOobRPID() {
		return oobRPID;
	}

	public void setOobRPID(String oobRPID) {
		this.oobRPID = oobRPID;
	}

	public String getOobSPID() {
		return oobSPID;
	}

	public void setOobSPID(String oobSPID) {
		this.oobSPID = oobSPID;
	}

	public String getOcsSPID() {
		return ocsSPID;
	}

	public void setOcsSPID(String ocsSPID) {
		this.ocsSPID = ocsSPID;
	}

	public String getOobSPName() {
		return oobSPName;
	}

	public void setOobSPName(String oobSPName) {
		this.oobSPName = oobSPName;
	}

	public String getOobServiceID() {
		return oobServiceID;
	}

	public void setOobServiceID(String oobServiceID) {
		this.oobServiceID = oobServiceID;
	}
	
	

}
