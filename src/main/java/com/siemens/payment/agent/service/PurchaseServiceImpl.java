package com.siemens.payment.agent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.siemens.payment.agent.audit.AuditService;
import com.siemens.payment.agent.model.OverrideRequest;
import com.siemens.payment.agent.model.PurchaseRequest;
import com.siemens.payment.agent.model.PurchaseResponse;
import com.siemens.payment.agent.pep.PolicyEnforcementPoint;
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

    private final ObjectMapper objectMapper = new ObjectMapper();

    // In a production scenario, this would be a distributed cache like Redis or a persistent store.
    // This map stores denied transaction IDs and their corresponding original purchase requests.
    private final Map<String, PurchaseRequest> deniedTransactions = new ConcurrentHashMap<>();

    @Override
    public PurchaseResponse processPurchase(PurchaseRequest request) {
        String transactionId = UUID.randomUUID().toString();
        // For demo purposes, we're using a static user ID
        // In a real implementation, this would come from the authentication context
        String userId = "user1";
        
        logAuditEvent(transactionId, "PURCHASE_REQUEST_RECEIVED", request);
        
        // Validate the purchase request
        try {
            requestValidationService.validatePurchaseRequest(request);
        } catch (IllegalArgumentException e) {
            PurchaseResponse response = new PurchaseResponse();
            response.setTransactionId(transactionId);
            response.setStatus("INVALID_REQUEST");
            response.setMessage("Invalid purchase request: " + e.getMessage());
            logAuditEvent(transactionId, "PURCHASE_REQUEST_INVALID", Map.of("error", e.getMessage()));
            return response;
        }

        boolean allowed = pep.evaluatePolicy(request, userId).block(); // Using block() for simplicity in this context.
        logAuditEvent(transactionId, "POLICY_EVALUATION_COMPLETED", Map.of("allowed", allowed, "userId", userId));

        PurchaseResponse response = new PurchaseResponse();
        response.setTransactionId(transactionId);

        if (allowed) {
            // Check if step-up authentication is required based on risk level
            boolean stepUpRequired = webAuthnService.isStepUpRequired(transactionId, request);
            logAuditEvent(transactionId, "STEP_UP_AUTHENTICATION_CHECK", Map.of("required", stepUpRequired));
            
            if (stepUpRequired) {
                // In a real implementation, we would trigger the WebAuthn flow here
                // For now, we'll simulate the authentication
                boolean authenticated = webAuthnService.performStepUpAuthentication(userId);
                logAuditEvent(transactionId, "STEP_UP_AUTHENTICATION_RESULT", Map.of("authenticated", authenticated));
                
                if (!authenticated) {
                    response.setStatus("DENIED");
                    response.setMessage("Purchase denied due to failed step-up authentication.");
                    logAuditEvent(transactionId, "PURCHASE_DENIED_STEP_UP_FAILED", response);
                    return response;
                }
            }
            
            response.setStatus("APPROVED");
            response.setMessage("Purchase approved by policy.");
            logAuditEvent(transactionId, "PURCHASE_APPROVED", response);
        } else {
            response.setStatus("DENIED");
            response.setMessage("Purchase denied by policy. An override may be possible.");
            deniedTransactions.put(transactionId, request); // Store for potential override
            logAuditEvent(transactionId, "PURCHASE_DENIED", response);
        }

        return response;
    }

    @Override
    public PurchaseResponse overridePurchase(String transactionId, OverrideRequest overrideRequest) {
        logAuditEvent(transactionId, "OVERRIDE_REQUEST_RECEIVED", overrideRequest);

        // 1. Validate the transaction is eligible for override
        if (!deniedTransactions.containsKey(transactionId)) {
            throw new IllegalArgumentException("Transaction ID not found or not eligible for override.");
        }

        // 2. In a real application, we would add a check here to ensure the user
        //    (overrideRequest.getUserId()) has the necessary permissions to perform an override.
        //    For example: if (!authorizationService.canUserOverride(overrideRequest.getUserId())) { ... }

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
