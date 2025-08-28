package com.siemens.payment.agent.service;

import com.siemens.payment.agent.model.OverrideRequest;
import com.siemens.payment.agent.model.PurchaseRequest;
import com.siemens.payment.agent.model.PurchaseResponse;

public interface PurchaseService {
    PurchaseResponse processPurchase(PurchaseRequest request);
    PurchaseResponse overridePurchase(String transactionId, OverrideRequest overrideRequest);
}
