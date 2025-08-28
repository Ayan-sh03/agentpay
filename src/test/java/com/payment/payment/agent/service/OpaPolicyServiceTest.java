package com.payment.payment.agent.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OpaPolicyServiceTest {

    @Mock
    private WebClient opaWebClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private OpaPolicyService opaPolicyService;

    @Test
    void updateOpaPolicy_WhenOpaReturnsSuccess_ShouldUpdatePolicy() {
        // Arrange
        when(opaWebClient.put()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(Mono.class), any(Class.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just("Policy updated successfully"));

        // Use reflection to set the current policy
        try {
            java.lang.reflect.Field field = OpaPolicyService.class.getDeclaredField("currentPolicy");
            field.setAccessible(true);
            field.set(opaPolicyService, "package payments\n\ndefault allow = false");
        } catch (Exception e) {
            throw new RuntimeException("Failed to set current policy", e);
        }

        // Act & Assert
        assertDoesNotThrow(() -> {
            java.lang.reflect.Method method = OpaPolicyService.class.getDeclaredMethod("updateOpaPolicy");
            method.setAccessible(true);
            method.invoke(opaPolicyService);
        });
    }

    @Test
    void updateOpaPolicy_WhenOpaReturnsError_ShouldHandleError() {
        // Arrange
        when(opaWebClient.put()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(Mono.class), any(Class.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class))
                .thenReturn(Mono.error(new WebClientResponseException(500, "Internal Server Error", null, null, null)));

        // Use reflection to set the current policy
        try {
            java.lang.reflect.Field field = OpaPolicyService.class.getDeclaredField("currentPolicy");
            field.setAccessible(true);
            field.set(opaPolicyService, "package payments\n\ndefault allow = false");
        } catch (Exception e) {
            throw new RuntimeException("Failed to set current policy", e);
        }

        // Act & Assert
        assertDoesNotThrow(() -> {
            java.lang.reflect.Method method = OpaPolicyService.class.getDeclaredMethod("updateOpaPolicy");
            method.setAccessible(true);
            method.invoke(opaPolicyService);
        });
    }

    @Test
    void getCurrentPolicy_WhenPolicyIsSet_ShouldReturnPolicy() {
        // Arrange
        String policy = "package payments\n\ndefault allow = false";
        
        // Use reflection to set the current policy
        try {
            java.lang.reflect.Field field = OpaPolicyService.class.getDeclaredField("currentPolicy");
            field.setAccessible(true);
            field.set(opaPolicyService, policy);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set current policy", e);
        }

        // Act
        String result = opaPolicyService.getCurrentPolicy();

        // Assert
        assertEquals(policy, result);
    }
}