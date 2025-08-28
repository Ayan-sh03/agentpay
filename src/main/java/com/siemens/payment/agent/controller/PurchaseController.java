package com.siemens.payment.agent.controller;

import com.siemens.payment.agent.model.OverrideRequest;
import com.siemens.payment.agent.model.PurchaseRequest;
import com.siemens.payment.agent.model.PurchaseResponse;
import com.siemens.payment.agent.service.PurchaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class PurchaseController {

    @Autowired
    private PurchaseService purchaseService;

    @PostMapping("/purchase")
    public PurchaseResponse initiatePurchase(@RequestBody PurchaseRequest request) {
        return purchaseService.processPurchase(request);
    }

    @PostMapping("/purchase/{transactionId}/override")
    public PurchaseResponse overridePurchase(@PathVariable String transactionId, @RequestBody OverrideRequest overrideRequest) {
        return purchaseService.overridePurchase(transactionId, overrideRequest);
    }
}
