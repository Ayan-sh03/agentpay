package com.payment.payment.agent.pep;

import com.payment.payment.agent.model.PurchaseRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PolicyEnforcementPointTest {

    @Mock
    private WebClient opaWebClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private PolicyEnforcementPoint policyEnforcementPoint;

    private PurchaseRequest purchaseRequest;

    @BeforeEach
    void setUp() {
        // Setup WebClient mock chain
        when(opaWebClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(Mono.class), any(Class.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

        policyEnforcementPoint = new PolicyEnforcementPoint();
        
        // Use reflection to inject the mock WebClient
        try {
            java.lang.reflect.Field field = PolicyEnforcementPoint.class.getDeclaredField("opaWebClient");
            field.setAccessible(true);
            field.set(policyEnforcementPoint, opaWebClient);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mock WebClient", e);
        }

        purchaseRequest = new PurchaseRequest();
        purchaseRequest.setUserId("test-user");
        purchaseRequest.setAmount(100.0);
        purchaseRequest.setMerchant("test-merchant");
        purchaseRequest.setMcc("1234");
    }

    @Test
    void evaluatePolicy_WhenOpaReturnsAllowed_ShouldReturnAllowedDecision() {
        // Arrange
        PolicyEnforcementPoint.OpaResponse opaResponse = new PolicyEnforcementPoint.OpaResponse();
        PolicyEnforcementPoint.OpaResult opaResult = new PolicyEnforcementPoint.OpaResult();
        opaResult.setAllow(true);
        opaResult.setExplanation(java.util.List.of("Purchase approved by policy"));
        opaResponse.setResult(opaResult);

        when(responseSpec.bodyToMono(PolicyEnforcementPoint.OpaResponse.class)).thenReturn(Mono.just(opaResponse));

        // Act
        Mono<PolicyEnforcementPoint.PolicyDecision> result = policyEnforcementPoint.evaluatePolicy(purchaseRequest, "test-user");

        // Assert
        StepVerifier.create(result)
                .expectNextMatches(decision -> decision.isAllowed() && decision.getExplanation().contains("Purchase approved by policy"))
                .verifyComplete();

        verify(opaWebClient, times(1)).post();
    }

    @Test
    void evaluatePolicy_WhenOpaReturnsDenied_ShouldReturnDeniedDecision() {
        // Arrange
        PolicyEnforcementPoint.OpaResponse opaResponse = new PolicyEnforcementPoint.OpaResponse();
        PolicyEnforcementPoint.OpaResult opaResult = new PolicyEnforcementPoint.OpaResult();
        opaResult.setAllow(false);
        opaResult.setExplanation(java.util.List.of("Purchase amount exceeds user spend cap"));
        opaResponse.setResult(opaResult);

        when(responseSpec.bodyToMono(PolicyEnforcementPoint.OpaResponse.class)).thenReturn(Mono.just(opaResponse));

        // Act
        Mono<PolicyEnforcementPoint.PolicyDecision> result = policyEnforcementPoint.evaluatePolicy(purchaseRequest, "test-user");

        // Assert
        StepVerifier.create(result)
                .expectNextMatches(decision -> !decision.isAllowed() && decision.getExplanation().contains("Purchase amount exceeds user spend cap"))
                .verifyComplete();

        verify(opaWebClient, times(1)).post();
    }

    @Test
    void evaluatePolicy_WhenOpaCallFails_ShouldReturnErrorDecision() {
        // Arrange
        when(responseSpec.bodyToMono(PolicyEnforcementPoint.OpaResponse.class))
                .thenReturn(Mono.error(new WebClientResponseException(500, "Internal Server Error", null, null, null)));

        // Act
        Mono<PolicyEnforcementPoint.PolicyDecision> result = policyEnforcementPoint.evaluatePolicy(purchaseRequest, "test-user");

        // Assert
        StepVerifier.create(result)
                .expectNextMatches(decision -> !decision.isAllowed() && decision.getExplanation().contains("Policy evaluation failed due to system error"))
                .verifyComplete();

        verify(opaWebClient, times(1)).post();
    }
}