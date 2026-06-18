package com.safetransact.safetransact;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Entity
@Table(name = "idempotency_keys", uniqueConstraints = {
        @UniqueConstraint(columnNames = "idempotencyKey")
})
@Data
public class IdempotencyKey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String idempotencyKey;

    private String responsePayload; // stores the cached response as JSON string

    private String status; // PROCESSING, COMPLETED

    private Instant createdAt;
}
