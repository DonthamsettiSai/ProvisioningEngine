package com.knot.provisioning.daoTest;

import java.math.BigInteger;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;

import com.knot.provisioning.bo.ConfigBo;
import com.knot.provisioning.bo.ProvisioningRequestObjectsBO;
import com.knot.provisioning.daoimpl.ConfigDaoImpl;
import com.knot.provisioning.daoimpl.WorkOrderDaoImpl;

@SpringBootTest(classes = { ProvisioningGeneratorTests.class })
@ActiveProfiles("test")
@ComponentScan("com.knot.*")
public class ProvisioningGeneratorTests {

	private static final Logger log = LogManager.getLogger(ProvisioningGeneratorTests.class);

	@Autowired
	private WorkOrderDaoImpl workOrderDaoImpl;

	@Autowired
	ConfigDaoImpl daoImpl;

	@Test
	public void updateProvRequest() {

	
		BigInteger requestId = BigInteger.valueOf(54);
		Integer status = 18;
		log.info("Junit Test Case Started : : updateProvRequest() ");
		boolean updateProvRequest = workOrderDaoImpl.updateProvRequest(requestId, status);
		if (updateProvRequest == true) {
			log.info("updateProvRequest() updated Sucessfully  ");
			log.info("Junit Test Case ended : updateProvRequest()");
		} else {
			log.info("error occured in  : updateProvRequest()");
		}

	}

	@Test
	public void getProvRequesObjects() {

		log.info("Junit Test Case Started : getProvRequesObjects()");
		Integer serviceId = 10363;
		Integer activityId = 71461;
		Integer actionId = 1;
		Integer paymentId = -1;
		List<ProvisioningRequestObjectsBO> provRequesObjects = workOrderDaoImpl.getProvRequesObjects(serviceId,
				activityId, actionId, paymentId);
		if (provRequesObjects.size() > 0) {
			log.info("Sucess==============" + provRequesObjects.size());
		} else {
			log.info("Junit Test Case Started : getProvRequesObjects()");
		}
		log.info("Junit Test Case Ended : getProvRequesObjects()");
	}

//	@Test
//	public void getProvRequesObjectss() {
//
//		log.info("Junit Test Case Started : getProvRequesObjectss()");
//		List<ProvisioningRequestObjectsBO> provRequesObjects = workOrderDaoImpl.getProvRequesObjects();
//		if (provRequesObjects.size() > 0) {
//			log.info("Sucess ");
//		} else {
//			log.info("failure ");
//		}
//		log.info("Junit Test Case Ended : getProvRequesObjectss()");
//
//	}

	@Test
	public void getAllConfigs() {
		log.info("Junit Test Case Started : ");
		String tenantID = "10231";
		List<ConfigBo> config = daoImpl.getAllConfigs(tenantID);
		if (config.size() > 0) {
			log.info("ConfigBo size  : " + config.size());
			log.info("Junit Test Case Ended : getProvRequesObjectss()");
		} else {
			log.info("error ocuured in  : getProvRequesObjectss()");
		}

	}

}
