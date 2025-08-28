
package com.payment.payment.agent.service;

import com.payment.payment.agent.audit.AuditService;
import com.payment.payment.agent.model.OverrideRequest;
import com.payment.payment.agent.model.PurchaseRequest;
import com.payment.payment.agent.model.PurchaseResponse;
import com.payment.payment.agent.pep.PolicyEnforcementPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurchaseServiceImplTest {

    @Mock
    private PolicyEnforcementPoint pep;

    @Mock
    private AuditService auditService;

    @Mock
    private WebAuthnService webAuthnService;

    @Mock
    private RequestValidationService requestValidationService;

    @InjectMocks
    private PurchaseServiceImpl purchaseService;

    private PurchaseRequest purchaseRequest;

    @BeforeEach
    void setUp() {
        purchaseRequest = new PurchaseRequest();
        purchaseRequest.setUserId("test-user");
        purchaseRequest.setAmount(100.0);
        purchaseRequest.setMerchant("test-merchant");
        purchaseRequest.setMcc("1234");
        purchaseRequest.setCurrency("USD");
    }

    @Test
    void processPurchase_WhenPolicyAllows_ShouldApprove() {
        // Arrange
        PolicyEnforcementPoint.PolicyDecision decision = new PolicyEnforcementPoint.PolicyDecision();
        decision.setAllowed(true);
        decision.setExplanation(List.of("Purchase approved by policy"));
        
        when(pep.evaluatePolicy(any(PurchaseRequest.class), anyString())).thenReturn(Mono.just(decision));
        when(webAuthnService.isStepUpRequired(anyString(), any())).thenReturn(false);
        doNothing().when(auditService).logEvent(anyString(), anyString(), anyString());
        doNothing().when(requestValidationService).validatePurchaseRequest(any(PurchaseRequest.class));

        // Act
        PurchaseResponse response = purchaseService.processPurchase(purchaseRequest).block();

        // Assert
        assertNotNull(response);
        assertEquals("APPROVED", response.getStatus());
        assertEquals("Purchase approved by policy.", response.getMessage());
        assertNotNull(response.getTransactionId());

        verify(requestValidationService, times(1)).validatePurchaseRequest(any(PurchaseRequest.class));
        verify(pep, times(1)).evaluatePolicy(any(PurchaseRequest.class), anyString());
        verify(webAuthnService, times(1)).isStepUpRequired(anyString(), any());
        // We're not verifying exact count of audit logs as it may vary based on implementation details
    }

    @Test
    void processPurchase_WhenPolicyDenies_ShouldDeny() {
        // Arrange
        PolicyEnforcementPoint.PolicyDecision decision = new PolicyEnforcementPoint.PolicyDecision();
        decision.setAllowed(false);
        decision.setExplanation(List.of("Purchase amount exceeds user spend cap"));
        
        when(pep.evaluatePolicy(any(PurchaseRequest.class), anyString())).thenReturn(Mono.just(decision));
        doNothing().when(auditService).logEvent(anyString(), anyString(), anyString());
        doNothing().when(requestValidationService).validatePurchaseRequest(any(PurchaseRequest.class));

        // Act
        PurchaseResponse response = purchaseService.processPurchase(purchaseRequest).block();

        // Assert
        assertNotNull(response);
        assertEquals("DENIED", response.getStatus());
        assertEquals("Purchase denied by policy. An override may be possible.", response.getMessage());
        assertNotNull(response.getTransactionId());

        verify(requestValidationService, times(1)).validatePurchaseRequest(any(PurchaseRequest.class));
        verify(pep, times(1)).evaluatePolicy(any(PurchaseRequest.class), anyString());
        // We're not verifying exact count of audit logs as it may vary based on implementation details
    }

    @Test
    void processPurchase_WhenStepUpRequiredAndSuccessful_ShouldApprove() {
        // Arrange
        PolicyEnforcementPoint.PolicyDecision decision = new PolicyEnforcementPoint.PolicyDecision();
        decision.setAllowed(true);
        decision.setExplanation(List.of("Purchase approved by policy"));
        
        when(pep.evaluatePolicy(any(PurchaseRequest.class), anyString())).thenReturn(Mono.just(decision));
        when(webAuthnService.isStepUpRequired(anyString(), any())).thenReturn(true);
        when(webAuthnService.performStepUpAuthentication(anyString())).thenReturn(true);
        doNothing().when(auditService).logEvent(anyString(), anyString(), anyString());
        doNothing().when(requestValidationService).validatePurchaseRequest(any(PurchaseRequest.class));

        // Act
        PurchaseResponse response = purchaseService.processPurchase(purchaseRequest).block();

        // Assert
        assertNotNull(response);
        assertEquals("APPROVED", response.getStatus());
        assertEquals("Purchase approved by policy.", response.getMessage());
        assertNotNull(response.getTransactionId());

        verify(requestValidationService, times(1)).validatePurchaseRequest(any(PurchaseRequest.class));
        verify(pep, times(1)).evaluatePolicy(any(PurchaseRequest.class), anyString());
        verify(webAuthnService, times(1)).isStepUpRequired(anyString(), any());
        verify(webAuthnService, times(1)).performStepUpAuthentication(anyString());
        // We're not verifying exact count of audit logs as it may vary based on implementation details
    }

    @Test
    void processPurchase_WhenStepUpRequiredAndFailed_ShouldDeny() {
        // Arrange
        PolicyEnforcementPoint.PolicyDecision decision = new PolicyEnforcementPoint.PolicyDecision();
        decision.setAllowed(true);
        decision.setExplanation(List.of("Purchase approved by policy"));
        
        when(pep.evaluatePolicy(any(PurchaseRequest.class), anyString())).thenReturn(Mono.just(decision));
        when(webAuthnService.isStepUpRequired(anyString(), any())).thenReturn(true);
        when(webAuthnService.performStepUpAuthentication(anyString())).thenReturn(false);
        doNothing().when(auditService).logEvent(anyString(), anyString(), anyString());
        doNothing().when(requestValidationService).validatePurchaseRequest(any(PurchaseRequest.class));

        // Act
        PurchaseResponse response = purchaseService.processPurchase(purchaseRequest).block();

        // Assert
        assertNotNull(response);
        assertEquals("DENIED", response.getStatus());
        assertEquals("Purchase denied due to failed step-up authentication.", response.getMessage());
        assertNotNull(response.getTransactionId());

        verify(requestValidationService, times(1)).validatePurchaseRequest(any(PurchaseRequest.class));
        verify(pep, times(1)).evaluatePolicy(any(PurchaseRequest.class), anyString());
        verify(webAuthnService, times(1)).isStepUpRequired(anyString(), any());
        verify(webAuthnService, times(1)).performStepUpAuthentication(anyString());
        // We're not verifying exact count of audit logs as it may vary based on implementation details
    }

    @Test
    void processPurchase_WhenValidationFails_ShouldReturnInvalidRequest() {
        // Arrange
        doThrow(new IllegalArgumentException("Purchase amount must be greater than zero"))
            .when(requestValidationService).validatePurchaseRequest(any(PurchaseRequest.class));
        doNothing().when(auditService).logEvent(anyString(), anyString(), anyString());

        // Act
        PurchaseResponse response = purchaseService.processPurchase(purchaseRequest).block();

        // Assert
        assertNotNull(response);
        assertEquals("INVALID_REQUEST", response.getStatus());
        assertTrue(response.getMessage().contains("Invalid purchase request"));
        assertNotNull(response.getTransactionId());

        verify(requestValidationService, times(1)).validatePurchaseRequest(any(PurchaseRequest.class));
        // We're not verifying exact count of audit logs as it may vary based on implementation details
    }

    @Test
    void overridePurchase_WhenTransactionIsValidForOverride_ShouldApproveWithOverrideStatus() {
        // Arrange
        // First, deny a purchase to make it available for override
        PolicyEnforcementPoint.PolicyDecision decision = new PolicyEnforcementPoint.PolicyDecision();
        decision.setAllowed(false);
        decision.setExplanation(List.of("Purchase amount exceeds user spend cap"));
        
        when(pep.evaluatePolicy(any(PurchaseRequest.class), anyString())).thenReturn(Mono.just(decision));
        doNothing().when(auditService).logEvent(anyString(), anyString(), anyString());
        doNothing().when(requestValidationService).validatePurchaseRequest(any(PurchaseRequest.class));

        PurchaseResponse deniedResponse = purchaseService.processPurchase(purchaseRequest).block();
        String transactionId = deniedResponse.getTransactionId();

        OverrideRequest overrideRequest = new OverrideRequest();
        overrideRequest.setUserId("admin-user");
        overrideRequest.setReason("Customer approved.");

        // Act
        PurchaseResponse overrideResponse = purchaseService.overridePurchase(transactionId, overrideRequest);

        // Assert
        assertNotNull(overrideResponse);
        assertEquals(transactionId, overrideResponse.getTransactionId());
        assertEquals("APPROVED_OVERRIDE", overrideResponse.getStatus());
        assertTrue(overrideResponse.getMessage().contains("Purchase approved by override from user 'admin-user'"));

        // Verify audit logs for the override call
        verify(auditService, atLeast(2)).logEvent(eq(transactionId), anyString(), anyString());
    }

    @Test
    void overridePurchase_WhenTransactionIdIsInvalid_ShouldThrowException() {
        // Arrange
        String invalidTransactionId = "invalid-tx-id";
        OverrideRequest overrideRequest = new OverrideRequest();
        overrideRequest.setUserId("admin-user");
        overrideRequest.setReason("Attempting to override non-existent transaction.");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            purchaseService.overridePurchase(invalidTransactionId, overrideRequest);
        });

        assertEquals("Transaction ID not found or not eligible for override.", exception.getMessage());
    }

    @Test
    void overridePurchase_WhenTransactionWasAlreadyApproved_ShouldThrowException() {
        // Arrange
        // First, approve a purchase
        PolicyEnforcementPoint.PolicyDecision decision = new PolicyEnforcementPoint.PolicyDecision();
        decision.setAllowed(true);
        decision.setExplanation(List.of("Purchase approved by policy"));
        
        when(pep.evaluatePolicy(any(PurchaseRequest.class), anyString())).thenReturn(Mono.just(decision));
        when(webAuthnService.isStepUpRequired(anyString(), any())).thenReturn(false);
        doNothing().when(auditService).logEvent(anyString(), anyString(), anyString());
        doNothing().when(requestValidationService).validatePurchaseRequest(any(PurchaseRequest.class));

        PurchaseResponse approvedResponse = purchaseService.processPurchase(purchaseRequest).block();
        String transactionId = approvedResponse.getTransactionId();

        OverrideRequest overrideRequest = new OverrideRequest();
        overrideRequest.setUserId("admin-user");
        overrideRequest.setReason("Attempting to override an already approved transaction.");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            purchaseService.overridePurchase(transactionId, overrideRequest);
        });

        assertEquals("Transaction ID not found or not eligible for override.", exception.getMessage());
    }
}
