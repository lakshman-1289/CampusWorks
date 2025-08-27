package com.campusworks.payment.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class RazorpayService {

    @Value("${app.payment.gateway.key}")
    private String razorpayKey;

    @Value("${app.payment.gateway.secret}")
    private String razorpaySecret;

    private RazorpayClient razorpayClient;

    private RazorpayClient getRazorpayClient() throws RazorpayException {
        if (razorpayClient == null) {
            razorpayClient = new RazorpayClient(razorpayKey, razorpaySecret);
        }
        return razorpayClient;
    }

    public PaymentService.RazorpayOrderResponse createOrder(BigDecimal amount, String receipt, String notes) {
        try {
            log.info("Creating Razorpay order for amount: {}, receipt: {}", amount, receipt);

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amount.multiply(BigDecimal.valueOf(100)).intValue()); // Convert to paise
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", receipt);
            orderRequest.put("notes", new JSONObject().put("description", notes));

            Order order = getRazorpayClient().orders.create(orderRequest);

            log.info("Razorpay order created successfully: {}", order.get("id"));

            return PaymentService.RazorpayOrderResponse.builder()
                    .id(order.get("id"))
                    .entity(order.get("entity"))
                    .amount(new BigDecimal(order.get("amount").toString()).divide(BigDecimal.valueOf(100)))
                    .currency(order.get("currency"))
                    .status(order.get("status"))
                    .build();

        } catch (RazorpayException e) {
            log.error("Failed to create Razorpay order: {}", e.getMessage());
            throw new RuntimeException("Failed to create payment order", e);
        }
    }

    public boolean verifyPayment(String orderId, String paymentId, String signature) {
        try {
            log.info("Verifying Razorpay payment: orderId={}, paymentId={}", orderId, paymentId);

            JSONObject attributes = new JSONObject();
            attributes.put("razorpay_order_id", orderId);
            attributes.put("razorpay_payment_id", paymentId);
            attributes.put("razorpay_signature", signature);

            boolean isValid = Utils.verifyPaymentSignature(attributes, razorpaySecret);
            
            log.info("Payment verification result: {}", isValid);
            return isValid;

        } catch (RazorpayException e) {
            log.error("Payment verification failed: {}", e.getMessage());
            return false;
        }
    }

    public void refundPayment(String paymentId, BigDecimal amount) {
        try {
            log.info("Initiating refund for payment: {}, amount: {}", paymentId, amount);

            JSONObject refundRequest = new JSONObject();
            refundRequest.put("amount", amount.multiply(BigDecimal.valueOf(100)).intValue()); // Convert to paise

            getRazorpayClient().payments.refund(paymentId, refundRequest);

            log.info("Refund initiated successfully for payment: {}", paymentId);

        } catch (RazorpayException e) {
            log.error("Failed to initiate refund for payment {}: {}", paymentId, e.getMessage());
            throw new RuntimeException("Failed to initiate refund", e);
        }
    }

    public String getKey() {
        return razorpayKey;
    }

    // For testing/simulation purposes
    public PaymentService.RazorpayOrderResponse createMockOrder(BigDecimal amount, String receipt) {
        log.info("Creating mock Razorpay order for amount: {}, receipt: {}", amount, receipt);
        
        return PaymentService.RazorpayOrderResponse.builder()
                .id("order_mock_" + System.currentTimeMillis())
                .entity("order")
                .amount(amount)
                .currency("INR")
                .status("created")
                .build();
    }

    public boolean verifyMockPayment(String orderId, String paymentId, String signature) {
        log.info("Mock payment verification: orderId={}, paymentId={}", orderId, paymentId);
        // For testing, always return true
        return true;
    }
}