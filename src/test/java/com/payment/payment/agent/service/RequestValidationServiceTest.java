package com.payment.payment.agent.service;

import com.payment.payment.agent.model.PurchaseRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class RequestValidationServiceTest {

    @InjectMocks
    private RequestValidationService requestValidationService;

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
    void validatePurchaseRequest_WhenValidRequest_ShouldNotThrowException() {
        // Act & Assert
        assertDoesNotThrow(() -> requestValidationService.validatePurchaseRequest(purchaseRequest));
    }

    @Test
    void validatePurchaseRequest_WhenRequestIsNull_ShouldThrowException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            requestValidationService.validatePurchaseRequest(null);
        });

        assertEquals("Purchase request cannot be null", exception.getMessage());
    }

    @Test
    void validatePurchaseRequest_WhenAmountIsZero_ShouldThrowException() {
        // Arrange
        purchaseRequest.setAmount(0);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            requestValidationService.validatePurchaseRequest(purchaseRequest);
        });

        assertEquals("Purchase amount must be greater than zero", exception.getMessage());
    }

    @Test
    void validatePurchaseRequest_WhenAmountIsNegative_ShouldThrowException() {
        // Arrange
        purchaseRequest.setAmount(-10);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            requestValidationService.validatePurchaseRequest(purchaseRequest);
        });

        assertEquals("Purchase amount must be greater than zero", exception.getMessage());
    }

    @Test
    void validatePurchaseRequest_WhenMerchantIsNull_ShouldThrowException() {
        // Arrange
        purchaseRequest.setMerchant(null);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            requestValidationService.validatePurchaseRequest(purchaseRequest);
        });

        assertEquals("Merchant information is required", exception.getMessage());
    }

    @Test
    void validatePurchaseRequest_WhenMerchantIsEmpty_ShouldThrowException() {
        // Arrange
        purchaseRequest.setMerchant("");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            requestValidationService.validatePurchaseRequest(purchaseRequest);
        });

        assertEquals("Merchant information is required", exception.getMessage());
    }

    @Test
    void validatePurchaseRequest_WhenMerchantIsWhitespace_ShouldThrowException() {
        // Arrange
        purchaseRequest.setMerchant("   ");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            requestValidationService.validatePurchaseRequest(purchaseRequest);
        });

        assertEquals("Merchant information is required", exception.getMessage());
    }

    @Test
    void validatePurchaseRequest_WhenMccIsNull_ShouldThrowException() {
        // Arrange
        purchaseRequest.setMcc(null);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            requestValidationService.validatePurchaseRequest(purchaseRequest);
        });

        assertEquals("MCC (Merchant Category Code) is required", exception.getMessage());
    }

    @Test
    void validatePurchaseRequest_WhenMccIsEmpty_ShouldThrowException() {
        // Arrange
        purchaseRequest.setMcc("");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            requestValidationService.validatePurchaseRequest(purchaseRequest);
        });

        assertEquals("MCC (Merchant Category Code) is required", exception.getMessage());
    }

    @Test
    void validatePurchaseRequest_WhenCurrencyIsNull_ShouldThrowException() {
        // Arrange
        purchaseRequest.setCurrency(null);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            requestValidationService.validatePurchaseRequest(purchaseRequest);
        });

        assertEquals("Currency information is required", exception.getMessage());
    }
}