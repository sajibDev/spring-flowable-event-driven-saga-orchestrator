package com.saga;

import com.saga.dto.CreateOrderRequest;
import com.saga.dto.InventoryResponse;
import com.saga.dto.OrderResponse;
import com.saga.dto.PaymentResponse;
import com.saga.dto.ShippingResponse;
import com.saga.model.OrderWorkflowState;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.QueryMethod;

/**
 * Order Workflow Interface following Temporal best practices.
 * 
 * Uses hybrid pattern:
 * - Activities: For outbound actions (publishing to RabbitMQ)
 * - Signals: For inbound events (service responses)
 * - Queries: For status checking without affecting workflow execution
 */
@WorkflowInterface
public interface OrderWorkflow {

    /**
     * Main workflow method to place an order.
     * Coordinates the saga across order, inventory, payment, and shipping services.
     *
     * @param orderId The unique order identifier
     * @param orderRequest The order request details
     */
    @WorkflowMethod
    void placeOrder(String orderId, CreateOrderRequest orderRequest);

    // Signal methods for external events from services

    /**
     * Signal method to receive order creation response.
     * @param orderResponse The order service response
     */
    @SignalMethod
    void orderReply(OrderResponse orderResponse);

    /**
     * Signal method to receive inventory reservation response.
     * @param inventoryResponse The inventory service response
     */
    @SignalMethod
    void inventoryReply(InventoryResponse inventoryResponse);

    /**
     * Signal method to receive payment processing response.
     * @param paymentResponse The payment service response
     */
    @SignalMethod
    void paymentReply(PaymentResponse paymentResponse);

    /**
     * Signal method to receive shipping creation response.
     * @param shippingResponse The shipping service response
     */
    @SignalMethod
    void shippingReply(ShippingResponse shippingResponse);

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

