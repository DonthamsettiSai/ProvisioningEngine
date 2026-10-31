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
    private OMSNetwork omsNet;

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
    public boolean saveProvisioningRequestObjects(
            Entry<BigInteger, Map<ProvisioningRequest, List<ProvisioningRequestDetails>>> entry) throws Exception {
        if (entry == null || entry.getKey() == null || entry.getValue() == null || entry.getValue().isEmpty()) {
            log.warn("saveProvisioningRequestObjects called with empty entry. workOrderId={}",
                    entry != null ? entry.getKey() : null);
            return false;
        }

        BigInteger workOrderID = entry.getKey();
        if (isDuplicateRequestAlreadySaved(workOrderID)) {
            log.info("Duplicate provisioning request already exists for workOrderId={}; skipping save.", workOrderID);
            return false;
        }

        Map<ProvisioningRequest, List<ProvisioningRequestDetails>> provMap = entry.getValue();
        Iterator<Entry<ProvisioningRequest, List<ProvisioningRequestDetails>>> iterator = provMap.entrySet().iterator();
        int status = 0;
        String description = "";
        BigInteger lastRequestId = null;

        while (iterator.hasNext()) {
            BigInteger requestID = BigInteger.ZERO;
            ProvisioningRequest provisioningRequest = null;
            try {
                Map.Entry<ProvisioningRequest, List<ProvisioningRequestDetails>> entry1 = iterator.next();
                if (entry1 == null || entry1.getKey() == null || entry1.getValue() == null) {
                    log.warn("Skipping empty provisioning request record for workOrderId={}", workOrderID);
                    continue;
                }

                String sequenceNo = this.getSeqNumber(cache.strConfigVal(Constants.PROV_REQ_SEQ_NAME, Constants.GLOBAL_TENANT));
                if (sequenceNo == null || sequenceNo.trim().isEmpty()) {
                    throw new IllegalStateException("Failed to generate request sequence number for workOrderId=" + workOrderID);
                }

                requestID = new BigInteger(sequenceNo);
                lastRequestId = requestID;
                provisioningRequest = entry1.getKey();
                provisioningRequest.setRequestID(requestID);
                BigInteger tenantID = provisioningRequest.getTenantID();
                String tenantReqId = new FileUtil().getFormatName(Constants.METHOD,
                        String.valueOf(provisioningRequest.getTenantID()));
                provisioningRequest.setTenantRequestID(tenantReqId);

                List<ProvisioningRequestDetails> detailsList = entry1.getValue();
                for (ProvisioningRequestDetails obj : detailsList) {
                    if (obj == null) {
                        continue;
                    }
                    obj.setRequestID(requestID);
                    String detailSeqNo = this.getSeqNumber(cache.strConfigVal(Constants.PROV_REQ_DTL_SEQ_NAME, Constants.GLOBAL_TENANT));
                    if (detailSeqNo == null || detailSeqNo.trim().isEmpty()) {
                        throw new IllegalStateException("Failed to generate detail sequence number for workOrderId=" + workOrderID);
                    }
                    BigInteger detailId = new BigInteger(detailSeqNo);
                    obj.setRequestDetailID(detailId);

                    String inputJson = obj.getRequestObject();
                    if (inputJson != null && inputJson.contains(Constants.REQ_DTL_ID)) {
                        String orderParam = cache.strConfigVal(Constants.ORDER_PARAM, tenantID.toString());
                        if (inputJson.contains(orderParam + Constants.REQ_DTL_ID)
                                || inputJson.contains("r|" + Constants.REQ_DTL_ID)) {
                            inputJson = inputJson.replace(orderParam + Constants.REQ_DTL_ID, String.valueOf(detailId));
                            inputJson = inputJson.replace("r|" + Constants.REQ_DTL_ID, String.valueOf(detailId));
                            obj.setRequestObject(inputJson);
                        }
                    }
                    if (inputJson != null && inputJson.contains(cache.strConfigVal(Constants.ORDER_PARAM, tenantID.toString()))) {
                        log.error("Order parameters not replaced for workOrderId={}, tenantId={}, inputJson={}",
                                workOrderID, tenantID, inputJson);
                        description = cache.strConfigVal(Constants.PARAM_REPLACE_ERROR, tenantID.toString());
                        status = cache.intConfigVal(Constants.GENERATION_FAILED, Constants.GLOBAL_TENANT);
                        omsNet.updateWorkOrder(workOrderID, description, status, tenantID, false);
                        continue;
                    }
                    em.merge(obj);
                }

                em.merge(provisioningRequest);
                status = cache.intConfigVal(Constants.REQUEST_GEN_SUCCESS, Constants.GLOBAL_TENANT);
                description = "Generation succeeded";
                log.info("Provisioning request saved successfully. workOrderId={}, requestId={}, tenantId={}",
                        workOrderID, requestID, tenantID);
            } catch (Exception e) {
                log.error("Error while saving provisioning request for workOrderId={}", workOrderID, e);
                status = cache.intConfigVal(Constants.GENERATION_FAILED, Constants.GLOBAL_TENANT);
                description = cache.strConfigVal(Constants.DEFAULT_ERROR,
                        provisioningRequest != null && provisioningRequest.getTenantID() != null
                                ? provisioningRequest.getTenantID().toString()
                                : Constants.GLOBAL_TENANT);
            } finally {
                try {
                    if (provisioningRequest != null && provisioningRequest.getTenantID() != null) {
                        omsNet.updateWorkOrder(workOrderID, description, status, provisioningRequest.getTenantID(), true);
                    }
                } catch (Exception updateEx) {
                    log.error("Error while updating work order status. workOrderId={}", workOrderID, updateEx);
                }
            }

            if (status == cache.intConfigVal(Constants.REQUEST_GEN_SUCCESS, Constants.GLOBAL_TENANT)
                    && lastRequestId != null) {
                return generateGroups(lastRequestId);
            }
        }
        return false;
    }

    private boolean isDuplicateRequestAlreadySaved(BigInteger workOrderID) {
        if (workOrderID == null) {
            return true;
        }
        try {
            Query query = em.createQuery("SELECT COUNT(pr) FROM ProvisioningRequest pr WHERE pr.workOrderID = :workOrderID");
            Long count = (Long) query.setParameter("workOrderID", workOrderID).getSingleResult();
            return count != null && count > 0;
        } catch (Exception e) {
            log.warn("Unable to validate duplicate request existence for workOrderId={}", workOrderID, e);
            return false;
        }
    }

    private boolean generateGroups(BigInteger requestID) {
        try {
            log.info("Start WorkOrderDaoImpl :: generateGroups() requestId={}", requestID);
            Query query = em.createNativeQuery("call UpdateGrpSubGrps( :requestID, @res )");
            query.setParameter("requestID", requestID);
            query.executeUpdate();
            return updateProvRequest(requestID, cache.intConfigVal(Constants.NEGOTIATION_PROGRESS, Constants.GLOBAL_TENANT));
        } catch (Exception e) {
            log.error("Error from WorkOrderDaoImpl - generateGroups()", e);
        }
        return false;
    }

    public String getSeqNumber(String id) {
        try {
            log.info("Sequence No creation for :- {}!!!", id);
            StoredProcedureQuery query = em.createStoredProcedureQuery("GET_SEQUENCE_ID");
            query.registerStoredProcedureParameter("P_IDNAME", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_NEXTID", String.class, ParameterMode.OUT);
            query.registerStoredProcedureParameter("P_RESULT", String.class, ParameterMode.OUT);
            query.setParameter("P_IDNAME", id);
            query.execute();
            String sequenceNo = (String) query.getOutputParameterValue("P_NEXTID");
            log.info("sequence Number :: {}", sequenceNo);
            return sequenceNo;
        } catch (Exception e) {
            log.error("Error from WorkOrderDaoImpl - getSeqNumber()", e);
            return null;
        }
    }

    @Transactional
    public boolean updateProvRequest(BigInteger requestID, int status) {
        boolean flag = false;
        StringBuilder builder = new StringBuilder();
        try {
            log.info("Starting updateProvRequest() requestID={}, status={}", requestID, status);
            builder.append("UPDATE provisioning_requests SET STATUS = :status ");
            builder.append(" WHERE REQUEST_ID = :requestID ");
            Query modelQuery = em.createNativeQuery(builder.toString());
            modelQuery.setParameter(STATUS2, status).setParameter("requestID", requestID);
            modelQuery.executeUpdate();
            flag = true;
            log.info("Completed updateProvRequest() requestID={}, status={}", requestID, status);
        } catch (Exception e) {
            log.error("Error from WorkOrderDaoImpl - updateProvRequest()", e);
        }
        return flag;
    }

    @Transactional
    public boolean saveProvisioningRequestGenerationError(ProvisioningRequestGenerationError obj) {
        try {
            log.info("Started WorkOrderDaoImpl :: saveProvisioningRequestGenerationError ()");
            if (obj == null) {
                return false;
            }
            String sequenceNo = this.getSeqNumber(cache.strConfigVal(Constants.ERROR_REC_ID_SEQ_NAME, Constants.GLOBAL_TENANT));
            if (sequenceNo == null) {
                return false;
            }
            BigInteger seqNo = new BigInteger(sequenceNo);
            obj.setErrorRecordID(seqNo);
            em.merge(obj);
            log.info("Ended WorkOrderDaoImpl :: saveProvisioningRequestGenerationError ()");
            return true;
        } catch (Exception e) {
            log.error("Error from WorkOrderDaoImpl - saveProvisioningRequestGenerationError()", e);
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
                return Collections.emptyList();
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
