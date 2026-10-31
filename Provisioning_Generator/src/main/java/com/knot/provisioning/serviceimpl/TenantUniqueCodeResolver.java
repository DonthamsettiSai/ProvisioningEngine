package com.knot.provisioning.serviceimpl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.knot.provisioning.service.TFiberDynamicService;

@Component
public class TenantUniqueCodeResolver {

	/** Spring will inject: beanName -> bean instance */
	private final Map<String, TFiberDynamicService> generatorsByBeanName;

	/** Our routing map: tenantKey/alias -> bean instance */
	private final Map<String, TFiberDynamicService> routing;

	public TenantUniqueCodeResolver(Map<String, TFiberDynamicService> generators) {
		this.generatorsByBeanName = Collections.unmodifiableMap(new HashMap<>(generators));
		this.routing = buildRoutingMap(generatorsByBeanName);
	}

	private Map<String, TFiberDynamicService> buildRoutingMap(Map<String, TFiberDynamicService> generators) {
		Map<String, TFiberDynamicService> map = new HashMap<>();
		generators.forEach((beanName, svc) -> map.put(beanName.toLowerCase(Locale.ROOT), svc));
		TFiberDynamicService tfiberSvc = generators.get("tfiberService"); 
		if (tfiberSvc != null) {
			map.put("tfiber", tfiberSvc);
			
		}
		return Collections.unmodifiableMap(map);
	}

	public TFiberDynamicService resolveOrDefault(String tenantId) {
		if (tenantId == null)
			return null; // or return a default
		return routing.get(tenantId.toLowerCase(Locale.ROOT));
	}
}
