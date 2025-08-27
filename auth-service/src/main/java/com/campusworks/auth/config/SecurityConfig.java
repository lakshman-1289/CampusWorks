package com.campusworks.auth.config;

import com.campusworks.auth.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableWebSecurity
@Slf4j
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CorsConfigurationSource corsConfigurationSource;
    
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                         @Qualifier("corsConfigurationSource") CorsConfigurationSource corsConfigurationSource) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.corsConfigurationSource = corsConfigurationSource;
        log.info("SecurityConfig initialized with JwtAuthenticationFilter and CorsConfigurationSource");
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        log.info("Creating BCryptPasswordEncoder bean");
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        log.info("Configuring SecurityFilterChain");
        
        http.csrf(csrf -> {
            log.debug("Disabling CSRF protection");
            csrf.disable();
        })
        .cors(cors -> {
            log.debug("Configuring CORS with custom configuration source");
            cors.configurationSource(corsConfigurationSource);
        })
        .sessionManagement(session -> {
            log.debug("Setting session creation policy to STATELESS");
            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        })
        .authorizeHttpRequests(auth -> {
            log.info("Configuring authorization rules");
            log.debug("Permitting access to public endpoints: /api/auth/register, /api/auth/login, /api/auth/verify-email, /api/auth/resend-verification, /api/auth/forgot-password, /api/auth/reset-password, /api/auth/logout, /api/auth/health, /api/auth/test");
            log.debug("Permitting access to actuator endpoints: /actuator/**");
            log.debug("Requiring authentication for protected endpoints: /api/auth/change-password, /api/auth/me");
            
            auth
                // Public endpoints - no authentication required
                .requestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/verify-email", 
                               "/api/auth/resend-verification", "/api/auth/forgot-password", 
                               "/api/auth/reset-password", "/api/auth/logout", "/api/auth/health", "/api/auth/test").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                // Protected endpoints - JWT authentication required
                .requestMatchers("/api/auth/change-password", "/api/auth/me").authenticated()
                .anyRequest().authenticated();
        })
        // Add JWT filter before UsernamePasswordAuthenticationFilter
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        log.info("SecurityFilterChain configured successfully");
        return http.build();
    }
}