package com.payment.payment.agent.service;

import com.payment.payment.agent.model.PurchaseRequest;
import lombok.Data;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;

/**
 * Mock payment processor for testing and development
 * Simulates real payment gateway behavior without actual charges
 * Perfect for side projects and demos
 */
@Service
public class MockPaymentProcessor {

    private final Random random = new Random();

    /**
     * Process payment (mock implementation)
     * Simulates various real-world payment scenarios
     */
    public Mono<PaymentResult> processPayment(PurchaseRequest request) {
        return Mono.fromCallable(() -> {
            // Simulate processing delay
            Thread.sleep(100 + random.nextInt(200)); // 100-300ms delay
            
            // Simulate different outcomes based on amount
            if (request.getAmount() > 10000) {
                return createFailedResult("Amount too high for demo processor");
            }
            
            if (request.getMerchant().contains("test_fail")) {
                return createFailedResult("Merchant not supported");
            }
            
            // 95% success rate for realistic simulation
            if (random.nextInt(100) < 5) {
                return createFailedResult("Random processing failure (simulated)");
            }
            
            return createSuccessResult(request);
        });
    }

    /**
     * Refund payment (mock implementation)
     */
    public Mono<PaymentResult> refundPayment(String transactionId, double amount) {
        return Mono.fromCallable(() -> {
            Thread.sleep(50 + random.nextInt(100)); // Faster for refunds
            
            return PaymentResult.builder()
                .success(true)
                .transactionId(transactionId)
                .message("Refund processed successfully")
                .processedAt(LocalDateTime.now())
                .metadata(Map.of("refund_amount", amount))
                .build();
        });
    }

    private PaymentResult createSuccessResult(PurchaseRequest request) {
        return PaymentResult.builder()
            .success(true)
            .transactionId(java.util.UUID.randomUUID().toString())
            .message("Payment processed successfully")
            .processedAt(LocalDateTime.now())
            .metadata(Map.of(
                "merchant", request.getMerchant(),
                "product_type", request.getProductType(),
                "amount", request.getAmount(),
                "currency", request.getCurrency()
            ))
            .build();
    }

    private PaymentResult createFailedResult(String reason) {
        return PaymentResult.builder()
            .success(false)
            .transactionId(null)
            .message("Payment failed: " + reason)
            .processedAt(LocalDateTime.now())
            .metadata(Map.of("failure_reason", reason))
            .build();
    }

    /**
     * Payment processing result
     */
    @Data
    @lombok.Builder
    public static class PaymentResult {
        private boolean success;
        private String transactionId;  // Payment processor transaction ID
        private String message;
        private LocalDateTime processedAt;
        private Map<String, Object> metadata;
    }
}