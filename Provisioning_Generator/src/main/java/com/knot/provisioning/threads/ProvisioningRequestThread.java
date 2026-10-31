package com.knot.provisioning.threads;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.knot.provisioning.dao.WorkOrderDao;
import com.knot.provisioning.model.ProvisioningRequest;
import com.knot.provisioning.model.ProvisioningRequestDetails;

public class ProvisioningRequestThread implements Runnable {

    private static final Logger log = LogManager.getLogger(ProvisioningRequestThread.class);

    private final Entry<BigInteger, Map<ProvisioningRequest, List<ProvisioningRequestDetails>>> obj;
    private final WorkOrderDao workOrderDao;
    private final Set<BigInteger> inFlightWorkOrders;
    private final String hostName;

    public ProvisioningRequestThread(
            Entry<BigInteger, Map<ProvisioningRequest, List<ProvisioningRequestDetails>>> obj,
            WorkOrderDao workOrderDao, Set<BigInteger> inFlightWorkOrders, String hostName) {
        this.obj = obj;
        this.workOrderDao = workOrderDao;
        this.inFlightWorkOrders = inFlightWorkOrders;
        this.hostName = hostName;
    }

    @Override
    public void run() {
        BigInteger workOrderId = obj != null && obj.getKey() != null ? obj.getKey() : null;
        log.info("Provisioning request thread started. host={}, workOrderId={}, threadName={}",
                hostName, workOrderId, Thread.currentThread().getName());
        try {
            if (workOrderId != null && obj.getValue() != null) {
                log.info("Persisting provisioning request for workOrderId={}, requestMapSize={}",
                        workOrderId, obj.getValue().size());
            }
            workOrderDao.saveProvisioningRequestObjects(obj);
            log.info("Provisioning request persisted successfully. host={}, workOrderId={}", hostName, workOrderId);
        } catch (Exception e) {
            log.error("Error while persisting provisioning request. host={}, workOrderId={}", hostName, workOrderId, e);
        } finally {
            if (workOrderId != null && inFlightWorkOrders != null) {
                inFlightWorkOrders.remove(workOrderId);
                log.info("Released in-flight work order lock. host={}, workOrderId={}", hostName, workOrderId);
            }
        }
    }
}
