package com.campusworks.auth.security;

import com.campusworks.auth.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Slf4j
public class JwtService {

    private final JwtProperties jwtProperties;

    private SecretKey getKey() {
        String secret = jwtProperties.getSecret();
        log.debug("Getting key from JWT properties. Secret length: {}", secret != null ? secret.length() : 0);
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generate token with custom claims
     */
    public String generateToken(String subject, Map<String, Object> claims) {
        Instant now = Instant.now();
        log.debug("Generating token for subject: {}", subject);
        log.debug("Claims: {}", claims);
        
        String token = Jwts.builder()
                .setSubject(subject)
                .addClaims(claims)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(jwtProperties.getExpMinutes() * 60)))
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
                
        log.debug("Generated token (first 20 chars): {}", token.length() > 20 ? token.substring(0, 20) + "..." : token);
        return token;
    }

    /**
     * Generate token with user information (for auth-service specific needs)
     */
    public String generateUserToken(Long userId, String email, String firstName, String lastName, 
                                   List<String> roles, boolean emailVerified) {
        log.debug("Generating user token for userId: {}, email: {}", userId, email);
        Map<String, Object> claims = Map.of(
            "email", email,
            "firstName", firstName,
            "lastName", lastName,
            "roles", roles,
            "emailVerified", emailVerified
        );
        return generateToken(userId.toString(), claims);
    }

    /**
     * Validate JWT token
     */
    public boolean validateToken(String token) {
        try {
            log.debug("Validating token...");
            log.debug("Token length: {}", token.length());
            log.debug("Using secret length: {}", jwtProperties.getSecret() != null ? jwtProperties.getSecret().length() : 0);
            
            Jwts.parserBuilder()
                    .setSigningKey(getKey())
                    .build()
                    .parseClaimsJws(token);
                    
            log.debug("Token validation successful");
            return true;
        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            log.debug("Token validation failed with exception", e);
            return false;
        }
    }

    /**
     * Extract claims from token
     */
    public Claims extractClaims(String token) {
        log.debug("Extracting claims from token...");
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        log.debug("Extracted claims: {}", claims);
        return claims;
    }

    /**
     * Extract specific claim from token
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        log.debug("Extracting specific claim from token...");
        final Claims claims = extractClaims(token);
        T result = claimsResolver.apply(claims);
        log.debug("Extracted claim result: {}", result);
        return result;
    }

    /**
     * Extract user ID from token
     */
    public Long extractUserId(String token) {
        log.debug("Extracting user ID from token...");
        String subject = extractClaim(token, Claims::getSubject);
        log.debug("Extracted subject: {}", subject);
        Long userId = Long.valueOf(subject);
        log.debug("Converted to userId: {}", userId);
        return userId;
    }

    /**
     * Get expiration time in seconds
     */
    public long getExpirationTimeInSeconds() {
        long expTime = jwtProperties.getExpirationTimeInSeconds();
        log.debug("Getting expiration time: {} seconds", expTime);
        return expTime;
    }

    /**
     * Extract token from Authorization header
     */
    public String extractTokenFromHeader(String authHeader) {
        log.debug("Extracting token from header: {}", authHeader);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("Header is null or doesn't start with 'Bearer '");
            return null;
        }
        String token = authHeader.substring(7);
        log.debug("Extracted token (first 20 chars): {}", token.length() > 20 ? token.substring(0, 20) + "..." : token);
        return token;
    }
}