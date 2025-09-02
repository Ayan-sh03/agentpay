package com.payment.payment.agent.service;

import com.payment.payment.agent.model.OverrideRequest;
import com.payment.payment.agent.model.PurchaseRequest;
import com.payment.payment.agent.model.PurchaseResponse;
import reactor.core.publisher.Mono;

public interface PurchaseService {
    reactor.core.publisher.Mono<PurchaseResponse> processPurchase(PurchaseRequest request);
    Mono<PurchaseResponse> overridePurchase(String transactionId, OverrideRequest overrideRequest);
}
