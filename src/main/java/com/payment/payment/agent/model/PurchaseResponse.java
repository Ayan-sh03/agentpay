package com.payment.payment.agent.model;

import lombok.Data;

@Data
public class PurchaseResponse {
    private String status;
    private String transactionId;
    private String message;
}
