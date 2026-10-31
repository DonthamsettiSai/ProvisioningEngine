package com.knot.provisioning.dto;

import java.io.Serializable;
import java.util.List;

/**
 * @author Sai D
 *
 */

public class ProvisionRatePlanDto implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String ratePlanID;

	private String ratePlanName;

	private List<ProvisionSPDto> servicePackageList;

	public String getRatePlanID() {
		return ratePlanID;
	}

	public void setRatePlanID(String ratePlanID) {
		this.ratePlanID = ratePlanID;
	}

	public String getRatePlanName() {
		return ratePlanName;
	}

	public void setRatePlanName(String ratePlanName) {
		this.ratePlanName = ratePlanName;
	}

	public List<ProvisionSPDto> getSerivcePackageList() {
		return servicePackageList;
	}

	public void setSerivcePackageList(List<ProvisionSPDto> serivcePackageList) {
		this.servicePackageList = serivcePackageList;
	}
}
