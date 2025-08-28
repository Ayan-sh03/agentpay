package com.payment.payment.agent.pep;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.payment.payment.agent.model.PurchaseRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class PolicyEnforcementPoint {

    @Autowired
    private WebClient opaWebClient;

    public Mono<PolicyDecision> evaluatePolicy(PurchaseRequest request, String userId) {
        System.out.println("=== OPA POLICY EVALUATION START ===");
        System.out.println("Request: " + request);
        System.out.println("User ID: " + userId);

        // This is a simplified request to OPA. In a real scenario, you would send more context.
        OpaRequest opaRequest = new OpaRequest(new OpaInput(request, userId));
        System.out.println("OPA Request: " + opaRequest);

        return opaWebClient.post()
                .uri("/v1/data/payments")
                .body(Mono.just(opaRequest), OpaRequest.class)
                .retrieve()
                .bodyToMono(OpaResponse.class)
                .map(response -> {
                    System.out.println("OPA Response: " + response);
                    PolicyDecision decision = convertToPolicyDecision(response);
                    System.out.println("Converted decision: " + decision.isAllowed() + ", explanation: " + decision.getExplanation());
                    System.out.println("=== OPA POLICY EVALUATION END ===");
                    return decision;
                })
                .onErrorResume(throwable -> {
                    System.out.println("OPA evaluation error: " + throwable.getMessage());
                    PolicyDecision errorDecision = createErrorDecision();
                    System.out.println("Error decision: " + errorDecision.isAllowed() + ", explanation: " + errorDecision.getExplanation());
                    System.out.println("=== OPA POLICY EVALUATION END (ERROR) ===");
                    return Mono.just(errorDecision);
                });
    }
    
    private PolicyDecision convertToPolicyDecision(OpaResponse opaResponse) {
        PolicyDecision decision = new PolicyDecision();
        if (opaResponse.getResult() != null) {
            decision.setAllowed(opaResponse.getResult().isAllow());
            decision.setExplanation(opaResponse.getResult().getExplanation());
        } else {
            decision.setAllowed(false);
            decision.setExplanation(List.of("No result from policy evaluation"));
        }
        return decision;
    }
    
    private PolicyDecision createErrorDecision() {
        PolicyDecision decision = new PolicyDecision();
        decision.setAllowed(false);
        decision.setExplanation(List.of("Policy evaluation failed due to system error"));
        return decision;
    }

    // Helper classes for OPA request and response

    private static class OpaRequest {
        private final OpaInput input;

        public OpaRequest(OpaInput input) {
            this.input = input;
        }

        public OpaInput getInput() {
            return input;
        }
    }

    private static class OpaInput {
        private final PurchaseRequest purchase;
        private final User user;

        public OpaInput(PurchaseRequest purchase, String userId) {
            this.purchase = purchase;
            this.user = new User(userId);
        }

        public PurchaseRequest getPurchase() {
            return purchase;
        }

        public User getUser() {
            return user;
        }
    }

    private static class User {
        private final String id;

        public User(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }

    static class OpaResponse {
        private OpaResult result;

        public OpaResult getResult() {
            return result;
        }

        public void setResult(OpaResult result) {
            this.result = result;
        }
    }

    static class OpaResult {
        private boolean allow;

        @JsonProperty("explanation")
        private List<String> explanation;

        public boolean isAllow() {
            return allow;
        }

        public void setAllow(boolean allow) {
            this.allow = allow;
        }

        public List<String> getExplanation() {
            return explanation;
        }

        public void setExplanation(List<String> explanation) {
            this.explanation = explanation;
        }
    }
    
    public static class PolicyDecision {
        private boolean allowed;
        private List<String> explanation;
        
        public boolean isAllowed() {
            return allowed;
        }
        
        public void setAllowed(boolean allowed) {
            this.allowed = allowed;
        }
        
        public List<String> getExplanation() {
            return explanation;
        }
        
        public void setExplanation(List<String> explanation) {
            this.explanation = explanation;
        }
    }
}
