package com.saga.exceptions;

/**
 * Exception thrown when payment processing fails.
 */
public class PaymentFailedException extends SagaException {
    
    public PaymentFailedException(String orderId, String reason) {
        super(orderId, "Payment failed: " + reason);
    }

    public PaymentFailedException(String orderId, String reason, Throwable cause) {
        super(orderId, "Payment failed: " + reason, cause);
    }
}

