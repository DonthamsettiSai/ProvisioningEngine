package com.knot.provisioning.bo;

import java.math.BigInteger;
import java.util.List;

public class Contract{

	private Integer workOrderID;

	private Integer actionID ;		
	
	private BigInteger servicePackageID;
	
	private BigInteger serviceID;
	
	private Integer workOrderItemID;

	private Integer status;	
	
	private Integer customerContractID; 		

	private Integer tenantID;

	private Integer partnerID; 	

	private Integer nodeID;		
	
	private BigInteger ratePlanID;	

	private String ratePlanType;		
	
	private BigInteger customerID;
	
	private BigInteger customerAccountID;
	
	private List<DeviceBO> devices;
	
	public Contract() {
	}

	public Contract(Contract bo){
	    this.customerContractID = bo.getCustomerContractID() ; 		
		this.tenantID = bo.getTenantID();    
		this.partnerID = bo.getPartnerID(); 	
		this.nodeID = bo.getNodeID();		
		this.ratePlanID = bo.getRatePlanID();		
		this.ratePlanType = bo.getRatePlanType();		
		this.customerID = bo.getCustomerID();	
		this.customerAccountID = bo.getCustomerAccountID();
		this.actionID = bo.getActionID();
		this.servicePackageID = bo.getServicePackageID();
		this.serviceID = bo.getServiceID();
		this.workOrderID = bo.getWorkOrderID();
		this.status = bo.getStatus();
		this.workOrderItemID = bo.getWorkOrderItemID();
	}

	/**
	 * @return the workOrderID
	 */
	public Integer getWorkOrderID() {
		return workOrderID;
	}

	/**
	 * @param workOrderID the workOrderID to set
	 */
	public void setWorkOrderID(Integer workOrderID) {
		this.workOrderID = workOrderID;
	}

	/**
	 * @return the actionID
	 */
	public Integer getActionID() {
		return actionID;
	}

	/**
	 * @param actionID the actionID to set
	 */
	public void setActionID(Integer actionID) {
		this.actionID = actionID;
	}

	/**
	 * @return the servicePackageID
	 */
	public BigInteger getServicePackageID() {
		return servicePackageID;
	}

	/**
	 * @param servicePackageID the servicePackageID to set
	 */
	public void setServicePackageID(BigInteger servicePackageID) {
		this.servicePackageID = servicePackageID;
	}

	/**
	 * @return the serviceID
	 */
	public BigInteger getServiceID() {
		return serviceID;
	}

	/**
	 * @param serviceID the serviceID to set
	 */
	public void setServiceID(BigInteger serviceID) {
		this.serviceID = serviceID;
	}

	/**
	 * @return the workOrderItemID
	 */
	public Integer getWorkOrderItemID() {
		return workOrderItemID;
	}

	/**
	 * @param workOrderItemID the workOrderItemID to set
	 */
	public void setWorkOrderItemID(Integer workOrderItemID) {
		this.workOrderItemID = workOrderItemID;
	}

	/**
	 * @return the status
	 */
	public Integer getStatus() {
		return status;
	}

	/**
	 * @param status the status to set
	 */
	public void setStatus(Integer status) {
		this.status = status;
	}

	/**
	 * @return the customerContractID
	 */
	public Integer getCustomerContractID() {
		return customerContractID;
	}

	/**
	 * @param customerContractID the customerContractID to set
	 */
	public void setCustomerContractID(Integer customerContractID) {
		this.customerContractID = customerContractID;
	}

	/**
	 * @return the tenantID
	 */
	public Integer getTenantID() {
		return tenantID;
	}

	/**
	 * @param tenantID the tenantID to set
	 */
	public void setTenantID(Integer tenantID) {
		this.tenantID = tenantID;
	}

	/**
	 * @return the partnerID
	 */
	public Integer getPartnerID() {
		return partnerID;
	}

	/**
	 * @param partnerID the partnerID to set
	 */
	public void setPartnerID(Integer partnerID) {
		this.partnerID = partnerID;
	}

	/**
	 * @return the nodeID
	 */
	public Integer getNodeID() {
		return nodeID;
	}

	/**
	 * @param nodeID the nodeID to set
	 */
	public void setNodeID(Integer nodeID) {
		this.nodeID = nodeID;
	}

	/**
	 * @return the ratePlanID
	 */
	public BigInteger getRatePlanID() {
		return ratePlanID;
	}

	/**
	 * @param ratePlanID the ratePlanID to set
	 */
	public void setRatePlanID(BigInteger ratePlanID) {
		this.ratePlanID = ratePlanID;
	}

	/**
	 * @return the ratePlanType
	 */
	public String getRatePlanType() {
		return ratePlanType;
	}

	/**
	 * @param ratePlanType the ratePlanType to set
	 */
	public void setRatePlanType(String ratePlanType) {
		this.ratePlanType = ratePlanType;
	}

	/**
	 * @return the customerID
	 */
	public BigInteger getCustomerID() {
		return customerID;
	}

	/**
	 * @param customerID the customerID to set
	 */
	public void setCustomerID(BigInteger customerID) {
		this.customerID = customerID;
	}

	/**
	 * @return the customerAccountID
	 */
	public BigInteger getCustomerAccountID() {
		return customerAccountID;
	}

	/**
	 * @param customerAccountID the customerAccountID to set
	 */
	public void setCustomerAccountID(BigInteger customerAccountID) {
		this.customerAccountID = customerAccountID;
	}

	/**
	 * @return the devices
	 */
	public List<DeviceBO> getDevices() {
		return devices;
	}

	/**
	 * @param devices the devices to set
	 */
	public void setDevices(List<DeviceBO> devices) {
		this.devices = devices;
	}
}

