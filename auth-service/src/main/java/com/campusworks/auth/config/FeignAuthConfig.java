package com.campusworks.auth.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

@Configuration
public class FeignAuthConfig {
    @Bean
    public RequestInterceptor bearerAuthRequestInterceptor() {
        return requestTemplate -> {
            try {
                RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
                if (attrs != null) {
                    Object auth = attrs.getAttribute("Authorization", RequestAttributes.SCOPE_REQUEST);
                    if (auth instanceof String authHeader) {
                        requestTemplate.header("Authorization", authHeader);
                    }
                }
            } catch (Exception ignored) {}
        };
    }
}


