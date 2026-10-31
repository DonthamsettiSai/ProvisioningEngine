package com.knot.provisioning.utill;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/**
 * 
 * @author Sai D
 *
 */

public class AuthenticationUtil {
	
	private static final Logger logger = LogManager.getLogger(AuthenticationUtil.class);

	private AuthenticationUtil() {
		// TODO
	}

	public static <T> HttpEntity<T> getHeaderWithTokenAndObject(String token, T t) {
		HttpHeaders headers = new HttpHeaders();
		Properties perProperties = loadProperties();
		if (token != null && !token.isEmpty())
			headers.add("Authorization", "Bearer " + token);
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.add(perProperties.getProperty("header.key"), "true");
		String trxId = ThreadContext.get("TRX_ID");
		if (trxId != null) {
			headers.add("TransactionId", trxId);
		}
		return new HttpEntity<>(t, headers);
	}

	public static HttpEntity<String> getHeaderWithToken(String token) {
		HttpHeaders headers = new HttpHeaders();
		Properties perProperties = loadProperties();
		if (token != null && !token.isEmpty())
			headers.add("Authorization", "Bearer " + token);
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.add(perProperties.getProperty("header.key"), "true");
		String trxId = ThreadContext.get("TRX_ID");
		if (trxId != null) {
			headers.add("TransactionId", trxId);
		}
		return new HttpEntity<>(headers);
	}

	private static Properties loadProperties() {
		Properties properties = new Properties();
		try (InputStream inputStream = AuthenticationUtil.class.getClassLoader()
				.getResourceAsStream("application.properties")) {
			if (inputStream != null) {
				properties.load(inputStream);
			}
		} catch (IOException e) {
			logger.error("The Exception AuthenticationUtil :: loadProperties()", e);
		}

		return properties;
	}

}
