package com.knot.provisioning.dao;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.knot.provisioning.bo.ProvisioningRequestObjectsBO;
import com.knot.provisioning.model.ProvisioningRequest;
import com.knot.provisioning.model.ProvisioningRequestDetails;
import com.knot.provisioning.model.ProvisioningRequestGenerationError;

public interface WorkOrderDao {

	
	public boolean saveProvisioningRequestObjects(Entry<BigInteger, Map<ProvisioningRequest, List<ProvisioningRequestDetails>>> obj) throws Exception;

	public List<ProvisioningRequestObjectsBO> getProvRequesObjects(Integer serviceID, Integer activityID,
			Integer actionID, Integer paymentID);

	public boolean saveProvisioningRequestGenerationError(ProvisioningRequestGenerationError obj);

}
