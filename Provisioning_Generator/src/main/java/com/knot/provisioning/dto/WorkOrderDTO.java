package com.knot.provisioning.dto;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

import com.knot.provisioning.bo.Contract;

/**
 * @author Sai D
 *
 */
public class WorkOrderDTO implements Serializable {

	private static final long serialVersionUID = 1L;	

	private Map<Integer,ArrayList<Contract>> contractMap;

	
	private BigInteger workOrderID;

	private Integer paymentID; 

	private Integer requestID;  

	
	private BigInteger tenantID;

	
	private BigInteger partnerID;

	private Integer actionID ;	

	
	private BigInteger customerID;	
	
	private Integer accountID;
	
	private String TntWorkOrderID;

	
	private BigInteger customerAccountID;

	private Integer status;	

	
	private BigInteger nodeID;

	private String createdBy;

	private String modifiedBy;

	private Date createdOn;

	private String createdIP;

	private Date modifiedOn;

	private String modifiedIPAddr;	

	private Integer activityID;		

	private String remarks;
	
	private Integer customerAccountType;

	
	private BigInteger resourceDtlsID;
	
	private String deviceType;

	private ArrayList<Contract> contractList;

	public WorkOrderDTO() {
	}

	public WorkOrderDTO(WorkOrderDTO wo) {
		this.contractMap = wo.getContractMap();
		this.workOrderID =  wo.getWorkOrderID();
		this.paymentID =  wo.getPaymentID();
		this.requestID =  wo.getRequestID();
		this.tenantID =  wo.getTenantID();
		this.partnerID =  wo.getPartnerID();
		this.actionID =  wo.getActionID();
		this.customerID =  wo.getCustomerID();
		/** this.accountID = wo.getAccountID(); */
		this.TntWorkOrderID = wo.getTntWorkOrderID();
		this.customerAccountID = wo.getCustomerAccountID();
		this.status =  wo.getStatus();
		this.nodeID =  wo.getNodeID();
		this.createdBy =  wo.getCreatedBy();
		this.modifiedBy =  wo.getModifiedBy();
		this.createdOn =  wo.getCreatedOn();
		this.createdIP = wo.getCreatedIP();
		this.modifiedOn =  wo.getModifiedOn();
		this.modifiedIPAddr =  wo.getModifiedIPAddr();
		this.activityID =  wo.getActivityID();
		this.remarks =  wo.getRemarks();
		this.customerAccountType = wo.getCustomerAccountType();
		this.resourceDtlsID = wo.getResourceDtlsID();
		this.deviceType = wo.getDeviceType();
	}

	/**
	 * @return the contractMap
	 */
	public Map<Integer, ArrayList<Contract>> getContractMap() {
		return contractMap;
	}

	/**
	 * @param contractMap the contractMap to set
	 */
	public void setContractMap(Map<Integer, ArrayList<Contract>> contractMap) {
		this.contractMap = contractMap;
	}

	/**
	 * @return the workOrderID
	 */
	public BigInteger getWorkOrderID() {
		return workOrderID;
	}

	public String getDeviceType() {
		return deviceType;
	}

	public void setDeviceType(String deviceType) {
		this.deviceType = deviceType;
	}

	/**
	 * @param workOrderID the workOrderID to set
	 */
	public void setWorkOrderID(BigInteger workOrderID) {
		this.workOrderID = workOrderID;
	}

	/**
	 * @return the paymentID
	 */
	public Integer getPaymentID() {
		return paymentID;
	}

	/**
	 * @param paymentID the paymentID to set
	 */
	public void setPaymentID(Integer paymentID) {
		this.paymentID = paymentID;
	}

	/**
	 * @return the requestID
	 */
	public Integer getRequestID() {
		return requestID;
	}

	/**
	 * @param requestID the requestID to set
	 */
	public void setRequestID(Integer requestID) {
		this.requestID = requestID;
	}

	/**
	 * @return the tenantID
	 */
	public BigInteger getTenantID() {
		return tenantID;
	}

	/**
	 * @param tenantID the tenantID to set
	 */
	public void setTenantID(BigInteger tenantID) {
		this.tenantID = tenantID;
	}

	/**
	 * @return the partnerID
	 */
	public BigInteger getPartnerID() {
		return partnerID;
	}

	/**
	 * @param partnerID the partnerID to set
	 */
	public void setPartnerID(BigInteger partnerID) {
		this.partnerID = partnerID;
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
	 * @return the nodeID
	 */
	public BigInteger getNodeID() {
		return nodeID;
	}

	/**
	 * @param nodeID the nodeID to set
	 */
	public void setNodeID(BigInteger nodeID) {
		this.nodeID = nodeID;
	}

	/**
	 * @return the createdBy
	 */
	public String getCreatedBy() {
		return createdBy;
	}

	/**
	 * @param createdBy the createdBy to set
	 */
	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	/**
	 * @return the modifiedBy
	 */
	public String getModifiedBy() {
		return modifiedBy;
	}

	/**
	 * @param modifiedBy the modifiedBy to set
	 */
	public void setModifiedBy(String modifiedBy) {
		this.modifiedBy = modifiedBy;
	}

	/**
	 * @return the createdOn
	 */
	public Date getCreatedOn() {
		return createdOn;
	}

	/**
	 * @param createdOn the createdOn to set
	 */
	public void setCreatedOn(Date createdOn) {
		this.createdOn = createdOn;
	}

	/**
	 * @return the createdIP
	 */
	public String getCreatedIP() {
		return createdIP;
	}

	/**
	 * @param createdIP the createdIP to set
	 */
	public void setCreatedIP(String createdIP) {
		this.createdIP = createdIP;
	}

	/**
	 * @return the modifiedOn
	 */
	public Date getModifiedOn() {
		return modifiedOn;
	}

	/**
	 * @param modifiedOn the modifiedOn to set
	 */
	public void setModifiedOn(Date modifiedOn) {
		this.modifiedOn = modifiedOn;
	}

	/**
	 * @return the modifiedIPAddr
	 */
	public String getModifiedIPAddr() {
		return modifiedIPAddr;
	}

	/**
	 * @param modifiedIPAddr the modifiedIPAddr to set
	 */
	public void setModifiedIPAddr(String modifiedIPAddr) {
		this.modifiedIPAddr = modifiedIPAddr;
	}

	/**
	 * @return the activityID
	 */
	public Integer getActivityID() {
		return activityID;
	}

	/**
	 * @param activityID the activityID to set
	 */
	public void setActivityID(Integer activityID) {
		this.activityID = activityID;
	}

	/**
	 * @return the remarks
	 */
	public String getRemarks() {
		return remarks;
	}

	/**
	 * @param remarks the remarks to set
	 */
	public void setRemarks(String remarks) {
		this.remarks = remarks;
	}

	public Integer getCustomerAccountType() {
		return customerAccountType;
	}

	public void setCustomerAccountType(Integer customerAccountType) {
		this.customerAccountType = customerAccountType;
	}

	/**
	 * @return the contractList
	 */
	public ArrayList<Contract> getContractList() {
		return contractList;
	}

	/**
	 * @param contractList the contractList to set
	 */
	public void setContractList(ArrayList<Contract> contractList) {
		this.contractList = contractList;
	}

	public Integer getAccountID() {
		return accountID;
	}

	public void setAccountID(Integer accountID) {
		this.accountID = accountID;
	}

	public BigInteger getResourceDtlsID() {
		return resourceDtlsID;
	}

	public void setResourceDtlsID(BigInteger resourceDtlsID) {
		this.resourceDtlsID = resourceDtlsID !=null ? resourceDtlsID : null ;
	}
	
	public String getTntWorkOrderID() {
		return TntWorkOrderID;
	}

	public void setTntWorkOrderID(String tntWorkOrderID) {
		TntWorkOrderID = tntWorkOrderID;
	}
}

