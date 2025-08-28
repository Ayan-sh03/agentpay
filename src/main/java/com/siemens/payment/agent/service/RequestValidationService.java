package com.siemens.payment.agent.service;

import com.siemens.payment.agent.model.PurchaseRequest;
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
        
        if (request.getMcc() == null || request.getMcc().trim().isEmpty()) {
            throw new IllegalArgumentException("MCC (Merchant Category Code) is required");
        }
    }
}