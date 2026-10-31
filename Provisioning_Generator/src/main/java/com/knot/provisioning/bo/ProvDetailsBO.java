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
public class ProvDetailsBO implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "CUSTOMER_ACCOUNT_ID")
	private String accountID;

	@Column(name = "TITLE")
	private String title;

	@Column(name = "ORGANIZATION_NAME")
	private String orgName;

	@Column(name = "FIRST_NAME")
	private String firstName;
	
	@Column(name = "MIDDLE_NAME")
	private String middleName;
	
	@Column(name = "LAST_NAME")
	private String lastName;
	
	@Column(name = "CUSTOMER_ACCOUNT_NAME")
	private String accountName;

	@Column(name = "PRIMARY_MOBILE")
	private String phoneNo;
	
	@Column(name = "PRIMARY_EMAIL")
	private String emailID;

	@Column(name = "ADDRESS")
	private String address;

	@Column(name = "ADDRESS_CITY")
	private String city;

	@Column(name = "ADDRESS_STATE")
	private String state;

	@Column(name = "ADDRESS_COUNTRY")
	private String country;
	//@Transient
	@Column(name = "ADDRESS_LOCALTY")
	private String locality;	
	
	@Column(name = "EXTERNAL_ID_1")
	private String externalID1;
	
	@Column(name = "EXTERNAL_ID_2")
	private String externalID2;
	
	@Column(name = "EXTERNAL_ID_3")
	private String externalID3;
	
	@Transient
	private String paymentID;
	
	@Transient
	private String paymentAmount;
	
	
	public String getPaymentID() {
		return paymentID;
	}

	public void setPaymentID(String paymentID) {
		this.paymentID = paymentID;
	}

	public String getPaymentAmount() {
		return paymentAmount;
	}

	public void setPaymentAmount(String paymentAmount) {
		this.paymentAmount = paymentAmount;
	}

	public String getExternalID3() {
		return externalID3;
	}

	public void setExternalID3(String externalID3) {
		this.externalID3 = externalID3;
	}

	public String getAccountID() {
		return accountID;
	}

	public void setAccountID(String accountID) {
		this.accountID = accountID;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getOrgName() {
		return orgName;
	}

	public void setOrgName(String orgName) {
		this.orgName = orgName;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getMiddleName() {
		return middleName;
	}

	public void setMiddleName(String middleName) {
		this.middleName = middleName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getAccountName() {
		return accountName;
	}

	public void setAccountName(String accountName) {
		this.accountName = accountName;
	}

	public String getPhoneNo() {
		return phoneNo;
	}

	public void setPhoneNo(String phoneNo) {
		this.phoneNo = phoneNo;
	}

	public String getEmailID() {
		return emailID;
	}

	public void setEmailID(String emailID) {
		this.emailID = emailID;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getLocality() {
		return locality;
	}

	public void setLocality(String locality) {
		this.locality = locality;
	}

	public String getExternalID1() {
		return externalID1;
	}

	public void setExternalID1(String externalID1) {
		this.externalID1 = externalID1;
	}
	
	
}
