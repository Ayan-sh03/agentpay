package com.siemens.payment.agent.model;

import lombok.Data;

@Data
public class OverrideRequest {
    private String userId; // The user who is overriding the decision
    private String reason;
}
