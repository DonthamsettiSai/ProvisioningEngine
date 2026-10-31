package com.knot.provisioning.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.google.gson.Gson;
import com.knot.provisioning.dto.ActivityDTO;
import com.knot.provisioning.dto.ResponseDto;
import com.knot.provisioning.service.CacheService;
import com.knot.provisioning.utill.AuthenticationUtil;
import com.knot.provisioning.utill.Constants;
import com.knot.redis.dto.TenantsDTO;

@Component
public class DefNetwork {

	private static final Logger log = LogManager.getLogger(DefNetwork.class);

	private RestTemplate restTemplate;
	private Gson gson;
	private UmsNetwork umsNetwork;
	private CacheService cache;

	@Autowired
	public void setCache(CacheService cache) {
		this.cache = cache;
	}

	@Autowired
	public void setRestTemplate(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	@Autowired
	public void setGson(Gson gson) {
		this.gson = gson;
	}

	@Autowired
	public void setUmsNetwork(UmsNetwork umsNetwork) {
		this.umsNetwork = umsNetwork;
	}

	public List<ActivityDTO> getActivityInfo(String activityCode, String tenantID, String activityID) {
		try {
			ActivityDTO in = new ActivityDTO();
			in.setActivityCode(activityCode);
			in.setTenantID(tenantID);
			in.setActivityID(activityID);
			String token = umsNetwork.generateSuperUserToken();
			HttpEntity<ActivityDTO> requestEntity = AuthenticationUtil.getHeaderWithTokenAndObject(token, in);
			String url = cache.strConfigVal(Constants.DEF_URL, Constants.GLOBAL_TENANT) + "getAllActivities";
			log.info("URL {}", url);
			String json = gson.toJson(in);
			log.info("Input :- {}", json);
			ResponseDto<ActivityDTO> defResp = restTemplate.exchange(url, HttpMethod.POST, requestEntity,
					new ParameterizedTypeReference<ResponseDto<ActivityDTO>>() {
					}).getBody();
			if (defResp != null && defResp.getResult() != null && !defResp.getResult().isEmpty()) {
				return defResp.getResult();
			}
		} catch (Exception e) {
			log.error("Unable to fetch Activities for proovided input from definition master activityCode {} {} {}",
					activityCode, tenantID, activityID);
		}
		return new ArrayList<>();
	}

	@Cacheable(value = "PROV_CONFIGURATIONS", key = "'allTenants'")
	public List<TenantsDTO> getAllTenants() {
		log.info("The Start of  ReferenceMasterNetwork:: getAllTenants()");
		log.info("Cache LOG FOR -- Get ALL Tenants");
		String token = umsNetwork.generateSuperUserToken();
		HttpEntity<String> httpEntity = AuthenticationUtil.getHeaderWithToken(token);
		String url = cache.strConfigVal(Constants.PMS_URL, Constants.GLOBAL_TENANT) + "/getAllTenants";
		log.info("GET All Tenants URL==  :: {}", url);
		try {
			ResponseDto<TenantsDTO> result = restTemplate
					.exchange(url, HttpMethod.GET, httpEntity, new ParameterizedTypeReference<ResponseDto<TenantsDTO>>() {
					}).getBody();
			return Optional.ofNullable(result).map(ResponseDto::getResult).orElse(Collections.emptyList());

		} catch (Exception e) {
			log.error("Erro {}", e.getMessage());
		}
		return Collections.emptyList();
	}
	
	public List<ActivityDTO> getActivityCode(String activityID, String tenantID) {
		try {
			log.info("Started DefinitionNetwork :: getActivityCode() ");
            log.debug("Entred in Cache Logs ::  DefinitionNetwork :: getActivityCode() activityID {}, tenantID {}",activityID, tenantID);
			ActivityDTO in = new ActivityDTO();
			in.setActivityID(activityID);
			in.setTenantID(tenantID);
			String token = umsNetwork.generateSuperUserToken();
			HttpEntity<ActivityDTO> requestEntity = AuthenticationUtil.getHeaderWithTokenAndObject(token, in);
			String url = cache.strConfigVal(Constants.DEF_URL, Constants.GLOBAL_TENANT) + "getAllActivities";
			log.info("URL :- {} Def " , url);
			log.info("Input:-{}" , gson!=null?gson.toJson(in):"");
			ResponseEntity<ResponseDto<ActivityDTO>> exchange = restTemplate.exchange(url, HttpMethod.POST, requestEntity,
					new ParameterizedTypeReference<ResponseDto<ActivityDTO>>() {
			});
			ResponseDto<ActivityDTO> defResp = exchange.getBody();
			if (exchange.getStatusCode().is2xxSuccessful() && defResp != null && defResp.getResult() != null) {
				return defResp.getResult();
			}
			log.info("Ended DefinitionNetwork :: getActivityCode() ");
			log.error("Reference master might be down. Resp from DEF:- {}",gson!=null?gson.toJson(exchange):"");
		} catch (Exception e) {
			log.error("Error while fetchinig Activitycode.", e);
		}
		return new ArrayList<>();
	}

}
