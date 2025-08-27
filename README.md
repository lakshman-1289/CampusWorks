# CampusWorks - Peer-to-Peer Task Marketplace for Students

## Overview

CampusWorks is a comprehensive microservices-based platform designed specifically for college students to create, bid on, and complete academic tasks. The platform enables students to post tasks (assignments, record-keeping, research work) with specified budgets, allowing other students to competitively bid on these tasks. The lowest bidder automatically wins, work is completed offline, submitted online for verification, and payments are processed securely.

## 🏗️ Architecture

The platform follows a microservices architecture with the following services:

### Core Services

1. **Eureka Server** (`:8761`) - Service Discovery
2. **API Gateway** (`:8080`) - Centralized routing and authentication
3. **Auth Service** - User authentication and authorization
4. **Profile Service** - User profile management and verification
5. **Task Service** - Task creation, management, and lifecycle
6. **Bidding Service** - Competitive bidding with automated winner selection
7. **Payment Service** - Escrow payments with Razorpay integration
8. **Chat Service** - Real-time communication between users
9. **Notification Service** - Multi-channel notifications
10. **Review Service** - Task completion reviews and ratings
11. **Admin Service** - Platform administration and monitoring

### Databases

- **MySQL** - Transactional data (Users, Tasks, Bids, Payments, Reviews)
- **MongoDB** - Chat messages, notifications, and unstructured data

## 🚀 Key Features

### For Task Owners (Students who need work done)
- Post academic tasks with detailed requirements
- Set minimum and maximum budget ranges
- Define bidding deadlines and completion deadlines
- Review submitted work and provide feedback
- Make secure payments through escrow system
- Rate and review task performers

### For Task Performers (Students who complete tasks)
- Browse available tasks by category, subject, and budget
- Place competitive bids on tasks
- Automatic winner selection (lowest bidder)
- Upload work submissions with files and descriptions
- Receive payments upon task approval
- Build reputation through reviews and ratings

### Platform Features
- JWT-based secure authentication
- Real-time chat for task clarification
- Push notifications and email alerts
- Comprehensive user profiles with verification
- Escrow payment system for security
- Multi-level admin controls
- Detailed analytics and reporting

## 🛠️ Technology Stack

- **Backend**: Spring Boot 3.x, Spring Cloud
- **Security**: JWT, Spring Security
- **Databases**: MySQL 8.0, MongoDB 6.x
- **Service Discovery**: Netflix Eureka
- **API Gateway**: Spring Cloud Gateway
- **Payment Gateway**: Razorpay
- **Real-time Communication**: WebSocket, STOMP
- **Containerization**: Docker, Docker Compose
- **Build Tool**: Maven
- **Documentation**: OpenAPI 3.0 (Swagger)

## 📋 Prerequisites

- Java 17 or higher
- Maven 3.8+
- Docker and Docker Compose
- MySQL 8.0
- MongoDB 6.x

## 🚀 Quick Start

### 1. Clone the Repository
```bash
git clone <repository-url>
cd campusworks-microservices
```

### 2. Start Infrastructure Services
```bash
docker-compose up mysql mongo -d
```

### 3. Build All Services
```bash
mvn clean install
```

### 4. Start All Services
```bash
docker-compose up -d
```

### 5. Access the Platform
- **API Gateway**: http://localhost:8080
- **Eureka Dashboard**: http://localhost:8761
- **API Documentation**: http://localhost:8080/swagger-ui.html

## 📚 API Documentation

Each service provides comprehensive OpenAPI documentation:

- **Auth Service**: http://localhost:8080/auth/swagger-ui.html
- **Task Service**: http://localhost:8080/tasks/swagger-ui.html
- **Bidding Service**: http://localhost:8080/bidding/swagger-ui.html
- **Payment Service**: http://localhost:8080/payments/swagger-ui.html
- **Profile Service**: http://localhost:8080/profiles/swagger-ui.html
- **Chat Service**: http://localhost:8080/chat/swagger-ui.html
- **Notification Service**: http://localhost:8080/notifications/swagger-ui.html

## 🔄 Typical Workflow

### 1. User Registration & Authentication
```
POST /auth/register - Register new user
POST /auth/login - User login
POST /auth/verify-email - Email verification
```

### 2. Profile Setup
```
PUT /profiles/complete - Complete user profile
POST /profiles/verify - Request verification
```

### 3. Task Creation & Bidding
```
POST /tasks - Create new task
GET /tasks/open - Browse open tasks
POST /bidding/bid - Place bid on task
GET /bidding/task/{taskId}/winner - Check winning bid
```

### 4. Work Submission & Payment
```
POST /tasks/{taskId}/submit - Submit completed work
PUT /tasks/{taskId}/review - Review and approve work
POST /payments/release - Release escrow payment
```

### 5. Communication & Reviews
```
POST /chat/conversations - Start conversation
GET /notifications/unread - Check notifications
POST /reviews - Submit task review
```

## 🔒 Security

- **JWT Authentication**: All API calls require valid JWT tokens
- **Role-based Access**: Different permissions for students, verified users, and admins
- **Escrow Payments**: Secure payment holding until work completion
- **Data Validation**: Comprehensive input validation across all services
- **Rate Limiting**: API rate limiting to prevent abuse

## 🧪 Testing

### Run Unit Tests
```bash
mvn test
```

### Run Integration Tests
```bash
mvn verify
```

### API Testing with Postman
Import the provided Postman collection from `/docs/api-tests/`

## 📊 Monitoring & Administration

### Service Health Checks
- **Eureka Dashboard**: Monitor service registration and health
- **Actuator Endpoints**: Health checks and metrics for each service
- **Admin Service**: Platform-wide monitoring and administration

### Key Metrics
- Active users and tasks
- Bidding competition rates
- Payment transaction volumes
- Service response times and error rates

## 🔧 Configuration

### Environment Variables
- `JWT_SECRET`: Secret key for JWT token generation
- `DB_URL`: Database connection URL
- `RAZORPAY_KEY_ID`: Razorpay API key
- `RAZORPAY_KEY_SECRET`: Razorpay secret
- `MAIL_HOST`: SMTP server configuration

### Properties Files
All services use `.properties` files for configuration (YAML files have been removed as requested).

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 📞 Support

For support and questions:
- Create an issue in the repository
- Contact the development team
- Check the documentation in `/docs/`

---

**CampusWorks** - Empowering students through collaborative academic task completion.