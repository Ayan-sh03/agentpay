package com.payment.payment.agent.model;

import lombok.Data;
import lombok.Builder;
import java.util.Set;
import java.time.LocalDateTime;

/**
 * Represents the context and capabilities of an AI agent
 * This is what gets stored in JWT claims for agent authentication
 */
@Data
@Builder
public class AgentContext {
    // Core agent identity
    private String agentId;
    private String agentName;
    private String agentType;  // "openai-gpt4", "anthropic-claude", "custom-bot", etc.
    
    // Ownership and permissions
    private String ownerId;    // Developer who owns this agent
    private String ownerEmail;
    private Set<String> capabilities;  // "digital_goods", "api_calls", "subscriptions"
    
    // Spending controls
    private Double dailySpendLimit;
    private Double monthlySpendLimit;
    private Double perTransactionLimit;
    
    // Metadata
    private LocalDateTime createdAt;
    private LocalDateTime lastUsedAt;
    private Boolean isActive;
    
    // API access levels
    private String accessLevel;  // "sandbox", "production", "premium"
    
    /**
     * Check if agent has a specific capability
     */
    public boolean hasCapability(String capability) {
        return capabilities != null && capabilities.contains(capability);
    }
    
    /**
     * Check if agent can make a purchase of given amount
     */
    public boolean canSpend(Double amount) {
        if (!isActive) return false;
        if (perTransactionLimit != null && amount > perTransactionLimit) return false;
        return true;
    }
}