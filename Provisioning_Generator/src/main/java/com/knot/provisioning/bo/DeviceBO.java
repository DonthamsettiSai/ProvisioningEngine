
package com.knot.provisioning.bo;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;

/**
 * 
 * @author Sai D
 *
 */

@Entity
public class DeviceBO implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "WO_ITEM_DETAIL_ID")
	private String woItemDtlID;

	@Column(name = "LOB_ID")
	private String lobID;

	@Column(name = "DEVICE_SERIAL_NO")
	private String serialNo;

	@Column(name = "DEVICE_MAC_ADDR")
	private String macAddress;
	
	@Column(name = "DEVICE_TYPE")
	private String deviceType;
	
	@Column(name = "DEVICE_CODE")
	private String deviceCode;
	
	@Column(name = "oldSimNo")
	private String oldSimNo;
	
	@Column(name = "oldMsisdnNo")
	private String oldMsisdnNo;
	
	@Transient
	private String resourceRecID;
	
	@Transient
	private String simResourceRecID;

	

	
	public String getOldSimNo() {
		return oldSimNo;
	}

	public void setOldSimNo(String oldSimNo) {
		this.oldSimNo = oldSimNo;
	}

	public String getOldMsisdnNo() {
		return oldMsisdnNo;
	}

	public void setOldMsisdnNo(String oldMsisdnNo) {
		this.oldMsisdnNo = oldMsisdnNo;
	}

	public String getDeviceCode() {
		return deviceCode;
	}

	public void setDeviceCode(String deviceCode) {
		this.deviceCode = deviceCode;
	}

	public String getDeviceType() {
		return deviceType;
	}

	public void setDeviceType(String deviceType) {
		this.deviceType = deviceType;
	}

	public String getWoItemDtlID() {
		return woItemDtlID;
	}

	public void setWoItemDtlID(String woItemDtlID) {
		this.woItemDtlID = woItemDtlID;
	}

	public String getSerialNo() {
		return serialNo;
	}

	public void setSerialNo(String serialNo) {
		this.serialNo = serialNo;
	}

	public String getMacAddress() {
		return macAddress;
	}

	public void setMacAddress(String macAddress) {
		this.macAddress = macAddress;
	}

	public String getLobID() {
		return lobID;
	}

	public void setLobID(String lobID) {
		this.lobID = lobID;
	}

	public String getResourceRecID() {
		return resourceRecID;
	}

	public void setResourceRecID(String resourceRecID) {
		this.resourceRecID = resourceRecID;
	}

	public String getSimResourceRecID() {
		return simResourceRecID;
	}

	public void setSimResourceRecID(String simResourceRecID) {
		this.simResourceRecID = simResourceRecID;
	}
	
	
}
