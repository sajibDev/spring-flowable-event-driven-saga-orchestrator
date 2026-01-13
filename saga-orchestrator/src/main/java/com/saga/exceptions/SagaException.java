package com.saga.exceptions;

/**
 * Base exception for all Saga-related failures.
 * Provides context about which order failed and why.
 */
public class SagaException extends RuntimeException {
    private final String orderId;
    private final String reason;

    public SagaException(String orderId, String reason) {
        super(String.format("Saga failed for order %s: %s", orderId, reason));
        this.orderId = orderId;
        this.reason = reason;
    }

    public SagaException(String orderId, String reason, Throwable cause) {
        super(String.format("Saga failed for order %s: %s", orderId, reason), cause);
        this.orderId = orderId;
        this.reason = reason;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getReason() {
        return reason;
    }
}

