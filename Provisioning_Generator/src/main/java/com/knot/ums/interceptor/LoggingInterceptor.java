package com.knot.ums.interceptor;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import org.apache.logging.log4j.ThreadContext;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class LoggingInterceptor implements HandlerInterceptor {

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {

		String hostname;
		try {
			hostname = InetAddress.getLocalHost().getHostName();
			ThreadContext.put("HOST_NAME", hostname);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

		boolean flag = false;
		String transaction_Id = request.getHeader("TransactionId");

		if (transaction_Id != null && !transaction_Id.isEmpty()) {
			ThreadContext.put("TRX_ID", transaction_Id);
			flag = true;
		} else {
			Date date = new Date();
			SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy");
			String transID = "API-REST-" + sdf.format(date) + "-" + UUID.randomUUID();
			ThreadContext.put("TRX_ID", transID);
			flag = true;

		}
		return flag;
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
			Exception ex) {
		ThreadContext.clearAll();
	}
}