package com.saga.activities;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/**
 * Activity interface for all order-related operations.
 * Activities handle side effects like publishing events to Kafka.
 * 
 * This follows Temporal best practices:
 * - Workflow code is deterministic (no side effects)
 * - Activities handle all external interactions
 * - Activities can be retried independently
 */
@ActivityInterface
public interface OrderActivities {
    
    /**
     * Publishes PROCESS_PAYMENT event to Kafka.
     * @param orderId The order ID
     */

    @ActivityMethod
    void publishOrderCreatedEvent(String orderId);

    @ActivityMethod
    void publishPaymentRequest(String orderId);
    
    /**
     * Publishes RESERVE_INVENTORY event to Kafka.
     * @param orderId The order ID
     */
    @ActivityMethod
    void publishInventoryRequest(String orderId);
    
    /**
     * Publishes PROCESS_SHIPPING event to Kafka.
     * @param orderId The order ID
     */
    @ActivityMethod
    void publishShippingRequest(String orderId);
    
    /**
     * Publishes COMPENSATE_PAYMENT event to Kafka.
     * This triggers payment refund in the payment service.
     * @param orderId The order ID
     */

    @ActivityMethod
    void compensateOrder(String orderId);

    @ActivityMethod
    void compensatePayment(String orderId);
    
    /**
     * Publishes COMPENSATE_INVENTORY event to Kafka.
     * This triggers inventory release in the inventory service.
     * @param orderId The order ID
     */
    @ActivityMethod
    void compensateInventory(String orderId);
    
    /**
     * Publishes COMPENSATE_SHIPPING event to Kafka.
     * This triggers shipping cancellation in the shipping service.
     * @param orderId The order ID
     */
    @ActivityMethod
    void compensateShipping(String orderId);
}

