package com.knot.provisioning.serviceimpl;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.JDBCException;
import org.hibernate.exception.GenericJDBCException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.knot.provisioning.bo.ProvisioningRequestObjectsBO;
import com.knot.provisioning.dao.CacheDao;
import com.knot.provisioning.dto.ActivityDTO;
import com.knot.provisioning.dto.OcsRPSPMapData;
import com.knot.provisioning.dto.ProvisionRatePlanDto;
import com.knot.provisioning.dto.ProvisionSPDto;
import com.knot.provisioning.model.ActivityConfig;
import com.knot.provisioning.network.CatalogueNetwork;
import com.knot.provisioning.network.DefNetwork;
import com.knot.provisioning.service.CacheService;
import com.knot.provisioning.utill.Constants;
import com.knot.redis.dto.OcsRPMapData;

@Service
public class CacheServiceImpl implements CacheService {

	public static final Logger log = LogManager.getLogger(CacheServiceImpl.class);

	private CacheDao dao;
	private CatalogueNetwork catNet;
	private DefNetwork defNet;

	@Autowired
	public void setDefNet(DefNetwork defNet) {
		this.defNet = defNet;
	}

	@Autowired
	public void setCatNet(CatalogueNetwork catNet) {
		this.catNet = catNet;
	}

	@Autowired
	public void setConfig(CacheDao dao) {
		this.dao = dao;
	}

	@Override
	public String strConfigVal(String code, String tenantID) {
		return dao.strConfigVal(code, tenantID);
	}

	@Override
	public Integer intConfigVal(String code, String tenantID) {
		return dao.intConfigVal(code, tenantID);
	}

	@Override
	public Float floatConfigVal(String code, String tenantID) {
		return dao.floatConfigVal(code, tenantID);
	}

	@Override
	@CachePut(value = "PROV_CONFIGURATIONS", key = "#code + '::' + #tenantID")
	public void updateValue(String code, String tenantID) {
		log.info("Cache Value Updated ");
	}

	@Override
	@Cacheable(value = "PROV_CONFIGURATIONS", key = "'PROV_PARMS'")
	public Map<String, Map<String, List<Map<String, String>>>> getHashMapFromJson() {
		List<ProvisionRatePlanDto> provisionRatePlanDtolist = catNet.getParameters();
		log.info( "Catalogue API call successful. Response size: {}", provisionRatePlanDtolist != null? provisionRatePlanDtolist.size() : 0);
		List<ProvisionSPDto> serivcePackageList = null;
		Map<String, Map<String, List<Map<String, String>>>> hashMapFromJSON = new HashMap<>();
		if(!CollectionUtils.isEmpty(provisionRatePlanDtolist)) {
		for (ProvisionRatePlanDto obj : provisionRatePlanDtolist) {
			String ratePlanID = "";
			try {
				ratePlanID = obj.getRatePlanID();
				serivcePackageList = obj.getSerivcePackageList();
				for (ProvisionSPDto servicePackagesMap : serivcePackageList) {
					String key = "";
					String servicePackageID = "";
					String serviceID = "";
					servicePackageID = servicePackagesMap.getServicePackageID();
					serviceID = servicePackagesMap.getServiceID();
					String serviceId = serviceID == null ? "-1" : serviceID;
					/** key = ratePlanID + ":" + servicePackageID + ":" + serviceId; */
					key = ratePlanID+"::"+serviceID;
					if (serviceID != null) {
						Map<String, List<Map<String, String>>> serviceParamMap = servicePackagesMap
								.getServiceParamList();
						if (!CollectionUtils.isEmpty(serviceParamMap)) {
							Iterator<Map.Entry<String, List<Map<String, String>>>> it = serviceParamMap.entrySet()
									.iterator();
							Map.Entry<String, List<Map<String, String>>> entry = it.next();
							if (!entry.getKey()
									.equalsIgnoreCase(this.strConfigVal(Constants.N_A, Constants.GLOBAL_TENANT))) {
								hashMapFromJSON.put(key, serviceParamMap);
							}
						}
					}
				}
			} catch (Exception e) {
				log.error("Error from ProvisioningServiceImpl - readJSON() Catalogue Server is unavailable ", e);
				if (e instanceof JDBCException || e instanceof GenericJDBCException) {
					throw e;
				}
			}
		}
		log.info("Catalogue configuration map created successfully. Size: {}", hashMapFromJSON.size());
	}
		return hashMapFromJSON;

	}
	
	
	@CachePut(value = "PROV_CONFIGURATIONS", key = "'PROV_PARMS'")
	public synchronized Map<String, Map<String, List<Map<String, String>>>> refreshHashMapFromJson() {
		log.info("Refreshing Catalogue configuration cache...");
		return loadCatalogueConfigurations();
	}
	
	private Map<String, Map<String, List<Map<String, String>>>> loadCatalogueConfigurations() {
		log.info("Calling Catalogue API to load latest configuration");
		List<ProvisionRatePlanDto> provisionRatePlanDtolist = null;
		provisionRatePlanDtolist = catNet.getParameters();
		Map<String, Map<String, List<Map<String, String>>>> hashMapFromJSON = new HashMap<>();
		List<ProvisionSPDto> serivcePackageList = null;
		if(!CollectionUtils.isEmpty(provisionRatePlanDtolist)) {
			for (ProvisionRatePlanDto obj : provisionRatePlanDtolist) {
				String ratePlanID = "";
				try {
					ratePlanID = obj.getRatePlanID();
					serivcePackageList = obj.getSerivcePackageList();
					for (ProvisionSPDto servicePackagesMap : serivcePackageList) {
						String key = "";
						String servicePackageID = "";
						String serviceID = "";
						servicePackageID = servicePackagesMap.getServicePackageID();
						serviceID = servicePackagesMap.getServiceID();
						String serviceId = serviceID == null ? "-1" : serviceID;
						/** key = ratePlanID + ":" + servicePackageID + ":" + serviceId; */
						key = ratePlanID+"::"+serviceID;
						if (serviceID != null) {
							Map<String, List<Map<String, String>>> serviceParamMap = servicePackagesMap
									.getServiceParamList();
							if (!CollectionUtils.isEmpty(serviceParamMap)) {
								Iterator<Map.Entry<String, List<Map<String, String>>>> it = serviceParamMap.entrySet()
										.iterator();
								Map.Entry<String, List<Map<String, String>>> entry = it.next();
								if (!entry.getKey()
										.equalsIgnoreCase(this.strConfigVal(Constants.N_A, Constants.GLOBAL_TENANT))) {
									hashMapFromJSON.put(key, serviceParamMap);
								}
							}
						}
					}
				} catch (Exception e) {
					log.error("Error from ProvisioningServiceImpl - readJSON() Catalogue Server is unavailable ", e);
					if (e instanceof JDBCException || e instanceof GenericJDBCException) {
						throw e;
					}
				}
			}
			log.info("Provisioning RatePlan Paramteres Map size loadaed SucessFully . Size: {}", provisionRatePlanDtolist.size());
		}

		return hashMapFromJSON;
	}
	
	

	@Override
	@Cacheable(value = "PROV_CONFIGURATIONS", key = "#tenantId+ '::' +#activityCode+ '::' +#activityID+ '::' +#isNSWO")
	public ActivityDTO getActivity(String tenantId, String activityCode, String activityID, boolean isNSWO) {
		List<ActivityDTO> nswoActivities = defNet
				.getActivityInfo(this.strConfigVal(Constants.NSWO, Constants.GLOBAL_TENANT), null, null);
		List<ActivityDTO> changeSimActivity = defNet.getActivityInfo(this.strConfigVal(Constants.CHANGE_SIM_ACTIVITY, Constants.GLOBAL_TENANT), null, null);

		Stream<ActivityDTO> stream = isNSWO ? nswoActivities.stream() : changeSimActivity.stream();
		Optional<ActivityDTO> optionalNSWO = stream.filter(act -> act.getTenantID().equalsIgnoreCase(tenantId))
				.filter(act -> activityCode == null ? true : act.getActivityCode().equalsIgnoreCase(activityCode))
				.filter(act -> activityID == null ? true : act.getActivityID().equalsIgnoreCase(activityID)).findAny();
		if (optionalNSWO.isPresent()) {
			return optionalNSWO.get();
		}
		log.error("No Activity Object found for provided input tenantID {} : activityCode {}: activityID: {}"
				, tenantId, activityCode, activityID);
		return null;
	}


	@Override
	@Cacheable(value = "PROV_CONFIGURATIONS", key = "#serviceID+ '::' +#activityID+ '::'+#actionID+ '::'+#paymentID+ '::'+#tenantID+ '::'+#deviceType")

		public List<ProvisioningRequestObjectsBO> getProvRequesObjects(BigInteger serviceID, Integer activityID,
				Integer actionID, Integer paymentID, BigInteger tenantID, String deviceType) {
		 log.info("getProvRequesObjects called with serviceID={}, activityID={}, actionID={}, paymentID={}, tenantID={}, deviceType={}",
                 serviceID, activityID, actionID, paymentID, tenantID, deviceType);
		try {
			List<ProvisioningRequestObjectsBO> provRequesObjects = dao.getProvRequesObjects();
			List<ProvisioningRequestObjectsBO> filteredList = provRequesObjects.stream()
					.filter(req -> normalFilter.test(req.getTenantID(), tenantID))

					.filter(req -> activityFilter.test(req.getActivityID(), activityID))
					.filter(req -> req.getAction().equalsIgnoreCase(actionID.toString()))
					.filter(req -> normalFilter.test(req.getServiceID(), serviceID))
					.filter(req -> req.getDeviceType() == null || deviceType == null || req.getDeviceType().equalsIgnoreCase(deviceType))
					.filter(req -> paymentFilter.test(req.getPaymentType(), paymentID)).collect(Collectors.toList());
			if (filteredList.isEmpty()) {
				log.error("No objects found aftering filtering data with serviceID:- " + serviceID + ", activityID:- "
						+ activityID + ", actionID:- " + actionID + ", paymentID:- " + paymentID + ", tenantID:- "
						+ tenantID);
			}else{
				 log.info("RequestObjects found  with serviceID={}, activityID={}, actionID={}, paymentID={}, tenantID={}, deviceType={}, provRequesObjectsSize={},filteredListSize={}",
		                  serviceID, activityID, actionID, paymentID, tenantID, deviceType, provRequesObjects.size(), filteredList.size());
			}
			return filteredList;
		} catch (Exception e) {
			log.error("An excpetion occured in getProvRequesObjects  with serviceID={}, activityID={}, actionID={}, paymentID={}, tenantID={}, deviceType={}",
	                  serviceID, activityID, actionID, paymentID, tenantID, deviceType);
			return new ArrayList<>();
		}
	}


	private static BiPredicate<BigInteger, BigInteger> normalFilter = (configured, got) -> {
	    return configured != null && got != null && configured.equals(got);
	};
	
	private static BiPredicate<Integer, Integer> activityFilter = (configured, got) -> {
	    return configured != null && got != null && configured.equals(got); 
	};

	private BiPredicate<Integer, Integer> paymentFilter = (req, payment) -> {
		try {
			return (int) req == (int) payment|| (int) req == (int) this.intConfigVal(Constants.GLOBAL_FLAG, Constants.GLOBAL_TENANT);
		} catch (Exception e) {
			return false;
		}
	};

	@Override
	@Cacheable(value = "PROV_CONFIGURATIONS", key = "#activityID+ '::' +#actionID+ '::'+#tenantID")

		public List<ProvisioningRequestObjectsBO> getProvRequestObjects(Integer activityID, Integer actionID, BigInteger tenantID) {
		List<ProvisioningRequestObjectsBO> filteredList = new ArrayList<>();
		if (actionID != null && activityID != null) {
			List<ProvisioningRequestObjectsBO> provRequesObjects = dao.getProvRequesObjects();
			filteredList = provRequesObjects.stream()
					.filter(req -> req.getAction().equalsIgnoreCase(actionID.toString())
							&& activityFilter.test(req.getActivityID(), activityID))
					.collect(Collectors.toList());
			if (filteredList.isEmpty()) {
				log.error("No objects found aftering filtering data with actionID:-  " + actionID + " activityID :- "
						+ activityID);
				return filteredList;
			}
		}

		return filteredList;

	}

	@Override
	public String getOCSRPID(String key) {
		List<OcsRPMapData> ocsData = catNet.loadOCSData();
		Map<String, String> ocsMap = new HashMap<>();
		ocsData.stream().forEach(o -> ocsMap.put(o.getOobRatePlanID()+Constants.KEY_SEP+o.getOcsName(), o.getOcsRatePlanID()));
		String ocsRPID = ocsMap.get(key);
		if(ocsRPID == null) {
			log.error("OCS RatePlanID not found for the key: " + key);
		}
		return ocsRPID;
	}
	@Override
	public String getOcsSPId(String key) {
		List<OcsRPSPMapData> ocsSpData = catNet.loadOCSSPData();
		Map<String, String> ocsSpMap = new HashMap<>();
		for (OcsRPSPMapData o : ocsSpData) {
			ocsSpMap.put(o.getOobRPID() + Constants.KEY_SEP + o.getOobServiceID(), o.getOcsSPID());
		}
		String ocsSPID = ocsSpMap.get(key);
		if (ocsSPID == null) {
			log.error("OCS ServicePakageId not found for the key: " + key);
		}
		return ocsSPID;
	}
	@Override
	public String getOcsCodeData(String key) {
		List<OcsRPMapData> ocsData = catNet.loadOCSData();
		Map<String, String> ocsMap = new HashMap<>();
		ocsData.stream().forEach(o -> ocsMap.put(o.getOobRatePlanID()+Constants.KEY_SEP+o.getOcsName(), o.getOcsID()));
		String ocsRPID = ocsMap.get(key);
		if(ocsRPID == null) {
			log.error("OCS RatePlanID not found for the key: " + key);
		}
		return ocsRPID;
	}
	
	@Override
	public String configValByCodeForGraceFullStop(String code, String tenantID) {
		return dao.configValByCodeForGraceFullStop(code, tenantID);
	}
	
	@Override
	public List<ActivityConfig> getAllActvityConfigs() {
		log.info("Started CacheServiceImpl :: getAllActvityConfigs() ");
		List<ActivityConfig> priorityList = null;
		try {
			priorityList = dao.getAllActvityConfigs();
			log.info("Ended CacheServiceImpl :: getAllActvityConfigs() ");
		} catch (Exception e) {
			log.error("An Exception Occured in  :: getAllActvityConfigs() ", e);
		}
		return priorityList;
	}
	

}
