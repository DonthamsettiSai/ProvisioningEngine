package com.knot.provisioning;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.web.client.RestTemplate;

import com.google.gson.Gson;

@Configuration
public class ConfigLoader {

	

	@Bean
	@Order(1)
	 RestTemplate restTemplate() {
		return new RestTemplate();
	}
	
	@Bean
	@Order(2)
	 Gson gson() {
		return new Gson();
	}
	
	

}
