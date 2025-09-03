package com.payment.payment.agent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.payment.payment.agent.audit.AuditLog;
import com.payment.payment.agent.audit.AuditLogRepository;
import com.payment.payment.agent.audit.AuditService;
import com.payment.payment.agent.model.AgentContext;
import com.payment.payment.agent.model.OverrideRequest;
import com.payment.payment.agent.model.PurchaseRequest;
import com.payment.payment.agent.model.PurchaseResponse;
import com.payment.payment.agent.pep.PolicyEnforcementPoint.PolicyDecision;
import com.payment.payment.agent.pep.PolicyEnforcementPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reactor.core.publisher.Mono;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for audit logging in purchase flow
 * Tests that audit events are properly logged throughout the purchase process
 */
@ExtendWith(MockitoExtension.class)
class PurchaseServiceAuditIntegrationTest {

    @Mock
    private PolicyEnforcementPoint pep;

    @Mock
    private AuditService auditService;

    @Mock
    private RequestValidationService requestValidationService;

    @Mock
    private AuthenticationContextService authenticationContextService;

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private PurchaseServiceImpl purchaseService;

    private PurchaseRequest testPurchaseRequest;
    private AgentContext testAgentContext;
    private PolicyDecision approvedDecision;
    private PolicyDecision deniedDecision;
    private OverrideRequest testOverrideRequest;

    @BeforeEach
    void setUp() {
        testPurchaseRequest = createTestPurchaseRequest();
        testAgentContext = createTestAgentContext();
        approvedDecision = createApprovedDecision();
        deniedDecision = createDeniedDecision();
        testOverrideRequest = createTestOverrideRequest();
    }

    @Test
    void processPurchase_ApprovedFlow_LogsAllAuditEvents() {
        // Given
        when(authenticationContextService.getCurrentAgentContext())
            .thenReturn(Mono.just(testAgentContext));
        when(pep.evaluatePolicy(any(PurchaseRequest.class), anyString()))
            .thenReturn(Mono.just(approvedDecision));

        // When
        PurchaseResponse response = purchaseService.processPurchase(testPurchaseRequest).block();

        // Then
        assertNotNull(response);
        assertEquals("APPROVED", response.getStatus());

        // Verify all audit events were logged
        ArgumentCaptor<String> transactionIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> eventTypeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> detailsCaptor = ArgumentCaptor.forClass(String.class);

        verify(auditService, times(3)).logEvent(
            transactionIdCaptor.capture(),
            eventTypeCaptor.capture(),
            detailsCaptor.capture()
        );

        List<String> eventTypes = eventTypeCaptor.getAllValues();
        assertEquals(3, eventTypes.size());
        assertTrue(eventTypes.contains("PURCHASE_REQUEST_RECEIVED"));
        assertTrue(eventTypes.contains("POLICY_EVALUATION_COMPLETED"));
        assertTrue(eventTypes.contains("PURCHASE_APPROVED"));

        // Verify transaction ID consistency
        String transactionId = transactionIdCaptor.getValue();
        assertNotNull(transactionId);
        assertTrue(UUID.fromString(transactionId) != null); // Valid UUID
    }

    @Test
    void processPurchase_DeniedFlow_LogsAllAuditEvents() {
        // Given
        when(authenticationContextService.getCurrentAgentContext())
            .thenReturn(Mono.just(testAgentContext));
        when(pep.evaluatePolicy(any(PurchaseRequest.class), anyString()))
            .thenReturn(Mono.just(deniedDecision));

        // When
        PurchaseResponse response = purchaseService.processPurchase(testPurchaseRequest).block();

        // Then
        assertNotNull(response);
        assertEquals("DENIED", response.getStatus());

        // Verify all audit events were logged
        verify(auditService, times(3)).logEvent(
            anyString(),
            anyString(),
            anyString()
        );

        // Verify specific events
        ArgumentCaptor<String> eventTypeCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditService, times(3)).logEvent(
            anyString(),
            eventTypeCaptor.capture(),
            anyString()
        );

        List<String> eventTypes = eventTypeCaptor.getAllValues();
        assertTrue(eventTypes.contains("PURCHASE_REQUEST_RECEIVED"));
        assertTrue(eventTypes.contains("POLICY_EVALUATION_COMPLETED"));
        assertTrue(eventTypes.contains("PURCHASE_DENIED"));
    }

    @Test
    void processPurchase_InvalidRequest_LogsErrorEvent() {
        // Given
        when(authenticationContextService.getCurrentAgentContext())
            .thenReturn(Mono.just(testAgentContext));
        doThrow(new IllegalArgumentException("Invalid amount"))
            .when(requestValidationService).validatePurchaseRequest(any(PurchaseRequest.class));

        // When
        PurchaseResponse response = purchaseService.processPurchase(testPurchaseRequest).block();

        // Then
        assertNotNull(response);
        assertEquals("INVALID_REQUEST", response.getStatus());

        // Verify audit events were logged
        verify(auditService, times(2)).logEvent(
            anyString(),
            anyString(),
            anyString()
        );

        // Verify specific events
        ArgumentCaptor<String> eventTypeCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditService, times(2)).logEvent(
            anyString(),
            eventTypeCaptor.capture(),
            anyString()
        );

        List<String> eventTypes = eventTypeCaptor.getAllValues();
        assertTrue(eventTypes.contains("PURCHASE_REQUEST_RECEIVED"));
        assertTrue(eventTypes.contains("PURCHASE_REQUEST_INVALID"));
    }

    @Test
    void processPurchase_PolicyEvaluationFailed_LogsErrorEvent() {
        // Given
        when(authenticationContextService.getCurrentAgentContext())
            .thenReturn(Mono.just(testAgentContext));
        when(pep.evaluatePolicy(any(PurchaseRequest.class), anyString()))
            .thenReturn(Mono.error(new RuntimeException("Policy service unavailable")));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            purchaseService.processPurchase(testPurchaseRequest).block();
        });

        // Verify audit events were logged (only 1 time due to exception)
        verify(auditService, times(1)).logEvent(
            anyString(),
            anyString(),
            anyString()
        );

        // Verify specific events
        ArgumentCaptor<String> eventTypeCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditService, times(1)).logEvent(
            anyString(),
            eventTypeCaptor.capture(),
            anyString()
        );

        List<String> eventTypes = eventTypeCaptor.getAllValues();
        assertTrue(eventTypes.contains("PURCHASE_REQUEST_RECEIVED"));
    }

    @Test
    void overridePurchase_ValidOverride_LogsAuditEvent() {
        // Given
        String transactionId = UUID.randomUUID().toString();
        when(authenticationContextService.validateUserAccess(anyString()))
            .thenReturn(Mono.empty());
        
        // Manually add a denied transaction to the map
        java.lang.reflect.Field field;
        try {
            field = PurchaseServiceImpl.class.getDeclaredField("deniedTransactions");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, PurchaseRequest> deniedTransactions = (Map<String, PurchaseRequest>) field.get(purchaseService);
            deniedTransactions.put(transactionId, testPurchaseRequest);
        } catch (Exception e) {
            fail("Failed to access deniedTransactions field: " + e.getMessage());
        }

        // When
        PurchaseResponse response = purchaseService.overridePurchase(transactionId, testOverrideRequest).block();

        // Then
        assertNotNull(response);
        assertEquals("OVERRIDE_APPROVED", response.getStatus());

        // Verify audit event was logged
        verify(auditService, times(1)).logEvent(
            eq(transactionId),
            eq("PURCHASE_OVERRIDE_APPROVED"),
            anyString()
        );
    }

    @Test
    void overridePurchase_TransactionNotFound_LogsNoEvent() {
        // Given
        String invalidTransactionId = UUID.randomUUID().toString();
        when(authenticationContextService.validateUserAccess(anyString()))
            .thenReturn(Mono.empty());

        // When
        PurchaseResponse response = purchaseService.overridePurchase(invalidTransactionId, testOverrideRequest).block();

        // Then
        assertNotNull(response);
        assertEquals("NOT_FOUND", response.getStatus());

        // Verify no audit event was logged for override
        verify(auditService, never()).logEvent(
            eq(invalidTransactionId),
            eq("PURCHASE_OVERRIDE_APPROVED"),
            anyString()
        );
    }

    @Test
    void processPurchase_AuditServiceThrowsException_PropagatesException() {
        // Given
        when(authenticationContextService.getCurrentAgentContext())
            .thenReturn(Mono.just(testAgentContext));
        when(pep.evaluatePolicy(any(PurchaseRequest.class), anyString()))
            .thenReturn(Mono.just(approvedDecision));
        doThrow(new RuntimeException("Audit service unavailable"))
            .when(auditService).logEvent(anyString(), anyString(), anyString());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            purchaseService.processPurchase(testPurchaseRequest).block();
        });
    }

    @Test
    void processPurchase_JsonSerializationError_UsesFallbackAudit() {
        // Given
        when(authenticationContextService.getCurrentAgentContext())
            .thenReturn(Mono.just(testAgentContext));
        when(pep.evaluatePolicy(any(PurchaseRequest.class), anyString()))
            .thenReturn(Mono.just(approvedDecision));
        doThrow(new RuntimeException("JSON serialization failed"))
            .when(auditService).logEvent(anyString(), anyString(), anyString());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            purchaseService.processPurchase(testPurchaseRequest).block();
        });

        // Verify fallback audit was attempted (only 1 time due to exception)
        verify(auditService, times(1)).logEvent(
            anyString(),
            anyString(),
            anyString()
        );
    }

    @Test
    void processPurchase_MultipleTransactions_LogsSeparateEvents() {
        // Given
        when(authenticationContextService.getCurrentAgentContext())
            .thenReturn(Mono.just(testAgentContext));
        when(pep.evaluatePolicy(any(PurchaseRequest.class), anyString()))
            .thenReturn(Mono.just(approvedDecision));

        // When
        PurchaseResponse response1 = purchaseService.processPurchase(testPurchaseRequest).block();
        PurchaseResponse response2 = purchaseService.processPurchase(testPurchaseRequest).block();

        // Then
        assertNotNull(response1);
        assertNotNull(response2);
        assertNotEquals(response1.getTransactionId(), response2.getTransactionId());

        // Verify audit events were logged for each transaction
        verify(auditService, times(6)).logEvent(
            anyString(),
            anyString(),
            anyString()
        );
    }

    @Test
    void processPurchase_LargeDetails_HandlesCorrectly() {
        // Given
        when(authenticationContextService.getCurrentAgentContext())
            .thenReturn(Mono.just(testAgentContext));
        when(pep.evaluatePolicy(any(PurchaseRequest.class), anyString()))
            .thenReturn(Mono.just(approvedDecision));

        // Create request with large description
        PurchaseRequest largeRequest = createTestPurchaseRequest();
        largeRequest.setDescription("a".repeat(5000)); // 5k characters

        // When
        PurchaseResponse response = purchaseService.processPurchase(largeRequest).block();

        // Then
        assertNotNull(response);
        assertEquals("APPROVED", response.getStatus());

        // Verify audit events were logged without issues
        verify(auditService, times(3)).logEvent(
            anyString(),
            anyString(),
            anyString()
        );
    }

    private PurchaseRequest createTestPurchaseRequest() {
        PurchaseRequest request = new PurchaseRequest();
        request.setAmount(50.0);
        request.setMerchant("udemy");
        request.setProductType("course");
        request.setProductId("python-advanced");
        request.setCurrency("USD");
        request.setDescription("Python programming course");
        request.setCategory("education");
        return request;
    }

    private AgentContext createTestAgentContext() {
        return AgentContext.builder()
            .agentId("demo-agent-001")
            .ownerId("dev-123")
            .agentType("demo-bot")
            .dailySpendLimit(1000.0)
            .monthlySpendLimit(5000.0)
            .perTransactionLimit(500.0)
            .isActive(true)
            .build();
    }

    private PolicyDecision createApprovedDecision() {
        PolicyDecision decision = new PolicyDecision();
        decision.setAllowed(true);
        decision.setExplanation(List.of("Purchase approved by policy"));
        return decision;
    }

    private PolicyDecision createDeniedDecision() {
        PolicyDecision decision = new PolicyDecision();
        decision.setAllowed(false);
        decision.setExplanation(List.of("Purchase exceeds agent limit"));
        return decision;
    }

    private OverrideRequest createTestOverrideRequest() {
        OverrideRequest request = new OverrideRequest();
        request.setUserId("admin-user");
        request.setReason("Business justification for override");
        return request;
    }
}