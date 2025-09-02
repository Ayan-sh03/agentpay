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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

@Service
public class PurchaseServiceImpl implements PurchaseService {

    @Autowired
    private PolicyEnforcementPoint pep;

    @Autowired
    private AuditService auditService;

    @Autowired
    private RequestValidationService requestValidationService;

    @Autowired
    private AuthenticationContextService authenticationContextService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(PurchaseServiceImpl.class);

    // In a production scenario, this would be a distributed cache like Redis or a persistent store.
    // This map stores denied transaction IDs and their corresponding original purchase requests.
    private final Map<String, PurchaseRequest> deniedTransactions = new ConcurrentHashMap<>();

    @Override
    public Mono<PurchaseResponse> processPurchase(PurchaseRequest request) {
        String transactionId = UUID.randomUUID().toString();
        log.info("purchase:start txId={}, request={}", transactionId, safeToString(request));

        return authenticationContextService.getCurrentAgentContext()
            .flatMap(agentContext -> {
                String agentId = agentContext.getAgentId();
                log.debug("agentId: {}", agentId);

                logAuditEvent(transactionId, "PURCHASE_REQUEST_RECEIVED", request);
                log.debug("audit: PURCHASE_REQUEST_RECEIVED");

                try {
                    log.debug("validation:start");
                    requestValidationService.validatePurchaseRequest(request);
                    log.debug("validation:ok");
                } catch (IllegalArgumentException e) {
                    log.warn("validation:failed txId={}, reason={}", transactionId, e.getMessage());
                    PurchaseResponse response = new PurchaseResponse();
                    response.setTransactionId(transactionId);
                    response.setStatus("INVALID_REQUEST");
                    response.setMessage("Invalid purchase request: " + e.getMessage());
                    logAuditEvent(transactionId, "PURCHASE_REQUEST_INVALID", Map.of("error", e.getMessage()));
                    log.info("purchase:end txId={} status={} message={}", transactionId, response.getStatus(), response.getMessage());
                    return Mono.just(response);
                }

                log.debug("policy:evaluate:start txId={}", transactionId);
                return pep.evaluatePolicy(request, agentId)
                    .map(decision -> {
                        boolean allowed = decision.isAllowed();
                        log.info("policy:evaluate:result txId={} allowed={} explanation={}", transactionId, allowed, decision.getExplanation());

                        logAuditEvent(transactionId, "POLICY_EVALUATION_COMPLETED", Map.of("allowed", allowed, "agentId", agentId, "explanation", decision.getExplanation()));

                        PurchaseResponse response = new PurchaseResponse();
                        response.setTransactionId(transactionId);

                        if (allowed) {
                            log.info("purchase:approved txId={}", transactionId);
                            response.setStatus("APPROVED");
                            response.setMessage("Purchase approved by policy and ready for payment processing.");
                            logAuditEvent(transactionId, "PURCHASE_APPROVED", response);
                        } else {
                            log.info("purchase:denied txId={} explanation={}", transactionId, decision.getExplanation());
                            response.setStatus("DENIED");
                            response.setMessage("Purchase denied by policy. Owner approval may be possible.");
                            deniedTransactions.put(transactionId, request);
                            logAuditEvent(transactionId, "PURCHASE_DENIED", Map.of("message", response.getMessage(), "explanation", decision.getExplanation()));
                        }

                        log.info("purchase:end txId={} status={} message={}", transactionId, response.getStatus(), response.getMessage());
                        return response;
                    });
            });
    }

    private String safeToString(PurchaseRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            return String.valueOf(request);
        }
    }

    @Override
    public Mono<PurchaseResponse> overridePurchase(String transactionId, OverrideRequest overrideRequest) {
        return authenticationContextService.validateUserAccess(overrideRequest.getUserId())
            .then(Mono.defer(() -> {
                PurchaseRequest originalRequest = deniedTransactions.get(transactionId);
                if (originalRequest == null) {
                    PurchaseResponse response = new PurchaseResponse();
                    response.setTransactionId(transactionId);
                    response.setStatus("NOT_FOUND");
                    response.setMessage("No denied transaction found for override.");
                    log.warn("override:failed txId={} reason=not_found", transactionId);
                    return Mono.just(response);
                }

                // Any override request is treated as approval (current design)
                try {
                    log.info("override:approved txId={} reason={}", transactionId, overrideRequest.getReason());
                    PurchaseResponse response = new PurchaseResponse();
                    response.setTransactionId(transactionId);
                    response.setStatus("OVERRIDE_APPROVED");
                    response.setMessage("Purchase override approved by owner: " + overrideRequest.getReason());
                    logAuditEvent(transactionId, "PURCHASE_OVERRIDE_APPROVED", Map.of("overrider", overrideRequest.getUserId(), "reason", overrideRequest.getReason()));
                    deniedTransactions.remove(transactionId); // Clean up
                    return Mono.just(response);
                } catch (Exception e) {
                    log.error("override:payment_failed txId={} error={}", transactionId, e.getMessage());
                    PurchaseResponse response = new PurchaseResponse();
                    response.setTransactionId(transactionId);
                    response.setStatus("PAYMENT_FAILED");
                    response.setMessage("Payment processing failed during override: " + e.getMessage());
                    logAuditEvent(transactionId, "PURCHASE_OVERRIDE_FAILED", Map.of("error", e.getMessage()));
                    return Mono.just(response);
                }
            }));
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
