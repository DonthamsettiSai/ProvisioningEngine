package com.knot.provisioning.daoimpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import com.knot.provisioning.bo.ProvisioningRequestObjectsBO;
import com.knot.provisioning.dao.CacheDao;
import com.knot.provisioning.model.ActivityConfig;
import com.knot.provisioning.utill.Constants;

@Repository
public class CacheDaoImpl implements CacheDao {
	
	private static final Logger log = LogManager.getLogger(CacheDaoImpl.class);


	private static final String TENANT_ID = "tenantID";
	private static final String CODE = "code";
	private static final String NA = "NA";
	private EntityManager em;

	@Autowired
	public void setEm(EntityManager em) {
		this.em = em;
	}


	@Cacheable(value="PROV_CONFIGURATIONS", key = "#code + '::' + #tenantID")
	@Override
	public String strConfigVal(final String code, final String tenantID) {
		
		log.info("Cache LOG FOR -- CODE $ {} AND TENANT $ {}", code, tenantID);
		log.debug("Enterd in CacheDaoImpl :: strConfigVal() Cache LOG FOR -- CODE $ {} AND TENANT $ {}", code, tenantID);

		String returnVal = NA;
		StringBuilder builder = new StringBuilder();
		builder.append("Select CONFIG_VALUE from prov_configurations ")
				.append(" where current_date() between effective_from and effective_to")
				.append(" and tenant_id = :tenantID  and code = :code ");
		try {
			Query q = em.createNativeQuery(builder.toString());
			q.setParameter(TENANT_ID, tenantID);
			q.setParameter(CODE, code);
			@SuppressWarnings("unchecked")
			List<String> resultList = q.getResultList();
			returnVal = resultList.isEmpty() ? NA : resultList.get(0);
	       log.info("Ended CacheDaoImpl :: strConfigVal() ");
		} catch (Exception e) {
			log.error(" Error {}", e.getMessage());
		}
	
		return returnVal;
	}
	
	@Override
	public Integer intConfigVal(String code, String tenantID) {
		String strVal = strConfigVal(code, tenantID);
		if (strVal != null) {
			return Integer.parseInt(strVal);
		}
		throw new RuntimeException(code + Constants.KEY_SEP + tenantID + " is not configured. ");
	}
	
	@Override
	public Long longConfigVal(String code, String tenantID) {
		String strVal = strConfigVal(code, tenantID);
		if (strVal != null) {
			return Long.parseLong(strVal);
		}
		throw new RuntimeException(code + Constants.KEY_SEP + tenantID + " is not configured. ");
	}


	@Override
	public Float floatConfigVal(String code, String tenantID) {
		String strVal = strConfigVal(code, tenantID);
		if (strVal != null) {
			return Float.parseFloat(strVal);
		}
		throw new RuntimeException(code + Constants.KEY_SEP + tenantID + " is not configured. ");
	}
	
	@SuppressWarnings("unchecked")
	@Override
	@Cacheable(value = "PROV_CONFIGURATIONS", key = "'getProvRequesObjects'")
	public List<ProvisioningRequestObjectsBO> getProvRequesObjects() {
		Query modelQuery = null;
		List<ProvisioningRequestObjectsBO> provisioningReqObj = new ArrayList<>();
		StringBuilder builder = new StringBuilder();
		try {
			builder.append("SELECT * FROM provisioning_request_objects ").append(" where  STATUS = :status ");
			builder.append(" ORDER BY SEQUENCE_ID,SUB_SEQUENCE_ID");
			modelQuery = em.createNativeQuery(builder.toString(), ProvisioningRequestObjectsBO.class);
			modelQuery.setParameter("status",intConfigVal(Constants.PROVI_REQ_OBJ_STATUS, Constants.GLOBAL_TENANT));
			provisioningReqObj = modelQuery.getResultList();
			if (provisioningReqObj == null) {
				return  Collections.emptyList();
			}
		} catch (Exception e) {
			log.error("Error from WorkOrderDaoImpl - getProvRequesObjects() ", e);
			return Collections.emptyList();
		}
		return provisioningReqObj;
	}
	
	@Override	
	   public String configValByCodeForGraceFullStop(final String code, final String tenantID) {
			log.info("Started Cache CacheDaoImpl :: configValByCode() LOG FOR -- CODE $ {} AND TENANT $ {}", code, tenantID);
			log.debug(" Entered in CacheDaoImpl :: configValByCode() CODE  {} AND TENANT  {}", code, tenantID);
			String returnVal = NA;
			if (!StringUtils.hasText(code) || !StringUtils.hasText(tenantID)) {
				returnVal = NA;
			}

			StringBuilder builder = new StringBuilder();
			builder.append("Select CONFIG_VALUE from prov_configurations ")
					.append(" where current_date() between effective_from and effective_to")
					.append(" and tenant_id = :tenantID  and code = :code ");
			try {
				Query q = em.createNativeQuery(builder.toString());
				q.setParameter(TENANT_ID, tenantID);
				q.setParameter(CODE, code);
				@SuppressWarnings("unchecked")
				List<String> resultList = q.getResultList();
				returnVal = resultList.isEmpty() ? NA : resultList.get(0);
				log.debug("Response From prov_configurations :: CONFIG_VALUE  ",returnVal);
				log.info("Ended CacheDaoImpl :: configValByCode()");
			} catch (Exception e) {
				log.error(" Error {}", e.getMessage());
			}
		
			return returnVal;
		}
	
	@Override
	@Cacheable(value = "PROV_CONFIGURATIONS", key = "'PROV_ACTIVITY_PRIORITES'")
	public List<ActivityConfig> getAllActvityConfigs(){
		log.info("Started CacheDaoImpl :: getAllActvityConfigs() ");
		StringBuilder builder = new StringBuilder();
		List<ActivityConfig> priorityList = null;
		try {
			builder.append(" SELECT * FROM  provisioning_activity_priorities WHERE NOW() BETWEEN EFFECTIVE_FROM AND EFFECTIVE_TO");
			Query q = em.createNativeQuery(builder.toString(), ActivityConfig.class);
			priorityList = q.getResultList();
			log.info("Ended CacheDaoImpl :: getAllActvityConfigs() ");
		} catch (Exception e) {
			log.error("An Exception Occured in  :: getAllActvityConfigs() ", e);
		}
		return priorityList;
	}
	

}
