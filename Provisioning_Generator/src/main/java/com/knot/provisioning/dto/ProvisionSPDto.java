/**
 * 
 */
package com.knot.provisioning.dto;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * @author Sai D
 *
 */

public class ProvisionSPDto implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String servicePackageID;

	private String serviceID;

	private Map<String, List<Map<String, String>>> serviceParamList;

	public String getServicePackageID() {
		return servicePackageID;
	}

	public void setServicePackageID(String servicePackageID) {
		this.servicePackageID = servicePackageID;
	}

	public String getServiceID() {
		return serviceID;
	}

	public void setServiceID(String serviceID) {
		this.serviceID = serviceID;
	}

	public Map<String, List<Map<String, String>>> getServiceParamList() {
		return serviceParamList;
	}

	public void setServiceParamList(Map<String, List<Map<String, String>>> serviceParamList) {
		this.serviceParamList = serviceParamList;
	}
}
