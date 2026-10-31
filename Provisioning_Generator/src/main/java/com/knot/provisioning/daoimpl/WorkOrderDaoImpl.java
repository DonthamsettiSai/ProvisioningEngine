package com.knot.provisioning.daoimpl;



import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.StoredProcedureQuery;
import jakarta.transaction.Transactional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.google.gson.Gson;
import com.knot.provisioning.bo.ProvisioningRequestObjectsBO;
import com.knot.provisioning.dao.CacheDao;
import com.knot.provisioning.dao.WorkOrderDao;
import com.knot.provisioning.model.ProvisioningRequest;
import com.knot.provisioning.model.ProvisioningRequestDetails;
import com.knot.provisioning.model.ProvisioningRequestGenerationError;
import com.knot.provisioning.network.OMSNetwork;
import com.knot.provisioning.utill.Constants;
import com.knot.provisioning.utill.FileUtil;

@Repository
public class WorkOrderDaoImpl implements WorkOrderDao {

	

	private static final String STATUS2 = "status";

	private static final Logger log = LogManager.getLogger(WorkOrderDaoImpl.class);

	private EntityManager em;
	private CacheDao cache;
	private Gson gson;
	
	@Autowired
	public void setCache(CacheDao cache) {
		this.cache = cache;
	}

	public EntityManager getEm() {
		return em;
	}

	@PersistenceContext
	public void setEm(EntityManager em) {
		this.em = em;
	}

	
	private OMSNetwork omsNet;

	@Autowired
	public void setOmsNet(OMSNetwork omsNet) {
		this.omsNet = omsNet;
	}
	
	@Autowired
	public void setGson(Gson gson) {
		this.gson = gson;
	}

	@Override
	@Transactional
		
		public boolean saveProvisioningRequestObjects(Entry<BigInteger, Map<ProvisioningRequest, List<ProvisioningRequestDetails>>> entry) throws Exception {log.info("Started WorkOrderDaoImpl :: saveProvisioningRequestObjects() ");
		String sequenceNo;

		BigInteger seqNo;

		BigInteger workOrderID = entry.getKey();
		Map<ProvisioningRequest, List<ProvisioningRequestDetails>> provMap = entry.getValue();
		Iterator<Entry<ProvisioningRequest, List<ProvisioningRequestDetails>>> it = provMap.entrySet().iterator();
		int status = 0;
		FileUtil util =new FileUtil();
		ProvisioningRequest provisioningRequest = null;
		String tenantReqId = "";
		BigInteger tenantID = null;
		String inputJson = "";
		String description = "";
		while (it.hasNext()) {

			BigInteger requestID = BigInteger.ZERO;
			try {
				log.debug("Entered in While loop condtion To save The request and request Details ");
				Map.Entry<ProvisioningRequest, List<ProvisioningRequestDetails>> entry1 = it.next();
				sequenceNo = this.getSeqNumber(cache.strConfigVal(Constants.PROV_REQ_SEQ_NAME, Constants.GLOBAL_TENANT));

				seqNo = new BigInteger(sequenceNo);
				requestID = seqNo;
				provisioningRequest = entry1.getKey();
				provisioningRequest.setRequestID(seqNo);
				tenantID = provisioningRequest.getTenantID();
				tenantReqId = util.getFormatName(Constants.METHOD,String.valueOf(provisioningRequest.getTenantID()));
				provisioningRequest.setTenantRequestID(tenantReqId);
				log.debug("Saving the Provisioning Request objects :: requestObj {}", gson.toJson(provisioningRequest));
//				provisioningRequest = em.merge(provisioningRequest);
				log.debug("Saved SucessFully ::::::::: ");

				List<ProvisioningRequestDetails> detailsList = entry1.getValue();
				for (ProvisioningRequestDetails Obj : detailsList) {
					Obj.setRequestID(seqNo);
					sequenceNo = this.getSeqNumber(cache.strConfigVal(Constants.PROV_REQ_DTL_SEQ_NAME, Constants.GLOBAL_TENANT));

					BigInteger seqNo1 = new BigInteger(sequenceNo);
					if(Obj.getRequestObject() !=null && Obj.getRequestObject().contains(Constants.REQ_DTL_ID)) {
						 inputJson = Obj.getRequestObject();
						String orderParam = cache.strConfigVal(Constants.ORDER_PARAM, tenantID.toString());
						 if(inputJson.contains(orderParam+Constants.REQ_DTL_ID) || inputJson.contains("r|"+Constants.REQ_DTL_ID)) {
							 inputJson = inputJson.replace(orderParam+Constants.REQ_DTL_ID, String.valueOf(seqNo1));	
							 inputJson = inputJson.replace("r|"+Constants.REQ_DTL_ID, String.valueOf(seqNo1));
							 Obj.setRequestObject(inputJson);
							 }
					} if(inputJson.contains(cache.strConfigVal(Constants.ORDER_PARAM, tenantID.toString()))){ 
						log.error("Order Parameters are not replaced !!! for Work Order  {}", workOrderID);
						log.error("inputJson {} ",inputJson);
						description = cache.strConfigVal(Constants.PARAM_REPLACE_ERROR, tenantID.toString());
						status = cache.intConfigVal(Constants.GENERATION_FAILED, Constants.GLOBAL_TENANT);
						omsNet.updateWorkOrder(workOrderID,description,status, tenantID, false);
				}else {
					Obj.setRequestDetailID(seqNo1);
					log.debug("Saving The Provisioning Request Details :: requestDetailsObj {}", gson.toJson(provisioningRequest));
					em.merge(Obj);
					provisioningRequest = em.merge(provisioningRequest);
					status = cache.intConfigVal(Constants.REQUEST_GEN_SUCCESS, Constants.GLOBAL_TENANT);
					description = "Generation successd";
					log.debug("Saved SucessFully ::::::::: ");
				}
			}

//				status = cache.intConfigVal(Constants.REQUEST_GEN_SUCCESS, Constants.GLOBAL_TENANT);
				log.info("Ended WorkOrderDaoImpl :: saveProvisioningRequestObjects() ");
			} catch (Exception e) {
				log.error("Error from WorkOrderDaoImpl - saveProvisioningRequestObjects() ", e);
				status = cache.intConfigVal(Constants.GENERATION_FAILED, Constants.GLOBAL_TENANT);
			} finally {
				try {
					omsNet.updateWorkOrder(workOrderID, description, status, provisioningRequest.getTenantID(), true);
					log.info(" request  created sucessFully for workOrder :: {}" ,provisioningRequest.getWorkOrderID() );
				} catch (Exception e) {
					log.error("Error from WorkOrderDaoImpl - updateWorkOrderError() ", e);
					status = cache.intConfigVal(Constants.GENERATION_FAILED, Constants.GLOBAL_TENANT);

				}
			}
			if (status == cache.intConfigVal(Constants.REQUEST_GEN_SUCCESS, Constants.GLOBAL_TENANT)) {
				return generateGroups(requestID);
			}
		}
		return false;
	}


		private boolean generateGroups(BigInteger requestID) {
		try {
			log.info("Start WorkOrderDaoImpl :: generateGroups()" );
			Query query = em.createNativeQuery("call UpdateGrpSubGrps( :requestID, @res )");
			query.setParameter("requestID", requestID);
			query.executeUpdate();
			return updateProvRequest(requestID, cache.intConfigVal(Constants.NEGOTIATION_PROGRESS, Constants.GLOBAL_TENANT));
		} catch (Exception e) {
			log.error("Error from WorkOrderDaoImpl - generateGroups() ", e);
		}
		return false;
	}

	public String getSeqNumber(String id) {
		try {
			log.info("Sequence No creation for :- {}!!!",id);
			StoredProcedureQuery query = em.createStoredProcedureQuery("GET_SEQUENCE_ID");
			query.registerStoredProcedureParameter("P_IDNAME", String.class, ParameterMode.IN);
			query.registerStoredProcedureParameter("P_NEXTID", String.class, ParameterMode.OUT);
			query.registerStoredProcedureParameter("P_RESULT", String.class, ParameterMode.OUT);
			query.setParameter("P_IDNAME", id);
			query.execute();
			String sequenceNo = (String) query.getOutputParameterValue("P_NEXTID");
			log.info("sequence Number :: {}" ,sequenceNo);
			return sequenceNo;
		} catch (Exception e) {
			log.error("Error from WorkOrderDaoImpl - getSeqNumber() ", e);

			return null;
		}
	}

	@Transactional

		public boolean updateProvRequest(BigInteger requestID, int status) {
		boolean flag = false;
		StringBuilder builder = new StringBuilder();
		try {
			log.info("Strated WorkOrderDaoImpl :: updateProvRequest() ");
			log.debug("Enterd WorkOrderDaoImpl :: updateProvRequest() requestID {}, status {}",requestID , status);
			builder.append("UPDATE provisioning_requests SET STATUS = :status ");
			builder.append(" WHERE REQUEST_ID = :requestID ");
			Query modelQuery = em.createNativeQuery(builder.toString());
			modelQuery.setParameter(STATUS2, status).setParameter("requestID", requestID);
			modelQuery.executeUpdate();
			flag = true;
			log.debug("Response Result Flag excueted by ", flag);
			log.info("Ended WorkOrderDaoImpl :: updateProvRequest() ");
		} catch (Exception e) {
			log.error("Error from WorkOrderDaoImpl - updateProvRequest() ", e);
		}
		return flag;
	}

	@Transactional
	public boolean saveProvisioningRequestGenerationError(ProvisioningRequestGenerationError obj) {
		try {
			log.info("Started WorkOrderDaoImpl :: saveProvisioningRequestGenerationError () ");
			log.debug("Entred in WorkOrderDaoImpl :: saveProvisioningRequestGenerationErrorobj {}", gson.toJson(obj));
			String sequenceNo = this.getSeqNumber(cache.strConfigVal(Constants.ERROR_REC_ID_SEQ_NAME, Constants.GLOBAL_TENANT));

			BigInteger seqNo = new BigInteger(sequenceNo);
			log.debug("sequenceNo {}", seqNo);
			obj.setErrorRecordID(seqNo);
			em.merge(obj);
			log.info("Ended WorkOrderDaoImpl :: saveProvisioningRequestGenerationError () ");
			return true;
		} catch (Exception e) {
			log.error("Error from WorkOrderDaoImpl - saveProvisioningRequestGenerationError() ", e);
			return false;
		}
	}

	@SuppressWarnings("unchecked")
	public List<ProvisioningRequestObjectsBO> getProvRequesObjects(Integer serviceID, Integer activityID,
			Integer actionID, Integer paymentID) {
		Query modelQuery = null;
		List<ProvisioningRequestObjectsBO> provisioningReqObj = new ArrayList<>();
		StringBuilder builder = new StringBuilder();
		try {
			log.info("Started WorkOrderDaoImpl :: getProvRequesObjects () ");
			log.debug("Enterd into WorkOrderDaoImpl :: getProvRequesObjects () By Passing as Arguments serviceID {}, activityID {}, actionID {}, paymentID {}", serviceID, activityID, actionID, paymentID);
			builder.append("SELECT PROVISIONING_REQUEST_ID,ACTIVITY_ID,SERVICE_ID,PAYMENT_TYPE,TENANT_ID,PARTNER_ID,LOB_ID,MAX_RETRIES,TIME_GAP_BETWEEN_RETRY,")
			.append("SEQUENCE_ID,SUB_SEQUENCE_ID,DEPENDENT_SEQUENCE_ID,DEPENDENT_SUB_SEQUENCE_ID,LOB_PROVISIONING_PRIORITY,SERVICE_PROVISIONING_PRIORITY,")
			.append("ACTION_ID,INTERFACE_TYPE,OBJECT_TYPE,TOKEN_ID,SERVER_URL,EXECUTION_ORDER,REQUEST_OBJECT,PURPOSE,WORK_ORDER_TYPE,EFFECTIVE_FROM,")
			.append("EFFECTIVE_TO,STATUS,CREATED_BY,CREATED_ON,CREATED_IPADDR,MODIFIED_BY,MODIFIED_ON,MODIFIED_IPADDR,RESPONSE_UPDATE_CATEGORY,SERVER_NAME")
			.append(",REQUEST_METHOD,TIME_GAP_UNIT,IS_ASYNC,PROCESS_TYPE FROM provisioning_request_objects WHERE SERVICE_ID = :serviceID");
			builder.append(" AND ACTIVITY_ID = :activityID ");
			builder.append(" AND ACTION_ID = :actionID ");
			builder.append(" and payment_type in ( :paymentID , -1 ) ");
			builder.append(" AND STATUS = :status ");
			builder.append(" ORDER BY SEQUENCE_ID,SUB_SEQUENCE_ID");
			modelQuery = em.createNativeQuery(builder.toString(), ProvisioningRequestObjectsBO.class);
			modelQuery.setParameter("serviceID", serviceID).setParameter("activityID", activityID)
					.setParameter("actionID", actionID).setParameter("paymentID", paymentID)
					.setParameter(STATUS2, cache.intConfigVal(Constants.PROVI_REQ_OBJ_STATUS, Constants.GLOBAL_TENANT));
			provisioningReqObj = modelQuery.getResultList();
			if (provisioningReqObj == null) {
				return  Collections.emptyList();
			}
			log.debug("ProvisioningReqObj Response Result List size {} ", provisioningReqObj.size());
			log.info("Ended WorkOrderDaoImpl :: getProvRequesObjects () ");
		} catch (Exception e) {
			log.error("Error from WorkOrderDaoImpl - getProvRequesObjects() ", e);
			return Collections.emptyList();
		}
		return provisioningReqObj;
	}

}
