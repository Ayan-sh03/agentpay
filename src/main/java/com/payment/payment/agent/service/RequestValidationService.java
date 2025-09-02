package com.payment.payment.agent.service;

import com.payment.payment.agent.model.PurchaseRequest;
import org.springframework.stereotype.Service;

@Service
public class RequestValidationService {
    
    public void validatePurchaseRequest(PurchaseRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Purchase request cannot be null");
        }
        
        if (request.getAmount() <= 0) {
            throw new IllegalArgumentException("Purchase amount must be greater than zero");
        }
        
        if (request.getMerchant() == null || request.getMerchant().trim().isEmpty()) {
            throw new IllegalArgumentException("Merchant information is required");
        }
        
        if (request.getCurrency() == null || request.getCurrency().trim().isEmpty()) {
            throw new IllegalArgumentException("Currency information is required");
        }
        
        if (request.getProductType() == null || request.getProductType().trim().isEmpty()) {
            throw new IllegalArgumentException("Product type is required for agent purchases");
        }
        
        if (request.getProductId() == null || request.getProductId().trim().isEmpty()) {
            throw new IllegalArgumentException("Product ID is required for agent purchases");
        }
    }
}