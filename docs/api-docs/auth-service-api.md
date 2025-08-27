# Auth Service API Documentation

## Overview
The Auth Service handles user authentication, registration, email verification, and JWT token management for the CampusWorks platform.

## Base URL
```
http://localhost:8080/api/auth
```

## Authentication
All endpoints except `/register`, `/login`, `/verify-email`, `/forgot-password`, `/resend-verification`, `/reset-password`, `/health`, and `/test` require a valid JWT token in the Authorization header:
```
Authorization: Bearer <jwt-token>
```

## Endpoints

### 1. User Registration
**POST** `/register`

Register a new user account.

**Request Body:**
```json
{
  "email": "student@university.edu",
  "password": "SecurePassword123!",
  "firstName": "John",
  "lastName": "Doe",
  "phoneNumber": "+1234567890",
  "college": "Sample University",
  "course": "Computer Science",
  "yearOfStudy": 2
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "message": "Registration successful. Please verify your email.",
  "user": {
    "id": 1,
    "email": "student@university.edu",
    "firstName": "John",
    "lastName": "Doe",
    "phoneNumber": "+1234567890",
    "college": "Sample University",
    "course": "Computer Science",
    "yearOfStudy": 2,
    "profileImageUrl": null,
    "roles": [
      "USER"
    ],
    "emailVerified": false,
    "phoneVerified": false,
    "profileCompleted": false,
    "createdAt": "2025-08-27T10:30:00",
    "lastLoginAt": null
  }
}
```

### 2. User Login
**POST** `/login`

Authenticate user and receive JWT token.

**Request Body:**
```json
{
  "email": "student@university.edu",
  "password": "SecurePassword123!"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "message": "Login successful",
  "user": {
    "id": 1,
    "email": "student@university.edu",
    "firstName": "John",
    "lastName": "Doe",
    "phoneNumber": "+1234567890",
    "college": "Sample University",
    "course": "Computer Science",
    "yearOfStudy": 2,
    "profileImageUrl": null,
    "roles": [
      "USER"
    ],
    "emailVerified": true,
    "phoneVerified": false,
    "profileCompleted": false,
    "createdAt": "2025-08-27T10:30:00",
    "lastLoginAt": "2025-08-27T10:30:00"
  }
}
```

### 3. Email Verification
**POST** `/verify-email`

Verify user email address using verification token.

**Query Parameters:**
```
token=verification_token_from_email
```

**Response:**
```json
{
  "success": true,
  "message": "Email verified successfully",
  "timestamp": 1625097600000
}
```

### 4. Resend Verification Email
**POST** `/resend-verification`

Resend email verification token.

**Query Parameters:**
```
email=student@university.edu
```

**Response:**
```json
{
  "success": true,
  "message": "Verification email sent",
  "timestamp": 1625097600000
}
```

### 5. Forgot Password
**POST** `/forgot-password`

Request password reset token.

**Query Parameters:**
```
email=student@university.edu
```

**Response:**
```json
{
  "success": true,
  "message": "Password reset email sent",
  "timestamp": 1625097600000
}
```

### 6. Reset Password
**POST** `/reset-password`

Reset password using reset token.

**Request Body:**
```json
{
  "token": "reset_token_from_email",
  "newPassword": "NewSecurePassword123!"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Password reset successfully",
  "timestamp": 1625097600000
}
```



### 7. Change Password
**POST** `/change-password`

Change user password (requires authentication).

**Request Body:**
```json
{
  "currentPassword": "CurrentPassword123!",
  "newPassword": "NewPassword123!"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Password changed successfully",
  "timestamp": 1625097600000
}
```

### 8. Get Current User
**GET** `/me`

Get information about the currently authenticated user.

**Response:**
```json
{
  "id": 1,
  "email": "student@university.edu",
  "firstName": "John",
  "lastName": "Doe",
  "phoneNumber": "+1234567890",
  "college": "Sample University",
  "course": "Computer Science",
  "yearOfStudy": 2,
  "profileImageUrl": null,
  "roles": [
    "USER"
  ],
  "emailVerified": true,
  "phoneVerified": false,
  "profileCompleted": false,
  "createdAt": "2025-08-27T10:30:00",
  "lastLoginAt": "2025-08-27T10:30:00"
}
```

### 9. Logout
**POST** `/logout`

Logout user (client-side token removal).

**Response:**
```json
{
  "success": true,
  "message": "Logout successful",
  "timestamp": 1625097600000
}
```

### 10. Health Check
**GET** `/health`

Check if the auth service is healthy.

**Response:**
```json
{
  "success": true,
  "message": "Auth service is healthy",
  "timestamp": 1625097600000
}
```

### 11. Test Endpoint
**GET** `/test`

Simple test endpoint to diagnose issues (no database required).

**Response:**
```json
{
  "success": true,
  "message": "Test endpoint working - no database required",
  "timestamp": 1625097600000
}
```

## Error Responses

### 400 Bad Request
```json
{
  "timestamp": "2025-08-27T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/auth/register",
  "validationErrors": {
    "email": "Email is required"
  }
}
```

### 401 Unauthorized
```json
{
  "timestamp": "2025-08-27T10:30:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid credentials",
  "path": "/api/auth/login"
}
```

### 403 Forbidden
```json
{
  "timestamp": "2025-08-27T10:30:00",
  "status": 403,
  "error": "Forbidden",
  "message": "Access denied",
  "path": "/api/auth/me"
}
```

### 404 Not Found
```json
{
  "timestamp": "2025-08-27T10:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "User not found",
  "path": "/api/auth/me"
}
```

### 409 Conflict
```json
{
  "timestamp": "2025-08-27T10:30:00",
  "status": 409,
  "error": "Conflict",
  "message": "Email already registered",
  "path": "/api/auth/register"
}
```

### 500 Internal Server Error
```json
{
  "timestamp": "2025-08-27T10:30:00",
  "status": 500,
  "error": "Internal Server Error",
  "message": "An unexpected error occurred. Please try again later.",
  "path": "/api/auth/register"
}
```

## Rate Limiting
- Registration: 5 attempts per hour per IP
- Login: 10 attempts per 15 minutes per IP
- Password reset: 3 attempts per hour per email

## Security Notes
- Passwords must be at least 8 characters with uppercase, lowercase, number, and special character
- JWT tokens expire in 2 hours
- Email verification tokens expire in 24 hours
- Password reset tokens expire in 1 hour