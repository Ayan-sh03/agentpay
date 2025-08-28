package com.siemens.payment.agent.pep;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.siemens.payment.agent.model.PurchaseRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Component
public class PolicyEnforcementPoint {

    @Autowired
    private WebClient opaWebClient;

    public Mono<PolicyDecision> evaluatePolicy(PurchaseRequest request, String userId) {
        // This is a simplified request to OPA. In a real scenario, you would send more context.
        OpaRequest opaRequest = new OpaRequest(new OpaInput(request, userId));

        return opaWebClient.post()
                .uri("/v1/data/payments")
                .body(Mono.just(opaRequest), OpaRequest.class)
                .retrieve()
                .bodyToMono(OpaResponse.class)
                .map(this::convertToPolicyDecision)
                .onErrorReturn(createErrorDecision());
    }
    
    private PolicyDecision convertToPolicyDecision(OpaResponse opaResponse) {
        PolicyDecision decision = new PolicyDecision();
        decision.setAllowed(opaResponse.getResult());
        decision.setExplanation(opaResponse.getExplanation());
        return decision;
    }
    
    private PolicyDecision createErrorDecision() {
        PolicyDecision decision = new PolicyDecision();
        decision.setAllowed(false);
        decision.setExplanation("Policy evaluation failed due to system error");
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

    private static class OpaResponse {
        private boolean result;
        
        @JsonProperty("explanation")
        private List<String> explanation;

        public boolean isResult() {
            return result;
        }

        public void setResult(boolean result) {
            this.result = result;
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
