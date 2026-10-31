package com.knot.provisioning.service;

import com.knot.provisioning.bo.ProvisioningRequestObjectsBO;
import com.knot.provisioning.dto.WorkOrderDTO;

public interface TFiberDynamicService {
	
	public String replaceInputJsonForTenatspecific(String inputJson, WorkOrderDTO workOrder, ProvisioningRequestObjectsBO provisioningObj) throws Exception;

}
