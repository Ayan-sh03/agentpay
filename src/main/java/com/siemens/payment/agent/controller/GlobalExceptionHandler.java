package com.siemens.payment.agent.controller;

import com.siemens.payment.agent.model.PurchaseResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.support.WebExchangeBindException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<PurchaseResponse> handleValidationException(WebExchangeBindException ex) {
        PurchaseResponse response = new PurchaseResponse();
        response.setStatus("VALIDATION_ERROR");
        response.setMessage("Validation failed: " + ex.getMessage());
        
        return ResponseEntity.badRequest().body(response);
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<PurchaseResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        PurchaseResponse response = new PurchaseResponse();
        response.setStatus("INVALID_REQUEST");
        response.setMessage("Invalid request: " + ex.getMessage());
        
        return ResponseEntity.badRequest().body(response);
    }
}