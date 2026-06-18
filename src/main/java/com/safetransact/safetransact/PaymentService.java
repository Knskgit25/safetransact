package com.safetransact.safetransact;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private IdempotencyKeyRepository idempotencyKeyRepository;

    @Transactional
    public Payment processPayment(String idempotencyKey, Payment newPaymentRequest) {

        // Step 1: Check if this idempotency key already exists
        Optional<IdempotencyKey> existingKey = idempotencyKeyRepository.findByIdempotencyKey(idempotencyKey);

        if (existingKey.isPresent()) {
            // Duplicate request — same key already processed or being processed.
            // In a full implementation, we'd parse responsePayload and return the cached Payment.
            // For now, we fetch the actual payment using the same idempotency key.
            return paymentRepository.findAll()
                    .stream()
                    .filter(p -> idempotencyKey.equals(p.getIdempotencyKey()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Idempotency key exists but payment not found"));
        }

        // Step 2: Try to insert a new idempotency key record FIRST.
        // The unique constraint on idempotencyKey protects us here:
        // if two threads race to insert the same key, only one will succeed.
        IdempotencyKey keyRecord = new IdempotencyKey();
        keyRecord.setIdempotencyKey(idempotencyKey);
        keyRecord.setStatus("PROCESSING");
        keyRecord.setCreatedAt(Instant.now());

        try {
            idempotencyKeyRepository.save(keyRecord);
        } catch (DataIntegrityViolationException e) {
            // Another concurrent request already inserted this key first.
            throw new RuntimeException("Duplicate request detected (race condition caught) for key: " + idempotencyKey);
        }

        // Step 3: Now safely process the actual payment.
        newPaymentRequest.setIdempotencyKey(idempotencyKey);
        newPaymentRequest.setStatus("SUCCESS");
        newPaymentRequest.setCreatedAt(Instant.now());

        Payment savedPayment = paymentRepository.save(newPaymentRequest);

        // Step 4: Mark idempotency key as completed
        keyRecord.setStatus("COMPLETED");
        idempotencyKeyRepository.save(keyRecord);

        return savedPayment;
    }
}