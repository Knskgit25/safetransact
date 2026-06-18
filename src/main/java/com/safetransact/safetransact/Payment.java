package com.safetransact.safetransact;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payments")
@Data
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String idempotencyKey;

    private BigDecimal amount;

    private String currency;

    private String status; // PENDING, SUCCESS, FAILED

    private String payerAccount;

    private String payeeAccount;

    private Instant createdAt;

    @Version
    private Long version; // this is the optimistic locking field
}