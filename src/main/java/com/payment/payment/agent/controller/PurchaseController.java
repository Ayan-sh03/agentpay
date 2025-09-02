package com.payment.payment.agent.controller;

import com.payment.payment.agent.model.OverrideRequest;
import com.payment.payment.agent.model.PurchaseRequest;
import com.payment.payment.agent.model.PurchaseResponse;
import com.payment.payment.agent.service.PurchaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1")
public class PurchaseController {

    @Autowired
    private PurchaseService purchaseService;

    @PostMapping("/purchase")
    public reactor.core.publisher.Mono<PurchaseResponse> initiatePurchase(@Valid @RequestBody PurchaseRequest request) {
        return purchaseService.processPurchase(request);
    }

    @PostMapping("/purchase/{transactionId}/override")
    public Mono<PurchaseResponse> overridePurchase(@PathVariable String transactionId, @Valid @RequestBody OverrideRequest overrideRequest) {
        return purchaseService.overridePurchase(transactionId, overrideRequest);
    }
}
