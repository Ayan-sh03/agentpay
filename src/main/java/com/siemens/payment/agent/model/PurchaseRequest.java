package com.siemens.payment.agent.model;

import lombok.Data;

@Data
public class PurchaseRequest {
    private String userId;
    private String agentId;
    private double amount;
    private String merchant;
    private String mcc; // Merchant Category Code
}
