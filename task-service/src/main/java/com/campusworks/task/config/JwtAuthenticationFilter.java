package com.campusworks.task.config;

import com.campusworks.task.service.SecurityUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProperties jwtProperties;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        log.debug("Processing JWT authentication for request: {} {}", request.getMethod(), request.getRequestURI());
        
        // Try to get user ID from gateway headers first (for service-to-service calls)
        String userId = request.getHeader("X-User-Id");
        
        if (userId != null) {
            log.debug("Found user info in gateway headers");
            // Request came through API Gateway with user info
            String userEmail = request.getHeader("X-User-Email");
            String userRoles = request.getHeader("X-User-Roles");
            
            List<SimpleGrantedAuthority> authorities = userRoles != null ?
                List.of(userRoles.split(",")).stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toList()) :
                List.of(new SimpleGrantedAuthority("ROLE_USER"));

            UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken(userId, null, authorities);
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("Authentication set from gateway headers for user: {}", userId);
        } else {
            log.debug("No gateway headers found, checking for JWT token");
            // Direct call to service, try JWT token
            String token = getTokenFromRequest(request);
            
            if (token != null && validateToken(token)) {
                log.debug("Valid JWT token found, extracting claims");
                Claims claims = getClaimsFromToken(token);
                String userIdFromToken = claims.getSubject();
                
                @SuppressWarnings("unchecked")
                List<String> roles = (List<String>) claims.get("roles");
                
                List<SimpleGrantedAuthority> authorities = roles != null ? 
                    roles.stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .collect(Collectors.toList()) : 
                    List.of(new SimpleGrantedAuthority("ROLE_USER"));

                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(userIdFromToken, null, authorities);
                
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("Authentication set from JWT token for user: {}", userIdFromToken);
            } else {
                log.debug("No valid JWT token found or token validation failed");
            }
        }
        
        filterChain.doFilter(request, response);
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(jwtProperties.getSecret().getBytes()).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            log.debug("JWT token validation failed: {}", e.getMessage());
            return false;
        }
    }

    private Claims getClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(jwtProperties.getSecret().getBytes())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}