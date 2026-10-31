package com.knot.provisioning;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.ProviderException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.google.gson.JsonObject;


@Component
public class CustomResponseFilter implements Filter {

	
	private static final String REQUEST_OBJECT = "RequestObject";
	Environment environment;
	@Autowired
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}


	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		ResponseWrapper responseWrapper = new ResponseWrapper((HttpServletResponse) response);
		HttpServletRequest httpServletRequest = (HttpServletRequest) request;

			
		if (httpServletRequest.getHeader(environment.getProperty("header.key")) != null || httpServletRequest.getRequestURI().contains("swagger")||
                httpServletRequest.getRequestURI().contains("/v3/api-docs")) {
			chain.doFilter(httpServletRequest, response);
		} else {
			if (httpServletRequest.getMethod().equals("GET")) {
				
				if(httpServletRequest.getHeader(REQUEST_OBJECT) != null && httpServletRequest.getHeader(REQUEST_OBJECT).length() > 0) {
					String replaceKey = httpServletRequest.getHeader(REQUEST_OBJECT);
					String decryptVal = CryptoConfiguration.decrypt(replaceKey);
					decryptVal=decryptVal.replace("\"", "").replace("= ", "=");
					StringBuffer newURI = httpServletRequest.getRequestURL().append("?"+decryptVal);
					String[] url = newURI.toString().split("/provgen");
					request.getRequestDispatcher(url[1]).forward(request, responseWrapper);
				} else if (request.getParameterMap().size() == 0) {
					chain.doFilter(httpServletRequest, responseWrapper);
				} else {
					throw new ProviderException("Unauthorized Request");
				}
			} else {
				// Pass the wrapped request and response along the filter chain
				chain.doFilter(httpServletRequest, responseWrapper);
			}

			// Get the response body as byte[]
			String responseBody = responseWrapper.getResponseBody();
			String encryMessage = CryptoConfiguration.encrypt(responseBody);
			JsonObject encryResponse = new JsonObject();
			encryResponse.addProperty("data", encryMessage);
			response.setContentType("application/json");
			response.resetBuffer();
			response.setCharacterEncoding("UTF-8");
			response.setContentLength(encryResponse.toString().getBytes(StandardCharsets.UTF_8).length);
			response.getOutputStream().write(encryResponse.toString().getBytes(StandardCharsets.UTF_8));
			response.getOutputStream().flush();

		}
	}

}
