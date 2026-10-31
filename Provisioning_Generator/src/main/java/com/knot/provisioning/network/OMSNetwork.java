package com.knot.provisioning.network;

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.gson.Gson;
import com.knot.provisioning.bo.ProvDetailsBO;
import com.knot.provisioning.bo.ServiceDevicesBO;
import com.knot.provisioning.dto.PostProvisioningFlagDto;
import com.knot.provisioning.dto.ResourceDto;
import com.knot.provisioning.dto.ResponseDto;
import com.knot.provisioning.dto.WorkOrderDTO;
import com.knot.provisioning.service.CacheService;
import com.knot.provisioning.utill.AuthenticationUtil;
import com.knot.provisioning.utill.Constants;

@Configuration
public class OMSNetwork {
	
	private static final String TENANT_ID = "tenantID";

	private static final Logger log = LogManager.getLogger(OMSNetwork.class);

	private RestTemplate restTemplate;
	private Gson gson;
	private CacheService cache;
	private UmsNetwork umsNetwork;
	
	@Autowired
	public void setRestTemplate(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}
	@Autowired
	public void setGson(Gson gson) {
		this.gson = gson;
	}
	@Autowired
	public void setCache(CacheService cache) {
		this.cache = cache;
	}
	@Autowired
	public void setUmsNetwork(UmsNetwork umsNetwork) {
		this.umsNetwork = umsNetwork;
	}

	public List<WorkOrderDTO> getWorkOrders(String tenantID,String maxRecords) {
		List<WorkOrderDTO> orders = new ArrayList<>();
		try {
			log.info("Started OMSNetwork :: getWorkOrders() ");
			log.debug("Entred OMSNetwork :: getWorkOrders() tenantID {}, maxRecords {}", tenantID,maxRecords);
			String token = umsNetwork.generateSuperUserToken();
			HttpEntity<String> httpEntity = AuthenticationUtil.getHeaderWithToken(token);
			String url = cache.strConfigVal(Constants.OMS_URL, Constants.GLOBAL_TENANT)+ "getAllActiveWorkOrders";
			UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url)
			.queryParam("status", cache.intConfigVal(Constants.NEW_WORK_ORDERS, Constants.GLOBAL_TENANT))
			.queryParam(TENANT_ID, tenantID);
			String uriString = builder.build().toUriString();
			log.info("OMS URL :- {}",uriString);
			ResponseDto<WorkOrderDTO> ordersResp = restTemplate.exchange(builder.build().toUri(), HttpMethod.GET, httpEntity, new ParameterizedTypeReference<ResponseDto<WorkOrderDTO>>() {
			}).getBody();
			if (ordersResp != null && ordersResp.getResult() != null && !ordersResp.getResult().isEmpty()) {
			    int maxRecordCount = "NA".equalsIgnoreCase(maxRecords) ? 0 : Integer.parseInt(maxRecords);
			    if (maxRecordCount <= 0) {
			        orders = Collections.emptyList();
			    } else if (ordersResp.getResult().size() <= maxRecordCount) {
			        orders = ordersResp.getResult();
			    } else {
			        orders = ordersResp.getResult().subList(0, maxRecordCount);
			    }
			}
			log.info("Ended OMSNetwork :: getWorkOrders() ");
		} catch (Exception e) {
			log.error("OMSNetwork :: getWorkOrders() :: ",e);
		}
		return orders;
	}

	public List<WorkOrderDTO> getContracts(List<WorkOrderDTO> workOrders) {
		List<WorkOrderDTO> ordersWithContracts = new ArrayList<>();
		try {
			log.info("Started OMSNetwork :: getContracts() ");
			log.debug("Enterd in OMSNetwork :: getContracts() workOrdersSize {} ",workOrders.size());
			String url = cache.strConfigVal(Constants.OMS_URL, Constants.GLOBAL_TENANT) + "getContracts";
			log.info("URL:- getContracts{}",url);
			String json = gson.toJson(workOrders);
			log.info("input:- {}",json);
			String token = umsNetwork.generateSuperUserToken();
			HttpEntity<List<WorkOrderDTO>> httpEntity = AuthenticationUtil.getHeaderWithTokenAndObject(token, workOrders);
			ResponseDto<WorkOrderDTO> ordersResp = restTemplate.exchange(url, HttpMethod.POST, httpEntity, new ParameterizedTypeReference<ResponseDto<WorkOrderDTO>>() {
			}).getBody();
			if(ordersResp != null && ordersResp.getResult() != null && !ordersResp.getResult().isEmpty()) {
				ordersWithContracts = ordersResp.getResult();
				log.debug("Response From OMSNetwork Contract List size {}",ordersWithContracts.size());
			}
			log.info("Ended OMSNetwork :: getContracts() ");
		} catch (Exception e) {
			log.error("OMSNetwork :: getContracts :: ",e);
			throw e;
		}
		return ordersWithContracts;
	}

	
		public boolean updateWorkOrder(BigInteger workOrderID, String remarks, int status, BigInteger tenantID,boolean updatePickedDate) {
		 	try {
			log.info("Started OMS Network :: updateWorkOrder() ");
			log.debug("Entred in OMSNetwork :: updateWorkOrder() workOrderID {}, remarks {}, status {}, tenantID {}, updatePickedDate {}", workOrderID, remarks, status, tenantID, updatePickedDate);
			String url = cache.strConfigVal(Constants.OMS_URL, Constants.GLOBAL_TENANT) + "updateWorkOrder";
			UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url)
			.queryParam("status", status)
			.queryParam("workorderID", workOrderID)
			.queryParam(TENANT_ID, tenantID)
			.queryParam("modifiedBy", cache.strConfigVal(Constants.GEN_ADMIN, tenantID.toString()))
			.queryParam("remarks", remarks);
			if (updatePickedDate) 
				builder.queryParam("isFromProv","Y" );
			
			String token = umsNetwork.generateSuperUserToken();
			HttpEntity<String> httpEntity = AuthenticationUtil.getHeaderWithToken(token);
			String uriString = builder.build().toUriString();
			log.info("URL:- updateWorkOrder {} ",uriString);
			ResponseDto<Boolean> ordersResp = restTemplate.exchange(builder.build().toUri(), HttpMethod.GET, httpEntity, new ParameterizedTypeReference<ResponseDto<Boolean>>() {
			}).getBody();
			if(ordersResp != null && ordersResp.getResult() != null && !ordersResp.getResult().isEmpty()) {
				log.info("OMS Network :: updateWorkOrder() Updated Sucess Fully");
				return ordersResp.getResult().get(0);
			}
			log.info("Ended OMS Network :: updateWorkOrder() ");
		} catch (Exception e) {
			log.error("OMSNetwork :: getContracts() :: ",e);
		}
		return false;
	}


		public String getNetworkID(BigInteger customerID, BigInteger customerAccountID, BigInteger lobID, BigInteger serviceID, BigInteger tenantID) {
		String netwID= "";
		try {
			log.info("Started OMSNetwork :: getNetworkID() ");
			log.debug("Entered OMSNetwork :: getNetworkID() customerID {}, customerAccountID {}, lobID {}, serviceID {}, tenantID {}", customerID,customerAccountID, lobID, serviceID, tenantID);
			String url = cache.strConfigVal(Constants.OMS_URL, Constants.GLOBAL_TENANT) + "getNetworkID";
			UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url)
					/** .queryParam("accountID", customerAccountID) */
			.queryParam("customerID", customerID)
					/** .queryParam("lobID", lobID) */
			.queryParam("serviceID", serviceID)
			.queryParam(TENANT_ID, tenantID);
			String token = umsNetwork.generateSuperUserToken();
			HttpEntity<String> httpEntity = AuthenticationUtil.getHeaderWithToken(token);
			String uriString = builder.build().toUriString();
			log.info("URL :- getNetworkID {}",uriString);
			ResponseDto<String> ordersResp = restTemplate.exchange(builder.build().toUri(), HttpMethod.GET, httpEntity, new ParameterizedTypeReference<ResponseDto<String>>() {
			}).getBody();
			if(ordersResp != null) {
				netwID = ordersResp.getId();
			}
			log.info("Ended OMSNetwork :: getNetworkID() ");
		} catch (Exception e) {
			log.error("OMSNetwork :: getNetworkID() :: ",e);
		}
		return netwID;
	}
	

		
		public List<ServiceDevicesBO> devicesAndServicesInfo(BigInteger workOrderID, BigInteger tenantID) {

		try {
			log.info("Started OMSNetwork :: devicesAndServicesInfo() ");
			log.debug("Enterd  OMSNetwork :: devicesAndServicesInfo() workOrderID {}, tenantID {}",workOrderID, tenantID);
			String url = cache.strConfigVal(Constants.OMS_URL, Constants.GLOBAL_TENANT) + "devicesAndServicesInfo?workOrderID="+workOrderID
					+"&tenantID"+tenantID;
			String token = umsNetwork.generateSuperUserToken();
			HttpEntity<String> httpEntity = AuthenticationUtil.getHeaderWithToken(token);
			log.info("URL :- devicesAndServicesInfo {}" ,url );
			ResponseDto<ServiceDevicesBO> ordersResp = restTemplate.exchange(url, HttpMethod.GET, httpEntity, new ParameterizedTypeReference<ResponseDto<ServiceDevicesBO>>() {
			}).getBody();
			if(ordersResp != null && ordersResp.getResult() != null && !ordersResp.getResult().isEmpty()) {
				return ordersResp.getResult();
			}
			log.info("Ended OMSNetwork :: devicesAndServicesInfo() ");
		} catch (Exception e) {
			log.error("OMSNetwork :: getContracts() :: ",e);
		}
		return Collections.emptyList();
	
	}

		public ProvDetailsBO getCustomerDetails(BigInteger customerID, BigInteger customerAccountID, BigInteger tenantID, Integer activityID ) {
		ProvDetailsBO custDtls= null;
		try {
			log.info("Started OMSNetwork :: getCustomerDetails()");
			log.debug("Entred in OMSNetwork :: getCustomerDetails() customerID {}, customerAccountID {}, tenantID {}", customerID, customerAccountID,tenantID);
			
			String url = cache.strConfigVal(Constants.OMS_URL, Constants.GLOBAL_TENANT) + "getCustomerProvDetails?accountID="+customerAccountID
					+"&customerID="+customerID
					+"&tenantID="+tenantID+"&activityID="+activityID;
			log.info("OMS URL:- getCustomerDetails {}",url);
			String token = umsNetwork.generateSuperUserToken();
			HttpEntity<String> httpEntity = AuthenticationUtil.getHeaderWithToken(token);
			ResponseDto<ProvDetailsBO> ordersResp = restTemplate.exchange(url, HttpMethod.GET, httpEntity, new ParameterizedTypeReference<ResponseDto<ProvDetailsBO>>() {
			}).getBody();
			if(ordersResp != null && ordersResp.getResult() != null && !ordersResp.getResult().isEmpty()) {
				custDtls = ordersResp.getResult().get(0);
				log.debug("Response From OMSNetwork :: getCustomerDetails() custDtls {}", gson.toJson(custDtls));
			}
			log.info("Ended OMSNetwork :: getCustomerDetails()");
		} catch (Exception e) {
			log.error("OMSNetwork :: getCustomerDetails() :: ",e);
		}
		return custDtls;
	}

		public List<Object[]> getOldAndNewSimInfo(BigInteger workOrderID, BigInteger tenantID) {
		try {
			log.info("Started OMSNetwork :: getOldAndNewSimInfo()");
			log.debug("Entred in OMSNetwork :: getOldAndNewSimInfo() workOrderID {}, tenantID {}", workOrderID, tenantID);	
			String token = umsNetwork.generateSuperUserToken();
			String url = cache.strConfigVal(Constants.OMS_URL, Constants.GLOBAL_TENANT) + "getOldAndNewSimInfo?workOrderID="+workOrderID+"&tenantID="+tenantID;
			HttpEntity<String> httpEntity = AuthenticationUtil.getHeaderWithToken(token);
			ResponseDto<Object[]> ordersResp = restTemplate.exchange(url, HttpMethod.GET, httpEntity, new ParameterizedTypeReference<ResponseDto<Object[]>>() {
			}).getBody();
			if(ordersResp != null && ordersResp.getResult() != null && !ordersResp.getResult().isEmpty()) {
				return ordersResp.getResult();
			}
			log.info("Ended OMSNetwork :: getOldAndNewSimInfo()");
		} catch (Exception e) {
			log.error("OMSNetwork :: getOldAndNewSimInfo() :: ",e);
		}
		return Collections.emptyList();
	}

		public ResourceDto getResourceDetails(BigInteger resourceDetailId) {
		try {
			log.info("Started OMSNetwork :: getResourceDetails()");
			log.debug("Entred in OMSNetwork :: getResourceDetails() resourceDetailId {} ",resourceDetailId);	
			String url = cache.strConfigVal(Constants.OMS_URL, Constants.GLOBAL_TENANT) + "getResourceDetails?resourceID="+resourceDetailId;
			log.info("URL:-" + url);
			log.info("input:- " + gson.toJson(resourceDetailId));
			String token = umsNetwork.generateSuperUserToken();
			HttpEntity<String> httpEntity = AuthenticationUtil.getHeaderWithToken(token);
			ResponseDto<ResourceDto> ordersResp = restTemplate.exchange(url, HttpMethod.GET, httpEntity,
					new ParameterizedTypeReference<ResponseDto<ResourceDto>>() {
					}).getBody();
			if (ordersResp != null && ordersResp.getResult() != null && !ordersResp.getResult().isEmpty()) {
				log.debug("Response From OMSNETWORK ResourceDto {}", gson.toJson(ordersResp.getResult().get(0)));
				return ordersResp.getResult().get(0);
			}
			log.info("Ended OMSNetwork :: getResourceDetails()");
		} catch (Exception e) {
			log.info("An Error Occured to fetch getResourceDetails() :: OmsNetwork ::", e);
		}
		return null;

	}

			public String getContractAttributesDetails(BigInteger accountID, BigInteger serviceID) {
			String response = "";
			log.info("Started OMSNetwork :: getContractAttributesDetails()");
			try {
				String url = cache.strConfigVal(Constants.OMS_URL, Constants.GLOBAL_TENANT) + "getContractAttributesDetails?customerAccountID="+accountID +"&serviceID="+serviceID;	
				String token = umsNetwork.generateSuperUserToken();
				HttpEntity<String> httpEntity = AuthenticationUtil.getHeaderWithToken(token);
				response = restTemplate.exchange(url, HttpMethod.GET, httpEntity,
						new ParameterizedTypeReference<String>() {
						}).getBody();
				log.info("url {} ",url);
				log.info("response:- " + gson.toJson(response));
			} catch (Exception e) {
				response = "";
				log.info("An Error Occured to fetch getContractAttributesDetails() :: OmsNetwork ::", e);
			}
			log.info("Ended OMSNetwork :: getContractAttributesDetails()");
			return response;
	}
		

			public PostProvisioningFlagDto postProvisioningUpdates(BigInteger workOrderID, String activityCode,
					String requestCompletionDate, String tenantID, BigInteger customerID) {
			/**
			 * http://172.16.110.238:8086/oms/postProvisioningUpdates?activityCode=NSWO&time*
			 * =2021-11-25&workOrderID=2106124
			 */
			try {
				log.info("Started OmsNetwork :: postProvisioningUpdates() ");
				log.debug("Entered in OmsNetwork :: postProvisioningUpdates() By passing an argumnets as workOrderID {}, activityCode {}, requestCompletionDate {}, tenantID {}, customerID {}",workOrderID, activityCode, requestCompletionDate , tenantID , customerID );
				String url = cache.strConfigVal(Constants.OMS_URL, Constants.GLOBAL_TENANT) + "postProvisioningUpdates";
				UriComponents uriComponent = UriComponentsBuilder.fromUriString(url)
						.queryParam("activityCode", activityCode).queryParam("workOrderID", workOrderID)
						.queryParam("time", requestCompletionDate)
						.queryParam("billCycleStartDate",
								LocalDate.now().toString())
						.queryParam("billCycleEndDate",
								LocalDate.now().toString())
						.queryParam(TENANT_ID, tenantID).build();
				String temp = uriComponent.toUriString();
				log.info("URL for OMS:- {}", temp);
				String token = umsNetwork.generateSuperUserToken();
				HttpEntity<String> httpEntity = AuthenticationUtil.getHeaderWithToken(token);
				ResponseDto<PostProvisioningFlagDto> ordersResp = restTemplate.exchange(uriComponent.toUri(), HttpMethod.GET,
						httpEntity, new ParameterizedTypeReference<ResponseDto<PostProvisioningFlagDto>>() {
						}).getBody();
				if (ordersResp != null) {
					temp = gson.toJson(ordersResp);
					log.info("Response from postProv procedure API:- {}", temp);
				}
				if (ordersResp != null && ordersResp.getResult() != null && !ordersResp.getResult().isEmpty()) {
					return ordersResp.getResult().get(0);
				}
			} catch (Exception e) {
				log.error("OMSNetwork :: postProvisioningUpdates() :: ", e);
			}
			return null;
		}

			public String getCustomerProvDetailss(BigInteger customerID, BigInteger customerAccountID, BigInteger tenantID, Integer activityID) {
			String response = "";
			log.info("Started OMSNetwork :: getCustomerProvDetailss()");
			try {
				String url = cache.strConfigVal(Constants.OMS_URL, Constants.GLOBAL_TENANT) + "getCustomerProvDetails?accountID="+customerAccountID
						+"&customerID="+customerID
						+"&tenantID="+tenantID+"&activityID="+activityID;
				log.info("OMS URL:- getCustomerDetails {}",url);
				String token = umsNetwork.generateSuperUserToken();
				HttpEntity<String> httpEntity = AuthenticationUtil.getHeaderWithToken(token);
				response = restTemplate.exchange(url, HttpMethod.GET, httpEntity,new ParameterizedTypeReference<String>(){}).getBody();
				log.info("url {} ",url);
				log.info("response:- " + gson.toJson(response));
			} catch (Exception e) {
				response = "";
				log.info("An Error Occured to fetch getCustomerProvDetailss() :: OmsNetwork ::", e);
			}
			log.info("Ended OMSNetwork :: getCustomerProvDetailss()");
			return response;
	}

			public String getMnpBroadcastDtls(BigInteger tenantID, BigInteger customerID, BigInteger customerAccountID) {
			String response = "";
			log.info("Started OMSNetwork :: getMnpBroadcastDtls()");
			try {
				String url = cache.strConfigVal(Constants.OMS_URL, Constants.GLOBAL_TENANT) + "getMnpBroadcastDtls?accountID="+customerAccountID
						+"&customerID="+customerID
						+"&tenantID="+tenantID;
				log.info("OMS URL:- getMnpBroadcastDtls {}",url);
				String token = umsNetwork.generateSuperUserToken();
				HttpEntity<String> httpEntity = AuthenticationUtil.getHeaderWithToken(token);
				response = restTemplate.exchange(url, HttpMethod.GET, httpEntity,new ParameterizedTypeReference<String>(){}).getBody();
				log.info("url {} ",url);
				log.info("response:- " + gson.toJson(response));
			} catch (Exception e) {
				response = "";
				log.info("An Error Occured to fetch getMnpBroadcastDtls() :: OmsNetwork ::", e);
			}
			log.info("Ended OMSNetwork :: getMnpBroadcastDtls()");
			return response;
	}
}
