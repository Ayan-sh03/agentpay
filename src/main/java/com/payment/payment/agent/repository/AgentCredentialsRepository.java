package com.payment.payment.agent.repository;

import com.payment.payment.agent.model.AgentCredentials;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

/**
 * Repository for managing agent credentials and metadata
 */
@Repository
public interface AgentCredentialsRepository extends JpaRepository<AgentCredentials, String> {
    
    /**
     * Find agent by API key hash (for authentication)
     */
    Optional<AgentCredentials> findByApiKeyHashAndIsActive(String apiKeyHash, Boolean isActive);
    
    /**
     * Find all agents owned by a specific developer
     */
    List<AgentCredentials> findByOwnerIdAndIsActive(String ownerId, Boolean isActive);
    
    /**
     * Find agents by type (for analytics)
     */
    @Query("SELECT a FROM AgentCredentials a WHERE a.agentType = :agentType AND a.isActive = true")
    List<AgentCredentials> findActiveAgentsByType(String agentType);
}