package com.safetransact.safetransact;

import com.safetransact.safetransact.dto.PaymentRequest;
import com.safetransact.safetransact.dto.PaymentResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody PaymentRequest paymentRequest) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new MissingIdempotencyKeyException("Idempotency-Key header is required");
        }

        Payment paymentEntity = new Payment();
        paymentEntity.setAmount(paymentRequest.getAmount());
        paymentEntity.setCurrency(paymentRequest.getCurrency());
        paymentEntity.setPayerAccount(paymentRequest.getPayerAccount());
        paymentEntity.setPayeeAccount(paymentRequest.getPayeeAccount());

        Payment result = paymentService.processPayment(idempotencyKey, paymentEntity);

        PaymentResponse response = new PaymentResponse(
                result.getId(),
                result.getAmount(),
                result.getCurrency(),
                result.getStatus(),
                result.getPayerAccount(),
                result.getPayeeAccount(),
                result.getCreatedAt()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}