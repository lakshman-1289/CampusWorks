package com.campusworks.auth.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT Authentication Filter to validate Bearer tokens
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTHORIZATION_HEADER = "Authorization";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        String requestURI = request.getRequestURI();
        String method = request.getMethod();
        String queryString = request.getQueryString();
        log.info("==== Incoming Request ====");
        log.info("Method: {} | URI: {} | Query: {}", method, requestURI, queryString);
        
        // Log all headers
        log.debug("Request Headers:");
        request.getHeaderNames().asIterator().forEachRemaining(headerName -> {
            log.debug("  {}: {}", headerName, request.getHeader(headerName));
        });
        
        // Check if this request should be filtered
        boolean shouldSkip = shouldNotFilter(request);
        log.info("Should skip JWT validation: {}", shouldSkip);
        
        if (shouldSkip) {
            log.info("Skipping JWT validation for public endpoint: {}", requestURI);
            filterChain.doFilter(request, response);
            log.info("==== Request Completed (Skipped JWT) ====");
            return;
        }
        
        try {
            String authHeader = request.getHeader(AUTHORIZATION_HEADER);
            log.info("Authorization header present: {}", authHeader != null);
            
            if (authHeader == null) {
                log.warn("❌ NO AUTHORIZATION HEADER - Returning 401 for protected endpoint: {}", requestURI);
                // Let Spring Security handle the 401 response
                filterChain.doFilter(request, response);
                log.info("==== Request Completed (No Auth Header) ====");
                return;
            }
            
            String token = extractTokenFromRequest(request);
            log.info("Extracted token from Authorization header: {}", token != null ? "[TOKEN_PRESENT]" : "null");
            
            if (token != null) {
                log.info("Validating JWT token...");
                boolean isValid = jwtService.validateToken(token);
                log.info("JWT token validation result: {}", isValid ? "✅ VALID" : "❌ INVALID");
                
                if (isValid) {
                    log.info("Creating authentication from valid token...");
                    Authentication authentication = createAuthentication(token);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.info("✅ JWT authentication successful and context set");
                    log.info("Authenticated user ID: {}", authentication.getPrincipal());
                    log.info("User authorities: {}", authentication.getAuthorities());
                } else {
                    log.warn("❌ JWT token validation failed - Clearing security context");
                    // Clear security context on authentication failure
                    SecurityContextHolder.clearContext();
                }
            } else {
                log.warn("❌ NO VALID TOKEN EXTRACTED - Clearing security context");
                // Clear security context when no token
                SecurityContextHolder.clearContext();
            }
        } catch (Exception e) {
            log.error("❌ JWT authentication failed with exception", e);
            // Clear security context on authentication failure
            SecurityContextHolder.clearContext();
        }
        
        log.info("Continuing filter chain...");
        filterChain.doFilter(request, response);
        log.info("==== Request Completed ====");
    }

    /**
     * Extract JWT token from Authorization header
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        log.debug("Raw Authorization header: {}", authHeader);
        
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            String token = authHeader.substring(BEARER_PREFIX.length());
            log.debug("Extracted token (first 20 chars): {}", token.length() > 20 ? token.substring(0, 20) + "..." : token);
            return token;
        }
        
        log.warn("Authorization header does not start with 'Bearer ' prefix");
        return null;
    }

    /**
     * Create Authentication object from validated JWT token
     */
    private Authentication createAuthentication(String token) {
        log.debug("Extracting claims from token...");
        Claims claims = jwtService.extractClaims(token);
        
        String userId = claims.getSubject();
        String email = claims.get("email", String.class);
        
        log.debug("Extracted userId from token: {}", userId);
        log.debug("Extracted email from token: {}", email);
        
        // Extract roles from claims
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) claims.get("roles");
        log.debug("Extracted roles from token: {}", roles);
        
        List<SimpleGrantedAuthority> authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());

        log.debug("Created authorities: {}", authorities);

        // Create authentication with userId as principal name
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userId, null, authorities);
        log.debug("Created authentication token with principal: {}", authToken.getPrincipal());
        return authToken;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        log.debug("Checking if path should be skipped: {}", path);
        
        // Skip JWT validation for public endpoints
        boolean shouldSkip = path.equals("/api/auth/register") ||
               path.equals("/api/auth/login") ||
               path.equals("/api/auth/verify-email") ||
               path.equals("/api/auth/resend-verification") ||
               path.equals("/api/auth/forgot-password") ||
               path.equals("/api/auth/reset-password") ||
               path.equals("/api/auth/logout") ||
               path.equals("/api/auth/health") ||
               path.equals("/api/auth/test") ||
               path.startsWith("/actuator/");
               
        log.debug("Path {} should be skipped: {}", path, shouldSkip);
        return shouldSkip;
    }
}