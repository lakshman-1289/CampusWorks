package com.campusworks.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import jakarta.annotation.PostConstruct;
import lombok.Data;

/**
 * JWT Configuration properties loaded from GitHub via Config Server
 * Values come from application.properties in GitHub repository
 */
@Component
@RefreshScope
@ConfigurationProperties(prefix = "security.jwt")
@Data
@Slf4j
public class JwtProperties {
    
    // No fallback values - must be loaded from Config Server
    private String secret;
    private long expMinutes;
    
    /**
     * Verify configuration after properties are loaded
     */
    @PostConstruct
    public void logConfiguration() {
        log.info("=== JWT Configuration Status ===");
        log.info("Secret loaded: {}", secret != null && !secret.isEmpty() ? "YES (length: " + secret.length() + ")" : "NO");
        log.info("Expiration minutes: {}", expMinutes);
        log.info("Expiration seconds: {}", getExpirationTimeInSeconds());
        
        if (secret == null || secret.isEmpty()) {
            log.error("CRITICAL: JWT secret is not loaded from GitHub configuration!");
        }
        if (expMinutes <= 0) {
            log.error("CRITICAL: JWT expiration time is not properly configured!");
        }
        
        if (secret != null && !secret.isEmpty() && expMinutes > 0) {
            log.info("✅ JWT Configuration loaded successfully from GitHub");
        } else {
            log.error("❌ JWT Configuration is incomplete - check Config Server connection");
        }
        log.info("===============================");
    }
    
    /**
     * Get expiration time in seconds
     */
    public long getExpirationTimeInSeconds() {
        return expMinutes * 60;
    }
}