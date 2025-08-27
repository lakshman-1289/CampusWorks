package com.campusworks.task.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {
    private String secret;
    private long expMinutes = 7200; // 2 hours default

    public long getExpirationTimeInSeconds() {
        return expMinutes * 60;
    }
}