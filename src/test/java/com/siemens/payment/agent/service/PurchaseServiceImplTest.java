
package com.siemens.payment.agent.service;

import com.siemens.payment.agent.audit.AuditService;
import com.siemens.payment.agent.model.OverrideRequest;
import com.siemens.payment.agent.model.PurchaseRequest;
import com.siemens.payment.agent.model.PurchaseResponse;
import com.siemens.payment.agent.pep.PolicyEnforcementPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurchaseServiceImplTest {

    @Mock
    private PolicyEnforcementPoint pep;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private PurchaseServiceImpl purchaseService;

    private PurchaseRequest purchaseRequest;

    @BeforeEach
    void setUp() {
        purchaseRequest = new PurchaseRequest();
        purchaseRequest.setUserId("test-user");
        purchaseRequest.setAmount(100.0);
        purchaseRequest.setMerchant("test-merchant");
    }

    @Test
    void processPurchase_WhenPolicyAllows_ShouldApprove() {
        // Arrange
        when(pep.evaluatePolicy(any(PurchaseRequest.class))).thenReturn(Mono.just(true));
        doNothing().when(auditService).logEvent(anyString(), anyString(), anyString());

        // Act
        PurchaseResponse response = purchaseService.processPurchase(purchaseRequest);

        // Assert
        assertNotNull(response);
        assertEquals("APPROVED", response.getStatus());
        assertEquals("Purchase approved by policy.", response.getMessage());
        assertNotNull(response.getTransactionId());

        verify(auditService, times(3)).logEvent(anyString(), anyString(), anyString());
    }

    @Test
    void processPurchase_WhenPolicyDenies_ShouldDeny() {
        // Arrange
        when(pep.evaluatePolicy(any(PurchaseRequest.class))).thenReturn(Mono.just(false));
        doNothing().when(auditService).logEvent(anyString(), anyString(), anyString());

        // Act
        PurchaseResponse response = purchaseService.processPurchase(purchaseRequest);

        // Assert
        assertNotNull(response);
        assertEquals("DENIED", response.getStatus());
        assertEquals("Purchase denied by policy. An override may be possible.", response.getMessage());
        assertNotNull(response.getTransactionId());

        verify(auditService, times(3)).logEvent(anyString(), anyString(), anyString());
    }

    @Test
    void overridePurchase_WhenTransactionIsValidForOverride_ShouldApproveWithOverrideStatus() {
        // Arrange
        // First, deny a purchase to make it available for override
        when(pep.evaluatePolicy(any(PurchaseRequest.class))).thenReturn(Mono.just(false));
        PurchaseResponse deniedResponse = purchaseService.processPurchase(purchaseRequest);
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
        when(pep.evaluatePolicy(any(PurchaseRequest.class))).thenReturn(Mono.just(true));
        PurchaseResponse approvedResponse = purchaseService.processPurchase(purchaseRequest);
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
