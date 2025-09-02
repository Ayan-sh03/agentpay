package com.payment.payment.agent.model;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;

/**
 * Purchase request model optimized for AI agents
 * Focus on digital goods and services agents typically need
 */
@Data
public class PurchaseRequest {
    // Agent context (will be populated from JWT)
    private String agentId;
    private String ownerId;
    
    @Positive(message = "Amount must be greater than zero")
    private double amount;
    
    @NotBlank(message = "Merchant is required")
    private String merchant;  // e.g., "openai_api", "udemy", "envato_market"
    
    @NotBlank(message = "Product type is required")
    private String productType;  // "course", "template", "api_credits", "subscription"
    
    @NotBlank(message = "Product ID is required") 
    private String productId;   // Specific item being purchased
    
    @Pattern(regexp = "USD|EUR|GBP", message = "Currency must be USD, EUR, or GBP")
    private String currency = "USD";
    
    // Agent-specific fields
    private String description;  // What the agent is buying
    private String category;     // "education", "design", "api", "tools"
    
    // Optional metadata for digital goods
    private String licenseType;  // "single_use", "unlimited", "commercial"
    private Integer quantity = 1;  // Number of licenses/credits
}
