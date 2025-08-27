# Task Service

## Overview
The Task Service is a microservice in the CampusWorks platform that manages tasks. It provides functionality for creating, updating, and managing tasks, as well as handling the task lifecycle from creation to completion.

## Features
- Task creation and management
- Task publishing and bidding
- Work submission and review
- Integration with other CampusWorks services
- JWT-based authentication
- Centralized configuration via Config Server
- Service discovery via Eureka

## Prerequisites
- Java 17
- Maven 3.8+
- MySQL 8.0+
- Docker (optional)

## Configuration
The service is configured through the Config Server. The following properties can be configured:

- Database connection details
- JWT secret and expiration
- Eureka server URL
- Service-specific properties

## Building the Service
```bash
mvn clean package
```

## Running the Service
### Local Development
```bash
mvn spring-boot:run
```

### Using Docker
```bash
docker build -t task-service .
docker run -p 9002:9002 task-service
```

## API Documentation
See [API Documentation](../docs/api-docs/task-service-api.md) for detailed API information.

## Dependencies
- auth-service (for JWT validation)
- profile-service (for user profile information)
- notification-service (for sending notifications)
- payment-service (for payment processing)
- bidding-service (for task bidding)

## Endpoints
All endpoints are prefixed with `/tasks`. See API documentation for details.

## Security
The service uses JWT tokens for authentication. All requests (except public endpoints) must include a valid JWT token in the Authorization header.

## Monitoring
The service exposes actuator endpoints for monitoring and health checks.

## Contributing
1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a pull request

## License
MIT License