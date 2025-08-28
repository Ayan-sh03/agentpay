package com.payment.payment.agent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.payment.agent.audit.AuditService;
import com.payment.payment.agent.model.OverrideRequest;
import com.payment.payment.agent.model.PurchaseRequest;
import com.payment.payment.agent.model.PurchaseResponse;
import com.payment.payment.agent.pep.PolicyEnforcementPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PurchaseServiceImpl implements PurchaseService {

    @Autowired
    private PolicyEnforcementPoint pep;

    @Autowired
    private AuditService auditService;

    @Autowired
    private WebAuthnService webAuthnService;
    
    @Autowired
    private RequestValidationService requestValidationService;

    @Autowired
    private AuthenticationContextService authenticationContextService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // In a production scenario, this would be a distributed cache like Redis or a persistent store.
    // This map stores denied transaction IDs and their corresponding original purchase requests.
    private final Map<String, PurchaseRequest> deniedTransactions = new ConcurrentHashMap<>();

    @Override
    public reactor.core.publisher.Mono<PurchaseResponse> processPurchase(PurchaseRequest request) {
        String transactionId = UUID.randomUUID().toString();
        System.out.println("=== PURCHASE PROCESS START ===");
        System.out.println("Transaction ID: " + transactionId);
        System.out.println("Request: " + request);

        String userId = authenticationContextService.getCurrentUserId();
        System.out.println("User ID: " + userId);

        logAuditEvent(transactionId, "PURCHASE_REQUEST_RECEIVED", request);
        System.out.println("Audit event logged: PURCHASE_REQUEST_RECEIVED");

        try {
            System.out.println("Starting request validation...");
            requestValidationService.validatePurchaseRequest(request);
            System.out.println("Request validation passed");
        } catch (IllegalArgumentException e) {
            System.out.println("Request validation failed: " + e.getMessage());
            PurchaseResponse response = new PurchaseResponse();
            response.setTransactionId(transactionId);
            response.setStatus("INVALID_REQUEST");
            response.setMessage("Invalid purchase request: " + e.getMessage());
            logAuditEvent(transactionId, "PURCHASE_REQUEST_INVALID", Map.of("error", e.getMessage()));
            System.out.println("Returning invalid request response: " + response);
            return reactor.core.publisher.Mono.just(response);
        }

        System.out.println("Calling policy evaluation...");
        return pep.evaluatePolicy(request, userId)
            .map(decision -> {
                boolean allowed = decision.isAllowed();
                System.out.println("=== POLICY EVALUATION RESULT ===");
                System.out.println("Allowed: " + allowed);
                System.out.println("Explanation: " + decision.getExplanation());
                System.out.println("User ID: " + userId);

                logAuditEvent(transactionId, "POLICY_EVALUATION_COMPLETED", Map.of("allowed", allowed, "userId", userId, "explanation", decision.getExplanation()));

                PurchaseResponse response = new PurchaseResponse();
                response.setTransactionId(transactionId);

                if (allowed) {
                    System.out.println("Purchase allowed, checking step-up authentication...");
                    boolean stepUpRequired = webAuthnService.isStepUpRequired(transactionId, request);
                    System.out.println("Step-up required: " + stepUpRequired);
                    logAuditEvent(transactionId, "STEP_UP_AUTHENTICATION_CHECK", Map.of("required", stepUpRequired));

                    if (stepUpRequired) {
                        System.out.println("Performing step-up authentication...");
                        boolean authenticated = webAuthnService.performStepUpAuthentication(userId);
                        System.out.println("Step-up authentication result: " + authenticated);
                        logAuditEvent(transactionId, "STEP_UP_AUTHENTICATION_RESULT", Map.of("authenticated", authenticated));

                        if (!authenticated) {
                            System.out.println("Step-up authentication failed, denying purchase");
                            response.setStatus("DENIED");
                            response.setMessage("Purchase denied due to failed step-up authentication.");
                            logAuditEvent(transactionId, "PURCHASE_DENIED_STEP_UP_FAILED", response);
                            System.out.println("Returning step-up failed response: " + response);
                            return response;
                        }
                    }

                    System.out.println("Purchase approved");
                    response.setStatus("APPROVED");
                    response.setMessage("Purchase approved by policy.");
                    logAuditEvent(transactionId, "PURCHASE_APPROVED", response);
                } else {
                    System.out.println("Purchase denied by policy");
                    response.setStatus("DENIED");
                    response.setMessage("Purchase denied by policy. An override may be possible.");
                    deniedTransactions.put(transactionId, request);
                    logAuditEvent(transactionId, "PURCHASE_DENIED", Map.of("message", response.getMessage(), "explanation", decision.getExplanation()));
                }

                System.out.println("Final response: " + response);
                System.out.println("=== PURCHASE PROCESS END ===");
                return response;
            });
    }

    @Override
    public PurchaseResponse overridePurchase(String transactionId, OverrideRequest overrideRequest) {
        logAuditEvent(transactionId, "OVERRIDE_REQUEST_RECEIVED", overrideRequest);

        // 1. Validate the transaction is eligible for override
        if (!deniedTransactions.containsKey(transactionId)) {
            throw new IllegalArgumentException("Transaction ID not found or not eligible for override.");
        }

        // 2. Validate that the requesting user has override permissions
        if (!authenticationContextService.canOverrideTransactions()) {
            throw new SecurityException("User does not have permission to perform transaction overrides");
        }

        // 3. Validate that the override request user ID matches the current authenticated user
        authenticationContextService.validateUserAccess(overrideRequest.getUserId());

        // 3. Process the override
        deniedTransactions.remove(transactionId); // The transaction is now being processed.

        PurchaseResponse response = new PurchaseResponse();
        response.setTransactionId(transactionId);
        response.setStatus("APPROVED_OVERRIDE");
        response.setMessage(String.format("Purchase approved by override from user '%s'. Reason: %s",
                overrideRequest.getUserId(), overrideRequest.getReason()));

        logAuditEvent(transactionId, "PURCHASE_OVERRIDE_COMPLETED", response);

        return response;
    }

    private void logAuditEvent(String transactionId, String eventType, Object data) {
        try {
            String details = objectMapper.writeValueAsString(data);
            auditService.logEvent(transactionId, eventType, details);
        } catch (JsonProcessingException e) {
            // Log a serialization failure, but don't fail the operation
            auditService.logEvent(transactionId, eventType, "{\"error\":\"Failed to serialize audit data.\"}");
        }
    }
}
