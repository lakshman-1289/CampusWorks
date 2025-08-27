# Spring Cloud Config Server Setup Guide

## Overview
This guide explains how to set up Spring Cloud Config Server with GitHub integration and automatic refresh capabilities for the CampusWorks microservices project.

## Architecture
```
GitHub Repository (campusworks-config)
    ↓
Config Server (Port 8888)
    ↓
Microservices (with @RefreshScope)
```

## Step 1: Create GitHub Configuration Repository

1. Create a new GitHub repository named `campusworks-config`
2. Add configuration files for each service:

```
campusworks-config/
├── application.properties           # Global configuration
├── application-dev.properties       # Global dev configuration
├── application-prod.properties      # Global prod configuration
├── auth-service.properties          # Auth service default
├── auth-service-dev.properties      # Auth service dev
├── auth-service-prod.properties     # Auth service prod
├── task-service.properties
├── task-service-dev.properties
├── bidding-service.properties
└── ... (other services)
```

## Step 2: Environment Variables

Set the following environment variables:

```bash
# Config Server GitHub Integration
CONFIG_GIT_URI=https://github.com/your-username/campusworks-config.git
CONFIG_GIT_USERNAME=your-github-username
CONFIG_GIT_TOKEN=your-github-personal-access-token

# Config Server Security
CONFIG_SERVER_USERNAME=admin
CONFIG_SERVER_PASSWORD=secure-password

# Eureka Discovery
EUREKA_URI=http://localhost:8761/eureka/

# JWT Configuration (in GitHub config files)
JWT_SECRET=your-256-bit-secret-key
JWT_EXPIRATION_MINUTES=120
```

## Step 3: Start Services in Order

1. **Eureka Server** (Port 8761)
```bash
cd eureka-server
mvn spring-boot:run
```

2. **Config Server** (Port 8888)
```bash
cd config-server
mvn spring-boot:run
```

3. **Auth Service** (Port 9001)
```bash
cd auth-service
mvn spring-boot:run
```

## Step 4: Test Configuration

### Check Config Server
```bash
# Get auth-service dev configuration
curl http://admin:admin123@localhost:8888/auth-service/dev

# Get auth-service prod configuration  
curl http://admin:admin123@localhost:8888/auth-service/prod
```

### Test RefreshScope
```bash
# Check current JWT config
curl http://localhost:9001/config/jwt

# Update configuration in GitHub, then refresh
curl -X POST http://localhost:9001/actuator/refresh

# Check updated JWT config
curl http://localhost:9001/config/jwt
```

## Step 5: GitHub Webhook Setup (Optional)

1. Go to your `campusworks-config` repository settings
2. Add webhook: `http://your-server:8888/webhook/github`
3. Select "application/json" content type
4. Choose "Just the push event"

This enables automatic configuration refresh when you push changes to GitHub.

## Configuration Examples

### auth-service-dev.properties
```properties
# JWT Configuration
security.jwt.secret=CAMPUSWORKS_DEV_SECRET_256_BITS
security.jwt.expMinutes=120
security.jwt.issuer=campusworks-dev

# Database
spring.datasource.url=jdbc:mysql://localhost:3306/authdb_dev
spring.datasource.username=root
spring.datasource.password=root

# Email
spring.mail.username=dev@campusworks.com
app.frontend.url=http://localhost:3000
```

### auth-service-prod.properties
```properties
# JWT Configuration  
security.jwt.secret=${JWT_SECRET}
security.jwt.expMinutes=${JWT_EXPIRATION_MINUTES:60}
security.jwt.issuer=campusworks

# Database
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASS}

# Email
spring.mail.username=${MAIL_USERNAME}
app.frontend.url=${FRONTEND_URL}
```

## RefreshScope Usage

### In Configuration Classes
```java
@Component
@RefreshScope
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {
    // Properties that can be refreshed dynamically
}
```

### In Controllers/Services
```java
@RestController
@RefreshScope  // Enables dynamic refresh
public class SomeController {
    @Value("${some.dynamic.property}")
    private String dynamicProperty;
}
```

## Refresh Endpoints

### Manual Refresh
```bash
# Refresh specific service
curl -X POST http://localhost:9001/actuator/refresh

# Refresh all services (via config server)
curl -X POST http://localhost:8888/webhook/refresh
```

### Check Refresh Status
```bash
# View actuator endpoints
curl http://localhost:9001/actuator

# Check health
curl http://localhost:9001/actuator/health
```

## Security Considerations

1. **GitHub Token**: Use a personal access token with minimal permissions
2. **Config Server Auth**: Use strong credentials for config server
3. **Webhook Security**: Consider adding webhook secret validation
4. **Environment Variables**: Never commit secrets to version control

## Troubleshooting

### Common Issues

1. **Config not loading**: Check bootstrap.properties and service name
2. **Refresh not working**: Verify @RefreshScope annotation
3. **GitHub connection**: Check token permissions and repository access
4. **Service discovery**: Ensure Eureka is running and services are registered

### Debug Commands
```bash
# Check config server health
curl http://localhost:8888/actuator/health

# View loaded configuration
curl http://admin:admin123@localhost:8888/auth-service/dev

# Check service registration
curl http://localhost:8761/eureka/apps
```

## Benefits

1. **Centralized Configuration**: All service configs in one GitHub repository
2. **Environment-Specific**: Different configs for dev/staging/prod
3. **Dynamic Refresh**: Update configuration without service restart
4. **Version Control**: Track configuration changes with Git history
5. **Security**: Sensitive values through environment variables
6. **Scalability**: Easy to add new services and environments