package com.saas.translator.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

@Service
public class PaymentService {

    @Value("${stripe.apiKey}")
    private String stripeApiKey;

    @Value("${payment.freeTierLimitChars}")
    private int freeTierLimitChars;

    private final Jedis redisClient;

    public PaymentService() {
        // Initialize Redis client (assumes Redis is running locally or configured)
        this.redisClient = new Jedis("localhost", 6379);
    }

    public PaymentIntent createPaymentIntent(String userId, int wordCount, String languagePair) throws StripeException {
        Stripe.apiKey = stripeApiKey;

        // Calculate cost based on word count and language pair complexity
        long amount = calculateCost(wordCount, languagePair);

        // Check free tier usage
        int usedChars = getUsedChars(userId);
        if (usedChars + wordCount <= freeTierLimitChars) {
            amount = 0; // Free tier
            incrementUsedChars(userId, wordCount);
        }

        PaymentIntentCreateParams params =
                PaymentIntentCreateParams.builder()
                        .setAmount(amount)
                        .setCurrency("usd")
                        .setPaymentMethodTypes(java.util.List.of("card"))
                        .build();

        return PaymentIntent.create(params);
    }

    private long calculateCost(int wordCount, String languagePair) {
        // TODO: Implement real cost calculation based on language pair complexity
        long costPerWord = 1; // in cents
        return wordCount * costPerWord;
    }

    private int getUsedChars(String userId) {
        String used = redisClient.get(userId);
        return used == null ? 0 : Integer.parseInt(used);
    }

    private void incrementUsedChars(String userId, int count) {
        redisClient.incrBy(userId, count);
    }

    // TODO: Implement GDPR-compliant data handling for payment info
    // TODO: Implement webhook handling for payment confirmation and automated refunds
}
