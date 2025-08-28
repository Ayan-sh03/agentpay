package com.payment.payment.agent.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class Token {

    @Id
    private String agentId;
    private String encryptedCredential;
}
