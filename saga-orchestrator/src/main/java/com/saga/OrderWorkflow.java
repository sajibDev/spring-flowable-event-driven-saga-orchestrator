package com.saga;

import com.saga.dto.CreateOrderRequest;
import com.saga.model.OrderWorkflowState;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.QueryMethod;

/**
 * Order Workflow Interface following Temporal best practices.
 * 
 * Uses hybrid pattern:
 * - Activities: For outbound actions (publishing to Kafka)
 * - Signals: For inbound events (service responses) 
 * - Queries: For status checking without affecting workflow execution
 */
@WorkflowInterface
public interface OrderWorkflow {

    /**
     * Main workflow method to place an order.
     * Coordinates the saga across payment, inventory, and shipping services.
     * 
     * @param orderId The unique order identifier
     */
    @WorkflowMethod
    void placeOrder(String orderId, CreateOrderRequest orderRequest);

    // Signal methods for external events from services

    @SignalMethod
    void onOrderCreated();

    @SignalMethod
    void onOrderFailed();

    @SignalMethod
    void onPaymentCompleted();

    @SignalMethod
    void onPaymentFailed();

    @SignalMethod
    void onInventoryReserved();

    @SignalMethod
    void onInventoryFailed();

    @SignalMethod
    void onShippingCompleted();

    @SignalMethod
    void onShippingFailed();
    
    // Query methods for workflow state inspection
    
    /**
     * Query the current state of the workflow.
     * Queries are read-only and don't affect workflow execution.
     * 
     * @return Current workflow state
     */
    @QueryMethod
    OrderWorkflowState getState();
    
    /**
     * Query the current status of the order.
     * 
     * @return Current order status as a string
     */
    @QueryMethod
    String getStatus();
}

