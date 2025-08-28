package com.siemens.payment.agent.model;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Data
public class PurchaseRequest {
    private String userId;
    private String agentId;
    
    @Positive(message = "Amount must be greater than zero")
    private double amount;
    
    @NotBlank(message = "Merchant is required")
    private String merchant;
    
    @NotBlank(message = "MCC (Merchant Category Code) is required")
    private String mcc; // Merchant Category Code
    
    @NotBlank(message = "Currency is required")
    private String currency = "USD"; // Default to USD
}
