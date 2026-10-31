package com.knot.provisioning.model;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
/**

  `TIME_GAP_UNIT` tinyint(4) DEFAULT NULL,
  `CURRENT_RETRY_COUNT` tinyint(4) DEFAULT NULL,
  `SLA_TIME` int(11) DEFAULT '0',
  `SLA_UNIT` tinyint(4) DEFAULT NULL,
  `SLA_VIOLATION_STATUS` char(1) DEFAULT NULL,
 */

@Entity
@Table(name = "provisioning_request_details")
@IdClass(ProvisioningRequestDetailPK.class)

public class ProvisioningRequestDetails implements Serializable {

	private static final long serialVersionUID = 1L;	

	@Id
 
	
	@Column(name = "REQUEST_DETAIL_ID") 
	private BigInteger requestDetailID;

	
	@Id
	@Column(name = "VERSION_NO")
	private float version;	



	
	@Column(name = "REQUEST_ID")
	private BigInteger requestID;  

	@Column(name = "LOB_ID")
	private BigInteger lobID; 

	@Column(name = "SERVICE_ID")
	private BigInteger serviceID;


	@Column(name = "SERVER_NAME")
	private String serverName;	

	@Column(name = "TOKEN_ID") 
	private String tokenID; 

	
	@Column(name = "SERVICE_PACKAGE_ID")
	private BigInteger servicePackageID;

	@Column(name = "RATE_PLAN_ID")
	private BigInteger ratePlanID;	


	@Column(name = "LOB_PRIORITY")
	private Integer lobPriority;    

	@Column(name = "RATE_PLAN_PRIORITY")
	private Integer ratePlanPriority;	

	@Column(name = "SEQUENCE_ID")
	private int sequenceID;

	@Column(name = "SUB_SEQUENCE_ID")
	private int subSequenceID;

	@Column(name = "DEPENDENT_SEQUENCE_ID")
	private int dependentSequenceID;

	@Column(name = "DEPENDENT_SUB_SEQUENCE_ID")
	private int dependentSubSequenceID;

	@Column(name = "SERVICE_PRIORITY")
	private int servicePriority;	

	@Column(name = "MAX_RETRIES")
	private int maxRetries;	

	@Column(name = "INTERFACE_TYPE")
	private String interfaceType;		

	@Column(name = "SERVER_URL")
	private String serverURL;

	@Column(name = "REQUEST_OBJECT")
	private String requestObject;	
	
	@Column(name = "REQUEST_METHOD")
	private String requestMethod;	
	
	@Column(name = "REQ_SER_RESP_ID")
	private String reqSerRespID;	

	@Column(name = "STATUS")
	private int status;	

	@Column(name = "REQUEST_CREATION_DATE")
	private Date requestCreationDate;

	@Column(name = "VERSION_DATE")
	private Date versionDate;

	@Column(name = "TIME_GAP_BETWEEN_RETRY")
	private Integer timeGapBetweenRetry;	

	@Column(name = "RESPONSE_UPDATE_CATEGORY")
	private Integer responseUpdateCategory;	
	
	@Column(name = "TIME_GAP_UNIT")	
	private Character timeGapUnit;

	
	@Column(name = "PROV_OBJECT_ID")
	private BigInteger provObjId;
	
	
	@Column(name = "IS_ASYNC")
	private String isAsync;
	
	
	@Column(name = "PROCESS_TYPE")
	private String processType;
	
	@Column(name = "DEVICE_TYPE")
	private String deviceType;
	
	@Column(name = "PARAM_REPL_LEVEL")
	private String paramResplaceLevel;
	
	@Column(name = "CALL_BACK_URL")
	private String callBackUrl;
	
	@Column(name = "CALL_BACK_URL_OBJECT")
	private String callBackUrlObj;
	
	@Column(name = "DEP_SUB_SEQ_PROCESSING_TYPE")
	private String depSubSeqProcessingType;
	
	@Column(name = "ADDL_COL1")
	private String addColumnOne;
	
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

	public BigInteger getProvObjId() {
		return provObjId;
	}

	public void setProvObjId(BigInteger provObjId) {
		this.provObjId = provObjId;
	}

	public String getRequestMethod() {
		return requestMethod;
	}

	public void setRequestMethod(String requestMethod) {
		this.requestMethod = requestMethod;
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

	public Integer getTimeGapBetweenRetry() {
		return timeGapBetweenRetry;
	}

	public void setTimeGapBetweenRetry(Integer timeGapBetweenRetry) {
		this.timeGapBetweenRetry = timeGapBetweenRetry;
	}

	

	public String getServerName() {
		return serverName;
	}

	public void setServerName(String serverName) {
		this.serverName = serverName;
	}

	public BigInteger getRequestDetailID() {
		return requestDetailID;
	}

	public void setRequestDetailID(BigInteger requestDetailID) {
		this.requestDetailID = requestDetailID;
	}

	public BigInteger getRequestID() {
		return requestID;
	}

	public void setRequestID(BigInteger requestID) {
		this.requestID = requestID;
	}

	public BigInteger getServiceID() {
		return serviceID;
	}

	public void setServiceID(BigInteger serviceID) {
		this.serviceID = serviceID;
	}

	public String getTokenID() {
		return tokenID;
	}

	public void setTokenID(String tokenID) {
		this.tokenID = tokenID;
	}

	public BigInteger getServicePackageID() {
		return servicePackageID;
	}

	public void setServicePackageID(BigInteger servicePackageID) {
		this.servicePackageID = servicePackageID;
	}

	public BigInteger getRatePlanID() {
		return ratePlanID;
	}

	public void setRatePlanID(BigInteger ratePlanID) {
		this.ratePlanID = ratePlanID;
	}

	public int getSequenceID() {
		return sequenceID;
	}

	public void setSequenceID(int sequenceID) {
		this.sequenceID = sequenceID;
	}

	public int getServicePriority() {
		return servicePriority;
	}

	public void setServicePriority(int servicePriority) {
		this.servicePriority = servicePriority;
	}

	public String getInterfaceType() {
		return interfaceType;
	}

	public void setInterfaceType(String interfaceType) {
		this.interfaceType = interfaceType;
	}

	public String getServerURL() {
		return serverURL;
	}

	public void setServerURL(String serverURL) {
		this.serverURL = serverURL;
	}

	public String getRequestObject() {
		return requestObject;
	}

	public void setRequestObject(String requestObject) {
		this.requestObject = requestObject;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public Date getRequestCreationDate() {
		return requestCreationDate;
	}

	public void setRequestCreationDate(Date requestCreationDate) {
		this.requestCreationDate = requestCreationDate;
	}	

	public float getVersion() {
		return version;
	}

	public void setVersion(float version) {
		this.version = version;
	}

	public Date getVersionDate() {
		return versionDate;
	}

	public void setVersionDate(Date versionDate) {
		this.versionDate = versionDate;
	}

	public BigInteger getLobID() {
		return lobID;
	}

	public void setLobID(BigInteger lobID) {
		this.lobID = lobID;
	}

	public Integer getLobPriority() {
		return lobPriority;
	}

	public void setLobPriority(Integer lobPriority) {
		this.lobPriority = lobPriority;
	}

	public Integer getRatePlanPriority() {
		return ratePlanPriority;
	}

	public void setRatePlanPriority(Integer ratePlanPriority) {
		this.ratePlanPriority = ratePlanPriority;
	}

	public int getSubSequenceID() {
		return subSequenceID;
	}

	public void setSubSequenceID(int subSequenceID) {
		this.subSequenceID = subSequenceID;
	}

	public int getDependentSequenceID() {
		return dependentSequenceID;
	}

	public void setDependentSequenceID(int dependentSequenceID) {
		this.dependentSequenceID = dependentSequenceID;
	}

	public int getDependentSubSequenceID() {
		return dependentSubSequenceID;
	}

	public void setDependentSubSequenceID(int dependentSubSequenceID) {
		this.dependentSubSequenceID = dependentSubSequenceID;
	}

	public int getMaxRetries() {
		return maxRetries;
	}

	public void setMaxRetries(int maxRetries) {
		this.maxRetries = maxRetries;
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

	public String getDeviceType() {
		return deviceType;
	}

	public void setDeviceType(String deviceType) {
		this.deviceType = deviceType;
	}

	public String getParamResplaceLevel() {
		return paramResplaceLevel;
	}

	public void setParamResplaceLevel(String paramResplaceLevel) {
		this.paramResplaceLevel = paramResplaceLevel;
	}

	public String getCallBackUrl() {
		return callBackUrl;
	}

	public void setCallBackUrl(String callBackUrl) {
		this.callBackUrl = callBackUrl;
	}
	
	public String getReqSerRespID() {
		return reqSerRespID;
	}

	public void setReqSerRespID(String reqSerRespID) {
		this.reqSerRespID = reqSerRespID;
	}

	public String getAddColumnOne() {
		return addColumnOne;
	}

	public void setAddColumnOne(String addColumnOne) {
		this.addColumnOne = addColumnOne;
	}
	
	
	
}

