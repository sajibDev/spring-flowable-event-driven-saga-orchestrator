package com.saga.exceptions;

/**
 * Exception thrown when shipping processing fails.
 */
public class ShippingFailedException extends SagaException {
    
    public ShippingFailedException(String orderId, String reason) {
        super(orderId, "Shipping failed: " + reason);
    }

    public ShippingFailedException(String orderId, String reason, Throwable cause) {
        super(orderId, "Shipping failed: " + reason, cause);
    }
}

