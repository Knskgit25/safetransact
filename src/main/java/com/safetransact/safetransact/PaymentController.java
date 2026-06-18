package com.safetransact.safetransact;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping
    public ResponseEntity<Payment> createPayment(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody Payment paymentRequest) {

        Payment result = paymentService.processPayment(idempotencyKey, paymentRequest);
        return ResponseEntity.ok(result);
    }
}
