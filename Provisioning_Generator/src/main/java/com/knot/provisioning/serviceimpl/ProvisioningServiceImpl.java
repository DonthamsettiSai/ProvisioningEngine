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

@Service
public class ProvisioningServiceImpl implements ProvisioningService {

	private static final Logger log = LogManager.getLogger(ProvisioningServiceImpl.class);

	private WorkOrderDao workOrderDao;
	private OMSNetwork omsNet;
	private Gson gson;
	private DefNetwork defNet;
	private CacheService cache;
	private TenantUniqueCodeResolver tntCodeRes;
	private CatalogueNetwork catNet;

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

	@Override
	public void generateProvisioningRequests() {

		log.info(
				"Started ProvisioningServiceImpl::generateProvisioningRequest - Looking for new Work Orders for provisioning request generation !!! ");
		String isGraceFullStopNeeded = cache.configValByCodeForGraceFullStop(Constants.PROV_GEN_GRACEFULL_STOP,
				Constants.GLOBAL_TENANT);
		if (StringUtils.hasText(isGraceFullStopNeeded) && isGraceFullStopNeeded.equalsIgnoreCase("Y")) {
			log.info("Application has been configured to stop processing.");
		} else {
			List<TenantsDTO> allTenants = defNet.getAllTenants();
			log.debug("Response From PMSNETWORK :: getAllTenants() TenantsDTOlistSize : {}", allTenants.size());

			int size = allTenants.isEmpty() ? 1 : allTenants.size();
			ScheduledExecutorService executor = Executors.newScheduledThreadPool(size);
			try {
				log.debug("Thread Size equals to number of tenants : size {} :: Started loop For all tenants", size);
				for (TenantsDTO dto : allTenants) {
					String tenantId = dto.getTenantID();
					ParentThread pthread = new ParentThread(tenantId);
					executor.scheduleAtFixedRate(pthread, 0, 5, TimeUnit.SECONDS);
					
				}
			} catch (Exception e) {
				log.info("Exception occurs in ",e);
			  }
			}	

		}

	private class ParentThread implements Runnable {

		String tenantID;
		String maxRecords;

		public ParentThread(String tenantId) {
			this.tenantID = tenantId;

		}

		int threadSize = cache.intConfigVal(Constants.THREADSIZE, Constants.GLOBAL_TENANT);
		ExecutorService executor = Executors.newScheduledThreadPool(threadSize);

		@Override
		public void run() {

			log.info("ParentThread started for tenant {}", tenantID);

			try {
				/** Load catalogue configuration */
//				hashMapFromJSON = cache.getHashMapFromJson();
				hashMapFromJSON = null;
				maxRecords = "100";
				Integer priority = null;

				/** Load activity priority configuration into cache */
				List<ActivityConfig> activityConfigs = cache.getAllActvityConfigs();
				Map<BigInteger, Integer> activityPriorityMap = activityConfigs == null ? Collections.emptyMap()
						: activityConfigs.stream().filter(Objects::nonNull)
								.filter(config -> config.getActivityId() != null)
								.filter(config -> config.getPriority() != null)
								.collect(Collectors.toMap(ActivityConfig::getActivityId, ActivityConfig::getPriority,
										(existing, replacement) -> replacement));

				log.info("TEST-3: Loaded activity priority map: {}", activityPriorityMap);
				if (activityPriorityMap.isEmpty()) {
					log.warn("No activity priority configuration found for tenant {}", tenantID);
				}
				/** Fetch work orders from OMS */
				List<WorkOrderDTO> workOrders = omsNet.getWorkOrders(tenantID, maxRecords);
				if (workOrders == null || workOrders.isEmpty()) {
					log.info("No work orders available for tenant {}", tenantID);
					try {
						Thread.sleep(10000);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						log.warn("ParentThread interrupted for tenant {}", tenantID);
					}
					return;
				}
				log.info("Fetched {} work orders for tenant {}", workOrders.size(), tenantID);
				// TEST-2: Capture the order in which work orders are received from OMS
				log.info("TEST-2 | OMS order before priority sorting: {}",
						workOrders.stream().map(WorkOrderDTO::getWorkOrderID).collect(Collectors.toList()));

				/**
				 * Sort work orders by priority (1, 2, 3, ...) if priority is same then sort
				 * workOrdersID
				 */
				// Remove work orders with NULL ACTIVITY_ID or no matching activity configuration
				workOrders.removeIf(wo ->
				    wo.getActivityID() == null ||
				    !activityPriorityMap.containsKey(
				        BigInteger.valueOf(wo.getActivityID())
				    )
				);
				workOrders.sort(Comparator
					    .comparingInt((WorkOrderDTO wo) -> {
					        Integer activityPriority  = activityPriorityMap.get(
					            BigInteger.valueOf(wo.getActivityID())
					        );
					        return activityPriority  == null ? Integer.MAX_VALUE : activityPriority;
					    })
					    .thenComparing(WorkOrderDTO::getWorkOrderID));
				// TEST LOG: Verify work order sorting by priority and Work Order ID
				log.info("TEST-2: Work orders after priority sorting: {}",
						workOrders.stream().map(WorkOrderDTO::getWorkOrderID).collect(Collectors.toList()));
				/** Group work orders by activity in priority order */
				Map<Integer, List<WorkOrderDTO>> activityWorkOrdersMap = new LinkedHashMap<>();
				Map<Integer, String> activityCodeMap = new HashMap<>();
				int missingPriorityCount = 0;
				int invalidActivityCount = 0;
				int specialOrderCount = 0;
				int catalogueDeferredCount = 0;
				/** Refresh the catalogue at most once in this polling cycle. */
				boolean catalogueRefreshAttempted = false;
				if (hashMapFromJSON == null || hashMapFromJSON.isEmpty()) {
					hashMapFromJSON = cache.refreshHashMapFromJson();
					catalogueRefreshAttempted = true;
				}
				/** Validate and categorize work orders */
				for (WorkOrderDTO workOrder : workOrders) {
					Integer activityId = workOrder.getActivityID();
					BigInteger orderTenantId = workOrder.getTenantID();
					if (activityId == null || orderTenantId == null) {
						invalidActivityCount++;
						continue;
					}
					priority = activityPriorityMap.get(BigInteger.valueOf(activityId));
					/** Do not process work orders without priority configuration. */
					if (priority == null) {
						log.warn(
								"Activity configuration for this priority missing. unable to process the order please configure  WorkOrderID={}, ActivityID={}, TenantID={}",
								workOrder.getWorkOrderID(), workOrder.getActivityID(), workOrder.getTenantID());

						continue;
					}
					try {
						List<ActivityDTO> activities = defNet.getActivityCode(activityId.toString(),
								orderTenantId.toString());
						ActivityDTO activityDTO = CollectionUtils.isEmpty(activities) ? null : activities.get(0);
						if (activityDTO == null || !StringUtils.hasText(activityDTO.getActivityCode())) {
							invalidActivityCount++;
							omsNet.updateWorkOrder(workOrder.getWorkOrderID(), "Activity configuration not found",
									cache.intConfigVal(Constants.GENERATION_FAILED, Constants.GLOBAL_TENANT),
									orderTenantId, false);
							continue;
						}
						String activityCode = activityDTO.getActivityCode();
						/** Existing manual activation / special activity handling */
						if (activityCode.equalsIgnoreCase(Constants.RENEWAL) || activityCode.equalsIgnoreCase("OBD")
								|| (StringUtils.hasText(workOrder.getCreatedBy())
										&& workOrder.getCreatedBy().equalsIgnoreCase("MSO"))) {
							omsNet.updateWorkOrder(workOrder.getWorkOrderID(), "Manual Activation", 45, orderTenantId,
									true);
							omsNet.postProvisioningUpdates(workOrder.getWorkOrderID(), activityCode,
									LocalDateTime.now().toString(), tenantID, workOrder.getWorkOrderID());
							omsNet.updateWorkOrder(workOrder.getWorkOrderID(), "Notification not sent !!", 19,
									orderTenantId, true);
							specialOrderCount++;
							continue;
						}
						/** Validate catalogue configuration */
						if (hashMapFromJSON == null || hashMapFromJSON.isEmpty()) {
							if (!catalogueRefreshAttempted) {
								hashMapFromJSON = cache.refreshHashMapFromJson();
								catalogueRefreshAttempted = true;
							}
							if (hashMapFromJSON == null || hashMapFromJSON.isEmpty()) {
								catalogueDeferredCount++;
								continue;
							}
						}
						/** Mark eligible work order as in progress */
						omsNet.updateWorkOrder(workOrder.getWorkOrderID(),
								cache.strConfigVal(Constants.WORK_ORD_REMARKS, Constants.GLOBAL_TENANT),
								cache.intConfigVal(Constants.WORK_ORDER_INPROGRESS, Constants.GLOBAL_TENANT),
								orderTenantId, true);
						/**
						 * Group work orders by activity so the correct activity code is passed to
						 * provisioning request generation.
						 */
						activityWorkOrdersMap.computeIfAbsent(activityId, key -> new ArrayList<>()).add(workOrder);

						activityCodeMap.putIfAbsent(activityId, activityCode);

					} catch (Exception e) {
						invalidActivityCount++;

						log.error("Failed to validate work orders for activity {}", activityId, e);
					}
				}

				/** Log one validation summary instead of logging each work order */
				log.info(
						"Work order validation summary: tenant={}, fetched={}, "
								+ "activityGroups={}, missingPriority={}, invalidActivity={}, "
								+ "specialOrders={}, catalogueDeferred={}",
						tenantID, workOrders.size(), activityWorkOrdersMap.size(), missingPriorityCount,
						invalidActivityCount, specialOrderCount, catalogueDeferredCount);

				/** Process activity groups in ascending priority order */
				for (Map.Entry<Integer, List<WorkOrderDTO>> activityEntry : activityWorkOrdersMap.entrySet()) {
					Integer activityId = activityEntry.getKey();
					List<WorkOrderDTO> activityOrders = activityEntry.getValue();

					// priority = activityPriorityMap.get(activityId);
					priority = activityPriorityMap.get(BigInteger.valueOf(activityId));
					String activityCode = activityCodeMap.get(activityId);

					log.info("Starting {}: activityId={}, activityCode={}, workOrderCount={}",
					 activityId, activityCode, activityOrders.size());
					// TEST-2: Capture the activity priority and work orders entering generator
					// processing
					log.info(
							"TEST-2 | Generator processing: priority={}, activityId={}, activityCode={}, workOrderIDs={}",
							priority, activityId, activityCode,
							activityOrders.stream().map(WorkOrderDTO::getWorkOrderID).collect(Collectors.toList()));
					int requestsSubmitted = 0;

					try {
						/** Retrieve contracts for this activity only */
						List<WorkOrderDTO> ordersWithContracts = omsNet.getContracts(activityOrders);
						if (ordersWithContracts == null || ordersWithContracts.isEmpty()) {
							log.warn("No contracted work orders for activity {}", activityId);
							continue;
						}
						Map<WorkOrderDTO, ArrayList<Contract>> workOrderMap = this.getWorkOrderMap(ordersWithContracts);
						if (workOrderMap == null || workOrderMap.isEmpty()) {
							log.warn("No contract mapping generated for activity {}", activityId);
							continue;
						}
						Map<BigInteger, Map<ProvisioningRequest, List<ProvisioningRequestDetails>>> provisioningMap = this
								.getProvisioningRequestObjects(workOrderMap, activityCode, priority);
						if (provisioningMap == null || provisioningMap.isEmpty()) {
							log.warn("No provisioning requests generated for activity {}", activityId);
							continue;
						}
						/** Submit all requests without clearing the map while iterating */
						for (Map.Entry<BigInteger, Map<ProvisioningRequest, List<ProvisioningRequestDetails>>> requestEntry : provisioningMap
								.entrySet()) {

							ProvisioningRequestThread requestThread = new ProvisioningRequestThread(requestEntry,
									workOrderDao);

							executor.submit(requestThread);
							requestsSubmitted++;
						}
						log.info("Submitted {} provisioning requests for {} " + "(activityId={})", requestsSubmitted,
								(priority), activityId);
					} catch (Exception e) {
						log.error("Failed processing activity group: activityId={}, priority={}", activityId, priority,
								e);
					}
				}

				log.info("Priority-based dispatch completed for tenant {}", tenantID);

			} catch (Exception e) {
				log.error("Error in ParentThread run() for tenant {}", tenantID, e);
			}
		}

		private Map<BigInteger, Map<ProvisioningRequest, List<ProvisioningRequestDetails>>> getProvisioningRequestObjects(
				Map<WorkOrderDTO, ArrayList<Contract>> workOrderMap, String activityCode, Integer priority) {
			log.info("Started ProvisioningServiceImpl :: getProvisioningRequestObjects() ");

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
			BigInteger tenantID = null;
			BigInteger resourceId = null;
			Integer activityID = null;
			BigInteger accountID = null;
			BigInteger customerID = null;
			BigInteger workOrderID;
			String tntWorkOrderID = null;
			boolean parameterFlag = false;
			boolean isInitialOrderForCustomer = false;
			String networkID = "";/** indicates does this order is initial order for customer or not. */
			String errorDescription = "";
			Map<String, String> resourceDTLmap = null;
			Integer customerAccountType = null;
			Iterator<Entry<WorkOrderDTO, ArrayList<Contract>>> it3 = workOrderMap.entrySet().iterator();
			/** Populating data for Service Provision */

			while (it3.hasNext()) {
				/** Iterating Work Orders */
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
					tenantID = workOrder.getTenantID();
					resourceId = workOrder.getResourceDtlsID();
					tntWorkOrderID = workOrder.getTntWorkOrderID();
					customerAccountType = workOrder.getCustomerAccountType();
					ProvDetailsBO provDetailsBO = null;
					/** GET ORDER PARAMETERS */
					if (resourceId == null) {
						provDetailsBO = omsNet.getCustomerDetails(workOrder.getCustomerID(),
								workOrder.getCustomerAccountID(), tenantID, workOrder.getActivityID());
						if (provDetailsBO == null) {
							log.error("No Customer Details Found for CustomerID:- " + workOrder.getCustomerID()
									+ ", AccountID:- " + workOrder.getCustomerAccountID());
							errorDescription = "No Customer Details Found for CustomerID ";
							omsNet.updateWorkOrder(workOrderID, errorDescription,
									cache.intConfigVal(Constants.GENERATION_FAILED, Constants.GLOBAL_TENANT), tenantID,
									false);
							continue;
						}
					}
					String customerDetails = gson.toJson(provDetailsBO);
					Map<Integer, ArrayList<Contract>> contractsMap = workOrder.getContractMap(); // END

					List<String> serviceCreateTypeList = new ArrayList<>(); // Contains service which is of create type

					/**
					 * outerloop: To break, the iteration of contracts of the work order, due to
					 * unavailability of any Order parameters. The provisioning request for the work
					 * order is not generated.
					 */// contractsMap = null;

					List<BigInteger> serviceIdsList = new ArrayList<>();
					if (contractsMap != null) {
						for (int mapKey = 1; mapKey <= contractsMap.size(); mapKey++) {
							listContracts = contractsMap.get(mapKey);
							String ocsKey = "";
							String rpSId = "";

							contractsLoop: for (Contract obj : listContracts) { // Iterating Contracts of each Work
																				// Order
								contracts = obj;

								BigInteger serviceID = contracts.getServiceID();
								List<DeviceBO> deviceList = contracts.getDevices();
								if (!CollectionUtils.isEmpty(deviceList)) {
									Map<String, ProvLobDto> allLobs = catNet.getAllLobs();
									for (Entry<String, ProvLobDto> entryMap : allLobs.entrySet()) {
										ProvLobDto lobObj = entryMap.getValue();
										String lobCode = lobObj.getLobCode();
										if (lobCode.equalsIgnoreCase(Constants.LOB_CODE_MOBILE_MARKET)) {

											serviceID = new BigInteger(Constants.GLOBAL_TENANT);
											serviceIdsList.add(serviceID);
											break;
										}
									}
									if (serviceIdsList.size() > 1) {
										continue contractsLoop;
									}
								}
								keyToCheck = contracts.getRatePlanID() + "::" + contracts.getServiceID();
								serviceParamMap = hashMapFromJSON.get(keyToCheck);
								log.info("keyToCheck: {} hashMapFromJSON.get(keyToCheck)={}", keyToCheck,
										serviceParamMap);
								if (serviceParamMap == null) {
									hashMapFromJSON = cache.refreshHashMapFromJson();
									log.info("Catalogue cache refreshed. Size: {}",
											hashMapFromJSON != null ? hashMapFromJSON.size() : 0);
									errorDescription = "New RatePlan is created, reloading Catalogue Params";
									serviceParamMap = hashMapFromJSON.get(keyToCheck);
									log.info("After Catalogue refresh, keyToCheck: {} serviceParamMap: {}", keyToCheck,
											serviceParamMap);
								}
								if (serviceParamMap != null) {
									// If Rateplan HashMap contains the key then only consider the contract.
									ocsKey = contracts.getRatePlanID().toString();
									rpSId = contracts.getRatePlanID() + Constants.KEY_SEP + contracts.getServiceID();
//									workOrder.getCustomerAccountType();
									List<ProvisioningRequestObjectsBO> provRequesObjectList = cache
											.getProvRequesObjects(serviceID, workOrder.getActivityID(),
													contracts.getActionID(), -1, workOrder.getTenantID(),
													workOrder.getDeviceType());
									if (provRequesObjectList.isEmpty()) {
										provRequesObjectList = cache.getProvRequesObjects(serviceID,
												workOrder.getActivityID(), contracts.getActionID(),
												workOrder.getCustomerAccountType(), workOrder.getTenantID(),
												workOrder.getDeviceType());
										if (provRequesObjectList.isEmpty())
											log.error("provisioning Request Objects not found.");
										errorDescription = "Provisioning Request Objects not found for this activity ";
									}
									/**
									 * List<String> serverNames = provRequesObjectList.stream().map(a ->
									 * a.getServerName()).collect(Collectors.toList()); config
									 */

									serviceLoop: for (ProvisioningRequestObjectsBO provisioningObj : provRequesObjectList) {
										String serverURL = provisioningObj.getServerURL();
										inputJson = provisioningObj.getApiObject();
										if (inputJson != null) {
											if (provisioningObj.getPurpose().equalsIgnoreCase(
													cache.strConfigVal(Constants.CUSTOMER, Constants.GLOBAL_TENANT))) { // only
																														// one
																														// time
																														// customer
																														// is
																														// create
																														// purpose
												isInitialOrderForCustomer = true;
												if (serviceCreateTypeList.contains(provisioningObj.getServerName())) {
													log.info("Customer Requrest Created for the server: {}",
															provisioningObj.getServerName());
													continue serviceLoop;
												} else {
													serviceCreateTypeList.add(provisioningObj.getServerName());
												}
											}
											// Replace Service Parameters
											if (serviceParamMap != null) {
												Iterator<Entry<String, List<Map<String, String>>>> it = serviceParamMap
														.entrySet().iterator();

												while (it.hasNext()) { // Server Iteration in the Rate Plan
													Map.Entry<String, List<Map<String, String>>> entry = it.next();
													list = entry.getValue();
													for (int i = 0; i < list.size(); i++) {
														if (provisioningObj.getInterfaceType()
																.equalsIgnoreCase("RESTFUL")) { // Server List
															inputJson = inputJson.replace(" ", "");
														}
														serverMap = list.get(i);
														// Service Parameters replacement
														Iterator<Entry<String, String>> it1 = serverMap.entrySet()
																.iterator();
														String replaceKey;
														while (it1.hasNext()) {
															Map.Entry<String, String> entry1 = it1.next();
															String Key = entry1.getKey();
															if (inputJson.contains(Key)) {
																if (serverMap.get(Key) != null) {
																	replaceKey = serverMap.get(Key);
																	inputJson = inputJson.replaceAll(
																			cache.strConfigVal(Constants.SERVICE_REGEX,
																					provisioningObj.getTenantID()
																							.toString())
																					+ entry1.getKey().toString(),
																			replaceKey); // json
																}
															}
														}
													}

												}
											}
											inputJson = this.replaceOrderParameters(customerDetails, deviceList,
													workOrder.getTenantID().toString(),
													ocsKey + Constants.KEY_SEP + provisioningObj.getServerName(),
													inputJson, rpSId, provisioningObj.getServerURL(), workOrder);
											/**
											 * @logic TFiberDynamicService is configured that exceute when tfiber tenant
											 *        is not null
											 */
											TFiberDynamicService beanObj = tntCodeRes
													.resolveOrDefault(cache.strConfigVal(Constants.UNQ_TNT_ID,
															provisioningObj.getTenantID().toString()));
											if (beanObj != null) {
												log.info("TFiberDynamicService beanObj {}", beanObj.toString());
												inputJson = beanObj.replaceInputJsonForTenatspecific(inputJson,
														workOrder, provisioningObj);
												log.info(
														"TFiberDynamicService obj is invoked, replaceing tfiber related input json file :: after replacing the object inputJson {} ",
														inputJson);
												if (serverURL.contains(cache.strConfigVal(Constants.ORDER_PARAM,
														provisioningObj.getTenantID().toString()))) {
													log.info("TFiberDynamicService serverURL {} Before replacing ",
															serverURL);
													List<String> OrderparamVal = FileUtil
															.foundResponseReplaceValues(serverURL);
													String omsResponseString = omsNet.getContractAttributesDetails(
															workOrder.getCustomerAccountID(),
															provisioningObj.getServiceID());
													Map<String, String> finalResponseMap = new HashMap<>();
													finalResponseMap = FileUtil
															.convertResponseJsonToMap(omsResponseString, null);
													for (String val : OrderparamVal) {
														String string = finalResponseMap.get(val);
														if (string != null) {
															serverURL = serverURL
																	.replace(
																			cache.strConfigVal(Constants.ORDER_PARAM,
																					provisioningObj.getTenantID()
																							.toString())
																					+ val,
																			finalResponseMap.get(val));
														}
													}
													finalResponseMap.clear();
													log.info("TFiberDynamicService serverURL {} after replacing ",
															serverURL);
												}
											}
											log.info("After replacement completed requestObj {}", inputJson);

											/**
											 * JSON should have NETWORK_ID to be replaced, and request should not be for
											 * creating customer.
											 */
											if (inputJson
													.contains(cache.strConfigVal(Constants.NETWORK_ID,
															provisioningObj.getTenantID().toString()))
													&& !provisioningObj.getPurpose()
															.equalsIgnoreCase(cache.strConfigVal(Constants.CUSTOMER,
																	Constants.GLOBAL_TENANT))) {
												networkID = omsNet.getNetworkID(contracts.getCustomerID(),
														contracts.getCustomerAccountID(), provisioningObj.getLobID(),
														provisioningObj.getServiceID(), tenantID);
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

										/** Replace External ID Parameter */
										if (StringUtils.hasText(serverURL)) {
											log.info("serverl url fetched from request objects  serverURL {} ",
													serverURL);
											if (serverURL.contains(cache.strConfigVal(Constants.EXTERNALID,
													provisioningObj.getTenantID().toString()))) {
												Map deviceParamMap = gson.fromJson(customerDetails, Map.class);
												Iterator it7 = deviceParamMap.entrySet().iterator();
												while (it7.hasNext()) {
													Map.Entry entry7 = (Map.Entry) it7.next();
													String tobeReplace = cache.strConfigVal(Constants.ORDER_REGEX,
															provisioningObj.getTenantID().toString())
															+ entry7.getKey().toString();
													serverURL = serverURL.replaceAll(tobeReplace,
															entry7.getValue().toString());
												}
											}

											/**
											 * @logic This below Logic is used to replace all sereverUrlOrderParam
											 *        Values Dynamically which is coming from oms response
											 */
											if (serverURL.contains(cache.strConfigVal(Constants.ORDER_PARAM,
													provisioningObj.getTenantID().toString()))) {
												List<String> OrderparamVal = FileUtil
														.foundResponseReplaceValues(serverURL);
												/***
												 * String omsResponseString =
												 * omsNet.getContractAttributesDetails(workOrder.getCustomerAccountID(),
												 * provisioningObj.getServiceID());
												 */
												String customerProvDetailss = omsNet.getCustomerProvDetailss(customerID,
														accountID, tenantID, activityID);
												Map<String, String> finalResponseMap = new HashMap<>();
												finalResponseMap = FileUtil
														.convertResponseJsonToMap(customerProvDetailss, null);
												for (String val : OrderparamVal) {
													String string = finalResponseMap.get(val);
													if (string != null) {
														serverURL = serverURL
																.replace(
																		cache.strConfigVal(Constants.ORDER_PARAM,
																				provisioningObj.getTenantID()
																						.toString())
																				+ val,
																		finalResponseMap.get(val));
													}
												}
												finalResponseMap.clear();
												/**
												 * serverURL = orderParamUrlReplacement(deviceList,
												 * workOrder.getTenantID().toString(), serverURL,
												 * ocsKey+Constants.KEY_SEP+provisioningObj.getServerName());
												 */
											}
											/** Replace Network ID Parameter */
											if (serverURL
													.contains(cache.strConfigVal(Constants.NETWORK_ID,
															provisioningObj.getTenantID().toString()))
													|| inputJson.contains(cache.strConfigVal(Constants.NETWORK_ID,
															provisioningObj.getTenantID().toString()))) {
												if (networkID.isEmpty()) {
													networkID = omsNet.getNetworkID(contracts.getCustomerID(),
															contracts.getCustomerAccountID(),
															provisioningObj.getLobID(), provisioningObj.getServiceID(),
															tenantID);
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
													log.error(
															"netowork ID not found for the input customerID: {}, accountID: {}, lobID: {}, serviceId: {}, tenantId: {} ",
															contracts.getCustomerID(), contracts.getCustomerAccountID(),
															provisioningObj.getLobID(), provisioningObj.getServiceID(),
															tenantID);
													omsNet.updateWorkOrder(workOrderID, "NetworkId not found",
															cache.intConfigVal(Constants.GENERATION_FAILED,
																	Constants.GLOBAL_TENANT),
															tenantID, false);
													throw new ProvisionException(
															"Unable to Replace Network Id in url. network id not found from oms.");
												}
											}
										} else {
											log.info("serverl url got null serverURL {} ", serverURL);
										}
										if (inputJson != null) {
											if (inputJson
													.contains(cache.strConfigVal(Constants.SERVICE_PARM,
															provisioningObj.getTenantID().toString()))
													&& inputJson.contains(cache.strConfigVal(Constants.ORDER_PARAM,
															provisioningObj.getTenantID().toString()))) {
												parameterFlag = true;
												log.error(
														"{} ServiceID {} Order && Service Parameters are not replaced !!!  in Work Order  ",
														workOrderID, provisioningObj.getServiceID());
												log.error("{}{}", workOrderID, inputJson);
											} else if (inputJson.contains(cache.strConfigVal(Constants.SERVICE_PARM,
													provisioningObj.getTenantID().toString()))) {
												parameterFlag = true;
												log.error(
														"{} ServiceID {} Service Parameters are not replaced !!!  in Work Order  ",
														workOrderID, provisioningObj.getServiceID());
												log.error("{} {}", workOrderID, inputJson);
											}
//											else if(inputJson.contains(cache.strConfigVal(Constants.ORDER_PARAM, provisioningObj.getTenantID().toString()))){ 
//												parameterFlag = true;											
//												log.error("{} ServiceID {} Order Parameters are not replaced !!!  in Work Order  ",workOrderID,provisioningObj.getServiceID());
//												log.error("{}{}",workOrderID,inputJson);
//											 }
										}
										if (!parameterFlag) {
											ProvisioningRequestDetails provisioningRequestDetails = getProvisioningRequestDetails(
													provisioningObj, contracts, mapKey, inputJson, serverURL,
													tntWorkOrderID, customerAccountType);
											detailsList.add(provisioningRequestDetails);
										}
									}
									if (provisioningRequest == null) { /**
																		 * Only one serviceProvisioningRequest for each
																		 * work order
																		 */
										provisioningRequest = getProvisioningRequest(contracts, workOrder, priority);
									}

								}
							}
						}
					} else {
						log.info(
								"ProvisioningServiceImp class : ContractsMap is null :: filtering data withOut contracts");
						List<ProvisioningRequestObjectsBO> provRequesObjects = cache.getProvRequestObjects(
								workOrder.getActivityID(), workOrder.getActionID(), workOrder.getTenantID());
						String inptJson = "";
						String serverURL = "";
						if (resourceId != null) {
							ResourceDto resourceDetails = omsNet.getResourceDetails(resourceId);
							resourceDTLmap = gson.fromJson(gson.toJson(resourceDetails), Map.class);
						}
						for (ProvisioningRequestObjectsBO requestObjects : provRequesObjects) {
							inptJson = requestObjects.getApiObject();
							serverURL = requestObjects.getServerURL();
							if (resourceDTLmap != null && !resourceDTLmap.isEmpty()) {
								for (Map.Entry<String, String> entry : resourceDTLmap.entrySet()) {
									String toReplace = cache.strConfigVal(Constants.ORDER_REGEX,
											requestObjects.getTenantID().toString()) + entry.getKey().toString();
									inptJson = inptJson.replaceAll(toReplace, entry.getValue());
								}
							}
							if (inptJson.contains(cache.strConfigVal(Constants.ORDER_PARAM,
									requestObjects.getTenantID().toString()))) {
								String customerProvDetailss = omsNet.getCustomerProvDetailss(customerID, accountID,
										tenantID, activityID);
								Map<String, String> finalResponseMap = FileUtil
										.convertResponseJsonToMap(customerProvDetailss, null);
								List<String> ordersParam = FileUtil.foundResponseReplaceValues(inptJson);
								for (String val : ordersParam) {
									String string = finalResponseMap.get(val);
									if (string != null) {
										inptJson = inptJson.replace(
												cache.strConfigVal(Constants.ORDER_PARAM,
														requestObjects.getTenantID().toString()) + val,
												finalResponseMap.get(val));
									}
								}
								finalResponseMap.clear();
							}
							if (activityCode.equalsIgnoreCase("MNP_BROADCAST")) {
								String mnpBroadcastDtls = omsNet.getMnpBroadcastDtls(tenantID, customerID, accountID);
								if (StringUtils.hasText(mnpBroadcastDtls)) {
									Map<String, String> finalResponseMap = FileUtil
											.convertResponseJsonToMap(mnpBroadcastDtls, null);
									List<String> ordersParam = FileUtil.foundResponseReplaceValues(inptJson);
									for (String val : ordersParam) {
										String string = finalResponseMap.get(val);
										if (string != null) {
											inptJson = inptJson.replace(
													cache.strConfigVal(Constants.ORDER_PARAM,
															requestObjects.getTenantID().toString()) + val,
													finalResponseMap.get(val));
										}
									}
									finalResponseMap.clear();
								}

							}
							/**
							 * @logic This below Logic is used to replace all sereverUrlOrderParam Values
							 *        Dynamically which is coming from oms response
							 */
							if (serverURL.contains(cache.strConfigVal(Constants.ORDER_PARAM,
									requestObjects.getTenantID().toString()))) {
								List<String> OrderparamVal = FileUtil.foundResponseReplaceValues(serverURL);
								/***
								 * String omsResponseString =
								 * omsNet.getContractAttributesDetails(workOrder.getCustomerAccountID(),
								 * provisioningObj.getServiceID());
								 */
								String customerProvDetailss = omsNet.getCustomerProvDetailss(customerID, accountID,
										tenantID, activityID);
								Map<String, String> finalResponseMap = new HashMap<>();
								finalResponseMap = FileUtil.convertResponseJsonToMap(customerProvDetailss, null);
								for (String val : OrderparamVal) {
									String string = finalResponseMap.get(val);
									if (string != null) {
										serverURL = serverURL.replace(
												cache.strConfigVal(Constants.ORDER_PARAM,
														requestObjects.getTenantID().toString()) + val,
												finalResponseMap.get(val));
									}
								}
								finalResponseMap.clear();
							}

							/** Replace Network ID Parameter */
							if (serverURL.contains(cache.strConfigVal(Constants.NETWORK_ID,
									requestObjects.getTenantID().toString()))) {
								if (networkID.isEmpty()) {
									networkID = omsNet.getNetworkID(customerID, accountID, requestObjects.getLobID(),
											requestObjects.getServiceID(), tenantID);
								}
								if (networkID != null) {
									serverURL = serverURL.replace(cache.strConfigVal(Constants.ORDER_PARAM,
											requestObjects.getTenantID().toString())
											+ cache.strConfigVal(Constants.NETWORK_ID,
													requestObjects.getTenantID().toString()),
											networkID);
								} else {
									log.error(
											"netowork ID not found for the input customerID: {}, accountID: {}, lobID: {}, serviceId: {}, tenantId: {} ",
											customerID, accountID, requestObjects.getLobID(),
											requestObjects.getServiceID(), tenantID);
									omsNet.updateWorkOrder(workOrderID, "NetworkId not found",
											cache.intConfigVal(Constants.GENERATION_FAILED, Constants.GLOBAL_TENANT),
											tenantID, false);
									throw new ProvisionException(
											"Unable to Replace Network Id in url. network id not found from oms.");
								}
							}
							ProvisioningRequestDetails provisioningRequestDetails = getProvisioningRequestDetails(
									requestObjects, null, 0, inptJson, serverURL, tntWorkOrderID, customerAccountType);
							detailsList.add(provisioningRequestDetails);
							if (provisioningRequest == null) { /**
																 * Only one serviceProvisioningRequest for each work
																 * order
																 */
								provisioningRequest = getProvisioningRequest(null, workOrder, priority);
							}
							if (inptJson.contains(cache.strConfigVal(Constants.ORDER_PARAM,
									requestObjects.getTenantID().toString()))) {
								parameterFlag = true;
								log.error("{} ServiceID {} Order Parameters are not replaced !!!  in Work Order  ",
										workOrderID, requestObjects.getServiceID());
								log.error("{}{}", workOrderID, inptJson);
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
							log.error("getProvisioningRequestObjects ::  ", e);
						}
						omsNet.updateWorkOrder(workOrderID, errorDescription,
								cache.intConfigVal(Constants.GENERATION_FAILED, Constants.GLOBAL_TENANT), tenantID,
								false);
					}
				} catch (Exception e) {
					log.error("ProvisioningServiceImpl :: getReqObjectsbycompareKeys : ", e);
				}
			}
			log.info("FinalMap Size :: {}", finalMap.size());
			return finalMap;
		}

		private String replaceOrderParameters(String customerDetails, List<DeviceBO> deviceList, String tenantID,
				String rpID, String inputJSON, String rpSpId, String url, WorkOrderDTO workOrderDTO) throws Exception {
			// Order parameters replacement
			String orderParam = customerDetails.trim();
			if (orderParam != null) {
				/**
				 * Map deviceParamMap = gson.fromJson(orderParam, Map.class); Iterator it7 =
				 * deviceParamMap.entrySet().iterator(); while (it7.hasNext()) { Map.Entry
				 * entry7 = (Map.Entry)it7.next(); String tobeReplace =
				 * cache.strConfigVal(Constants.ORDER_REGEX,
				 * tenantID)+entry7.getKey().toString(); String dateReplace =
				 * cache.strConfigVal(Constants.ORDER_PARAM, tenantID)+"currentDate"; //
				 * if(url.contains("TisService") && inputJSON.contains("accountID")) { //
				 * inputJSON = inputJSON.replaceAll(tobeReplace,
				 * entry7.getValue().toString()+"87"); // } // else { inputJSON =
				 * inputJSON.replaceAll(tobeReplace, entry7.getValue().toString()); // }
				 * if(inputJSON.contains(dateReplace)) { inputJSON =
				 * inputJSON.replace(dateReplace,FileUtil.getDateFormatter()); }
				 * 
				 * }
				 */
				if (inputJSON.contains(cache.strConfigVal(Constants.ORDER_PARAM, tenantID))) {
					String responseString = omsNet.getCustomerProvDetailss(workOrderDTO.getCustomerID(),
							workOrderDTO.getCustomerAccountID(), workOrderDTO.getTenantID(),
							workOrderDTO.getActivityID());
					Map<String, String> finalResponseMap = FileUtil.convertResponseJsonToMap(responseString, null);
					List<String> ordersParam = FileUtil.foundResponseReplaceValues(inputJSON);

					for (String val : ordersParam) {
						if (inputJSON.contains(cache.strConfigVal(Constants.ORDER_PARAM, tenantID) + val)) {
							log.info("Found input json replacement ");
							String string = finalResponseMap.get(val);
							if (string != null) {
								inputJSON = inputJSON.replace(cache.strConfigVal(Constants.ORDER_PARAM, tenantID) + val,
										finalResponseMap.get(val));
							}
						} else {
							log.info("Key not Found From the response :: ", val);
						}

					}
					finalResponseMap.clear();
				}
			}
			/** Device parameters replacement */
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
						/**
						 * if key doesnt contains in the string how the following else block will
						 * helpfull. else{ API = API.replace(temp,"NotAvailable"); }
						 */
					}

				}
			}
			String ocskey = cache.strConfigVal(Constants.ORDER_PARAM, tenantID)
					+ cache.strConfigVal(Constants.OCS_RP_ID, Constants.GLOBAL_TENANT);
			String ocsSPkey = cache.strConfigVal(Constants.ORDER_PARAM, tenantID) + "OCS_SP_ID";
			String ocsrpid = cache.getOCSRPID(rpID);
			String ocsSpId = cache.getOcsSPId(rpSpId);
			if (ocsrpid == null) {
				ocsrpid = cache.getOCSRPID(rpID);
				if (ocsrpid == null)
					log.info("not a ocs managed RP. ");
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

		private String orderParamUrlReplacement(List<DeviceBO> deviceList, String tenantID, String url, String rpID) {
			for (DeviceBO deviceBO : deviceList) {
				String ky = cache.strConfigVal(Constants.ORDER_PARAM, tenantID) + deviceBO.getDeviceCode()
						+ cache.strConfigVal(Constants.PARAM_SEP, tenantID);
				Map deviceParamMap = gson.fromJson(gson.toJson(deviceBO), Map.class);
				Iterator it7 = deviceParamMap.entrySet().iterator();
				while (it7.hasNext()) {
					Map.Entry entry7 = (Map.Entry) it7.next();
					String temp = ky + entry7.getKey();
					if (url.contains(temp)) {
						if (entry7.getValue() != null) {
							url = url.replace(temp, entry7.getValue().toString());
						} else {
							url = url.replace(temp, cache.strConfigVal(Constants.NOT_AVAILABLE, tenantID));
						}
					}
					/**
					 * if key doesnt contains in the string how the following else block will
					 * helpfull. else{ API = API.replace(temp,"NotAvailable"); }
					 */
				}
				String ocsCodeData = cache.getOcsCodeData(rpID);
				if (url.contains("O|OCS_CODE"))
					url = url.replace("O|OCS_CODE", ocsCodeData);
			}
			return url;
		}

		private Map<WorkOrderDTO, ArrayList<Contract>> getWorkOrderMap(List<WorkOrderDTO> ordersWithContracts) {
			Map<WorkOrderDTO, ArrayList<Contract>> workOrderMap = new HashMap<>();
			ordersWithContracts.stream().forEach(cont -> {
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
						try {
							omsNet.updateWorkOrder(workOrderID, "No Contracts from OMS API",
									cache.intConfigVal(Constants.GENERATION_FAILED, Constants.GLOBAL_TENANT),
									cont.getTenantID(), false);
						} catch (Exception e) {
							log.error("Error while updating workorder", e);
						}
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

			provisioningRequest
					.setCustomerAccountID(workOrder.getCustomerAccountID() != null ? workOrder.getCustomerAccountID()
							: BigInteger.valueOf(-1));
			provisioningRequest.setRequestCreationDate(new Date());
			provisioningRequest.setStatus(cache.intConfigVal(Constants.PROV_REQ_GEN_SUCCESS, Constants.GLOBAL_TENANT));
			provisioningRequest
					.setCreatedBy(cache.strConfigVal(Constants.DEF_CREAT_BY, workOrder.getTenantID().toString()));
			provisioningRequest.setCreatedOn(new Date());
			provisioningRequest.setModifiedOn(new Date());
			provisioningRequest.setActionID(workOrder.getActionID());
			provisioningRequest.setWorkOrderID(workOrder.getWorkOrderID());
			provisioningRequest.setTenantID(workOrder.getTenantID());
			provisioningRequest.setPartnerID(workOrder.getPartnerID());
			provisioningRequest.setActivityID(workOrder.getActivityID());
			provisioningRequest.setNodeID(workOrder.getNodeID());
			provisioningRequest.setPriority(String.valueOf(priority));
			provisioningRequest
					.setResourceDetailId(workOrder.getResourceDtlsID() != null ? workOrder.getResourceDtlsID() : null);
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

			provisioningRequestDetails
					.setRatePlanID(contract != null ? contract.getRatePlanID() : BigInteger.valueOf(-1));
			provisioningRequestDetails
					.setRatePlanPriority(Integer.valueOf(ratePlanPriority) != null ? ratePlanPriority : -1);

			provisioningRequestDetails
					.setServicePackageID(contract != null ? contract.getServicePackageID() : BigInteger.valueOf(-1));
			provisioningRequestDetails.setServerName(provisioningObj.getServerName()); // Check
			provisioningRequestDetails.setTokenID(provisioningObj.getTokenID());
			provisioningRequestDetails.setInterfaceType(provisioningObj.getInterfaceType());
			provisioningRequestDetails.setServerURL(serverURL);
			/** provisioningRequestDetails.setServerURL(provisioningObj.getServerURL()); */
			provisioningRequestDetails.setReqSerRespID(tntWorkOrderID);
			provisioningRequestDetails
					.setStatus(cache.intConfigVal(Constants.PROV_REQ_GEN_SUCCESS, Constants.GLOBAL_TENANT));
			provisioningRequestDetails
					.setVersion(cache.floatConfigVal(Constants.DEFAULT_VERSION, Constants.GLOBAL_TENANT));
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
			provisioningRequestDetails
					.setAddColumnOne(customerAccountType == null ? "-1" : customerAccountType.toString());

			return provisioningRequestDetails;
		}

		@SuppressWarnings("rawtypes")
		private String replaceOrderParameters(String customerDetails, List<DeviceBO> deviceList, String tenantID,
				String rpID, String rpSPID, String inputJSON) throws Exception {
			// Order parameters replacement
			String orderParam = customerDetails.trim();
			if (orderParam != null) {
				Map deviceParamMap = gson.fromJson(orderParam, Map.class);
				Iterator it7 = deviceParamMap.entrySet().iterator();
				while (it7.hasNext()) {
					Map.Entry entry7 = (Map.Entry) it7.next();
					String tobeReplace = cache.strConfigVal(Constants.ORDER_REGEX, tenantID)
							+ entry7.getKey().toString();
					inputJSON = inputJSON.replaceAll(tobeReplace, entry7.getValue().toString());
					// serverURL=serverURL.replaceAll(tobeReplace, entry7.getValue().toString());

				}
			}
			// Device parameters replacement
			if (deviceList != null) {
				for (DeviceBO deviceBO : deviceList) {
					String ky = cache.strConfigVal(Constants.ORDER_PARAM, tenantID) + deviceBO.getDeviceCode()
							+ cache.strConfigVal(Constants.PARAM_SEP, tenantID);
					Map deviceParamMap = gson.fromJson(gson.toJson(deviceBO), Map.class);
					Iterator it7 = deviceParamMap.entrySet().iterator();
					while (it7.hasNext()) {
						Map.Entry entry7 = (Map.Entry) it7.next();
						String temp = ky + entry7.getKey();
						if (inputJSON.contains(temp)) {
							if (entry7.getValue() != null) {
								inputJSON = inputJSON.replace(temp, entry7.getValue().toString());
							} else {
								inputJSON = inputJSON.replace(temp,
										cache.strConfigVal(Constants.NOT_AVAILABLE, tenantID));
							}
						}
//					if key doesnt contains in the string how the following else block will helpfull.
//					else{
//						API =  API.replace(temp,"NotAvailable");
//					}
					}
				}
			}
			String ocskey = cache.strConfigVal(Constants.ORDER_PARAM, tenantID)
					+ cache.strConfigVal(Constants.OCS_RP_ID, Constants.GLOBAL_TENANT);
			String ocsSPkey = cache.strConfigVal(Constants.ORDER_PARAM, tenantID) + "OCS_SP_ID";// cache.strConfigVal(Constants.OCS_RP_ID,
																								// Constants.GLOBAL_TENANT);;
			String ocsrpid = cache.getOCSRPID(rpID);
			String ocsSpId = cache.getOcsSPId(rpSPID);
			if (ocsrpid == null) {
				ocsrpid = cache.getOCSRPID(rpID);
				if (ocsrpid == null)
					log.info("not a ocs managed RP. ");
				return inputJSON;
			}
			if (inputJSON.contains(ocskey) || inputJSON.contains(ocsSPkey)) {
				inputJSON = inputJSON.replace(ocskey, ocsrpid);
				inputJSON = inputJSON.replace(ocsSPkey, ocsSpId);
			}
			return inputJSON;
		}

//	  the below 2 methods are used for changeSim or change sim activity. uncomment
//	  these methods when not using. 
//	  make sure we send an appropriate value for paymentType while fetching provisioningrequestobjects. 

		private Map<BigInteger, Map<ProvisioningRequest, List<ProvisioningRequestDetails>>> getContractDevicesInfo(
				List<WorkOrderDTO> workOrders) {
			Map<BigInteger, Map<ProvisioningRequest, List<ProvisioningRequestDetails>>> data = new HashMap<>();
			String inputJson = "";
			try {
				for (WorkOrderDTO wo : workOrders) {

					BigInteger tenantID = null;
					tenantID = wo.getTenantID();
					Map<ProvisioningRequest, List<ProvisioningRequestDetails>> requests = new HashMap<>();
					ProvisioningRequest provisioningRequest = this.getProvisioningRequest(null, wo, null);
					List<ServiceDevicesBO> serviceDevices = omsNet.devicesAndServicesInfo(wo.getWorkOrderID(),
							tenantID);
					List<ProvisioningRequestDetails> provReqDtls = new ArrayList<>();
					boolean parameterFlag = false;
					for (ServiceDevicesBO dev : serviceDevices) {
						List<ProvisioningRequestObjectsBO> provRequesObjects = cache.getProvRequesObjects(
								dev.getServiceID(), wo.getActivityID(), wo.getActionID(), -1, wo.getTenantID(),
								wo.getDeviceType());
						totalServices += provRequesObjects.size();
						for (ProvisioningRequestObjectsBO obj : provRequesObjects) {
							ProvisioningRequestDetails provisioningRequestDtl = this.getProvisioningRequestDtls(obj,
									dev);
							inputJson = obj.getApiObject();
							String nswoAct = "";
							try {
								ActivityDTO nswo = cache.getActivity(obj.getTenantID().toString(),
										cache.strConfigVal(Constants.NSWO, obj.getTenantID().toString()), null, true);
//							ActivityDTO nswo = defNet.getActivityInfo(cache.strConfigVal(Constants.NSWO, obj.getTenantID().toString()), obj.getTenantID().toString(), null);
								nswoAct = nswo.getActivityID();
							} catch (Exception e) {
								log.error("Failed to fetch New Sales Activity..");
								throw new ProvisionException("Problem in connecting to Definition netork");
							}
							if (inputJson
									.contains(cache.strConfigVal(Constants.NETWORK_ID, wo.getTenantID().toString()))
									&& !wo.getActivityID().equals(nswoAct)) {
								String networkID = omsNet.getNetworkID(wo.getCustomerID(), wo.getCustomerAccountID(),
										obj.getLobID(), dev.getServiceID(), obj.getTenantID());
								if (networkID != null) {
									inputJson = inputJson.replaceAll(cache.strConfigVal(Constants.ORDER_REGEX,
											wo.getTenantID().toString())
											+ cache.strConfigVal(Constants.NETWORK_ID, wo.getTenantID().toString()),
											networkID);
								} else {
//								API =  API.replaceAll("\"client\"[ ]*:[^,}\\]]*[,]?", ""); //the following line will be remove keyvalue pair of networkID but client must be the key
									inputJson = inputJson.replaceAll(
											cache.strConfigVal(Constants.ORDER_REGEX, wo.getTenantID().toString())
													+ cache.strConfigVal(Constants.NETWORK_ID,
															wo.getTenantID().toString()),
											cache.strConfigVal(Constants.N_A, Constants.GLOBAL_TENANT));
								}
							}
							List<Object[]> oldAndNewSimInfo = omsNet.getOldAndNewSimInfo(wo.getWorkOrderID(),
									obj.getTenantID());
							for (Object[] rec : oldAndNewSimInfo) {
								inputJson = inputJson.replaceAll(
										cache.strConfigVal(Constants.ORDER_REGEX, wo.getTenantID().toString())
												+ rec[0].toString(),
										String.valueOf(rec[1]));
								inputJson = inputJson.replaceAll(
										cache.strConfigVal(Constants.ORDER_REGEX, wo.getTenantID().toString())
												+ rec[0].toString(),
										String.valueOf(rec[1]));
							}
							boolean parameterFlag1 = false;
							if (inputJson
									.contains(cache.strConfigVal(Constants.SERVICE_PARM, wo.getTenantID().toString()))
									&& inputJson.contains(
											cache.strConfigVal(Constants.ORDER_PARAM, wo.getTenantID().toString()))) {
								parameterFlag1 = true;
								log.error(wo.getWorkOrderID() + " ServiceID " + obj.getServiceID()
										+ " Order && Service Parameters are not replaced !!!  in Work Order  ");
								log.error(wo.getWorkOrderID() + " " + inputJson);
								// break outerloop;
							} else if (inputJson.contains(
									cache.strConfigVal(Constants.SERVICE_PARM, wo.getTenantID().toString()))) {
								parameterFlag1 = true;
								log.error(wo.getWorkOrderID() + " ServiceID " + obj.getServiceID()
										+ " Service Parameters are not replaced !!!  in Work Order  ");
								log.error(wo.getWorkOrderID() + " " + inputJson);
								// break outerloop;
							} else if (inputJson
									.contains(cache.strConfigVal(Constants.ORDER_PARAM, wo.getTenantID().toString()))) {
								parameterFlag1 = true;
								log.error(wo.getWorkOrderID() + " ServiceID " + obj.getServiceID()
										+ " Order Parameters are not replaced !!!  in Work Order  ");
								log.error(wo.getWorkOrderID() + " " + inputJson);
								// break outerloop;
							}
							if (!parameterFlag1) {
								provisioningRequestDtl.setRequestObject(inputJson);
								provReqDtls.add(provisioningRequestDtl);
							}
						}
					}
					// the only case parameter flag will be true when total services are not equlas
					// to total
					parameterFlag = provReqDtls.size() != 0 && provReqDtls.size() != totalServices;
					if (provReqDtls.size() == totalServices) {
						requests.put(provisioningRequest, provReqDtls);
						data.put(wo.getWorkOrderID(), requests);
					} else {
						ProvisioningRequestGenerationError obj = new ProvisioningRequestGenerationError();
						String errorDescription = "";
						if (parameterFlag) {
							errorDescription = cache.strConfigVal(Constants.PARAM_REPLACE_ERROR,
									wo.getTenantID().toString());// "Parameter are not replaced, check error logs with
																	// workOrderID";
							obj.setErrorDescription(inputJson);
						} else if (provReqDtls.isEmpty()) {
							errorDescription = cache.strConfigVal(Constants.CONTRACTS_NOT_IN_CAT,
									wo.getTenantID().toString()); // "All the Contracts are not available in
																	// Catalogue.";
							obj.setErrorDescription(errorDescription);
						} else {
							errorDescription = cache.strConfigVal(Constants.DEFAULT_ERROR, wo.getTenantID().toString());// "Error
																														// while
																														// generating
																														// request
																														// !!!";
							obj.setErrorDescription(errorDescription);
						}
						omsNet.updateWorkOrder(wo.getWorkOrderID(), errorDescription,
								cache.intConfigVal(Constants.GENERATION_FAILED, Constants.GLOBAL_TENANT), tenantID,
								false);
					}
					totalServices = 0;
				}

			} catch (Exception e) {
				log.error("Error from ProvisioningServiceImpl - getContractDevicesInfo() ", e);
			}

			return data;

		}

		private ProvisioningRequestDetails getProvisioningRequestDtls(ProvisioningRequestObjectsBO obj,
				ServiceDevicesBO dev) {
			ProvisioningRequestDetails prov = new ProvisioningRequestDetails();
			prov.setDependentSequenceID(obj.getDependentSequenceID());
			prov.setDependentSubSequenceID(obj.getDependentSubSequenceID());
			prov.setInterfaceType(obj.getInterfaceType());
			prov.setLobID(obj.getLobID());
			prov.setLobPriority(obj.getLobProvisioningPriority());
			prov.setMaxRetries(obj.getMaxRetries());
			prov.setRatePlanID(dev.getRatePlanID());
			try {
				prov.setRatePlanPriority(cache.intConfigVal(Constants.GLOBAL_FLAG, obj.getTenantID().toString()));
			} catch (Exception e) {
				log.error("Might be Global flag configuration missed. please check it.");
			}
			prov.setRequestCreationDate(new Date());
			prov.setResponseUpdateCategory(obj.getResponseUpdateCategory());
			prov.setSequenceID(obj.getSequenceID());
			prov.setServerName(obj.getServerName());
			prov.setServerURL(obj.getServerURL());
			prov.setServiceID(dev.getServiceID());
			prov.setServicePackageID(dev.getServicePackageID());
			prov.setServicePriority(obj.getServiceProvisioningPriority());
			prov.setStatus(20);
			prov.setSubSequenceID(obj.getSubSequenceID());
			prov.setTimeGapBetweenRetry(obj.getTimeGapBetweenRetry());
			prov.setTokenID(obj.getTokenID());
			return prov;
		}

	}
}
