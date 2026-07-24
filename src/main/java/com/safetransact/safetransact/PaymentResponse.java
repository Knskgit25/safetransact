package com.safetransact.safetransact.dto;

import com.safetransact.safetransact.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

public class PaymentResponse {

    private String id;
    private BigDecimal amount;
    private String currency;
    private PaymentStatus status;
    private String payerAccount;
    private String payeeAccount;
    private Instant createdAt;

    public PaymentResponse(String id, BigDecimal amount, String currency, PaymentStatus status,
                           String payerAccount, String payeeAccount, Instant createdAt) {
        this.id = id;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.payerAccount = payerAccount;
        this.payeeAccount = payeeAccount;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public PaymentStatus getStatus() { return status; }
    public String getPayerAccount() { return payerAccount; }
    public String getPayeeAccount() { return payeeAccount; }
    public Instant getCreatedAt() { return createdAt; }
}
