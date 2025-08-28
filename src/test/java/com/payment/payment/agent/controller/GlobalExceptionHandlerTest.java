package com.payment.payment.agent.controller;

import com.payment.payment.agent.model.PurchaseResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.support.WebExchangeBindException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    @Test
    void handleValidationException_WhenWebExchangeBindException_ShouldReturnBadRequest() {
        // Arrange
        WebExchangeBindException exception = mock(WebExchangeBindException.class);
        when(exception.getMessage()).thenReturn("Validation failed");

        // Act
        ResponseEntity<PurchaseResponse> response = globalExceptionHandler.handleValidationException(exception);

        // Assert
        assertNotNull(response);
        assertEquals(400, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals("VALIDATION_ERROR", response.getBody().getStatus());
        assertTrue(response.getBody().getMessage().contains("Validation failed"));
    }

    @Test
    void handleIllegalArgumentException_WhenIllegalArgumentException_ShouldReturnBadRequest() {
        // Arrange
        IllegalArgumentException exception = new IllegalArgumentException("Invalid request parameter");

        // Act
        ResponseEntity<PurchaseResponse> response = globalExceptionHandler.handleIllegalArgumentException(exception);

        // Assert
        assertNotNull(response);
        assertEquals(400, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals("INVALID_REQUEST", response.getBody().getStatus());
        assertTrue(response.getBody().getMessage().contains("Invalid request"));
    }
}