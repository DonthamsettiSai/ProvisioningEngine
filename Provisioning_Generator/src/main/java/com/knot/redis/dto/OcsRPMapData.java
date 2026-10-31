package com.knot.redis.dto;

import java.io.Serializable;

public class OcsRPMapData implements Serializable {

	private static final long serialVersionUID = 1L;

	private String ocsRatePlanID;
	
	private String oobRatePlanID;
	
	private String ocsName;
	
	private String ocsID;

	public String getOcsID() {
		return ocsID;
	}

	public void setOcsID(String ocsID) {
		this.ocsID = ocsID;
	}

	public String getOcsRatePlanID() {
		return ocsRatePlanID;
	}

	public void setOcsRatePlanID(String ocsRatePlanID) {
		this.ocsRatePlanID = ocsRatePlanID;
	}

	public String getOobRatePlanID() {
		return oobRatePlanID;
	}

	public void setOobRatePlanID(String oobRatePlanID) {
		this.oobRatePlanID = oobRatePlanID;
	}

	public String getOcsName() {
		return ocsName;
	}

	public void setOcsName(String ocsName) {
		this.ocsName = ocsName;
	}
	
	
}
