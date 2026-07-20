package com.safetransact.safetransact;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class PaymentServiceConcurrencyTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void concurrentDuplicateRequests_shouldProcessExactlyOnce() throws InterruptedException {

        User sender = new User();
        sender.setName("ConcurrentAlice");
        sender.setEmail("conc-alice-" + UUID.randomUUID() + "@test.com");
        sender.setBalance(new BigDecimal("1000.00"));
        sender.setCreatedAt(Instant.now());
        userRepository.save(sender);

        User receiver = new User();
        receiver.setName("ConcurrentBob");
        receiver.setEmail("conc-bob-" + UUID.randomUUID() + "@test.com");
        receiver.setBalance(new BigDecimal("0.00"));
        receiver.setCreatedAt(Instant.now());
        userRepository.save(receiver);

        String idempotencyKey = "concurrent-test-" + UUID.randomUUID();
        int threadCount = 50; // pehle chhoti scale pe test

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger conflictCount = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();

                    Payment request = new Payment();
                    request.setAmount(new BigDecimal("100.00"));
                    request.setCurrency("INR");
                    request.setPayerAccount(sender.getEmail());
                    request.setPayeeAccount(receiver.getEmail());

                    paymentService.processPayment(idempotencyKey, request);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    conflictCount.incrementAndGet();
                    System.out.println("Thread got exception: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();

        // Timeout laga diya — 20 sec mein khatam na ho toh test fail ho, hang nahi hoga
        boolean completed = doneLatch.await(20, TimeUnit.SECONDS);
        executor.shutdownNow();

        System.out.println("Completed within timeout: " + completed);
        System.out.println("Successful calls: " + successCount.get() + ", Rejected/conflict calls: " + conflictCount.get());

        assertTrue(completed, "Test 20 second timeout ke andar complete nahi hua — deadlock ho sakta hai");

        long paymentCount = paymentRepository.findAll().stream()
                .filter(p -> idempotencyKey.equals(p.getIdempotencyKey()))
                .count();
        assertEquals(1, paymentCount, "Idempotency key ke liye sirf ek Payment record banna chahiye");

        User updatedSender = userRepository.findById(sender.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("900.00").compareTo(updatedSender.getBalance()),
                "Balance sirf ek baar deduct hona chahiye, duplicate processing nahi");
    }
}