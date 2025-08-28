package com.siemens.payment.agent.model;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class OverrideRequest {
    @NotBlank(message = "User ID is required")
    private String userId; // The user who is overriding the decision
    
    @NotBlank(message = "Reason is required")
    private String reason;
}
