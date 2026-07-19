package com.safetransact.safetransact;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private IdempotencyKeyRepository idempotencyKeyRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public Payment processPayment(String idempotencyKey, Payment newPaymentRequest) {

        Optional<IdempotencyKey> existingKey = idempotencyKeyRepository.findByIdempotencyKey(idempotencyKey);

        if (existingKey.isPresent()) {
            IdempotencyKey key = existingKey.get();

            if (key.getStatus() == IdempotencyStatus.PROCESSING) {
                throw new RequestInProgressException(
                        "A request with this idempotency key is still being processed: " + idempotencyKey);
            }

            return paymentRepository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> new DuplicateRequestException(
                            "Idempotency key marked COMPLETED but payment record missing: " + idempotencyKey));
        }

        IdempotencyKey keyRecord = new IdempotencyKey();
        keyRecord.setIdempotencyKey(idempotencyKey);
        keyRecord.setStatus(IdempotencyStatus.PROCESSING);
        keyRecord.setCreatedAt(Instant.now());

        try {
            idempotencyKeyRepository.save(keyRecord);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateRequestException("Duplicate request detected (race condition caught) for key: " + idempotencyKey);
        }

        // ---- Money transfer logic starts here ----

        BigDecimal amount = newPaymentRequest.getAmount();

        User sender = userRepository.findByEmail(newPaymentRequest.getPayerAccount())
                .orElseThrow(() -> new UserNotFoundException("Payer not found: " + newPaymentRequest.getPayerAccount()));

        User receiver = userRepository.findByEmail(newPaymentRequest.getPayeeAccount())
                .orElseThrow(() -> new UserNotFoundException("Payee not found: " + newPaymentRequest.getPayeeAccount()));

        if (sender.getBalance().compareTo(amount) < 0) {
            newPaymentRequest.setIdempotencyKey(idempotencyKey);
            newPaymentRequest.setStatus(PaymentStatus.FAILED);
            newPaymentRequest.setCreatedAt(Instant.now());
            paymentRepository.save(newPaymentRequest);

            keyRecord.setStatus(IdempotencyStatus.COMPLETED);
            idempotencyKeyRepository.save(keyRecord);

            throw new InsufficientBalanceException("Sender has insufficient balance: " + sender.getEmail());
        }

        sender.setBalance(sender.getBalance().subtract(amount));
        receiver.setBalance(receiver.getBalance().add(amount));

        userRepository.save(sender);
        userRepository.save(receiver);

        // ---- Money transfer logic ends here ----

        newPaymentRequest.setIdempotencyKey(idempotencyKey);
        newPaymentRequest.setStatus(PaymentStatus.SUCCESS);
        newPaymentRequest.setCreatedAt(Instant.now());

        Payment savedPayment = paymentRepository.save(newPaymentRequest);

        keyRecord.setStatus(IdempotencyStatus.COMPLETED);
        idempotencyKeyRepository.save(keyRecord);

        return savedPayment;
    }
}