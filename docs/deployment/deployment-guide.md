# CampusWorks Deployment Guide

## Overview

This guide provides comprehensive instructions for deploying the CampusWorks platform in various environments, from local development to production deployment.

## Deployment Architecture

### Development Environment
- Single machine deployment using Docker Compose
- All services running on localhost
- Shared MySQL and MongoDB instances
- File storage using local filesystem

### Production Environment
- Containerized deployment using Docker Swarm or Kubernetes
- Load-balanced services with multiple instances
- Dedicated database clusters (MySQL and MongoDB)
- Cloud storage integration (AWS S3, Google Cloud Storage)
- Redis for caching and session management
- NGINX for reverse proxy and SSL termination

## Prerequisites

### System Requirements
- **CPU**: Minimum 4 cores (8 cores recommended for production)
- **RAM**: Minimum 8GB (16GB recommended for production)
- **Storage**: Minimum 50GB SSD (100GB+ for production)
- **Network**: Stable internet connection

### Software Requirements
- Docker 20.10+
- Docker Compose 2.0+
- Java 17+ (for local development)
- Maven 3.8+ (for building from source)

## Local Development Deployment

### 1. Clone and Setup
```bash
# Clone the repository
git clone <repository-url>
cd campusworks-microservices

# Create necessary directories
mkdir -p logs data/mysql data/mongodb
```

### 2. Environment Configuration
```bash
# Copy environment template
cp .env.template .env

# Edit environment variables
nano .env
```

### 3. Environment Variables (.env file)
```bash
# Database Configuration
MYSQL_ROOT_PASSWORD=campusworks2024
MYSQL_DATABASE=campusworks
MONGODB_DATABASE=campusworks

# JWT Configuration
JWT_SECRET=your-super-secret-jwt-key-change-this-in-production
JWT_EXPIRATION=3600

# Email Configuration
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password

# Payment Gateway (Razorpay)
RAZORPAY_KEY_ID=your-razorpay-key-id
RAZORPAY_KEY_SECRET=your-razorpay-secret

# File Storage
STORAGE_TYPE=local
STORAGE_PATH=/app/uploads

# Logging
LOG_LEVEL=INFO
LOG_PATH=/app/logs
```

### 4. Build and Deploy
```bash
# Build all services
mvn clean package -DskipTests

# Start infrastructure services first
docker-compose up -d mysql mongo redis

# Wait for databases to be ready (30-60 seconds)
sleep 60

# Start all services
docker-compose up -d

# Check service status
docker-compose ps
```

### 5. Verify Deployment
```bash
# Check service health
curl http://localhost:8761  # Eureka Server
curl http://localhost:8080/actuator/health  # API Gateway

# View logs
docker-compose logs -f api-gateway
docker-compose logs -f auth-service
```

## Production Deployment

### 1. Docker Swarm Deployment

#### Initialize Swarm
```bash
# On manager node
docker swarm init

# Add worker nodes
docker swarm join --token <worker-token> <manager-ip>:2377
```

#### Deploy Stack
```bash
# Create production docker-compose.prod.yml
# Deploy stack
docker stack deploy -c docker-compose.prod.yml campusworks

# Check services
docker service ls
docker service logs campusworks_api-gateway
```

### 2. Kubernetes Deployment

#### Namespace and ConfigMaps
```yaml
# namespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: campusworks

---
# configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: campusworks-config
  namespace: campusworks
data:
  JWT_EXPIRATION: "3600"
  LOG_LEVEL: "INFO"
  MYSQL_DATABASE: "campusworks"
  MONGODB_DATABASE: "campusworks"
```

#### Database Deployments
```yaml
# mysql-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: mysql
  namespace: campusworks
spec:
  replicas: 1
  selector:
    matchLabels:
      app: mysql
  template:
    metadata:
      labels:
        app: mysql
    spec:
      containers:
      - name: mysql
        image: mysql:8.0
        ports:
        - containerPort: 3306
        env:
        - name: MYSQL_ROOT_PASSWORD
          valueFrom:
            secretKeyRef:
              name: mysql-secret
              key: password
        volumeMounts:
        - name: mysql-storage
          mountPath: /var/lib/mysql
      volumes:
      - name: mysql-storage
        persistentVolumeClaim:
          claimName: mysql-pvc

---
apiVersion: v1
kind: Service
metadata:
  name: mysql
  namespace: campusworks
spec:
  selector:
    app: mysql
  ports:
  - port: 3306
    targetPort: 3306
```

#### Application Deployments
```yaml
# auth-service-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: auth-service
  namespace: campusworks
spec:
  replicas: 2
  selector:
    matchLabels:
      app: auth-service
  template:
    metadata:
      labels:
        app: auth-service
    spec:
      containers:
      - name: auth-service
        image: campusworks/auth-service:latest
        ports:
        - containerPort: 8080
        env:
        - name: DB_URL
          value: "jdbc:mysql://mysql:3306/authdb"
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: campusworks-secrets
              key: jwt-secret
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10

---
apiVersion: v1
kind: Service
metadata:
  name: auth-service
  namespace: campusworks
spec:
  selector:
    app: auth-service
  ports:
  - port: 8080
    targetPort: 8080
```

### 3. AWS ECS Deployment

#### Task Definition
```json
{
  "family": "campusworks-auth-service",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "512",
  "memory": "1024",
  "executionRoleArn": "arn:aws:iam::account:role/ecsTaskExecutionRole",
  "containerDefinitions": [
    {
      "name": "auth-service",
      "image": "your-account.dkr.ecr.region.amazonaws.com/campusworks/auth-service:latest",
      "portMappings": [
        {
          "containerPort": 8080,
          "protocol": "tcp"
        }
      ],
      "environment": [
        {
          "name": "DB_URL",
          "value": "jdbc:mysql://rds-endpoint:3306/authdb"
        }
      ],
      "secrets": [
        {
          "name": "JWT_SECRET",
          "valueFrom": "arn:aws:ssm:region:account:parameter/campusworks/jwt-secret"
        }
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/campusworks",
          "awslogs-region": "us-east-1",
          "awslogs-stream-prefix": "auth-service"
        }
      }
    }
  ]
}
```

## Database Setup

### MySQL Setup
```sql
-- Create databases for each service
CREATE DATABASE IF NOT EXISTS authdb;
CREATE DATABASE IF NOT EXISTS taskdb;
CREATE DATABASE IF NOT EXISTS biddingdb;
CREATE DATABASE IF NOT EXISTS paymentdb;
CREATE DATABASE IF NOT EXISTS profiledb;
CREATE DATABASE IF NOT EXISTS reviewdb;
CREATE DATABASE IF NOT EXISTS admindb;

-- Create service user
CREATE USER 'campusworks'@'%' IDENTIFIED BY 'strong-password';
GRANT ALL PRIVILEGES ON authdb.* TO 'campusworks'@'%';
GRANT ALL PRIVILEGES ON taskdb.* TO 'campusworks'@'%';
GRANT ALL PRIVILEGES ON biddingdb.* TO 'campusworks'@'%';
GRANT ALL PRIVILEGES ON paymentdb.* TO 'campusworks'@'%';
GRANT ALL PRIVILEGES ON profiledb.* TO 'campusworks'@'%';
GRANT ALL PRIVILEGES ON reviewdb.* TO 'campusworks'@'%';
GRANT ALL PRIVILEGES ON admindb.* TO 'campusworks'@'%';
FLUSH PRIVILEGES;
```

### MongoDB Setup
```javascript
// Connect to MongoDB and create databases
use campuschat;
db.createCollection("conversations");
db.createCollection("messages");

use campusnotify;
db.createCollection("notifications");
db.createCollection("user_preferences");

// Create indexes for performance
db.conversations.createIndex({"participants": 1});
db.messages.createIndex({"conversationId": 1, "timestamp": -1});
db.notifications.createIndex({"userId": 1, "read": 1, "createdAt": -1});
```

## SSL/TLS Configuration

### NGINX Configuration
```nginx
# /etc/nginx/sites-available/campusworks
server {
    listen 80;
    server_name your-domain.com;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name your-domain.com;

    ssl_certificate /etc/ssl/certs/your-domain.crt;
    ssl_certificate_key /etc/ssl/private/your-domain.key;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-RSA-AES256-GCM-SHA512:DHE-RSA-AES256-GCM-SHA512:ECDHE-RSA-AES256-GCM-SHA384:DHE-RSA-AES256-GCM-SHA384;
    ssl_prefer_server_ciphers off;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # WebSocket support for chat
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }

    location /health {
        access_log off;
        proxy_pass http://localhost:8080/actuator/health;
    }
}
```

## Monitoring and Logging

### ELK Stack Setup
```yaml
# elk-stack.yml
version: '3.8'
services:
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.5.0
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
    ports:
      - "9200:9200"
    volumes:
      - elasticsearch_data:/usr/share/elasticsearch/data

  logstash:
    image: docker.elastic.co/logstash/logstash:8.5.0
    volumes:
      - ./logstash.conf:/usr/share/logstash/pipeline/logstash.conf
    ports:
      - "5044:5044"
    depends_on:
      - elasticsearch

  kibana:
    image: docker.elastic.co/kibana/kibana:8.5.0
    ports:
      - "5601:5601"
    environment:
      - ELASTICSEARCH_HOSTS=http://elasticsearch:9200
    depends_on:
      - elasticsearch

volumes:
  elasticsearch_data:
```

### Prometheus Monitoring
```yaml
# prometheus.yml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'campusworks-services'
    static_configs:
      - targets: 
        - 'auth-service:8080'
        - 'task-service:8080'
        - 'bidding-service:8080'
        - 'payment-service:8080'
    metrics_path: '/actuator/prometheus'
```

## Backup and Recovery

### Database Backup Scripts
```bash
#!/bin/bash
# mysql-backup.sh

DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/backups/mysql"
MYSQL_HOST="localhost"
MYSQL_USER="root"
MYSQL_PASS="password"

# Create backup directory
mkdir -p $BACKUP_DIR

# Backup each database
for db in authdb taskdb biddingdb paymentdb profiledb reviewdb admindb; do
    mysqldump -h$MYSQL_HOST -u$MYSQL_USER -p$MYSQL_PASS $db > $BACKUP_DIR/${db}_$DATE.sql
    gzip $BACKUP_DIR/${db}_$DATE.sql
done

# MongoDB backup
mongodump --out $BACKUP_DIR/mongodb_$DATE

# Clean old backups (keep 7 days)
find $BACKUP_DIR -name "*.gz" -mtime +7 -delete
find $BACKUP_DIR -name "mongodb_*" -mtime +7 -exec rm -rf {} \;
```

### Automated Backup with Cron
```bash
# Add to crontab: crontab -e
# Daily backup at 2 AM
0 2 * * * /path/to/mysql-backup.sh

# Weekly full backup at 1 AM on Sundays
0 1 * * 0 /path/to/full-backup.sh
```

## Security Configuration

### Security Checklist
- [ ] Change default passwords for all services
- [ ] Use environment variables for sensitive data
- [ ] Enable SSL/TLS for all external communications
- [ ] Configure firewall rules to restrict access
- [ ] Set up VPN for internal service communication
- [ ] Enable audit logging for all databases
- [ ] Configure rate limiting on API Gateway
- [ ] Set up intrusion detection system
- [ ] Regular security updates and patches
- [ ] Backup encryption and secure storage

### Firewall Configuration
```bash
# UFW (Ubuntu Firewall) configuration
ufw default deny incoming
ufw default allow outgoing

# Allow SSH (change port if needed)
ufw allow 22/tcp

# Allow HTTP and HTTPS
ufw allow 80/tcp
ufw allow 443/tcp

# Allow only internal access to databases
ufw allow from 10.0.0.0/8 to any port 3306
ufw allow from 10.0.0.0/8 to any port 27017

# Enable firewall
ufw enable
```

## Troubleshooting

### Common Issues

#### Service Discovery Issues
```bash
# Check Eureka server
curl http://localhost:8761/eureka/apps

# Restart service registration
docker-compose restart auth-service
```

#### Database Connection Issues
```bash
# Check MySQL connectivity
mysql -h localhost -u root -p -e "SHOW DATABASES;"

# Check MongoDB connectivity
mongo --eval "db.adminCommand('ping')"
```

#### Memory Issues
```bash
# Check memory usage
docker stats

# Increase JVM heap size
export JAVA_OPTS="-Xmx2g -Xms1g"
```

### Log Analysis
```bash
# Service logs
docker-compose logs -f auth-service
docker-compose logs -f --tail=100 task-service

# System logs
journalctl -u docker
journalctl -f
```

## Performance Optimization

### JVM Tuning
```bash
# Production JVM settings
export JAVA_OPTS="-Xmx2g -Xms2g -XX:+UseG1GC -XX:+UseStringDeduplication -XX:+DisableExplicitGC"
```

### Database Optimization
```sql
-- MySQL performance tuning
SET GLOBAL innodb_buffer_pool_size = 2147483648;  -- 2GB
SET GLOBAL query_cache_size = 268435456;  -- 256MB
SET GLOBAL max_connections = 500;
```

### Caching Configuration
```properties
# Redis caching
spring.cache.type=redis
spring.redis.host=redis
spring.redis.port=6379
spring.redis.timeout=2000ms
spring.cache.redis.time-to-live=3600000
```

This comprehensive deployment guide ensures successful deployment and operation of the CampusWorks platform in various environments.