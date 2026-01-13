package com.saga.exceptions;

/**
 * Exception thrown when inventory reservation fails.
 */
public class InventoryFailedException extends SagaException {
    
    public InventoryFailedException(String orderId, String reason) {
        super(orderId, "Inventory reservation failed: " + reason);
    }

    public InventoryFailedException(String orderId, String reason, Throwable cause) {
        super(orderId, "Inventory reservation failed: " + reason, cause);
    }
}

