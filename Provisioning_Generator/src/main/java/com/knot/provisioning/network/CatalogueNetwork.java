package com.knot.provisioning.network;

import java.security.ProviderException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.knot.provisioning.dto.OcsRPSPMapData;
import com.knot.provisioning.dto.ProvLobDto;
import com.knot.provisioning.dto.ProvisionRatePlanDto;
import com.knot.provisioning.dto.ResponseDto;
import com.knot.provisioning.service.CacheService;
import com.knot.provisioning.utill.AuthenticationUtil;
import com.knot.provisioning.utill.Constants;
import com.knot.redis.dto.OcsRPMapData;

@Component
public class CatalogueNetwork {

	private static final String PROBLEM_FETCHING_PARAMETERS = "Problem Fetching Parameters from Catalogue URL.";

	private static final Logger log = LogManager.getLogger(CatalogueNetwork.class);

	private RestTemplate restTemplate;
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
	public void setUmsNetwork(UmsNetwork umsNetwork) {
		this.umsNetwork = umsNetwork;
	}

	public List<ProvisionRatePlanDto> getParameters() {
		String url = "";
		try {
			log.info("Started CatalogueNetwork :: getParameters() ");
			String token = umsNetwork.generateSuperUserToken();
			HttpEntity<String> httpEntity = AuthenticationUtil.getHeaderWithToken(token);
			url = cache.strConfigVal(Constants.CAT_URL, Constants.GLOBAL_TENANT) + "getProvisionParamsValues";
			log.info("Catalogue URL:- {}", url);
			ResponseDto<ProvisionRatePlanDto> response = restTemplate.exchange(url, HttpMethod.GET, httpEntity,
					new ParameterizedTypeReference<ResponseDto<ProvisionRatePlanDto>>() {
					}).getBody();
			if (response != null && response.getResult() != null && !response.getResult().isEmpty()) {
				log.debug("Response Result   From CatalogueNetwork :: getParameters() ProvisionRatePlanDtoList Size {}",response.getResult().size());
				log.info("Ended CatalogueNetwork :: getParameters() ");
				return response.getResult();
			}
			else {
				throw new ProviderException(PROBLEM_FETCHING_PARAMETERS);
			}
		} catch (Exception e) {
			log.error("CatalogueNetwork :: getParameters", e);
			return new ArrayList<>();
//			throw e;
		}
	}

	@Cacheable(value = "PROV_CONFIGURATIONS", key = "'OCS_RATE_PLANS'")
	public List<OcsRPMapData> loadOCSData() {
		String url = "";
		try {
			log.info("Started CatalogueNetwork :: loadOCSData");
			log.debug("This loadOCSData() Brings 'OCS_RATE_PLANS' Key From cache");
			String token = umsNetwork.generateSuperUserToken();
			HttpEntity<String> httpEntity = AuthenticationUtil.getHeaderWithToken(token);
			url = cache.strConfigVal(Constants.CAT_URL, Constants.GLOBAL_TENANT) + "getAllOcsRatePlanMappings";
			log.info("Catalogue URL:- {}", url);
			ResponseDto<OcsRPMapData> response = restTemplate.exchange(url, HttpMethod.GET, httpEntity,
					new ParameterizedTypeReference<ResponseDto<OcsRPMapData>>() {
					}).getBody();
			if (response != null && response.getResult() != null) {
				log.debug("Response Result OcsRPMapData list size From Catlouge ::{}",response.getResult().size());
				return response.getResult();
			} else
				throw new ProviderException(PROBLEM_FETCHING_PARAMETERS);
		} catch (Exception e) {
			log.error("CatalogueNetwork :: loadOCSData", e);
			throw e;
		}
	}

	@Cacheable(value = "PROV_CONFIGURATIONS", key = "'OCS_RATE_PLANS_SP'")
	public List<OcsRPSPMapData> loadOCSSPData() {
		String url = "";
		try {
			log.info("Started CatalogueNetwork :: loadOCSSPData");
			log.debug("This loadOCSSPData() Brings 'OCS_RATE_PLANS_SP' Key From cache");
			String token = umsNetwork.generateSuperUserToken();
			HttpEntity<String> httpEntity = AuthenticationUtil.getHeaderWithToken(token);
			url = cache.strConfigVal(Constants.CAT_URL, Constants.GLOBAL_TENANT) + "getAllOcsRPSPMappings";
			log.info("Catalogue URL:- {}", url);
			ResponseDto<OcsRPSPMapData> response = restTemplate.exchange(url, HttpMethod.GET, httpEntity,
					new ParameterizedTypeReference<ResponseDto<OcsRPSPMapData>>() {
					}).getBody();
			if (response != null && response.getResult() != null) {
				log.debug("Response Result OcsRPSPMapData list size From Catlouge ::{}",response.getResult().size());
				return response.getResult();
			} else
				throw new ProviderException(PROBLEM_FETCHING_PARAMETERS);
		} catch (Exception e) {
			log.error("CatalogueNetwork :: loadOCSData", e);
			throw e;
		}
	}
	
	public Map<String, ProvLobDto> getAllLobs() {
		log.info("Enterd in CatalogueNetwork :: getAllLobs() {}");
		Map<String, ProvLobDto> map = new HashMap<>();
		try {
			String token = umsNetwork.generateSuperUserToken();
			String url = cache.strConfigVal(Constants.CAT_URL, Constants.GLOBAL_TENANT)+"getAllLobs";
			HttpEntity<String> httpEntity = AuthenticationUtil.getHeaderWithToken(token);
			ResponseDto<ProvLobDto> response = restTemplate.exchange(url, HttpMethod.GET, httpEntity,
					new ParameterizedTypeReference<ResponseDto<ProvLobDto>>() {
					}).getBody();
			if (response != null && response.getResult() != null && !response.getResult().isEmpty()) {
				response.getResult().stream().forEach(o -> map.put(o.getLobID(), o));
			}
			log.info("Ended in CatalogueNetwork :: getAllLobs() {}");
		} catch (Exception e) {
			log.error("CatalogueNetwork :: getAllLobs() ", e);
			throw e;
		}
		return map;
	}

}
