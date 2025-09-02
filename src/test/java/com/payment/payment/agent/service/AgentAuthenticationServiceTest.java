package com.payment.payment.agent.service;

import com.payment.payment.agent.model.AgentContext;
import com.payment.payment.agent.model.AgentCredentials;
import com.payment.payment.agent.repository.AgentCredentialsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Basic unit tests for agent authentication
 * Focus on core functionality, not edge cases
 */
@ExtendWith(MockitoExtension.class)
class AgentAuthenticationServiceTest {

    @Mock
    private AgentCredentialsRepository agentRepository;

    @InjectMocks
    private AgentAuthenticationService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "jwtSecret", "test-secret-key-for-testing-only");
        ReflectionTestUtils.setField(authService, "jwtExpirationSeconds", 3600);
    }

    @Test
    void testAuthenticateAgent_ValidApiKey_ReturnsJwt() {
        // Given
        String apiKey = "valid-api-key";
        String apiKeyHash = String.valueOf(apiKey.hashCode());
        
        AgentCredentials agent = createTestAgent();
        when(agentRepository.findByApiKeyHashAndIsActive(apiKeyHash, true))
            .thenReturn(Optional.of(agent));
        when(agentRepository.save(any(AgentCredentials.class))).thenReturn(agent);

        // When
        Optional<String> result = authService.authenticateAgent(apiKey);

        // Then
        assertTrue(result.isPresent());
        assertNotNull(result.get());
        assertTrue(result.get().length() > 50); // JWT should be reasonably long
        
        verify(agentRepository).save(agent); // Should update last used time
    }

    @Test
    void testAuthenticateAgent_InvalidApiKey_ReturnsEmpty() {
        // Given
        String apiKey = "invalid-api-key";
        String apiKeyHash = String.valueOf(apiKey.hashCode());
        
        when(agentRepository.findByApiKeyHashAndIsActive(apiKeyHash, true))
            .thenReturn(Optional.empty());

        // When
        Optional<String> result = authService.authenticateAgent(apiKey);

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void testValidateToken_ValidJwt_ReturnsAgentContext() {
        // Given - first create a JWT
        AgentCredentials agent = createTestAgent();
        when(agentRepository.findByApiKeyHashAndIsActive(anyString(), eq(true)))
            .thenReturn(Optional.of(agent));
        when(agentRepository.save(any(AgentCredentials.class))).thenReturn(agent);
        
        String jwt = authService.authenticateAgent("valid-key").orElseThrow();

        // When
        Optional<AgentContext> result = authService.validateToken(jwt);

        // Then
        assertTrue(result.isPresent());
        AgentContext context = result.get();
        assertEquals("demo-agent-001", context.getAgentId());
        assertEquals("dev-123", context.getOwnerId());
        assertTrue(context.hasCapability("digital_goods"));
    }

    private AgentCredentials createTestAgent() {
        AgentCredentials agent = new AgentCredentials();
        agent.setAgentId("demo-agent-001");
        agent.setOwnerId("dev-123");
        agent.setApiKeyHash("1234567890");
        agent.setAgentType("demo-bot");
        agent.setIsActive(true);
        agent.setCreatedAt(LocalDateTime.now());
        agent.setDailySpendLimit(1000.0);
        agent.setMonthlySpendLimit(5000.0);
        agent.setPerTransactionLimit(500.0);
        agent.setCapabilities("digital_goods,api_calls");
        return agent;
    }
}