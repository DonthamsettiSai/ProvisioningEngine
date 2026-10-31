package com.knot.provisioning.model;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "provisioning_request_generation_errors")
public class ProvisioningRequestGenerationError implements Serializable {

	/** 
	 */	  

	private static final long serialVersionUID = 1L;		

	@Id

	
	@Column(name = "ERROR_RECORD_ID") 
	private BigInteger errorRecordID;    
	
	@Column(name = "WORK_ORDER_ID")  
	private BigInteger workOrderID; 
   

	@Column(name = "ERROR_FOUND_DATE")  
	private Date errorFoundDate; 	

	@Column(name = "ERROR_CODE") 
	private String errorCode;	

	@Column(name = "ERROR_DESCRIPTION") 
	private String errorDescription;		

	@Column(name = "STATUS") 
	private Integer status;

	public BigInteger getErrorRecordID() {
		return errorRecordID;
	}

	public void setErrorRecordID(BigInteger errorRecordID) {
		this.errorRecordID = errorRecordID;
	}

	public BigInteger getWorkOrderID() {
		return workOrderID;
	}

	public void setWorkOrderID(BigInteger workOrderID) {
		this.workOrderID = workOrderID;
	}	

	public Date getErrorFoundDate() {
		return errorFoundDate;
	}

	public void setErrorFoundDate(Date errorFoundDate) {
		this.errorFoundDate = errorFoundDate;
	}

	public String getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}

	public String getErrorDescription() {
		return errorDescription;
	}

	public void setErrorDescription(String errorDescription) {
		this.errorDescription = errorDescription;
	}

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}	  
	
}

