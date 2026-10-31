package com.knot.provisioning.bo;

import java.io.Serializable;
import java.math.BigInteger;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * @author Sai D
 *
 */
@Entity
public class ServiceDevicesBO implements Serializable{
	
	private static final long serialVersionUID = 1L;
	

	
	@Column(name = "RATE_PLAN_ID")
	private BigInteger ratePlanID;
	
	@Column(name = "SERVICE_PACKAGE_ID")
	private BigInteger servicePackageID;
	
	@Id
	@Column(name = "SERVICE_ID")
	private BigInteger serviceID;
	
	@Column(name = "LOB_ID")
	private BigInteger lobID;
	


	public BigInteger getRatePlanID() {
		return ratePlanID;
	}

	public void setRatePlanID(BigInteger ratePlanID) {
		this.ratePlanID = ratePlanID;
	}

	public BigInteger getServicePackageID() {
		return servicePackageID;
	}

	public void setServicePackageID(BigInteger servicePackageID) {
		this.servicePackageID = servicePackageID;
	}

	public BigInteger getServiceID() {
		return serviceID;
	}

	public void setServiceID(BigInteger serviceID) {
		this.serviceID = serviceID;
	}

	public BigInteger getLobID() {
		return lobID;
	}

	public void setLobID(BigInteger lobID) {
		this.lobID = lobID;
	}
}
