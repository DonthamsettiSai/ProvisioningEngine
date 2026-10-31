package com.knot.provisioning;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.security.ProviderException;

import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;



@RestControllerAdvice
public class RestOrderControlerAdvice extends RequestBodyAdviceAdapter{
	
	@Value("${header.key}")
	private String headerKey;
	
	private static final Logger log = LogManager.getLogger(RestOrderControlerAdvice.class);
	
	@Override
	public boolean supports(MethodParameter methodParameter, Type targetType,
			Class<? extends HttpMessageConverter<?>> converterType) {
		return methodParameter.hasParameterAnnotations();
	}
	
	@Override
	public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter parameter, Type targetType,
			Class<? extends HttpMessageConverter<?>> converterType) throws IOException {
		log.debug("Entered into RestOrderControlerAdvice :: beforeBodyRead()");
		byte[] decryptedBytes = null;
		HttpHeaders headers=inputMessage.getHeaders();
		if(headers.containsKey(headerKey)) {
			return new DecodeHttpInputMessage(headers, inputMessage.getBody());
		}else {
			try (InputStream inputStream = inputMessage.getBody()) {
				decryptedBytes=CryptoConfiguration.decryptByte(Base64.decodeBase64(StreamUtils.copyToByteArray(inputStream)));
			} catch (Exception e) {
				throw new ProviderException("Unauthorized Request");
			}
			return new DecodeHttpInputMessage(inputMessage.getHeaders(), new ByteArrayInputStream(decryptedBytes));
		}
		
		
	}


}
