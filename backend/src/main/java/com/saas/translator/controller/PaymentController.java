package com.saas.translator.controller;

import com.saas.translator.service.PaymentService;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    @Autowired
    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/create-payment-intent")
    public ResponseEntity<?> createPaymentIntent(@RequestParam String userId,
                                                 @RequestParam int wordCount,
                                                 @RequestParam String languagePair) {
        try {
            PaymentIntent paymentIntent = paymentService.createPaymentIntent(userId, wordCount, languagePair);
            return ResponseEntity.ok(paymentIntent.toJson());
        } catch (StripeException e) {
            return ResponseEntity.status(500).body("Payment processing failed: " + e.getMessage());
        }
    }

    // TODO: Implement webhook endpoint for payment confirmation and refunds
}
