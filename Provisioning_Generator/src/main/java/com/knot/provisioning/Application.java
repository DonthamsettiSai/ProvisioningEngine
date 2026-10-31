package com.knot.provisioning;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
//import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import com.knot.jwt.security.JwtRequestFilter;
import com.knot.jwt.security.JwtService;
import com.knot.provisioning.service.ProvisioningService;

/**
 * 
 * @author sai
 *
 */

@SpringBootApplication
@EnableCaching
public class Application {

	private static final Logger log = LogManager.getLogger(Application.class);

	public static void main(String[] args) {
		try {
			String hostname = java.net.InetAddress.getLocalHost().getHostName();
			ThreadContext.put("HOST_NAME", hostname);
		} catch (Exception e) {
			ThreadContext.put("HOST_NAME", "UNKNOWN");
		}
		ThreadContext.put("TRX_ID", "API-REST-" + java.util.UUID.randomUUID());
		ApplicationContext context = SpringApplication.run(Application.class, args);
		try {
			context.getBean(ProvisioningService.class).generateProvisioningRequests();
		} catch (Exception e) {
			log.error("Reason for Exiting the Application is:- ", e);
		}
	}

	@Bean
	 JwtService jwtService() {
		return new JwtService();
	}

	@Bean
	 JwtRequestFilter jwtRequestFilter() {
		return new JwtRequestFilter();
	}

}
