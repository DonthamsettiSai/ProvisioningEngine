
package com.knot.provisioning.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "provisioning_activity_priorities")
public class ActivityConfig implements Serializable {

	/**
	 * @author SAI D
	 */
	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "ACTIVITY_RECORD_ID")
	private BigInteger configId;

	@Column(name = "ACTIVITY_CODE", nullable = false, length = 50)
	private String activityCode;

	@Column(name = "ACTIVITY_NAME", nullable = false, length = 100)
	private String activityName;

	@Column(name = "ACTIVITY_ID", nullable = false)
	private BigInteger activityId;

	@Column(name = "TENANT_ID", nullable = false)
	private BigInteger tenantId;

	@Column(name = "PRIORITY")
	private Integer priority;

	@Column(name = "WEIGHT_PERCENT", nullable = false, precision = 5, scale = 2)
	private BigDecimal weightPercent;

	@Column(name = "STATUS", nullable = false)
	private Integer status;

	@Column(name = "CREATED_BY")
	private String createdBy;

	@Column(name = "CREATED_ON", nullable = false)
	private LocalDateTime createdOn;

	@Column(name = "CREATED_IPADDR")
	private String createdIpAddr;

	@Column(name = "MODIFIED_BY")
	private String modifiedBy;

	@Column(name = "MODIFIED_ON", nullable = false)
	private LocalDateTime modifiedOn;

	@Column(name = "MODIFIED_IPADDR")
	private String modifiedIpAddr;

	@Id
	@Column(name = "EFFECTIVE_FROM", nullable = false)
	private LocalDateTime effectiveFrom;

	@Column(name = "EFFECTIVE_TO", nullable = false)
	private LocalDateTime effectiveTo;


	public void setActivityCode(String activityCode) {
		this.activityCode = activityCode;
	}

	public String getActivityName() {
		return activityName;
	}

	public void setActivityName(String activityName) {
		this.activityName = activityName;
	}

	public BigInteger getConfigId() {
		return configId;
	}

	public void setConfigId(BigInteger configId) {
		this.configId = configId;
	}

	public BigInteger getActivityId() {
		return activityId;
	}

	public void setActivityId(BigInteger activityId) {
		this.activityId = activityId;
	}

	public BigInteger getTenantId() {
		return tenantId;
	}

	public void setTenantId(BigInteger tenantId) {
		this.tenantId = tenantId;
	}

	public String getActivityCode() {
		return activityCode;
	}

	public Integer getPriority() {
		return priority;
	}

	public void setPriority(Integer priority) {
		this.priority = priority;
	}

	public BigDecimal getWeightPercent() {
		return weightPercent;
	}

	public void setWeightPercent(BigDecimal weightPercent) {
		this.weightPercent = weightPercent;
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

	public LocalDateTime getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(LocalDateTime createdOn) {
		this.createdOn = createdOn;
	}

	public String getCreatedIpAddr() {
		return createdIpAddr;
	}

	public void setCreatedIpAddr(String createdIpAddr) {
		this.createdIpAddr = createdIpAddr;
	}

	public String getModifiedBy() {
		return modifiedBy;
	}

	public void setModifiedBy(String modifiedBy) {
		this.modifiedBy = modifiedBy;
	}

	public LocalDateTime getModifiedOn() {
		return modifiedOn;
	}

	public void setModifiedOn(LocalDateTime modifiedOn) {
		this.modifiedOn = modifiedOn;
	}

	public String getModifiedIpAddr() {
		return modifiedIpAddr;
	}

	public void setModifiedIpAddr(String modifiedIpAddr) {
		this.modifiedIpAddr = modifiedIpAddr;
	}

	public LocalDateTime getEffectiveFrom() {
		return effectiveFrom;
	}

	public void setEffectiveFrom(LocalDateTime effectiveFrom) {
		this.effectiveFrom = effectiveFrom;
	}

	public LocalDateTime getEffectiveTo() {
		return effectiveTo;
	}

	public void setEffectiveTo(LocalDateTime effectiveTo) {
		this.effectiveTo = effectiveTo;
	}

	public ActivityConfig() {
		super();
	}

}
