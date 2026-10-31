package com.knot.provisioning.bo;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 
 * @author Sai D
 */

@Entity
@Table(name = "provisioning_request_objects")
public class ProvisioningRequestObjectsBO implements Serializable {

	private static final long serialVersionUID = 1L;		 
	@Id	

	
	@Column(name = "PROVISIONING_REQUEST_ID") 
	private BigInteger provisioningRequestID;
	

	
	@Column(name = "ACTIVITY_ID")
	private Integer activityID;
	

	
	@Column(name = "SERVICE_ID")
	private BigInteger serviceID;
	


	@Column(name = "PAYMENT_TYPE")
	private Integer paymentType;

	
	@Column(name = "TENANT_ID")
	private BigInteger tenantID;

	@Column(name = "PARTNER_ID")
	private BigInteger partnerID;

	@Column(name = "LOB_ID")
	private BigInteger lobID;


	@Column(name = "MAX_RETRIES")
	private Integer maxRetries;	
	
	@Column(name = "TIME_GAP_BETWEEN_RETRY")
	private Integer timeGapBetweenRetry;
	
	@Column(name = "SEQUENCE_ID")
	private Integer sequenceID;

	@Column(name = "SUB_SEQUENCE_ID")
	private Integer subSequenceID;	

	@Column(name = "DEPENDENT_SEQUENCE_ID")
	private Integer dependentSequenceID;	

	@Column(name = "DEPENDENT_SUB_SEQUENCE_ID")
	private Integer dependentSubSequenceID;

	@Column(name = "LOB_PROVISIONING_PRIORITY")
	private Integer lobProvisioningPriority;

	@Column(name = "SERVICE_PROVISIONING_PRIORITY")
	private Integer serviceProvisioningPriority; 

	@Column(name = "ACTION_ID") 
	private String action;

	@Column(name = "INTERFACE_TYPE") 
	private String interfaceType;

	@Column(name = "OBJECT_TYPE") 
	private String objectType;

	@Column(name = "TOKEN_ID")
	private String tokenID;

	@Column(name = "SERVER_URL") 
	private String serverURL;

	@Column(name = "EXECUTION_ORDER")
	private String executionOrder;

	@Column(name = "REQUEST_OBJECT")
	@JsonProperty("APIobject")
	private String apiObject;
	 
	@Column(name = "PURPOSE")
	private String purpose;
	
	@Column(name = "WORK_ORDER_TYPE")
	private String workOrderType;

	@Column(name = "EFFECTIVE_FROM")
	private Date effectiveFrom;

	@Column(name = "EFFECTIVE_TO")
	private Date effectiveTo;

	@Column(name = "STATUS")
	private Integer status;

	@Column(name = "CREATED_BY")
	private String createdBy;

	@Column(name = "CREATED_ON")
	@JsonProperty("CreatedOn")
	private Date createdOn;

	@Column(name = "CREATED_IPADDR")
	private String createdIPADDR;

	@Column(name = "MODIFIED_BY")
	private String modifiedBY;

	@Column(name = "MODIFIED_ON")
	private Date modifiedOn;

	@Column(name = "MODIFIED_IPADDR")
	private String modifiedIPADDR;

	@Column(name = "RESPONSE_UPDATE_CATEGORY")
	private Integer responseUpdateCategory;	
	
	
	@Column(name = "SERVER_NAME")
	private String serverName;
	
	
	@Column(name = "REQUEST_METHOD")
	private String requestMethod;
	
	
	@Column(name = "TIME_GAP_UNIT")
	private Character timeGapUnit;	
	
	@Column(name = "IS_ASYNC")
	private String isAsync;
	
	
	@Column(name = "PROCESS_TYPE")
	private String processType;
	
	@Column(name = "PARAM_REPL_LEVEL")
	private String paramReplaceLevel;
	
	@Column(name = "DEVICE_TYPE")
	private String deviceType;
	
	@Column(name = "CALL_BACK_URL")
	private String callBackUrl;
	
	@Column(name = "CALL_BACK_URL_OBJECT")
	private String callBackUrlObj;
	
	@Column(name = "DEP_SUB_SEQ_PROCESSING_TYPE")
	private String depSubSeqProcessingType;
	
	public String getDepSubSeqProcessingType() {
		return depSubSeqProcessingType;
	}

	public void setDepSubSeqProcessingType(String depSubSeqProcessingType) {
		this.depSubSeqProcessingType = depSubSeqProcessingType;
	}

	public String getCallBackUrlObj() {
		return callBackUrlObj;
	}

	public void setCallBackUrlObj(String callBackUrlObj) {
		this.callBackUrlObj = callBackUrlObj;
	}

	public String getParamReplaceLevel() {
		return paramReplaceLevel;
	}

	public void setParamReplaceLevel(String paramReplaceLevel) {
		this.paramReplaceLevel = paramReplaceLevel;
	}

	public String getCallBackUrl() {
		return callBackUrl;
	}

	public void setCallBackUrl(String callBackUrl) {
		this.callBackUrl = callBackUrl;
	}

	public String getDeviceType() {
		return deviceType;
	}

	public void setDeviceType(String deviceType) {
		this.deviceType = deviceType;
	}
	

	public String getRequestMethod() {
		return requestMethod;
	}

	public void setRequestMethod(String requestMethod) {
		this.requestMethod = requestMethod;
	}

	public String getServerName() {
		return serverName;
	}

	public void setServerName(String serverName) {
		this.serverName = serverName;
	}

	public Character getTimeGapUnit() {
		return timeGapUnit;
	}

	public void setTimeGapUnit(Character timeGapUnit) {
		this.timeGapUnit = timeGapUnit;
	}

	public Integer getResponseUpdateCategory() {
		return responseUpdateCategory;
	}

	public void setResponseUpdateCategory(Integer responseUpdateCategory) {
		this.responseUpdateCategory = responseUpdateCategory;
	}

	public Integer getMaxRetries() {
		return maxRetries;
	}

	public void setMaxRetries(Integer maxRetries) {
		this.maxRetries = maxRetries;
	}

	public BigInteger getServiceID() {
		return serviceID;
	}

	public void setServiceID(BigInteger serviceID) {
		this.serviceID = serviceID;
	}

	public Integer getPaymentType() {
		return paymentType;
	}

	public void setPaymentType(Integer paymentType) {
		this.paymentType = paymentType;
	}

	public String getInterfaceType() {
		return interfaceType;
	}

	public void setInterfaceType(String interfaceType) {
		this.interfaceType = interfaceType;
	}

	public String getObjectType() {
		return objectType;
	}

	public void setObjectType(String objectType) {
		this.objectType = objectType;
	}

	public String getTokenID() {
		return tokenID;
	}

	public void setTokenID(String tokenID) {
		this.tokenID = tokenID;
	}

	public String getServerURL() {
		return serverURL;
	}

	public void setServerURL(String serverURL) {
		this.serverURL = serverURL;
	}

	public String getExecutionOrder() {
		return executionOrder;
	}

	public void setExecutionOrder(String executionOrder) {
		this.executionOrder = executionOrder;
	}

	public Integer getSequenceID() {
		return sequenceID;
	}

	public void setSequenceID(Integer sequenceID) {
		this.sequenceID = sequenceID;
	}

	public Integer getSubSequenceID() {
		return subSequenceID;
	}

	public void setSubSequenceID(Integer subSequenceID) {
		this.subSequenceID = subSequenceID;
	}

	

	public String getApiObject() {
		return apiObject;
	}

	public void setApiObject(String apiObject) {
		this.apiObject = apiObject;
	}

	public Date getEffectiveFrom() {
		return effectiveFrom;
	}

	public void setEffectiveFrom(Date effectiveFrom) {
		this.effectiveFrom = effectiveFrom;
	}

	public Date getEffectiveTo() {
		return effectiveTo;
	}

	public void setEffectiveTo(Date effectiveTo) {
		this.effectiveTo = effectiveTo;
	}

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public Date getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(Date createdOn) {
		this.createdOn = createdOn;
	}

	public String getCreatedIPADDR() {
		return createdIPADDR;
	}

	public void setCreatedIPADDR(String createdIPADDR) {
		this.createdIPADDR = createdIPADDR;
	}

	public String getModifiedBY() {
		return modifiedBY;
	}

	public void setModifiedBY(String modifiedBY) {
		this.modifiedBY = modifiedBY;
	}

	public Date getModifiedOn() {
		return modifiedOn;
	}

	public void setModifiedOn(Date modifiedOn) {
		this.modifiedOn = modifiedOn;
	}

	public String getModifiedIPADDR() {
		return modifiedIPADDR;
	}

	public void setModifiedIPADDR(String modifiedIPADDR) {
		this.modifiedIPADDR = modifiedIPADDR;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public BigInteger getProvisioningRequestID() {
		return provisioningRequestID;
	}

	public void setProvisioningRequestID(BigInteger provisioningRequestID) {
		this.provisioningRequestID = provisioningRequestID;
	}

	public BigInteger getTenantID() {
		return tenantID;
	}

	public void setTenantID(BigInteger tenantID) {
		this.tenantID = tenantID;
	}

	public BigInteger getPartnerID() {
		return partnerID;
	}

	public void setPartnerID(BigInteger partnerID) {
		this.partnerID = partnerID;
	}

	public BigInteger getLobID() {
		return lobID;
	}

	public void setLobID(BigInteger lobID) {
		this.lobID = lobID;
	}

	public Integer getDependentSequenceID() {
		return dependentSequenceID;
	}

	public void setDependentSequenceID(Integer dependentSequenceID) {
		this.dependentSequenceID = dependentSequenceID;
	}

	public Integer getDependentSubSequenceID() {
		return dependentSubSequenceID;
	}

	public void setDependentSubSequenceID(Integer dependentSubSequenceID) {
		this.dependentSubSequenceID = dependentSubSequenceID;
	}

	public Integer getLobProvisioningPriority() {
		return lobProvisioningPriority;
	}

	public void setLobProvisioningPriority(Integer lobProvisioningPriority) {
		this.lobProvisioningPriority = lobProvisioningPriority;
	}

	public Integer getServiceProvisioningPriority() {
		return serviceProvisioningPriority;
	}

	public void setServiceProvisioningPriority(Integer serviceProvisioningPriority) {
		this.serviceProvisioningPriority = serviceProvisioningPriority;
	}

	public Integer getTimeGapBetweenRetry() {
		return timeGapBetweenRetry;
	}

	public void setTimeGapBetweenRetry(Integer timeGapBetweenRetry) {
		this.timeGapBetweenRetry = timeGapBetweenRetry;
	}

	public String getPurpose() {
		return purpose;
	}

	public void setPurpose(String purpose) {
		this.purpose = purpose;
	}

	public String getWorkOrderType() {
		return workOrderType;
	}

	public void setWorkOrderType(String workOrderType) {
		this.workOrderType = workOrderType;
	}

	public Integer getActivityID() {
		return activityID;
	}

	public void setActivityID(Integer activityID) {
		this.activityID = activityID;
	}	

	public String getIsAsync() {
		return isAsync;
	}

	public void setIsAsync(String isAsync) {
		this.isAsync = isAsync;
	}

	public String getProcessType() {
		return processType;
	}

	public void setProcessType(String processType) {
		this.processType = processType;
	}
}

