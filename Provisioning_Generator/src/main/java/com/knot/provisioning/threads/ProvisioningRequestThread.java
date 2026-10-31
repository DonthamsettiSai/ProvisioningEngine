package com.knot.provisioning.threads;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.knot.provisioning.dao.WorkOrderDao;
import com.knot.provisioning.model.ProvisioningRequest;
import com.knot.provisioning.model.ProvisioningRequestDetails;

public class ProvisioningRequestThread implements Runnable{
	
	private static final Logger log = LogManager.getLogger(ProvisioningRequestThread.class);
	
	
	private Entry<BigInteger, Map<ProvisioningRequest, List<ProvisioningRequestDetails>>> obj;	
	private WorkOrderDao workOrderDao;
	
		
		public ProvisioningRequestThread(Entry<BigInteger, Map<ProvisioningRequest, List<ProvisioningRequestDetails>>>  obj, WorkOrderDao workOrderDao){this.obj = obj;
		this.workOrderDao = workOrderDao;
	}
	public void run(){
		try{				
			workOrderDao.saveProvisioningRequestObjects(obj);	// Generate request for each work order
		}catch(Exception e ){
			log.error("Error from ProvisioningServiceImpl - ProvisioningRequestThread ", e);					
		}
		return;
	}
}
