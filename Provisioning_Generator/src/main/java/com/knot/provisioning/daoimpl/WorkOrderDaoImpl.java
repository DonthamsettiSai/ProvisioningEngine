package com.knot.provisioning.serviceimpl;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.google.gson.Gson;
import com.knot.provisioning.bo.Contract;
import com.knot.provisioning.bo.DeviceBO;
import com.knot.provisioning.bo.ProvDetailsBO;
import com.knot.provisioning.bo.ProvisioningRequestObjectsBO;
import com.knot.provisioning.bo.ServiceDevicesBO;
import com.knot.provisioning.dao.WorkOrderDao;
import com.knot.provisioning.dto.ActivityDTO;
import com.knot.provisioning.dto.ProvLobDto;
import com.knot.provisioning.dto.ResourceDto;
import com.knot.provisioning.dto.WorkOrderDTO;
import com.knot.provisioning.model.ActivityConfig;
import com.knot.provisioning.model.ProvisioningRequest;
import com.knot.provisioning.model.ProvisioningRequestDetails;
import com.knot.provisioning.model.ProvisioningRequestGenerationError;
import com.knot.provisioning.network.CatalogueNetwork;
import com.knot.provisioning.network.DefNetwork;
import com.knot.provisioning.network.OMSNetwork;
import com.knot.provisioning.service.CacheService;
import com.knot.provisioning.service.ProvisioningService;
import com.knot.provisioning.service.TFiberDynamicService;
import com.knot.provisioning.threads.ProvisioningRequestThread;
import com.knot.provisioning.utill.Constants;
import com.knot.provisioning.utill.FileUtil;
import com.knot.provisioning.utill.ProvisionException;
import com.knot.redis.dto.TenantsDTO;

import jakarta.annotation.PreDestroy;

@Service
public class ProvisioningServiceImpl implements ProvisioningService {

    private static final Logger log = LogManager.getLogger(ProvisioningServiceImpl.class);
    private final Set<BigInteger> inFlightWorkOrders = ConcurrentHashMap.newKeySet();

    private WorkOrderDao workOrderDao;
    private OMSNetwork omsNet;
    private Gson gson;
    private DefNetwork defNet;
    private CacheService cache;
    private TenantUniqueCodeResolver tntCodeRes;
    private CatalogueNetwork catNet;
    private ScheduledExecutorService pollingExecutor;
    private String hostName;

    @Autowired
    public void setCatNet(CatalogueNetwork catNet) {
        this.catNet = catNet;
    }

    @Autowired
    public void setTntCodeRes(TenantUniqueCodeResolver tntCodeRes) {
        this.tntCodeRes = tntCodeRes;
    }

    @Autowired
    public void setCache(CacheService cache) {
        this.cache = cache;
    }

    @Autowired
    public void setDefNet(DefNetwork defNet) {
        this.defNet = defNet;
    }

    @Autowired
    public void setWorkOrderDao(WorkOrderDao workOrderDao) {
        this.workOrderDao = workOrderDao;
    }

    @Autowired
    public void setOmsNet(OMSNetwork omsNet) {
        this.omsNet = omsNet;
    }

    @Autowired
    public void setGson(Gson gson) {
        this.gson = gson;
    }

    public void setHashMapFromJSON(Map<String, Map<String, List<Map<String, String>>>> hashMapFromJSON) {
        this.hashMapFromJSON = hashMapFromJSON;
    }

    public void setTotalServices(Integer totalServices) {
        this.totalServices = totalServices;
    }

    private Map<String, Map<String, List<Map<String, String>>>> hashMapFromJSON;
    private Integer totalServices = 0;

    private String resolveHostName() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    @PreDestroy
    public void shutdown() {
        hostName = resolveHostName();
        log.info("Provisioning engine shutdown initiated on host={}, activeInFlightOrders={}", hostName,
                inFlightWorkOrders.size());
        if (pollingExecutor != null && !pollingExecutor.isShutdown()) {
            pollingExecutor.shutdown();
            try {
                if (!pollingExecutor.awaitTermination(15, TimeUnit.SECONDS)) {
                    pollingExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                pollingExecutor.shutdownNow();
            }
        }
    }

    @Override
    public void generateProvisioningRequests() {
        hostName = resolveHostName();
        log.info("Started generateProvisioningRequests() on host={}, thread={}", hostName,
                Thread.currentThread().getName());

        if (cache == null || defNet == null || omsNet == null) {
            log.error("Critical dependency missing for provisioning generation. cache={}, defNet={}, omsNet={}",
                    cache != null, defNet != null, omsNet != null);
            return;
        }

        String isGraceFullStopNeeded = cache.configValByCodeForGraceFullStop(Constants.PROV_GEN_GRACEFULL_STOP,
                Constants.GLOBAL_TENANT);
        if (StringUtils.hasText(isGraceFullStopNeeded) && isGraceFullStopNeeded.equalsIgnoreCase("Y")) {
            log.info("Application has been configured to stop processing on host={}", hostName);
            return;
        }

        List<TenantsDTO> allTenants = defNet.getAllTenants();
        log.info("Fetched tenant list for provisioning generation. host={}, tenantCount={}", hostName,
                allTenants == null ? 0 : allTenants.size());

        if (pollingExecutor != null && !pollingExecutor.isShutdown()) {
            log.info("Polling executor already started. host={}, activeTasks={}", hostName,
                    pollingExecutor.isShutdown());
            return;
        }

        int size = (allTenants == null || allTenants.isEmpty()) ? 1 : allTenants.size();
        pollingExecutor = Executors.newScheduledThreadPool(Math.max(1, size));

        try {
            for (TenantsDTO dto : allTenants) {
                if (dto == null || !StringUtils.hasText(dto.getTenantID())) {
                    log.warn("Skipping invalid tenant row from tenant list on host={}", hostName);
                    continue;
                }
                String tenantId = dto.getTenantID();
                ParentThread pthread = new ParentThread(tenantId);
                log.info("Scheduling tenant polling. tenantId={}, host={}, poolSize={}", tenantId, hostName, size);
                pollingExecutor.scheduleWithFixedDelay(pthread, 0, 5, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            log.error("Exception while scheduling provisioning polling tasks on host={}", hostName, e);
        }
    }

    private class ParentThread implements Runnable {

        private final String tenantID;
        private final String maxRecords;
        private final String localHostName;

        public ParentThread(String tenantId) {
            this.tenantID = tenantId;
            this.maxRecords = "100";
            this.localHostName = resolveHostName();
        }

        @Override
        public void run() {
            log.info("ParentThread started. tenantId={}, host={}, thread={}", tenantID, localHostName,
                    Thread.currentThread().getName());
            try {
                hashMapFromJSON = null;
                Integer priority = null;

                List<ActivityConfig> activityConfigs = cache.getAllActvityConfigs();
                Map<BigInteger, Integer> activityPriorityMap = activityConfigs == null ? Collections.emptyMap()
                        : activityConfigs.stream().filter(Objects::nonNull)
                                .filter(config -> config.getActivityId() != null)
                                .collect(Collectors.toMap(ActivityConfig::getActivityId, ActivityConfig::getPriority,
                                        (existing, replacement) -> replacement));

                log.info("Loaded activity priority map. tenantId={}, host={}, activityPriorityMap={}", tenantID,
                        localHostName, activityPriorityMap);

                List<WorkOrderDTO> workOrders = omsNet.getWorkOrders(tenantID, maxRecords);
                if (workOrders == null || workOrders.isEmpty()) {
                    log.info("No work orders available. tenantId={}, host={}", tenantID, localHostName);
                    return;
                }

                log.info("Fetched work orders. tenantId={}, host={}, count={}, workOrderIds={}", tenantID, localHostName,
                        workOrders.size(), workOrders.stream().map(WorkOrderDTO::getWorkOrderID).collect(Collectors.toList()));

                workOrders.sort(Comparator
                        .comparingInt(wo -> resolveActivityPriority(wo.getActivityID(), activityPriorityMap))
                        .thenComparing(WorkOrderDTO::getWorkOrderID));

                log.info("Work orders after priority sorting. tenantId={}, host={}, workOrderIds={}", tenantID, localHostName,
                        workOrders.stream().map(WorkOrderDTO::getWorkOrderID).collect(Collectors.toList()));

                Map<Integer, List<WorkOrderDTO>> activityWorkOrdersMap = new LinkedHashMap<>();
                Map<Integer, String> activityCodeMap = new HashMap<>();
                int missingPriorityCount = 0;
                int invalidActivityCount = 0;
                int specialOrderCount = 0;
                int catalogueDeferredCount = 0;
                boolean catalogueRefreshAttempted = false;
                if (hashMapFromJSON == null || hashMapFromJSON.isEmpty()) {
                    hashMapFromJSON = cache.refreshHashMapFromJson();
                    catalogueRefreshAttempted = true;
                }

                for (WorkOrderDTO workOrder : workOrders) {
                    Integer activityId = workOrder.getActivityID();
                    BigInteger orderTenantId = workOrder.getTenantID();
                    if (activityId == null || orderTenantId == null) {
                        invalidActivityCount++;
                        log.warn("Skipping work order with missing activity or tenant. workOrderId={}, activityId={}, tenantId={}",
                                workOrder.getWorkOrderID(), activityId, orderTenantId);
                        continue;
                    }

                    int resolvedPriority = resolveActivityPriority(activityId, activityPriorityMap);
                    if (resolvedPriority == 0 && !activityPriorityMap.containsKey(BigInteger.valueOf(activityId))) {
                        missingPriorityCount++;
                        log.warn("No priority configured for activity. processing with default priority 0. workOrderId={}, activityId={}, tenantId={}",
                                workOrder.getWorkOrderID(), activityId, orderTenantId);
                    }
                    priority = resolvedPriority;

                    try {
                        List<ActivityDTO> activities = defNet.getActivityCode(activityId.toString(), orderTenantId.toString());
                        ActivityDTO activityDTO = CollectionUtils.isEmpty(activities) ? null : activities.get(0);
                        if (activityDTO == null || !StringUtils.hasText(activityDTO.getActivityCode())) {
                            invalidActivityCount++;
                            log.warn("Activity configuration not found. tenantId={}, workOrderId={}, activityId={}",
                                    orderTenantId, workOrder.getWorkOrderID(), activityId);
                            omsNet.updateWorkOrder(workOrder.getWorkOrderID(), "Activity configuration not found",
                                    cache.intConfigVal(Constants.GENERATION_FAILED, Constants.GLOBAL_TENANT),
                                    orderTenantId, false);
                            continue;
                        }

                        String activityCode = activityDTO.getActivityCode();
                        if (activityCode.equalsIgnoreCase(Constants.RENEWAL) || activityCode.equalsIgnoreCase("OBD")
                                || (StringUtils.hasText(workOrder.getCreatedBy())
                                        && workOrder.getCreatedBy().equalsIgnoreCase("MSO"))) {
                            omsNet.updateWorkOrder(workOrder.getWorkOrderID(), "Manual Activation", 45, orderTenantId, true);
                            omsNet.postProvisioningUpdates(workOrder.getWorkOrderID(), activityCode,
                                    LocalDateTime.now().toString(), tenantID, workOrder.getWorkOrderID());
                            omsNet.updateWorkOrder(workOrder.getWorkOrderID(), "Notification not sent !!", 19,
                                    orderTenantId, true);
                            specialOrderCount++;
                            continue;
                        }

                        if (hashMapFromJSON == null || hashMapFromJSON.isEmpty()) {
                            if (!catalogueRefreshAttempted) {
                                hashMapFromJSON = cache.refreshHashMapFromJson();
                                catalogueRefreshAttempted = true;
                            }
                            if (hashMapFromJSON == null || hashMapFromJSON.isEmpty()) {
                                catalogueDeferredCount++;
                                log.warn("Catalogue not available for tenantId={}, activityId={}, workOrderId={}",
                                        tenantID, activityId, workOrder.getWorkOrderID());
                                continue;
                            }
                        }

                        omsNet.updateWorkOrder(workOrder.getWorkOrderID(),
                                cache.strConfigVal(Constants.WORK_ORD_REMARKS, Constants.GLOBAL_TENANT),
                                cache.intConfigVal(Constants.WORK_ORDER_INPROGRESS, Constants.GLOBAL_TENANT),
                                orderTenantId, true);
                        activityWorkOrdersMap.computeIfAbsent(activityId, key -> new ArrayList<>()).add(workOrder);
                        activityCodeMap.putIfAbsent(activityId, activityCode);

                    } catch (Exception e) {
                        invalidActivityCount++;
                        log.error("Failed to validate work order. tenantId={}, workOrderId={}, activityId={}",
                                tenantID, workOrder.getWorkOrderID(), activityId, e);
                    }
                }

                log.info("Work order validation summary. tenantId={}, host={}, fetched={}, activityGroups={}, missingPriority={}, invalidActivity={}, specialOrders={}, catalogueDeferred={}",
                        tenantID, localHostName, workOrders.size(), activityWorkOrdersMap.size(), missingPriorityCount,
                        invalidActivityCount, specialOrderCount, catalogueDeferredCount);

                for (Map.Entry<Integer, List<WorkOrderDTO>> activityEntry : activityWorkOrdersMap.entrySet()) {
                    Integer activityId = activityEntry.getKey();
                    List<WorkOrderDTO> activityOrders = activityEntry.getValue();
                    priority = resolveActivityPriority(activityId, activityPriorityMap);
                    String activityCode = activityCodeMap.get(activityId);

                    log.info("Processing activity group. tenantId={}, host={}, activityId={}, activityCode={}, priority={}, workOrderIds={}",
                            tenantID, localHostName, activityId, activityCode, priority,
                            activityOrders.stream().map(WorkOrderDTO::getWorkOrderID).collect(Collectors.toList()));

                    int requestsSubmitted = 0;
                    try {
                        List<WorkOrderDTO> ordersWithContracts = omsNet.getContracts(activityOrders);
                        if (ordersWithContracts == null || ordersWithContracts.isEmpty()) {
                            log.warn("No contracted work orders for activity. tenantId={}, host={}, activityId={}",
                                    tenantID, localHostName, activityId);
                            continue;
                        }
                        Map<WorkOrderDTO, ArrayList<Contract>> workOrderMap = this.getWorkOrderMap(ordersWithContracts);
                        if (workOrderMap == null || workOrderMap.isEmpty()) {
                            log.warn("No contract mapping generated. tenantId={}, host={}, activityId={}",
                                    tenantID, localHostName, activityId);
                            continue;
                        }
                        Map<BigInteger, Map<ProvisioningRequest, List<ProvisioningRequestDetails>>> provisioningMap = this
                                .getProvisioningRequestObjects(workOrderMap, activityCode, priority);
                        if (provisioningMap == null || provisioningMap.isEmpty()) {
                            log.warn("No provisioning requests generated. tenantId={}, host={}, activityId={}",
                                    tenantID, localHostName, activityId);
                            continue;
                        }

                        for (Map.Entry<BigInteger, Map<ProvisioningRequest, List<ProvisioningRequestDetails>>> requestEntry : provisioningMap.entrySet()) {
                            BigInteger workOrderId = requestEntry.getKey();
                            if (workOrderId != null && !inFlightWorkOrders.add(workOrderId)) {
                                log.warn("Duplicate provisioning generation skipped. tenantId={}, host={}, workOrderId={}",
                                        tenantID, localHostName, workOrderId);
                                continue;
                            }
                            ProvisioningRequestThread requestThread = new ProvisioningRequestThread(requestEntry,
                                    workOrderDao, inFlightWorkOrders, localHostName);
                            ThreadPoolHolder.getExecutor().submit(requestThread);
                            requestsSubmitted++;
                        }
                        log.info("Submitted {} provisioning requests. tenantId={}, host={}, activityId={}, priority={}",
                                requestsSubmitted, tenantID, localHostName, activityId, priority);
                    } catch (Exception e) {
                        log.error("Failed processing activity group. tenantId={}, host={}, activityId={}, priority={}",
                                tenantID, localHostName, activityId, priority, e);
                    }
                }
                log.info("Priority-based dispatch completed. tenantId={}, host={}", tenantID, localHostName);
            } catch (Exception e) {
                log.error("Error in ParentThread run() for tenantId={}, host={}", tenantID, localHostName, e);
            }
        }

        private int resolveActivityPriority(Integer activityId, Map<BigInteger, Integer> activityPriorityMap) {
            if (activityId == null) {
                return 0;
            }
            Integer configuredPriority = activityPriorityMap.get(BigInteger.valueOf(activityId));
            return configuredPriority == null ? 0 : configuredPriority;
        }

        private Map<BigInteger, Map<ProvisioningRequest, List<ProvisioningRequestDetails>>> getProvisioningRequestObjects(
                Map<WorkOrderDTO, ArrayList<Contract>> workOrderMap, String activityCode, Integer priority) {
            log.info("Started getProvisioningRequestObjects() tenantId={}, host={}, activityCode={}, priority={}",
                    tenantID, localHostName, activityCode, priority);
            Map<BigInteger, Map<ProvisioningRequest, List<ProvisioningRequestDetails>>> finalMap = new HashMap<>();
            Map<ProvisioningRequest, List<ProvisioningRequestDetails>> provisioningMap;
            String inputJson = "";
            List<ProvisioningRequestDetails> detailsList = null;
            List<Map<String, String>> list;
            ArrayList<Contract> listContracts;
            Contract contracts;
            Map<String, String> serverMap = null;
            Map<String, List<Map<String, String>>> serviceParamMap = null;
            String keyToCheck;
            BigInteger tenantIDValue = null;
            BigInteger resourceId = null;
            Integer activityID = null;
            BigInteger accountID = null;
            BigInteger customerID = null;
            BigInteger workOrderID;
            String tntWorkOrderID = null;
            boolean parameterFlag = false;
            String networkID = "";
            String errorDescription = "";
            Integer customerAccountType = null;
            Iterator<Entry<WorkOrderDTO, ArrayList<Contract>>> it3 = workOrderMap.entrySet().iterator();

            while (it3.hasNext()) {
                try {
                    ProvisioningRequest provisioningRequest = null;
                    provisioningMap = new HashMap<>();
                    detailsList = new ArrayList<>();
                    parameterFlag = false;

                    Entry<WorkOrderDTO, ArrayList<Contract>> entry3 = it3.next();
                    WorkOrderDTO workOrder = entry3.getKey();
                    workOrderID = workOrder.getWorkOrderID();
                    activityID = workOrder.getActivityID();
                    accountID = workOrder.getCustomerAccountID();
                    customerID = workOrder.getCustomerID();
                    tenantIDValue = workOrder.getTenantID();
                    resourceId = workOrder.getResourceDtlsID();
                    tntWorkOrderID = workOrder.getTntWorkOrderID();
                    customerAccountType = workOrder.getCustomerAccountType();
                    ProvDetailsBO provDetailsBO = null;

                    if (resourceId == null) {
                        provDetailsBO = omsNet.getCustomerDetails(workOrder.getCustomerID(),
                                workOrder.getCustomerAccountID(), tenantIDValue, workOrder.getActivityID());
                        if (provDetailsBO == null) {
                            log.error("No Customer Details Found for CustomerID={}, AccountID={}",
                                    workOrder.getCustomerID(), workOrder.getCustomerAccountID());
                            errorDescription = "No Customer Details Found for CustomerID ";
                            omsNet.updateWorkOrder(workOrderID, errorDescription,
                                    cache.intConfigVal(Constants.GENERATION_FAILED, Constants.GLOBAL_TENANT), tenantIDValue,
                                    false);
                            continue;
                        }
                    }
                    String customerDetails = gson.toJson(provDetailsBO);
                    Map<Integer, ArrayList<Contract>> contractsMap = workOrder.getContractMap();
                    List<String> serviceCreateTypeList = new ArrayList<>();
                    List<BigInteger> serviceIdsList = new ArrayList<>();
                    if (contractsMap != null) {
                        for (int mapKey = 1; mapKey <= contractsMap.size(); mapKey++) {
                            listContracts = contractsMap.get(mapKey);
                            String ocsKey = "";
                            String rpSId = "";
                            for (Contract obj : listContracts) {
                                contracts = obj;
                                BigInteger serviceID = contracts.getServiceID();
                                List<DeviceBO> deviceList = contracts.getDevices();
                                if (!CollectionUtils.isEmpty(deviceList)) {
                                    Map<String, ProvLobDto> allLobs = catNet.getAllLobs();
                                    for (Entry<String, ProvLobDto> entryMap : allLobs.entrySet()) {
                                        ProvLobDto lobObj = entryMap.getValue();
                                        if (lobObj != null && lobObj.getLobCode() != null
                                                && lobObj.getLobCode().equalsIgnoreCase(Constants.LOB_CODE_MOBILE_MARKET)) {
                                            serviceID = new BigInteger(Constants.GLOBAL_TENANT);
                                            serviceIdsList.add(serviceID);
                                            break;
                                        }
                                    }
                                    if (serviceIdsList.size() > 1) {
                                        continue;
                                    }
                                }
                                keyToCheck = contracts.getRatePlanID() + "::" + contracts.getServiceID();
                                serviceParamMap = hashMapFromJSON.get(keyToCheck);
                                if (serviceParamMap == null) {
                                    hashMapFromJSON = cache.refreshHashMapFromJson();
                                    serviceParamMap = hashMapFromJSON.get(keyToCheck);
                                }
                                if (serviceParamMap != null) {
                                    ocsKey = contracts.getRatePlanID().toString();
                                    rpSId = contracts.getRatePlanID() + Constants.KEY_SEP + contracts.getServiceID();
                                    List<ProvisioningRequestObjectsBO> provRequesObjectList = cache.getProvRequesObjects(serviceID,
                                            workOrder.getActivityID(), contracts.getActionID(), -1, workOrder.getTenantID(),
                                            workOrder.getDeviceType());
                                    if (provRequesObjectList.isEmpty()) {
                                        provRequesObjectList = cache.getProvRequesObjects(serviceID,
                                                workOrder.getActivityID(), contracts.getActionID(),
                                                workOrder.getCustomerAccountType(), workOrder.getTenantID(),
                                                workOrder.getDeviceType());
                                        if (provRequesObjectList.isEmpty()) {
                                            log.error("Provisioning Request Objects not found. tenantId={}, workOrderId={}, serviceId={}",
                                                    tenantIDValue, workOrderID, serviceID);
                                            errorDescription = "Provisioning Request Objects not found for this activity ";
                                        }
                                    }
                                    for (ProvisioningRequestObjectsBO provisioningObj : provRequesObjectList) {
                                        String serverURL = provisioningObj.getServerURL();
                                        inputJson = provisioningObj.getApiObject();
                                        if (inputJson != null) {
                                            if (provisioningObj.getPurpose().equalsIgnoreCase(
                                                    cache.strConfigVal(Constants.CUSTOMER, Constants.GLOBAL_TENANT))) {
                                                if (serviceCreateTypeList.contains(provisioningObj.getServerName())) {
                                                    continue;
                                                }
                                                serviceCreateTypeList.add(provisioningObj.getServerName());
                                            }
                                            if (serviceParamMap != null) {
                                                Iterator<Entry<String, List<Map<String, String>>>> it = serviceParamMap.entrySet().iterator();
                                                while (it.hasNext()) {
                                                    Map.Entry<String, List<Map<String, String>>> entry = it.next();
                                                    list = entry.getValue();
                                                    for (int i = 0; i < list.size(); i++) {
                                                        if (provisioningObj.getInterfaceType().equalsIgnoreCase("RESTFUL")) {
                                                            inputJson = inputJson.replace(" ", "");
                                                        }
                                                        serverMap = list.get(i);
                                                        Iterator<Entry<String, String>> it1 = serverMap.entrySet().iterator();
                                                        while (it1.hasNext()) {
                                                            Map.Entry<String, String> entry1 = it1.next();
                                                            String key = entry1.getKey();
                                                            if (inputJson.contains(key) && serverMap.get(key) != null) {
                                                                inputJson = inputJson.replaceAll(
                                                                        cache.strConfigVal(Constants.SERVICE_REGEX,
                                                                                provisioningObj.getTenantID().toString()) + key,
                                                                        serverMap.get(key));
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            inputJson = this.replaceOrderParameters(customerDetails, deviceList,
                                                    workOrder.getTenantID().toString(),
                                                    ocsKey + Constants.KEY_SEP + provisioningObj.getServerName(),
                                                    inputJson, rpSId, provisioningObj.getServerURL(), workOrder);
                                            if (inputJson != null && inputJson.contains(cache.strConfigVal(Constants.NETWORK_ID,
                                                    provisioningObj.getTenantID().toString()))
                                                    && !provisioningObj.getPurpose().equalsIgnoreCase(
                                                            cache.strConfigVal(Constants.CUSTOMER, Constants.GLOBAL_TENANT))) {
                                                networkID = omsNet.getNetworkID(contracts.getCustomerID(),
                                                        contracts.getCustomerAccountID(), provisioningObj.getLobID(),
                                                        provisioningObj.getServiceID(), tenantIDValue);
                                                if (networkID != null) {
                                                    inputJson = inputJson.replaceAll(
                                                            cache.strConfigVal(Constants.ORDER_REGEX,
                                                                    provisioningObj.getTenantID().toString())
                                                                    + cache.strConfigVal(Constants.NETWORK_ID,
                                                                            provisioningObj.getTenantID().toString()),
                                                            networkID);
                                                } else {
                                                    inputJson = inputJson.replaceAll(
                                                            cache.strConfigVal(Constants.ORDER_REGEX,
                                                                    provisioningObj.getTenantID().toString())
                                                                    + cache.strConfigVal(Constants.NETWORK_ID,
                                                                            provisioningObj.getTenantID().toString()),
                                                            cache.strConfigVal(Constants.N_A, Constants.GLOBAL_TENANT));
                                                }
                                            }
                                        }

                                        if (StringUtils.hasText(serverURL)) {
                                            if (serverURL.contains(cache.strConfigVal(Constants.ORDER_PARAM,
                                                    provisioningObj.getTenantID().toString()))) {
                                                String customerProvDetailss = omsNet.getCustomerProvDetailss(customerID,
                                                        accountID, tenantIDValue, activityID);
                                                Map<String, String> finalResponseMap = FileUtil
                                                        .convertResponseJsonToMap(customerProvDetailss, null);
                                                List<String> orderParamValues = FileUtil.foundResponseReplaceValues(serverURL);
                                                for (String val : orderParamValues) {
                                                    if (finalResponseMap.get(val) != null) {
                                                        serverURL = serverURL.replace(
                                                                cache.strConfigVal(Constants.ORDER_PARAM,
                                                                        provisioningObj.getTenantID().toString()) + val,
                                                                finalResponseMap.get(val));
                                                    }
                                                }
                                            }
                                            if (serverURL.contains(cache.strConfigVal(Constants.NETWORK_ID,
                                                    provisioningObj.getTenantID().toString()))
                                                    || inputJson.contains(cache.strConfigVal(Constants.NETWORK_ID,
                                                            provisioningObj.getTenantID().toString()))) {
                                                if (networkID.isEmpty()) {
                                                    networkID = omsNet.getNetworkID(contracts.getCustomerID(),
                                                            contracts.getCustomerAccountID(),
                                                            provisioningObj.getLobID(), provisioningObj.getServiceID(),
                                                            tenantIDValue);
                                                }
                                                if (networkID != null) {
                                                    serverURL = serverURL.replace(
                                                            cache.strConfigVal(Constants.ORDER_PARAM,
                                                                    provisioningObj.getTenantID().toString())
                                                                    + cache.strConfigVal(Constants.NETWORK_ID,
                                                                            provisioningObj.getTenantID().toString()),
                                                            networkID);
                                                    inputJson = inputJson.replace(
                                                            cache.strConfigVal(Constants.ORDER_PARAM,
                                                                    provisioningObj.getTenantID().toString())
                                                                    + cache.strConfigVal(Constants.NETWORK_ID,
                                                                            provisioningObj.getTenantID().toString()),
                                                            networkID);
                                                } else {
                                                    throw new ProvisionException(
                                                            "Unable to Replace Network Id in url. network id not found from oms.");
                                                }
                                            }
                                        }

                                        if (inputJson != null) {
                                            if (inputJson.contains(cache.strConfigVal(Constants.SERVICE_PARM,
                                                    provisioningObj.getTenantID().toString()))
                                                    && inputJson.contains(cache.strConfigVal(Constants.ORDER_PARAM,
                                                            provisioningObj.getTenantID().toString()))) {
                                                parameterFlag = true;
                                                log.error("Order && Service parameters not replaced. workOrderId={}, serviceId={}",
                                                        workOrderID, provisioningObj.getServiceID());
                                            } else if (inputJson.contains(cache.strConfigVal(Constants.SERVICE_PARM,
                                                    provisioningObj.getTenantID().toString()))) {
                                                parameterFlag = true;
                                                log.error("Service parameters not replaced. workOrderId={}, serviceId={}",
                                                        workOrderID, provisioningObj.getServiceID());
                                            }
                                        }
                                        if (!parameterFlag) {
                                            ProvisioningRequestDetails provisioningRequestDetails = getProvisioningRequestDetails(
                                                    provisioningObj, contracts, mapKey, inputJson, serverURL,
                                                    tntWorkOrderID, customerAccountType);
                                            detailsList.add(provisioningRequestDetails);
                                        }
                                    }
                                    if (provisioningRequest == null) {
                                        provisioningRequest = getProvisioningRequest(contracts, workOrder, priority);
                                    }
                                }
                            }
                        }
                    } else {
                        log.info("ContractsMap is null. workOrderId={}, activityId={}, tenantId={}",
                                workOrder.getWorkOrderID(), workOrder.getActivityID(), workOrder.getTenantID());
                        List<ProvisioningRequestObjectsBO> provRequesObjects = cache.getProvRequestObjects(
                                workOrder.getActivityID(), workOrder.getActionID(), workOrder.getTenantID());
                        String inptJson = "";
                        String serverURL = "";
                        if (resourceId != null) {
                            ResourceDto resourceDetails = omsNet.getResourceDetails(resourceId);
                            Map<String, String> resourceDTLmap = gson.fromJson(gson.toJson(resourceDetails), Map.class);
                            if (resourceDTLmap != null) {
                                for (Map.Entry<String, String> entry : resourceDTLmap.entrySet()) {
                                    String toReplace = cache.strConfigVal(Constants.ORDER_REGEX,
                                            workOrder.getTenantID().toString()) + entry.getKey();
                                    inptJson = inptJson.replaceAll(toReplace, entry.getValue());
                                }
                            }
                        }
                        for (ProvisioningRequestObjectsBO requestObjects : provRequesObjects) {
                            inptJson = requestObjects.getApiObject();
                            serverURL = requestObjects.getServerURL();
                            if (inptJson != null && inptJson.contains(cache.strConfigVal(Constants.ORDER_PARAM,
                                    requestObjects.getTenantID().toString()))) {
                                String customerProvDetailss = omsNet.getCustomerProvDetailss(customerID, accountID,
                                        tenantIDValue, activityID);
                                Map<String, String> finalResponseMap = FileUtil.convertResponseJsonToMap(customerProvDetailss, null);
                                List<String> ordersParam = FileUtil.foundResponseReplaceValues(inptJson);
                                for (String val : ordersParam) {
                                    if (finalResponseMap.get(val) != null) {
                                        inptJson = inptJson.replace(
                                                cache.strConfigVal(Constants.ORDER_PARAM,
                                                        requestObjects.getTenantID().toString()) + val,
                                                finalResponseMap.get(val));
                                    }
                                }
                            }
                            if (serverURL.contains(cache.strConfigVal(Constants.ORDER_PARAM,
                                    requestObjects.getTenantID().toString()))) {
                                String customerProvDetailss = omsNet.getCustomerProvDetailss(customerID, accountID,
                                        tenantIDValue, activityID);
                                Map<String, String> finalResponseMap = FileUtil.convertResponseJsonToMap(customerProvDetailss, null);
                                List<String> orderParamValues = FileUtil.foundResponseReplaceValues(serverURL);
                                for (String val : orderParamValues) {
                                    if (finalResponseMap.get(val) != null) {
                                        serverURL = serverURL.replace(
                                                cache.strConfigVal(Constants.ORDER_PARAM,
                                                        requestObjects.getTenantID().toString()) + val,
                                                finalResponseMap.get(val));
                                    }
                                }
                            }
                            if (serverURL.contains(cache.strConfigVal(Constants.NETWORK_ID,
                                    requestObjects.getTenantID().toString()))) {
                                if (networkID.isEmpty()) {
                                    networkID = omsNet.getNetworkID(customerID, accountID, requestObjects.getLobID(),
                                            requestObjects.getServiceID(), tenantIDValue);
                                }
                                if (networkID != null) {
                                    serverURL = serverURL.replace(cache.strConfigVal(Constants.ORDER_PARAM,
                                            requestObjects.getTenantID().toString())
                                            + cache.strConfigVal(Constants.NETWORK_ID,
                                                    requestObjects.getTenantID().toString()),
                                            networkID);
                                } else {
                                    throw new ProvisionException("Unable to Replace Network Id in url. network id not found from oms.");
                                }
                            }
                            ProvisioningRequestDetails provisioningRequestDetails = getProvisioningRequestDetails(
                                    requestObjects, null, 0, inptJson, serverURL, tntWorkOrderID, customerAccountType);
                            detailsList.add(provisioningRequestDetails);
                            if (provisioningRequest == null) {
                                provisioningRequest = getProvisioningRequest(null, workOrder, priority);
                            }
                            if (inptJson != null && inptJson.contains(cache.strConfigVal(Constants.ORDER_PARAM,
                                    requestObjects.getTenantID().toString()))) {
                                parameterFlag = true;
                                log.error("Order parameters not replaced. workOrderId={}, serviceId={}",
                                        workOrderID, requestObjects.getServiceID());
                            }
                        }
                    }

                    if (!detailsList.isEmpty() && !parameterFlag) {
                        provisioningMap.put(provisioningRequest, detailsList);
                        finalMap.put(workOrderID, provisioningMap);
                    } else {
                        ProvisioningRequestGenerationError obj = new ProvisioningRequestGenerationError();
                        if (parameterFlag) {
                            errorDescription = cache.strConfigVal(Constants.PARAM_REPLACE_ERROR,
                                    workOrder.getTenantID().toString());
                            obj.setErrorDescription(inputJson);
                        } else if (detailsList.isEmpty()) {
                            obj.setErrorDescription(errorDescription);
                        } else {
                            errorDescription = cache.strConfigVal(Constants.DEFAULT_ERROR,
                                    workOrder.getTenantID().toString());
                            obj.setErrorDescription(errorDescription);
                        }
                        try {
                            obj.setWorkOrderID(workOrderID);
                            obj.setStatus(11);
                            obj.setErrorFoundDate(new Date());
                            obj.setErrorDescription(errorDescription);
                            workOrderDao.saveProvisioningRequestGenerationError(obj);
                        } catch (Exception e) {
                            log.error("getProvisioningRequestObjects :: error while saving generation error.", e);
                        }
                        omsNet.updateWorkOrder(workOrderID, errorDescription,
                                cache.intConfigVal(Constants.GENERATION_FAILED, Constants.GLOBAL_TENANT), tenantIDValue,
                                false);
                    }
                } catch (Exception e) {
                    log.error("ProvisioningServiceImpl :: getReqObjectsbycompareKeys : {}", e.getMessage(), e);
                }
            }
            log.info("FinalMap Size :: {} host={}, tenantId={}", finalMap.size(), localHostName, tenantID);
            return finalMap;
        }

        private String replaceOrderParameters(String customerDetails, List<DeviceBO> deviceList, String tenantID,
                String rpID, String inputJSON, String rpSpId, String url, WorkOrderDTO workOrderDTO) throws Exception {
            if (customerDetails != null && !customerDetails.trim().isEmpty() && inputJSON.contains(cache.strConfigVal(Constants.ORDER_PARAM, tenantID))) {
                String responseString = omsNet.getCustomerProvDetailss(workOrderDTO.getCustomerID(),
                        workOrderDTO.getCustomerAccountID(), workOrderDTO.getTenantID(),
                        workOrderDTO.getActivityID());
                Map<String, String> finalResponseMap = FileUtil.convertResponseJsonToMap(responseString, null);
                List<String> ordersParam = FileUtil.foundResponseReplaceValues(inputJSON);
                for (String val : ordersParam) {
                    if (inputJSON.contains(cache.strConfigVal(Constants.ORDER_PARAM, tenantID) + val)
                            && finalResponseMap.get(val) != null) {
                        inputJSON = inputJSON.replace(cache.strConfigVal(Constants.ORDER_PARAM, tenantID) + val,
                                finalResponseMap.get(val));
                    }
                }
            }
            if (deviceList != null) {
                for (DeviceBO deviceBO : deviceList) {
                    String ky = cache.strConfigVal(Constants.ORDER_PARAM, tenantID) + deviceBO.getDeviceCode()
                            + cache.strConfigVal(Constants.PARAM_SEP, tenantID);
                    Map deviceParamMap = gson.fromJson(gson.toJson(deviceBO), Map.class);
                    Iterator it7 = deviceParamMap.entrySet().iterator();
                    while (it7.hasNext()) {
                        Map.Entry entry7 = (Map.Entry) it7.next();
                        String temp = ky + entry7.getKey();
                        if (inputJSON.contains(temp) || url.contains(temp)) {
                            if (entry7.getValue() != null) {
                                inputJSON = inputJSON.replace(temp, entry7.getValue().toString());
                                url = url.replace(temp, entry7.getValue().toString());
                            } else {
                                inputJSON = inputJSON.replace(temp,
                                        cache.strConfigVal(Constants.NOT_AVAILABLE, tenantID));
                            }
                        }
                    }
                }
            }
            String ocskey = cache.strConfigVal(Constants.ORDER_PARAM, tenantID)
                    + cache.strConfigVal(Constants.OCS_RP_ID, Constants.GLOBAL_TENANT);
            String ocsSPkey = cache.strConfigVal(Constants.ORDER_PARAM, tenantID) + "OCS_SP_ID";
            String ocsrpid = cache.getOCSRPID(rpID);
            String ocsSpId = cache.getOcsSPId(rpSpId);
            if (ocsrpid == null) {
                log.info("Not an OCS managed RP. rpId={}, tenantId={}", rpID, tenantID);
                return inputJSON;
            }
            if (inputJSON.contains(ocskey)) {
                inputJSON = inputJSON.replace(ocskey, ocsrpid);
            }
            if (inputJSON.contains(ocsSPkey)) {
                inputJSON = inputJSON.replace(ocsSPkey, ocsSpId);
            }
            return inputJSON;
        }

        private Map<WorkOrderDTO, ArrayList<Contract>> getWorkOrderMap(List<WorkOrderDTO> ordersWithContracts) {
            Map<WorkOrderDTO, ArrayList<Contract>> workOrderMap = new HashMap<>();
            ordersWithContracts.forEach(cont -> {
                try {
                    WorkOrderDTO wo = new WorkOrderDTO(cont);
                    Map<Integer, ArrayList<Contract>> contractMap = new HashMap<>();
                    ArrayList<Contract> bundleContracts = new ArrayList<>();
                    ArrayList<Contract> addOnContracts = new ArrayList<>();
                    ArrayList<Contract> oneTimeContracts = new ArrayList<>();
                    ArrayList<Contract> vasContracts = new ArrayList<>();
                    ArrayList<Contract> allContracts = new ArrayList<>();
                    int priority = 1;
                    if (cont.getContractList() != null && !cont.getContractList().isEmpty()) {
                        for (Contract obj1 : cont.getContractList()) {
                            String ratePlanType = obj1.getRatePlanType();
                            Contract contract = new Contract(obj1);
                            if (ratePlanType.equalsIgnoreCase(
                                    cache.strConfigVal(Constants.BUNDLE, obj1.getTenantID().toString()))
                                    || ratePlanType.equalsIgnoreCase(
                                            cache.strConfigVal(Constants.STANDALONE, obj1.getTenantID().toString()))) {
                                bundleContracts.add(obj1);
                            } else if (ratePlanType.equalsIgnoreCase(
                                    cache.strConfigVal(Constants.ADD_ON, obj1.getTenantID().toString()))) {
                                addOnContracts.add(obj1);
                            } else if (ratePlanType.equalsIgnoreCase(
                                    cache.strConfigVal(Constants.ONE_TIME, obj1.getTenantID().toString()))) {
                                oneTimeContracts.add(obj1);
                            } else if (ratePlanType.equalsIgnoreCase(
                                    cache.strConfigVal(Constants.VAS, obj1.getTenantID().toString()))) {
                                vasContracts.add(obj1);
                            }
                            allContracts.add(contract);
                        }
                    } else {
                        BigInteger workOrderID = cont.getWorkOrderID();
                        log.error("{} has no contracts returned from oms API..", workOrderID);
                        omsNet.updateWorkOrder(workOrderID, "No Contracts from OMS API",
                                cache.intConfigVal(Constants.GENERATION_FAILED, Constants.GLOBAL_TENANT),
                                cont.getTenantID(), false);
                    }
                    if (!bundleContracts.isEmpty()) {
                        contractMap.put(priority, bundleContracts);
                        priority++;
                    }
                    if (!addOnContracts.isEmpty()) {
                        contractMap.put(priority, addOnContracts);
                        priority++;
                    }
                    if (!oneTimeContracts.isEmpty()) {
                        contractMap.put(priority, oneTimeContracts);
                        priority++;
                    }
                    if (!vasContracts.isEmpty()) {
                        contractMap.put(priority, vasContracts);
                        priority++;
                    }
                    if (!contractMap.isEmpty()) {
                        wo.setContractMap(contractMap);
                        workOrderMap.put(wo, allContracts);
                    }
                } catch (Exception e) {
                    log.error("Error while prioritising contracts.", e);
                    omsNet.updateWorkOrder(cont.getWorkOrderID(), e.getMessage(),
                            cache.intConfigVal(Constants.GENERATION_FAILED, Constants.GLOBAL_TENANT),
                            cont.getTenantID(), false);
                }
            });
            return workOrderMap;
        }

        private ProvisioningRequest getProvisioningRequest(Contract contracts, WorkOrderDTO workOrder, Integer priority)
                throws Exception {
            ProvisioningRequest provisioningRequest = new ProvisioningRequest();
            provisioningRequest.setCustomerID(
                    workOrder.getCustomerID() != null ? workOrder.getCustomerID() : BigInteger.valueOf(-1));
            provisioningRequest.setCustomerAccountID(workOrder.getCustomerAccountID() != null ? workOrder.getCustomerAccountID()
                    : BigInteger.valueOf(-1));
            provisioningRequest.setRequestCreationDate(new Date());
            provisioningRequest.setStatus(cache.intConfigVal(Constants.PROV_REQ_GEN_SUCCESS, Constants.GLOBAL_TENANT));
            provisioningRequest.setCreatedBy(cache.strConfigVal(Constants.DEF_CREAT_BY, workOrder.getTenantID().toString()));
            provisioningRequest.setCreatedOn(new Date());
            provisioningRequest.setModifiedOn(new Date());
            provisioningRequest.setActionID(workOrder.getActionID());
            provisioningRequest.setWorkOrderID(workOrder.getWorkOrderID());
            provisioningRequest.setTenantID(workOrder.getTenantID());
            provisioningRequest.setPartnerID(workOrder.getPartnerID());
            provisioningRequest.setActivityID(workOrder.getActivityID());
            provisioningRequest.setNodeID(workOrder.getNodeID());
            provisioningRequest.setPriority(String.valueOf(priority == null ? 0 : priority));
            provisioningRequest.setResourceDetailId(workOrder.getResourceDtlsID() != null ? workOrder.getResourceDtlsID() : null);
            return provisioningRequest;
        }

        private ProvisioningRequestDetails getProvisioningRequestDetails(ProvisioningRequestObjectsBO provisioningObj,
                Contract contract, int ratePlanPriority, String inputJSON, String serverURL, String tntWorkOrderID,
                Integer customerAccountType) throws Exception {
            ProvisioningRequestDetails provisioningRequestDetails = new ProvisioningRequestDetails();
            provisioningRequestDetails.setLobID(provisioningObj.getLobID());
            provisioningRequestDetails.setLobPriority(provisioningObj.getLobProvisioningPriority());
            provisioningRequestDetails.setServiceID(
                    provisioningObj.getServiceID() != null ? provisioningObj.getServiceID() : BigInteger.valueOf(-1));
            provisioningRequestDetails.setServicePriority(provisioningObj.getServiceProvisioningPriority() != null
                    ? provisioningObj.getServiceProvisioningPriority()
                    : -1);
            provisioningRequestDetails.setRatePlanID(contract != null ? contract.getRatePlanID() : BigInteger.valueOf(-1));
            provisioningRequestDetails.setRatePlanPriority(ratePlanPriority);
            provisioningRequestDetails.setServicePackageID(contract != null ? contract.getServicePackageID() : BigInteger.valueOf(-1));
            provisioningRequestDetails.setServerName(provisioningObj.getServerName());
            provisioningRequestDetails.setTokenID(provisioningObj.getTokenID());
            provisioningRequestDetails.setInterfaceType(provisioningObj.getInterfaceType());
            provisioningRequestDetails.setServerURL(serverURL);
            provisioningRequestDetails.setReqSerRespID(tntWorkOrderID);
            provisioningRequestDetails.setStatus(cache.intConfigVal(Constants.PROV_REQ_GEN_SUCCESS, Constants.GLOBAL_TENANT));
            provisioningRequestDetails.setVersion(cache.floatConfigVal(Constants.DEFAULT_VERSION, Constants.GLOBAL_TENANT));
            provisioningRequestDetails.setRequestCreationDate(new Date());
            provisioningRequestDetails.setRequestObject(inputJSON);
            provisioningRequestDetails.setSequenceID(provisioningObj.getSequenceID());
            provisioningRequestDetails.setSubSequenceID(provisioningObj.getSubSequenceID());
            provisioningRequestDetails.setDependentSequenceID(provisioningObj.getDependentSequenceID());
            provisioningRequestDetails.setDependentSubSequenceID(provisioningObj.getDependentSubSequenceID());
            provisioningRequestDetails.setMaxRetries(provisioningObj.getMaxRetries());
            provisioningRequestDetails.setTimeGapBetweenRetry(provisioningObj.getTimeGapBetweenRetry());
            provisioningRequestDetails.setResponseUpdateCategory(provisioningObj.getResponseUpdateCategory());
            provisioningRequestDetails.setTimeGapUnit(provisioningObj.getTimeGapUnit());
            provisioningRequestDetails.setRequestMethod(provisioningObj.getRequestMethod());
            provisioningRequestDetails.setProvObjId(provisioningObj.getProvisioningRequestID());
            provisioningRequestDetails.setIsAsync(provisioningObj.getIsAsync());
            provisioningRequestDetails.setProcessType(provisioningObj.getProcessType());
            provisioningRequestDetails.setDeviceType(provisioningObj.getDeviceType());
            provisioningRequestDetails.setParamResplaceLevel(provisioningObj.getParamReplaceLevel());
            provisioningRequestDetails.setCallBackUrl(provisioningObj.getCallBackUrl());
            provisioningRequestDetails.setCallBackUrlObj(provisioningObj.getCallBackUrlObj());
            provisioningRequestDetails.setDepSubSeqProcessingType(provisioningObj.getDepSubSeqProcessingType());
            provisioningRequestDetails.setAddColumnOne(customerAccountType == null ? "-1" : customerAccountType.toString());
            return provisioningRequestDetails;
        }
    }

    private static class ThreadPoolHolder {
        private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);

        private static ExecutorService getExecutor() {
            return EXECUTOR;
        }
    }
}







































































































































































































































































































































































































































































































































n


























































































































































































































































































n



n





































{"owner":"DonthamsettiSai","repo":"ProvisioningEngine","branch":"fix/provisioning-production-hardening"}






































































































































n






























































































n















n















n














































































n
























n
























n












n



n











n



















n


































n

































n


















n



n



















n








n




























n





















n



n







n



n







n





n








n












n















n



n

















n










n














n











n

















n




n














n






n








n




n











n













n











n








n









n






n





n









n


n










n








n







n









n




n



n







n



n

n







n






n





n








n


n
n








n





n







n








n


n



n








n


n




n


n




n






n









n




n







n



n






n



n





n








n




n



n







n


n
n


n




n



n




n





n



n





n



n


n



n




n



n




n




n


n



n



n


n




n



n




n


n



n




n




n



n




n



n


n
n



n




n


n



n



n




n



n




n

n







n



n


n




n



n


n



n



n


n



n




n


n



n



n



n



n


n



n



n



n


n



n



n


n
n


n



n



n


n



n



n



n


n



n


n



n



n


n



n


n



n


n



nnn










































































n







n






n






n






n





n


n
n




n




n


n



n



n



n


n



n



n


n



n


n



n



n


n



n



n



n


n



n



n


n
n


n



n


n



n



n


n



n


n



n


n



n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n



n


n
n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n


n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n




















n



n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n



n


n


\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n"}]} one more file omitted?