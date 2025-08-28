package com.siemens.payment.agent.service;

import com.siemens.payment.agent.model.Token;
import com.siemens.payment.agent.model.TokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TokenVaultService {

    @Autowired
    private TokenRepository tokenRepository;

    @Autowired
    private EncryptionService encryptionService;

    public void storeToken(String agentId, String credential) throws Exception {
        String encryptedCredential = encryptionService.encrypt(credential);
        Token token = new Token();
        token.setAgentId(agentId);
        token.setEncryptedCredential(encryptedCredential);
        tokenRepository.save(token);
    }

    public String retrieveCredential(String agentId) throws Exception {
        Token token = tokenRepository.findById(agentId).orElse(null);
        if (token != null) {
            return encryptionService.decrypt(token.getEncryptedCredential());
        }
        return null;
    }
}
