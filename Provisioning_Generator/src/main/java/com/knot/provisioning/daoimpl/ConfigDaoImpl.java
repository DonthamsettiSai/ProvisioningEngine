package com.knot.provisioning.daoimpl;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.knot.provisioning.bo.ConfigBo;
import com.knot.provisioning.model.ActivityConfig;
import com.knot.provisioning.utill.Constants;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

@Repository
public class ConfigDaoImpl {
	
	public static final Logger log = LogManager.getLogger(ConfigDaoImpl.class);
	
	
	private EntityManager em;
	@Autowired
	public void setEm(EntityManager em) {
		this.em = em;
	}

	@SuppressWarnings("unchecked")
	public List<ConfigBo> getAllConfigs(String tenantID) {
		StringBuilder builder = new StringBuilder();
		log.info("Started ConfigDaoImpl :: getAllConfigs() ");
		try {
			log.debug("Enterd  ConfigDaoImpl :: getAllConfigs() by passing as argument tenantID {}", tenantID);
			builder.append("Select CONFIG_ID,TENANT_ID,EFFECTIVE_FROM,CODE,CONFIG_VALUE from prov_configurations where current_date between effective_from and effective_to and status = "+Constants.ACTIVE_STATUS)
			.append(tenantID != null && tenantID.isEmpty() ? " and tenant_id = :tenantID" : "");
			Query q = em.createNativeQuery(builder.toString(), ConfigBo.class);
			if(tenantID != null && tenantID.isEmpty()) q.setParameter("tenantID", tenantID);
			return q.getResultList();
		} catch (Exception e) {
			log.error("CustomerCreationDaoImpl :: getAllConfigs() ", e);
			throw e;
		}
	}
	
}
