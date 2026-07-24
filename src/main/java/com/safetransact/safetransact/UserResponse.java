package com.safetransact.safetransact.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class UserResponse {

    private Long id;
    private String name;
    private String email;
    private BigDecimal balance;
    private Instant createdAt;

    public UserResponse(Long id, String name, String email, BigDecimal balance, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.balance = balance;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public BigDecimal getBalance() { return balance; }
    public Instant getCreatedAt() { return createdAt; }
}
