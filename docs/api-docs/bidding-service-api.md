# Bidding Service API Documentation

## Overview
The Bidding Service manages the competitive bidding process for tasks, including bid submission, winner selection, and bid management with automated workflows.

## Base URL
```
http://localhost:8080/bidding
```

## Authentication
All endpoints require a valid JWT token in the Authorization header:
```
Authorization: Bearer <jwt-token>
```

## Endpoints

### 1. Place Bid
**POST** `/bid`

Submit a bid for an open task.

**Request Body:**
```json
{
  "taskId": 1,
  "amount": 75.00,
  "proposal": "I have extensive experience with data entry and Excel. I have completed similar psychology research projects before and can guarantee accurate and timely completion. I will double-check all entries for accuracy.",
  "estimatedCompletionTime": "2 days",
  "previousWorkSamples": [
    {
      "fileName": "sample_work_1.xlsx",
      "fileUrl": "https://storage.campusworks.com/samples/sample_work_1.xlsx"
    }
  ]
}
```

**Response:**
```json
{
  "success": true,
  "message": "Bid placed successfully",
  "data": {
    "id": 101,
    "taskId": 1,
    "bidderId": 456,
    "amount": 75.00,
    "proposal": "I have extensive experience with data entry...",
    "status": "ACTIVE",
    "estimatedCompletionTime": "2 days",
    "createdAt": "2024-01-15T11:30:00Z",
    "rank": 3
  }
}
```

### 2. Get Task Bids
**GET** `/task/{taskId}/bids`

Get all bids for a specific task (task owner only).

**Query Parameters:**
- `sortBy` (optional): Sort by 'amount', 'createdAt', or 'rating' (default: 'amount')
- `sortDirection` (optional): 'asc' or 'desc' (default: 'asc')

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": 101,
      "bidderId": 456,
      "bidderName": "John Doe",
      "bidderRating": 4.6,
      "bidderCompletedTasks": 15,
      "amount": 65.00,
      "proposal": "I have extensive experience with data entry and Excel...",
      "estimatedCompletionTime": "2 days",
      "status": "ACTIVE",
      "rank": 1,
      "previousWorkSamples": [
        {
          "fileName": "sample_work_1.xlsx",
          "fileUrl": "https://storage.campusworks.com/samples/sample_work_1.xlsx"
        }
      ],
      "createdAt": "2024-01-15T11:00:00Z"
    },
    {
      "id": 102,
      "bidderId": 789,
      "bidderName": "Sarah Wilson",
      "bidderRating": 4.8,
      "bidderCompletedTasks": 22,
      "amount": 70.00,
      "proposal": "Psychology student with research experience...",
      "estimatedCompletionTime": "1.5 days",
      "status": "ACTIVE",
      "rank": 2,
      "createdAt": "2024-01-15T11:15:00Z"
    }
  ]
}
```

### 3. Get My Bids
**GET** `/my-bids`

Get all bids placed by the current user.

**Query Parameters:**
- `status` (optional): Filter by bid status ('ACTIVE', 'WON', 'LOST', 'WITHDRAWN')
- `page` (optional): Page number (default: 0)
- `size` (optional): Page size (default: 20)

**Response:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 101,
        "taskId": 1,
        "taskTitle": "Data Entry for Research Project",
        "taskOwnerName": "Jane Smith",
        "amount": 75.00,
        "status": "ACTIVE",
        "rank": 3,
        "totalBids": 8,
        "currentLowestBid": 65.00,
        "biddingEndTime": "2024-01-16T10:30:00Z",
        "timeRemaining": "13h 45m",
        "createdAt": "2024-01-15T11:30:00Z"
      }
    ],
    "pageable": {
      "page": 0,
      "size": 20,
      "totalElements": 1,
      "totalPages": 1
    }
  }
}
```

### 4. Update Bid
**PUT** `/bid/{bidId}`

Update an existing bid (only if bidding is still open).

**Request Body:**
```json
{
  "amount": 68.00,
  "proposal": "Updated proposal with more details about my experience...",
  "estimatedCompletionTime": "1.5 days"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Bid updated successfully",
  "data": {
    "id": 101,
    "amount": 68.00,
    "proposal": "Updated proposal with more details...",
    "rank": 2,
    "updatedAt": "2024-01-15T12:00:00Z"
  }
}
```

### 5. Withdraw Bid
**DELETE** `/bid/{bidId}`

Withdraw a bid (only if bidding is still open).

**Response:**
```json
{
  "success": true,
  "message": "Bid withdrawn successfully",
  "data": {
    "id": 101,
    "status": "WITHDRAWN",
    "withdrawnAt": "2024-01-15T12:30:00Z"
  }
}
```

### 6. Get Winning Bid
**GET** `/task/{taskId}/winner`

Get the winning bid for a task.

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 101,
    "taskId": 1,
    "bidderId": 456,
    "bidderName": "John Doe",
    "bidderRating": 4.6,
    "amount": 65.00,
    "proposal": "I have extensive experience with data entry...",
    "status": "WON",
    "selectedAt": "2024-01-16T10:30:00Z",
    "selectionMethod": "AUTOMATIC_LOWEST"
  }
}
```

### 7. Manual Winner Selection
**POST** `/task/{taskId}/select-winner`

Manually select a winner (task owner only, overrides automatic selection).

**Request Body:**
```json
{
  "bidId": 102,
  "reason": "Selected based on bidder's high rating and relevant experience"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Winner selected successfully",
  "data": {
    "taskId": 1,
    "winningBidId": 102,
    "winnerUserId": 789,
    "winnerName": "Sarah Wilson",
    "winningAmount": 70.00,
    "selectionMethod": "MANUAL_OVERRIDE",
    "selectedAt": "2024-01-15T14:00:00Z"
  }
}
```

### 8. Get Bidding Statistics
**GET** `/statistics`

Get bidding statistics for the current user.

**Response:**
```json
{
  "success": true,
  "data": {
    "totalBidsPlaced": 25,
    "bidsWon": 8,
    "bidsLost": 15,
    "activeBids": 2,
    "winRate": 32.0,
    "averageBidAmount": 85.50,
    "averageWinningAmount": 78.25,
    "totalEarnings": 626.00,
    "highestBidAmount": 150.00,
    "lowestBidAmount": 25.00,
    "lastBidDate": "2024-01-15T11:30:00Z"
  }
}
```

### 9. Get Bidding Analytics
**GET** `/task/{taskId}/analytics`

Get detailed bidding analytics for a task (task owner only).

**Response:**
```json
{
  "success": true,
  "data": {
    "taskId": 1,
    "totalBids": 8,
    "averageBidAmount": 78.75,
    "lowestBidAmount": 65.00,
    "highestBidAmount": 95.00,
    "medianBidAmount": 77.50,
    "bidderRatingDistribution": {
      "4.0-4.5": 2,
      "4.5-5.0": 6
    },
    "bidSubmissionTimeline": [
      {
        "hour": "11:00",
        "bidCount": 3
      },
      {
        "hour": "12:00",
        "bidCount": 2
      },
      {
        "hour": "13:00",
        "bidCount": 3
      }
    ],
    "competitionLevel": "HIGH",
    "recommendedAction": "AUTO_SELECT",
    "estimatedSavings": 15.00
  }
}
```

### 10. Get Trending Tasks
**GET** `/trending`

Get tasks with high bidding activity.

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "taskId": 5,
      "taskTitle": "Website Content Writing",
      "bidCount": 15,
      "averageBidAmount": 120.00,
      "biddingEndTime": "2024-01-16T18:00:00Z",
      "competitionLevel": "VERY_HIGH",
      "category": "WRITING"
    },
    {
      "taskId": 3,
      "taskTitle": "Statistics Data Analysis",
      "bidCount": 12,
      "averageBidAmount": 95.00,
      "biddingEndTime": "2024-01-16T14:00:00Z",
      "competitionLevel": "HIGH",
      "category": "RESEARCH"
    }
  ]
}
```

### 11. Validate Bid Eligibility
**GET** `/task/{taskId}/can-bid`

Check if current user can bid on a specific task.

**Response:**
```json
{
  "success": true,
  "data": {
    "canBid": true,
    "alreadyBid": false,
    "isOwner": false,
    "biddingOpen": true,
    "timeRemaining": "14h 23m",
    "restrictions": []
  }
}
```

### 12. Bid Recommendations
**GET** `/task/{taskId}/recommendations`

Get bid amount recommendations based on market analysis.

**Response:**
```json
{
  "success": true,
  "data": {
    "taskId": 1,
    "currentLowestBid": 65.00,
    "recommendedBidRange": {
      "min": 60.00,
      "max": 70.00
    },
    "optimalBidAmount": 63.00,
    "winProbability": {
      "at60": 95,
      "at63": 85,
      "at65": 70,
      "at70": 40
    },
    "marketAnalysis": {
      "averageBidForCategory": 78.50,
      "competitionLevel": "MEDIUM",
      "priceTrend": "DECREASING"
    }
  }
}
```

## Bid Status Flow

1. **ACTIVE** → Bid is active and competing
2. **WON** → Bid won the task
3. **LOST** → Bid lost (another bid won)
4. **WITHDRAWN** → Bid withdrawn by bidder
5. **EXPIRED** → Bidding period ended without winning

## Winner Selection Methods

- **AUTOMATIC_LOWEST** → Automatically selects lowest bid when bidding ends
- **MANUAL_OVERRIDE** → Task owner manually selects a specific bid
- **NO_WINNER** → No winner selected (e.g., all bids too high)

## Error Responses

### 400 Bad Request
```json
{
  "success": false,
  "message": "Bid amount must be between task minimum and maximum budget"
}
```

### 403 Forbidden
```json
{
  "success": false,
  "message": "Cannot bid on your own task"
}
```

### 409 Conflict
```json
{
  "success": false,
  "message": "You have already placed a bid on this task"
}
```

### 410 Gone
```json
{
  "success": false,
  "message": "Bidding period has ended for this task"
}
```

## Business Rules

- Users cannot bid on their own tasks
- Only one bid per user per task (can be updated)
- Bid amount must be within task budget range
- Bids can only be placed/updated while bidding is open
- Automatic winner selection occurs when bidding period ends
- Winning bidder is automatically assigned to the task