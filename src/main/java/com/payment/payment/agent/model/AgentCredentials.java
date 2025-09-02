package com.payment.payment.agent.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Stores agent API credentials in encrypted format
 * Replaces the old Token model with agent-specific fields
 */
@Entity
@Table(name = "agent_credentials")
@Data
public class AgentCredentials {
    
    @Id
    private String agentId;
    
    @Column(nullable = false)
    private String ownerId;  // Developer who owns this agent
    
    @Column(nullable = false)
    private String apiKeyHash;  // Hashed API key for validation
    
    @Column(nullable = false)
    private String encryptedSecrets;  // Encrypted payment credentials
    
    @Column(nullable = false)
    private String agentType;  // "openai-gpt4", "custom-bot", etc.
    
    @Column(nullable = false)
    private Boolean isActive = true;
    
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    private LocalDateTime lastUsedAt;
    
    // Spending limits stored in DB
    private Double dailySpendLimit;
    private Double monthlySpendLimit;
    private Double perTransactionLimit;
    
    // Capabilities as comma-separated string (simple approach for now)
    private String capabilities;  // "digital_goods,api_calls,subscriptions"
}