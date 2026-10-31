package com.knot.provisioning.serviceimpl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.knot.provisioning.bo.ProvisioningRequestObjectsBO;
import com.knot.provisioning.dto.WorkOrderDTO;
import com.knot.provisioning.network.OMSNetwork;
import com.knot.provisioning.service.CacheService;
import com.knot.provisioning.service.TFiberDynamicService;
import com.knot.provisioning.utill.Constants;
import com.knot.provisioning.utill.FileUtil;

@Service("tfiberService")
public class TFiberDynamicServiceImpl implements TFiberDynamicService{
	
	private static final Logger log = LogManager.getLogger(TFiberDynamicServiceImpl.class);

	private OMSNetwork omsNet;
	
	private CacheService cache;
	
	@Autowired
	public void setOmsNet(OMSNetwork omsNet) {
		this.omsNet = omsNet;
	}

	@Autowired
	public void setCache(CacheService cache) {
		this.cache = cache;
	}


	@Override
	/**
     * @logic This below Logic is used to replace all requestOrderParam Values Dynamically which is coming from oms response
	 * 117 b tenant is used to Replace T-fiber related Order parameters GI-BB-NOKIA
	 * */
	public String replaceInputJsonForTenatspecific(String inputJson, WorkOrderDTO workOrder,ProvisioningRequestObjectsBO provisioningObj) throws Exception {
		log.info("Started TFiberDynamicServiceImpl :: replaceInputJsonForTenatspecific()  inputJson {}, workOrderID {}",
				inputJson, workOrder.getWorkOrderID());
		String responseString = omsNet.getContractAttributesDetails(workOrder.getCustomerAccountID(),provisioningObj.getServiceID());
		List<String> ordersParam = FileUtil.foundResponseReplaceValues(inputJson);
		Map<String, String> finalResponseMap = new HashMap<>();
		for (String val : ordersParam) {
			finalResponseMap = FileUtil.convertResponseJsonToMap(responseString, val);
			if (inputJson.contains(cache.strConfigVal(Constants.ORDER_PARAM, provisioningObj.getTenantID().toString()) + val)) {
				log.info("Found input json replacement ");
				String key = finalResponseMap.get(val);
				if (key == null || key.trim().isEmpty()) {
					log.info("Removing JSON field for OMS key: {}", val);
					/** * This will replace if oms response is came empty or null we will remove entire  key in inputJson */
					String regex = ",?\\s*\"" + Pattern.quote(val) + "\"\\s*:\\s*(\"O\\|" + Pattern.quote(val)+ "\"|O\\|" + Pattern.quote(val) + ")";
					inputJson = inputJson.replaceAll(regex, "");
				} else {
					inputJson = inputJson.replace(cache.strConfigVal(Constants.ORDER_PARAM, provisioningObj.getTenantID().toString())+val, key);
				}
			} else {
				log.info("Key not Found From the response :: ", val);

			}
			finalResponseMap.clear();
		}
		return inputJson;
	}
	
}
