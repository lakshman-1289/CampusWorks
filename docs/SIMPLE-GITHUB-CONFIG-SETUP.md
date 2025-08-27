# Simple GitHub Configuration Setup

## Overview
This is a minimal setup with just one `application.properties` file containing only JWT secret key and expiration time.

## Step 1: Create GitHub Repository

1. **Go to GitHub** and create a new repository
2. **Repository name**: `campusworks-config`
3. **Visibility**: Private (recommended) or Public
4. **Don't initialize** with README

## Step 2: Create Single Configuration File

Create only one file named `application.properties` with this content:

```properties
# CampusWorks JWT Configuration
# This file contains centralized JWT configuration for all microservices

# JWT Secret Key (256-bit minimum for HS256 algorithm)
security.jwt.secret=CAMPUSWORKS_CENTRALIZED_SECRET_KEY_FOR_ALL_MICROSERVICES_256_BITS_MINIMUM_CHANGE_IN_PRODUCTION

# JWT Expiration Time in Minutes
security.jwt.expMinutes=120
```

## Step 3: Upload to GitHub

### Option A: Using GitHub Web Interface
1. Go to your repository
2. Click "creating a new file"
3. Name it `application.properties`
4. Copy the content above
5. Commit the file

### Option B: Using Git Commands
```bash
# Clone repository
git clone https://github.com/YOUR-USERNAME/campusworks-config.git
cd campusworks-config

# Create the file
echo "# CampusWorks JWT Configuration" > application.properties
echo "security.jwt.secret=CAMPUSWORKS_CENTRALIZED_SECRET_KEY_FOR_ALL_MICROSERVICES_256_BITS_MINIMUM_CHANGE_IN_PRODUCTION" >> application.properties
echo "security.jwt.expMinutes=120" >> application.properties

# Commit and push
git add application.properties
git commit -m "Add JWT configuration"
git push origin main
```

## Step 4: Set Environment Variables

Set these environment variables for your config server:

```bash
# Windows Command Prompt
set CONFIG_GIT_URI=https://github.com/YOUR-USERNAME/campusworks-config.git
set CONFIG_GIT_USERNAME=your-github-username
set CONFIG_GIT_TOKEN=your-personal-access-token

# Windows PowerShell
$env:CONFIG_GIT_URI="https://github.com/YOUR-USERNAME/campusworks-config.git"
$env:CONFIG_GIT_USERNAME="your-github-username"
$env:CONFIG_GIT_TOKEN="your-personal-access-token"
```

## Step 5: Test Configuration

1. **Start Eureka Server** (Port 8761)
2. **Start Config Server** (Port 8888)
3. **Test configuration retrieval:**

```bash
# Test configuration loading
curl http://admin:admin123@localhost:8888/application/default

# Should return JWT configuration
```

## Step 6: Test with Auth Service

1. **Start Auth Service** (Port 9001)
2. **Check JWT configuration:**

```bash
# View current JWT config
curl http://localhost:9001/config/jwt
```

3. **Test configuration refresh:**

```bash
# Update JWT expiration in GitHub (change from 120 to 180 minutes)
# Then refresh configuration
curl -X POST http://localhost:9001/actuator/refresh

# Check updated configuration
curl http://localhost:9001/config/jwt
```

## Step 7: Update JWT Configuration Dynamically

1. **Go to your GitHub repository**
2. **Edit `application.properties`**
3. **Change `security.jwt.expMinutes=120` to `security.jwt.expMinutes=180`**
4. **Commit the change**
5. **Refresh the service:**

```bash
curl -X POST http://localhost:9001/actuator/refresh
```

6. **Verify the change:**

```bash
curl http://localhost:9001/config/jwt
# Should show expMinutes: 180
```

## File Structure

Your GitHub repository will have only one file:

```
campusworks-config/
└── application.properties
```

## Configuration Properties Explanation

- **`security.jwt.secret`**: The secret key used to sign JWT tokens (must be 256-bit for HS256)
- **`security.jwt.expMinutes`**: JWT token expiration time in minutes

## Benefits of This Simple Setup

1. **Minimal Configuration**: Only essential JWT properties
2. **Easy to Manage**: Single file to maintain
3. **Dynamic Updates**: Change JWT settings without service restart
4. **Centralized**: All services use the same JWT configuration
5. **Version Controlled**: Track JWT configuration changes

## Security Notes

- **Never commit real production secrets** to GitHub
- **Use environment variables** for production secrets
- **Rotate JWT secret** regularly in production
- **Use strong, random 256-bit keys** for production

This simple setup gives you centralized JWT configuration with dynamic refresh capability! 🚀