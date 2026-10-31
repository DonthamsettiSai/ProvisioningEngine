package com.knot.provisioning;



import java.lang.reflect.InvocationTargetException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.knot.jwt.security.JwtRequestFilter;
 
@EnableWebSecurity
@Configuration
class WebSecurityConfig {
 
    JwtRequestFilter jwtRequestFilter;

    @Autowired
    public void setJwtRequestFilter(JwtRequestFilter jwtRequestFilter) {
        this.jwtRequestFilter = jwtRequestFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        try {
            http
                .cors(cors -> {})
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authorize -> authorize
                		.requestMatchers(
                        		"/favicon.ico", "/swagger*", "/swagger.json", "/swagger-resources/**", "/swagger-*/**",
        						"/webjars/**", "/v3/api-docs/**", "/swagger-ui.html"
                        )
                    .permitAll()
                    .anyRequest()
                    .authenticated()
                )
                .headers(headers -> headers
                    .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true))
                    .frameOptions(frame -> frame.sameOrigin())
                    .addHeaderWriter(new StaticHeadersWriter("X-XSS-Protection", "1; mode=block"))
                )
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            cause.printStackTrace(); // This will show the actual exception
        }
        return http.build();
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**").allowedOrigins("*");
            }
        };
    }
}



