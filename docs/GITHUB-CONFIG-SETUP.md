# GitHub Configuration Repository Setup Guide

## Step 1: Create GitHub Repository

1. **Login to GitHub** (https://github.com)
2. **Click "New repository"** (green button)
3. **Repository settings:**
   - **Repository name**: `campusworks-config`
   - **Description**: `Configuration repository for CampusWorks microservices`
   - **Visibility**: Choose Private (recommended) or Public
   - **Don't initialize** with README, .gitignore, or license

4. **Click "Create repository"**

## Step 2: Clone Repository Locally

```bash
# Clone the empty repository
git clone https://github.com/YOUR-USERNAME/campusworks-config.git
cd campusworks-config
```

## Step 3: Add Configuration Files

Copy the configuration files from your project to the GitHub repository:

```bash
# Copy configuration files to GitHub repository
cp /path/to/CampusWorks/campusworks-microservices/config-examples/github-config/* .
```

**Required file structure:**
```
campusworks-config/
├── application.properties              # Global config
├── application-dev.properties          # Global dev config
├── application-prod.properties         # Global prod config
├── auth-service-dev.properties         # Auth service dev
├── auth-service-prod.properties        # Auth service prod
├── task-service-dev.properties         # Task service dev
├── task-service-prod.properties        # Task service prod
├── bidding-service-dev.properties      # Bidding service dev
├── bidding-service-prod.properties     # Bidding service prod
└── README.md                           # Documentation
```

## Step 4: Create Additional Service Configurations

Create configuration files for other services following the same pattern:

### bidding-service-dev.properties
```properties
server.port=9003
security.jwt.secret=CAMPUSWORKS_DEV_SECRET_KEY_FOR_DEVELOPMENT_USE_256_BITS_MINIMUM_CHANGE_IN_PRODUCTION
security.jwt.issuer=campusworks-dev
spring.datasource.url=jdbc:mysql://localhost:3306/biddingdb_dev?createDatabaseIfNotExist=true&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=root
app.services.auth-service.url=http://localhost:9001
app.services.task-service.url=http://localhost:9002
app.bidding.max-bids-per-task=50
app.bidding.auto-select-after-hours=72
logging.level.com.campusworks.bidding=DEBUG
```

### payment-service-dev.properties
```properties
server.port=9005
security.jwt.secret=CAMPUSWORKS_DEV_SECRET_KEY_FOR_DEVELOPMENT_USE_256_BITS_MINIMUM_CHANGE_IN_PRODUCTION
security.jwt.issuer=campusworks-dev
spring.datasource.url=jdbc:mysql://localhost:3306/paymentdb_dev?createDatabaseIfNotExist=true&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=root
app.services.auth-service.url=http://localhost:9001
app.payment.razorpay.key-id=rzp_test_your_key_id
app.payment.razorpay.key-secret=your_test_secret
logging.level.com.campusworks.payment=DEBUG
```

## Step 5: Commit and Push Files

```bash
# Add all configuration files
git add .

# Commit with meaningful message
git commit -m "Initial configuration files for CampusWorks microservices"

# Push to GitHub
git push origin main
```

## Step 6: Create GitHub Personal Access Token (for Private Repo)

If your repository is **private**, you need a Personal Access Token:

1. **Go to GitHub Settings** → **Developer settings** → **Personal access tokens** → **Tokens (classic)**
2. **Click "Generate new token (classic)"**
3. **Configure token:**
   - **Note**: `CampusWorks Config Server Access`
   - **Expiration**: Choose appropriate duration (90 days recommended)
   - **Scopes**: Select `repo` (Full control of private repositories)
4. **Generate token** and **copy it immediately** (you won't see it again!)

## Step 7: Update Config Server Environment Variables

Set these environment variables for your config server:

```bash
# For Windows (Command Prompt)
set CONFIG_GIT_URI=https://github.com/YOUR-USERNAME/campusworks-config.git
set CONFIG_GIT_USERNAME=your-github-username
set CONFIG_GIT_TOKEN=your-personal-access-token

# For Windows (PowerShell)
$env:CONFIG_GIT_URI="https://github.com/YOUR-USERNAME/campusworks-config.git"
$env:CONFIG_GIT_USERNAME="your-github-username"
$env:CONFIG_GIT_TOKEN="your-personal-access-token"

# For Linux/Mac
export CONFIG_GIT_URI=https://github.com/YOUR-USERNAME/campusworks-config.git
export CONFIG_GIT_USERNAME=your-github-username
export CONFIG_GIT_TOKEN=your-personal-access-token
```

## Step 8: Test Configuration Server

1. **Start Eureka Server** (Port 8761)
2. **Start Config Server** (Port 8888)
3. **Test configuration retrieval:**

```bash
# Test auth-service dev configuration
curl http://admin:admin123@localhost:8888/auth-service/dev

# Test auth-service prod configuration
curl http://admin:admin123@localhost:8888/auth-service/prod

# Test task-service dev configuration
curl http://admin:admin123@localhost:8888/task-service/dev
```

## Step 9: Update Microservices

For each microservice, add the config client dependencies and bootstrap.properties:

### Add to pom.xml:
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-config</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-bootstrap</artifactId>
</dependency>
```

### Create bootstrap.properties:
```properties
spring.application.name=task-service  # Change per service
spring.profiles.active=dev
spring.cloud.config.uri=http://localhost:8888
spring.cloud.config.username=admin
spring.cloud.config.password=admin123
spring.cloud.config.fail-fast=true
eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
```

## Step 10: Set Up GitHub Webhook (Optional)

For automatic configuration refresh when you push changes:

1. **Go to your GitHub repository**
2. **Settings** → **Webhooks** → **Add webhook**
3. **Configure webhook:**
   - **Payload URL**: `http://your-server:8888/webhook/github`
   - **Content type**: `application/json`
   - **Events**: Just the push event
   - **Active**: ✓ checked

## Step 11: Test RefreshScope

1. **Start auth-service** with config client
2. **Check current configuration:**
```bash
curl http://localhost:9001/config/jwt
```

3. **Update configuration in GitHub** (change JWT expiration)
4. **Refresh configuration:**
```bash
curl -X POST http://localhost:9001/actuator/refresh
```

5. **Check updated configuration:**
```bash
curl http://localhost:9001/config/jwt
```

## Environment Variables Summary

Create a `.env` file or set these environment variables:

```bash
# Config Server GitHub Integration
CONFIG_GIT_URI=https://github.com/YOUR-USERNAME/campusworks-config.git
CONFIG_GIT_USERNAME=your-github-username
CONFIG_GIT_TOKEN=your-personal-access-token

# Config Server Security
CONFIG_SERVER_USERNAME=admin
CONFIG_SERVER_PASSWORD=secure-password

# Service Discovery
EUREKA_URI=http://localhost:8761/eureka/

# Production Environment Variables (set in GitHub config files)
JWT_SECRET=your-production-256-bit-secret-key
JWT_EXPIRATION_MINUTES=120
AUTH_DB_URL=jdbc:mysql://prod-server:3306/authdb
AUTH_DB_USER=prod_user
AUTH_DB_PASS=prod_password
MAIL_USERNAME=production@campusworks.com
MAIL_PASSWORD=production-app-password
FRONTEND_URL=https://campusworks.com
```

## Troubleshooting

### Common Issues:

1. **401 Unauthorized**: Check GitHub token permissions
2. **Repository not found**: Verify repository URL and access rights
3. **Configuration not loading**: Check service name matches file name
4. **Refresh not working**: Verify @RefreshScope annotation

### Debug Commands:
```bash
# Check config server health
curl http://localhost:8888/actuator/health

# View all available configurations
curl http://admin:admin123@localhost:8888/actuator/env

# Check service registration with Eureka
curl http://localhost:8761/eureka/apps
```

## Security Best Practices

1. **Never commit sensitive data** to configuration files
2. **Use environment variables** for secrets in production
3. **Rotate GitHub tokens** regularly
4. **Use strong passwords** for config server authentication
5. **Consider encrypting** sensitive configuration values

Your GitHub configuration repository is now ready for Spring Cloud Config Server! 🚀