package com.siemens.payment.agent.pep;

import com.siemens.payment.agent.model.PurchaseRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class PolicyEnforcementPoint {

    @Autowired
    private WebClient opaWebClient;

    public Mono<Boolean> evaluatePolicy(PurchaseRequest request, String userId) {
        // This is a simplified request to OPA. In a real scenario, you would send more context.
        OpaRequest opaRequest = new OpaRequest(new OpaInput(request, userId));

        return opaWebClient.post()
                .body(Mono.just(opaRequest), OpaRequest.class)
                .retrieve()
                .bodyToMono(OpaResponse.class)
                .map(OpaResponse::isResult)
                .onErrorReturn(false);
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

        public boolean isResult() {
            return result;
        }

        public void setResult(boolean result) {
            this.result = result;
        }
    }
}
