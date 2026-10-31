package com.knot.provisioning;

import java.net.InetAddress;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;

import com.knot.jwt.security.JwtRequestFilter;
import com.knot.jwt.security.JwtService;
import com.knot.provisioning.service.ProvisioningService;

@SpringBootApplication
@EnableCaching
public class Application {

    private static final Logger log = LogManager.getLogger(Application.class);

    public static void main(String[] args) {
        String hostname = resolveHostName();
        ThreadContext.put("HOST_NAME", hostname);
        ThreadContext.put("TRX_ID", "API-REST-" + UUID.randomUUID());
        log.info("Starting Provisioning Engine Application on host={}", hostname);
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public ApplicationRunner provisioningStartupRunner(ProvisioningService provisioningService) {
        return args -> {
            String hostname = resolveHostName();
            log.info("Provisioning startup runner invoked on host={}, threadName={}", hostname,
                    Thread.currentThread().getName());
            try {
                provisioningService.generateProvisioningRequests();
            } catch (Exception e) {
                log.error("Provisioning startup failed on host={}", hostname, e);
            }
        };
    }

    @Bean
    public JwtService jwtService() {
        return new JwtService();
    }

    @Bean
    public JwtRequestFilter jwtRequestFilter() {
        return new JwtRequestFilter();
    }

    private static String resolveHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }
}
