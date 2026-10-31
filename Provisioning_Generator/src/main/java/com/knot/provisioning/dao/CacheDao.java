package com.knot.provisioning.dao;

import java.util.List;

import com.knot.provisioning.bo.ProvisioningRequestObjectsBO;
import com.knot.provisioning.model.ActivityConfig;

public interface CacheDao {

	String strConfigVal(String code, String tenantID);

	Integer intConfigVal(String code, String tenantID);

	Long longConfigVal(String code, String tenantID);

	Float floatConfigVal(String code, String tenantID);

	List<ProvisioningRequestObjectsBO> getProvRequesObjects();
	
	public String configValByCodeForGraceFullStop(String code, String tenantID);

	List<ActivityConfig> getAllActvityConfigs();

}
