package com.knot.provisioning.service;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import com.knot.provisioning.bo.ProvisioningRequestObjectsBO;
import com.knot.provisioning.dto.ActivityDTO;
import com.knot.provisioning.model.ActivityConfig;

public interface CacheService {

	String strConfigVal(String code, String tenantID);

	Integer intConfigVal(String code, String tenantID);

	void updateValue(String code, String tenantID);

	Float floatConfigVal(String code, String tenantID);

	Map<String, Map<String, List<Map<String, String>>>> getHashMapFromJson();

	ActivityDTO getActivity(String tenantId, String activityCode, String activityID, boolean isNSWO);

	List<ProvisioningRequestObjectsBO> getProvRequesObjects(BigInteger serviceID, Integer activityID, Integer actionID,
			Integer customerAccountType, BigInteger tenantID, String deviceType);

	
	List<ProvisioningRequestObjectsBO> getProvRequestObjects(Integer activityID, Integer actionID, BigInteger tenantID);

	String getOCSRPID(String rpID);

	String getOcsSPId(String rpSpId);

	String getOcsCodeData(String key);
	
	public String configValByCodeForGraceFullStop(String code, String tenantID);
	
	public Map<String, Map<String, List<Map<String, String>>>> refreshHashMapFromJson();
	
	public List<ActivityConfig> getAllActvityConfigs();
	
	

}
