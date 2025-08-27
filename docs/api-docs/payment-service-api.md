# Payment Service API Documentation

## Overview
The Payment Service manages the complete payment lifecycle including escrow management, payment gateway integration with Razorpay, commission calculation, and refund processing for the CampusWorks platform.

## Base URL
```
http://localhost:8080/payments
```

## Authentication
All endpoints require a valid JWT token in the Authorization header:
```
Authorization: Bearer <jwt-token>
```

## Endpoints

### 1. Create Escrow Payment
**POST** `/escrow/create`

Create an escrow payment when a task is assigned to a bidder.

**Request Body:**
```json
{
  "taskId": 1,
  "payerUserId": 123,
  "payeeUserId": 456,
  "amount": 75.00,
  "currency": "INR",
  "description": "Escrow payment for Data Entry Research Project"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Escrow payment created successfully",
  "data": {
    "id": 501,
    "taskId": 1,
    "payerUserId": 123,
    "payeeUserId": 456,
    "grossAmount": 75.00,
    "platformCommission": 7.50,
    "netAmount": 67.50,
    "currency": "INR",
    "status": "ESCROW_HELD",
    "escrowId": "ESC_001_20240115",
    "createdAt": "2024-01-15T10:30:00Z",
    "estimatedReleaseDate": "2024-01-19T10:30:00Z"
  }
}
```

### 2. Process Payment
**POST** `/process`

Process payment through Razorpay gateway (called by frontend after task assignment).

**Request Body:**
```json
{
  "paymentId": 501,
  "razorpayPaymentId": "pay_JQ8JgQVTqvU9if",
  "razorpayOrderId": "order_JQ8Jf4XGKqU9if",
  "razorpaySignature": "signature_hash_here",
  "paymentMethod": "card",
  "paymentDetails": {
    "cardLastFour": "1234",
    "cardNetwork": "visa"
  }
}
```

**Response:**
```json
{
  "success": true,
  "message": "Payment processed successfully",
  "data": {
    "id": 501,
    "status": "PAYMENT_COMPLETED",
    "razorpayPaymentId": "pay_JQ8JgQVTqvU9if",
    "processedAt": "2024-01-15T10:35:00Z",
    "escrowStatus": "FUNDS_SECURED",
    "transactionId": "TXN_501_20240115"
  }
}
```

### 3. Release Escrow Payment
**POST** `/escrow/{paymentId}/release`

Release escrow payment to the task performer after work completion and approval.

**Request Body:**
```json
{
  "releaseAmount": 67.50,
  "bonusAmount": 5.00,
  "releaseReason": "Task completed successfully with excellent quality",
  "performanceRating": 5
}
```

**Response:**
```json
{
  "success": true,
  "message": "Escrow payment released successfully",
  "data": {
    "id": 501,
    "status": "PAYMENT_RELEASED",
    "releaseAmount": 67.50,
    "bonusAmount": 5.00,
    "totalPayoutAmount": 72.50,
    "releasedAt": "2024-01-18T16:00:00Z",
    "payoutId": "payout_123456789",
    "estimatedTransferTime": "2-3 business days"
  }
}
```

### 4. Refund Payment
**POST** `/escrow/{paymentId}/refund`

Refund escrow payment to the task owner (in case of cancellation or dispute resolution).

**Request Body:**
```json
{
  "refundAmount": 75.00,
  "refundReason": "Task cancelled by owner",
  "refundType": "FULL_REFUND",
  "deductCommission": false
}
```

**Response:**
```json
{
  "success": true,
  "message": "Refund processed successfully",
  "data": {
    "id": 501,
    "status": "PAYMENT_REFUNDED",
    "refundAmount": 75.00,
    "refundedAt": "2024-01-16T09:00:00Z",
    "razorpayRefundId": "rfnd_JQ8Jf4XGKqU9if",
    "estimatedRefundTime": "5-7 business days",
    "refundReason": "Task cancelled by owner"
  }
}
```

### 5. Get Payment Details
**GET** `/{paymentId}`

Get detailed information about a specific payment.

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 501,
    "taskId": 1,
    "taskTitle": "Data Entry for Research Project",
    "payerUserId": 123,
    "payerUserName": "Jane Smith",
    "payeeUserId": 456,
    "payeeUserName": "John Doe",
    "grossAmount": 75.00,
    "platformCommission": 7.50,
    "platformCommissionRate": 10.0,
    "netAmount": 67.50,
    "bonusAmount": 0.00,
    "currency": "INR",
    "status": "ESCROW_HELD",
    "paymentMethod": "card",
    "razorpayPaymentId": "pay_JQ8JgQVTqvU9if",
    "razorpayOrderId": "order_JQ8Jf4XGKqU9if",
    "escrowId": "ESC_001_20240115",
    "createdAt": "2024-01-15T10:30:00Z",
    "processedAt": "2024-01-15T10:35:00Z",
    "estimatedReleaseDate": "2024-01-19T10:30:00Z",
    "timeline": [
      {
        "status": "ESCROW_CREATED",
        "timestamp": "2024-01-15T10:30:00Z",
        "description": "Escrow payment created"
      },
      {
        "status": "PAYMENT_COMPLETED",
        "timestamp": "2024-01-15T10:35:00Z",
        "description": "Payment processed through gateway"
      }
    ]
  }
}
```

### 6. Get User Payment History
**GET** `/user/history`

Get payment history for the current user.

**Query Parameters:**
- `role` (optional): 'payer' or 'payee' (default: both)
- `status` (optional): Filter by payment status
- `page` (optional): Page number (default: 0)
- `size` (optional): Page size (default: 20)
- `fromDate` (optional): Filter from date (ISO format)
- `toDate` (optional): Filter to date (ISO format)

**Response:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 501,
        "taskId": 1,
        "taskTitle": "Data Entry for Research Project",
        "role": "PAYER",
        "otherPartyName": "John Doe",
        "amount": 75.00,
        "netAmount": 67.50,
        "status": "PAYMENT_RELEASED",
        "createdAt": "2024-01-15T10:30:00Z",
        "completedAt": "2024-01-18T16:00:00Z"
      }
    ],
    "pageable": {
      "page": 0,
      "size": 20,
      "totalElements": 1,
      "totalPages": 1
    },
    "summary": {
      "totalPaid": 850.00,
      "totalReceived": 650.00,
      "totalCommissionPaid": 85.00,
      "pendingPayments": 150.00,
      "completedTransactions": 12
    }
  }
}
```

### 7. Get Payment Statistics
**GET** `/statistics`

Get payment statistics for the current user.

**Response:**
```json
{
  "success": true,
  "data": {
    "totalPayments": 15,
    "totalAmountPaid": 1250.00,
    "totalAmountReceived": 950.00,
    "totalCommissionPaid": 125.00,
    "averagePaymentAmount": 83.33,
    "largestPayment": 200.00,
    "smallestPayment": 25.00,
    "pendingEscrowAmount": 225.00,
    "paymentsByStatus": {
      "ESCROW_HELD": 3,
      "PAYMENT_RELEASED": 10,
      "PAYMENT_REFUNDED": 2
    },
    "monthlyBreakdown": [
      {
        "month": "2024-01",
        "totalPaid": 450.00,
        "totalReceived": 350.00,
        "transactionCount": 8
      }
    ]
  }
}
```

### 8. Create Payout Request
**POST** `/payout/request`

Request payout of accumulated earnings to bank account.

**Request Body:**
```json
{
  "amount": 500.00,
  "bankAccountId": "acc_123456789",
  "purpose": "Regular withdrawal of task earnings"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Payout request created successfully",
  "data": {
    "id": "payout_req_001",
    "amount": 500.00,
    "status": "PAYOUT_PENDING",
    "bankAccountId": "acc_123456789",
    "estimatedTransferTime": "2-3 business days",
    "processingFee": 5.00,
    "netPayoutAmount": 495.00,
    "createdAt": "2024-01-18T14:00:00Z"
  }
}
```

### 9. Get Wallet Balance
**GET** `/wallet/balance`

Get current wallet balance for the user.

**Response:**
```json
{
  "success": true,
  "data": {
    "userId": 456,
    "availableBalance": 750.00,
    "escrowBalance": 225.00,
    "totalEarnings": 1200.00,
    "pendingPayouts": 100.00,
    "currency": "INR",
    "lastUpdated": "2024-01-18T16:30:00Z",
    "recentTransactions": [
      {
        "id": 501,
        "type": "PAYMENT_RECEIVED",
        "amount": 67.50,
        "description": "Payment for Data Entry task",
        "timestamp": "2024-01-18T16:00:00Z"
      }
    ]
  }
}
```

### 10. Handle Payment Webhook
**POST** `/webhook/razorpay`

Handle webhooks from Razorpay for payment status updates.

**Request Body:**
```json
{
  "entity": "event",
  "account_id": "acc_BFQ7uQEaa30PjA",
  "event": "payment.captured",
  "contains": ["payment"],
  "payload": {
    "payment": {
      "entity": {
        "id": "pay_JQ8JgQVTqvU9if",
        "amount": 7500,
        "currency": "INR",
        "status": "captured"
      }
    }
  }
}
```

**Response:**
```json
{
  "success": true,
  "message": "Webhook processed successfully"
}
```

### 11. Dispute Payment
**POST** `/dispute/create`

Create a payment dispute (in case of disagreement).

**Request Body:**
```json
{
  "paymentId": 501,
  "disputeType": "QUALITY_ISSUE",
  "description": "The submitted work does not meet the requirements specified in the task",
  "evidence": [
    {
      "type": "SCREENSHOT",
      "fileUrl": "https://storage.campusworks.com/disputes/evidence_001.jpg"
    }
  ]
}
```

**Response:**
```json
{
  "success": true,
  "message": "Dispute created successfully",
  "data": {
    "disputeId": "DISP_001",
    "paymentId": 501,
    "status": "DISPUTE_OPEN",
    "estimatedResolutionTime": "5-7 business days",
    "createdAt": "2024-01-20T10:00:00Z"
  }
}
```

### 12. Get Commission Structure
**GET** `/commission/structure`

Get current platform commission structure.

**Response:**
```json
{
  "success": true,
  "data": {
    "defaultCommissionRate": 10.0,
    "tierBasedCommission": [
      {
        "tier": "NEW_USER",
        "minTransactions": 0,
        "maxTransactions": 5,
        "commissionRate": 12.0
      },
      {
        "tier": "REGULAR_USER",
        "minTransactions": 6,
        "maxTransactions": 20,
        "commissionRate": 10.0
      },
      {
        "tier": "VIP_USER",
        "minTransactions": 21,
        "maxTransactions": null,
        "commissionRate": 8.0
      }
    ],
    "minimumCommission": 2.00,
    "maximumCommission": 50.00,
    "currency": "INR"
  }
}
```

## Payment Status Flow

1. **ESCROW_CREATED** → Escrow payment record created
2. **PAYMENT_PENDING** → Awaiting payment gateway processing
3. **PAYMENT_COMPLETED** → Payment successfully processed
4. **ESCROW_HELD** → Funds secured in escrow
5. **PAYMENT_RELEASED** → Funds released to payee
6. **PAYMENT_REFUNDED** → Funds refunded to payer
7. **PAYMENT_FAILED** → Payment processing failed
8. **DISPUTE_RAISED** → Payment disputed

## Payment Methods Supported

- **Credit/Debit Cards** (Visa, Mastercard, RuPay)
- **Net Banking** (All major Indian banks)
- **UPI** (PhonePe, Google Pay, Paytm, etc.)
- **Wallets** (Paytm, Mobikwik, etc.)

## Error Responses

### 400 Bad Request
```json
{
  "success": false,
  "message": "Insufficient wallet balance for payment"
}
```

### 404 Not Found
```json
{
  "success": false,
  "message": "Payment record not found"
}
```

### 409 Conflict
```json
{
  "success": false,
  "message": "Payment has already been processed"
}
```

### 422 Unprocessable Entity
```json
{
  "success": false,
  "message": "Payment gateway declined the transaction",
  "gatewayError": {
    "code": "CARD_DECLINED",
    "description": "Your card was declined by the issuing bank"
  }
}
```

## Security & Compliance

- **PCI DSS Compliance** through Razorpay
- **Encrypted** payment data storage
- **Webhook signature verification** for security
- **Escrow protection** for both parties
- **Dispute resolution** mechanism
- **Audit trail** for all transactions