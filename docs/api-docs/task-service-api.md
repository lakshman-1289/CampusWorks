# Task Service API Documentation

## Overview
The Task Service manages tasks in the CampusWorks platform. It provides functionality for creating, updating, and managing tasks, as well as handling the task lifecycle from creation to completion.

## Base URL
All URLs referenced in the documentation have the following base:
```
http://localhost:8080/api/tasks
```

## Authentication
All endpoints except public endpoints require a valid JWT token in the Authorization header:
```
Authorization: Bearer <token>
```

## Public Endpoints

### Get Open Tasks
```
GET /public
```
Retrieve a paginated list of open tasks.

**Parameters:**
- `page` (optional, default: 0) - Page number
- `size` (optional, default: 20) - Page size
- `category` (optional) - Filter by category
- `subject` (optional) - Filter by subject

**Response:**
```json
{
  "content": [
    {
      "id": 1,
      "ownerId": 100,
      "ownerName": "John Doe",
      "title": "Math Assignment",
      "description": "Complete chapter 5 exercises",
      "budget": 50.00,
      "requirements": "Show all work",
      "status": "OPEN",
      "category": "ASSIGNMENT",
      "priority": "MEDIUM",
      "deadline": "2023-12-31T23:59:59",
      "biddingDeadline": "2023-12-25T23:59:59",
      "assignedUserId": null,
      "assignedUserName": null,
      "finalPrice": null,
      "createdAt": "2023-12-01T10:00:00",
      "updatedAt": "2023-12-01T10:00:00"
    }
  ],
  "pageable": "...",
  "totalElements": 1,
  "totalPages": 1,
  "last": true,
  "size": 20,
  "number": 0,
  "sort": "...",
  "numberOfElements": 1,
  "first": true,
  "empty": false
}
```

### Search Tasks
```
GET /public/search
```
Search for tasks by keyword.

**Parameters:**
- `keyword` (required) - Search keyword
- `page` (optional, default: 0) - Page number
- `size` (optional, default: 20) - Page size

**Response:**
Same as Get Open Tasks

### Get Task by ID
```
GET /public/{id}
```
Retrieve a specific task by ID.

**Response:**
```json
{
  "id": 1,
  "ownerId": 100,
  "ownerName": "John Doe",
  "title": "Math Assignment",
  "description": "Complete chapter 5 exercises",
  "budget": 50.00,
  "requirements": "Show all work",
  "status": "OPEN",
  "category": "ASSIGNMENT",
  "priority": "MEDIUM",
  "deadline": "2023-12-31T23:59:59",
  "biddingDeadline": "2023-12-25T23:59:59",
  "assignedUserId": null,
  "assignedUserName": null,
  "finalPrice": null,
  "createdAt": "2023-12-01T10:00:00",
  "updatedAt": "2023-12-01T10:00:00"
}
```

## Authenticated Endpoints

### Create Task
```
POST /
```
Create a new task.

**Request Body:**
```json
{
  "title": "Math Assignment",
  "description": "Complete chapter 5 exercises",
  "budget": 50.00,
  "requirements": "Show all work",
  "category": "ASSIGNMENT",
  "priority": "MEDIUM",
  "deadline": "2023-12-31T23:59:59",
  "biddingDeadline": "2023-12-25T23:59:59"
}
```

**Response:**
```json
{
  "id": 1,
  "ownerId": 100,
  "ownerName": "John Doe",
  "title": "Math Assignment",
  "description": "Complete chapter 5 exercises",
  "budget": 50.00,
  "requirements": "Show all work",
  "status": "DRAFT",
  "category": "ASSIGNMENT",
  "priority": "MEDIUM",
  "deadline": "2023-12-31T23:59:59",
  "biddingDeadline": "2023-12-25T23:59:59",
  "assignedUserId": null,
  "assignedUserName": null,
  "finalPrice": null,
  "createdAt": "2023-12-01T10:00:00",
  "updatedAt": "2023-12-01T10:00:00"
}
```

### Publish Task
```
POST /{id}/publish
```
Publish a draft task to make it available for bidding.

**Response:**
```json
{
  "id": 1,
  "ownerId": 100,
  "ownerName": "John Doe",
  "title": "Math Assignment",
  "description": "Complete chapter 5 exercises",
  "budget": 50.00,
  "requirements": "Show all work",
  "status": "OPEN",
  "category": "ASSIGNMENT",
  "priority": "MEDIUM",
  "deadline": "2023-12-31T23:59:59",
  "biddingDeadline": "2023-12-25T23:59:59",
  "assignedUserId": null,
  "assignedUserName": null,
  "finalPrice": null,
  "createdAt": "2023-12-01T10:00:00",
  "updatedAt": "2023-12-01T11:00:00"
}
```

### Update Task
```
PUT /{id}
```
Update an existing task.

**Request Body:**
```json
{
  "title": "Updated Math Assignment",
  "description": "Complete chapter 5 and 6 exercises",
  "requirements": "Show all work and include diagrams",
  "priority": "HIGH",
  "deadline": "2023-12-30T23:59:59"
}
```

**Response:**
```json
{
  "id": 1,
  "ownerId": 100,
  "ownerName": "John Doe",
  "title": "Updated Math Assignment",
  "description": "Complete chapter 5 and 6 exercises",
  "budget": 50.00,
  "requirements": "Show all work and include diagrams",
  "status": "DRAFT",
  "category": "ASSIGNMENT",
  "priority": "HIGH",
  "deadline": "2023-12-30T23:59:59",
  "biddingDeadline": "2023-12-25T23:59:59",
  "assignedUserId": null,
  "assignedUserName": null,
  "finalPrice": null,
  "createdAt": "2023-12-01T10:00:00",
  "updatedAt": "2023-12-01T12:00:00"
}
```

### Cancel Task
```
DELETE /{id}
```
Cancel a task.

**Response:**
```
204 No Content
```

### Get My Tasks
```
GET /my-tasks
```
Get tasks created by the current user.

**Parameters:**
- `status` (optional) - Filter by status

**Response:**
```json
[
  {
    "id": 1,
    "ownerId": 100,
    "ownerName": "John Doe",
    "title": "Math Assignment",
    "description": "Complete chapter 5 exercises",
    "budget": 50.00,
    "requirements": "Show all work",
    "status": "OPEN",
    "category": "ASSIGNMENT",
    "priority": "MEDIUM",
    "deadline": "2023-12-31T23:59:59",
    "biddingDeadline": "2023-12-25T23:59:59",
    "assignedUserId": null,
    "assignedUserName": null,
    "finalPrice": null,
    "createdAt": "2023-12-01T10:00:00",
    "updatedAt": "2023-12-01T10:00:00"
  }
]
```

### Get Assigned Tasks
```
GET /assigned-to-me
```
Get tasks assigned to the current user.

**Parameters:**
- `status` (optional) - Filter by status

**Response:**
```json
[
  {
    "id": 2,
    "ownerId": 101,
    "ownerName": "Jane Smith",
    "title": "Physics Lab Report",
    "description": "Write lab report for experiment 3",
    "budget": 75.00,
    "requirements": "Include data analysis",
    "status": "ASSIGNED",
    "category": "LAB_RECORD",
    "priority": "HIGH",
    "deadline": "2023-12-20T23:59:59",
    "biddingDeadline": "2023-12-15T23:59:59",
    "assignedUserId": 100,
    "assignedUserName": "John Doe",
    "finalPrice": 70.00,
    "createdAt": "2023-12-01T09:00:00",
    "updatedAt": "2023-12-01T09:30:00"
  }
]
```

### Submit Work
```
POST /{id}/submit-work
```
Submit completed work for a task.

**Request Body:**
```json
{
  "workSubmissionUrl": "https://example.com/work.pdf",
  "workSubmissionNotes": "Please find the completed assignment attached."
}
```

**Response:**
```json
{
  "id": 2,
  "ownerId": 101,
  "ownerName": "Jane Smith",
  "title": "Physics Lab Report",
  "description": "Write lab report for experiment 3",
  "budget": 75.00,
  "requirements": "Include data analysis",
  "status": "SUBMITTED",
  "category": "LAB_RECORD",
  "priority": "HIGH",
  "deadline": "2023-12-20T23:59:59",
  "biddingDeadline": "2023-12-15T23:59:59",
  "assignedUserId": 100,
  "assignedUserName": "John Doe",
  "finalPrice": 70.00,
  "createdAt": "2023-12-01T09:00:00",
  "updatedAt": "2023-12-01T13:00:00"
}
```

### Review Work
```
POST /{id}/review-work
```
Review submitted work for a task.

**Request Body:**
```json
{
  "accepted": true,
  "rejectionReason": null
}
```

Or for rejected work:
```json
{
  "accepted": false,
  "rejectionReason": "Work does not meet requirements. Please include more detailed analysis."
}
```

**Response:**
```json
{
  "id": 2,
  "ownerId": 101,
  "ownerName": "Jane Smith",
  "title": "Physics Lab Report",
  "description": "Write lab report for experiment 3",
  "budget": 75.00,
  "requirements": "Include data analysis",
  "status": "COMPLETED",
  "category": "LAB_RECORD",
  "priority": "HIGH",
  "deadline": "2023-12-20T23:59:59",
  "biddingDeadline": "2023-12-15T23:59:59",
  "assignedUserId": 100,
  "assignedUserName": "John Doe",
  "finalPrice": 70.00,
  "createdAt": "2023-12-01T09:00:00",
  "updatedAt": "2023-12-01T14:00:00"
}
```

## Internal Service Endpoints

### Assign Task
```
POST /{id}/assign
```
Assign a task to a user (used by bidding service).

**Request Body:**
```json
{
  "assignedUserId": 100,
  "finalPrice": 70.00
}
```

### Update Task Status
```
POST /{id}/status
```
Update task status (used by other services).

**Request Body:**
```
COMPLETED
```

## Task Statuses
- `DRAFT` - Task created but not published
- `OPEN` - Task published, accepting bids
- `BIDDING_CLOSED` - Bidding period ended, selecting winner
- `ASSIGNED` - Task assigned to winner
- `IN_PROGRESS` - Work in progress
- `SUBMITTED` - Work submitted by assignee
- `COMPLETED` - Work accepted and payment processed
- `CANCELLED` - Task cancelled
- `REJECTED` - Work rejected

## Task Categories
- `ASSIGNMENT` - Academic assignments
- `LAB_RECORD` - Lab record writing
- `NOTES` - Note taking/making
- `PROJECT` - Small projects
- `RESEARCH` - Research work
- `PRESENTATION` - PPT creation
- `OTHER` - Other tasks

## Task Priorities
- `LOW`
- `MEDIUM`
- `HIGH`
- `URGENT`