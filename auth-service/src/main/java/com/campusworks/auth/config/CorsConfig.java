package com.campusworks.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Value("${app.frontend.additional-urls:http://localhost:3001}")
    private String additionalUrls;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Parse and set allowed origins
        List<String> allowedOrigins = Arrays.asList(frontendUrl);
        
        // Add additional URLs if configured
        if (additionalUrls != null && !additionalUrls.trim().isEmpty()) {
            String[] additionalUrlArray = additionalUrls.split(",");
            allowedOrigins = new java.util.ArrayList<>(allowedOrigins);
            for (String url : additionalUrlArray) {
                allowedOrigins.add(url.trim());
            }
        }
        configuration.setAllowedOriginPatterns(allowedOrigins);
        
        // Configure other CORS settings
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}