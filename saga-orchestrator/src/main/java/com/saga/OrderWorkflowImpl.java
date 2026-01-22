package com.saga;

import com.saga.activities.OrderActivities;
import com.saga.config.WorkflowOptionsConfig;
import com.saga.dto.CreateOrderRequest;
import com.saga.dto.InventoryResponse;
import com.saga.dto.OrderResponse;
import com.saga.dto.PaymentResponse;
import com.saga.dto.ShippingResponse;
import com.saga.exceptions.InventoryFailedException;
import com.saga.exceptions.OrderFailedException;
import com.saga.exceptions.PaymentFailedException;
import com.saga.exceptions.ShippingFailedException;
import com.saga.model.OrderWorkflowState;
import com.saga.model.OrderWorkflowState.OrderStatus;
import com.saga.model.OrderWorkflowState.WorkflowStep;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.Saga;
import org.slf4j.Logger;

/**
 * Order Workflow Implementation following Temporal best practices.
 * <p>
 * Key improvements:
 * 1. Uses Activities for all side effects (RabbitMQ publishing)
 * 2. Keeps Signals for external events (durable, event-driven)
 * 3. Adds timeouts to prevent infinite waiting
 * 4. Real compensations via Activities
 * 5. Structured state tracking for queries
 * 6. Proper error handling with custom exceptions
 * 7. Response-based signals with status checking
 */
public class OrderWorkflowImpl implements OrderWorkflow {

    private static final Logger logger = Workflow.getLogger(OrderWorkflowImpl.class);

    // Status constants for Order
    private static final String ORDER_CREATED = "ORDER_CREATED";
    private static final String ORDER_CREATION_FAILED = "ORDER_CREATION_FAILED";

    // Status constants for Inventory
    private static final String INVENTORY_RESERVED = "INVENTORY_RESERVED";
    private static final String INVENTORY_RESERVATION_FAILED = "INVENTORY_RESERVATION_FAILED";

    // Status constants for Payment
    private static final String PAYMENT_COMPLETED = "PAYMENT_COMPLETED";
    private static final String PAYMENT_FAILED = "PAYMENT_FAILED";

    // Status constants for Shipping
    private static final String SHIPPING_COMPLETED = "SHIPPING_COMPLETED";
    private static final String SHIPPING_FAILED = "SHIPPING_FAILED";

    // Activity stub with proper retry and timeout configuration
    private final OrderActivities activities = Workflow.newActivityStub(
            OrderActivities.class,
            WorkflowOptionsConfig.getDefaultActivityOptions()
    );

    // Activity stub for compensations with more aggressive retry policy
    private final OrderActivities compensationActivities = Workflow.newActivityStub(
            OrderActivities.class,
            WorkflowOptionsConfig.getCompensationActivityOptions()
    );

    // Response objects for signal-based coordination (event-driven pattern)
    private OrderResponse orderResponse;
    private InventoryResponse inventoryResponse;
    private PaymentResponse paymentResponse;
    private ShippingResponse shippingResponse;

    // Workflow state for queries
    private final OrderWorkflowState state = new OrderWorkflowState();

    @Override
    public void placeOrder(String orderId, CreateOrderRequest orderRequest) {
        logger.info("Starting order workflow for order: {}", orderId);
        state.setOrderId(orderId);
        state.setStatus(OrderStatus.PROCESSING);

        // Configure saga for compensation handling
        Saga.Options sagaOptions = new Saga.Options.Builder()
                .setContinueWithError(false)
                .build();
        Saga saga = new Saga(sagaOptions);

        try {
            // ===== ORDER STEP =====
            state.setCurrentStep(WorkflowStep.ORDER);
            logger.info("Processing order: {}", orderId);

            // Activity to publish order request
            activities.publishOrderCreatedEvent(orderId);
            saga.addCompensation(() -> compensationActivities.compensateOrder(orderId));

            boolean orderReceived = Workflow.await(
                    WorkflowOptionsConfig.SIGNAL_WAIT_TIMEOUT,
                    () -> orderResponse != null &&
                          (orderResponse.getOrderStatus().equals(ORDER_CREATED) ||
                           orderResponse.getOrderStatus().equals(ORDER_CREATION_FAILED)));

            if (!orderReceived) {
                logger.error("Order timeout for order: {}", orderId);
                throw new OrderFailedException(orderId, "Order creation timeout");
            }

            if (orderResponse.getOrderStatus().equals(ORDER_CREATION_FAILED)) {
                logger.warn("Order creation failed for order: {}", orderId);
                state.setStatus(OrderStatus.FAILED);
                state.setFailureReason(orderResponse.getMessage());
                return;
            }

            logger.info("Order created for order: {}", orderId);
            state.addCompletedStep(WorkflowStep.ORDER);

            // ===== INVENTORY STEP =====
            state.setCurrentStep(WorkflowStep.INVENTORY);
            logger.info("Processing inventory reservation for order: {}", orderId);

            // Activity to publish inventory request
            activities.publishInventoryRequest(orderId);
            saga.addCompensation(() -> compensationActivities.compensateInventory(orderId));

            boolean inventoryReceived = Workflow.await(
                    WorkflowOptionsConfig.SIGNAL_WAIT_TIMEOUT,
                    () -> inventoryResponse != null &&
                          (inventoryResponse.getInventoryStatus().equals(INVENTORY_RESERVED) ||
                           inventoryResponse.getInventoryStatus().equals(INVENTORY_RESERVATION_FAILED)));

            if (!inventoryReceived) {
                logger.error("Inventory timeout for order: {}", orderId);
                throw new InventoryFailedException(orderId, "Inventory reservation timeout");
            }

            if (inventoryResponse.getInventoryStatus().equals(INVENTORY_RESERVATION_FAILED)) {
                logger.warn("Inventory reservation failed for order: {}, compensating", orderId);
                state.setStatus(OrderStatus.FAILED);
                state.setFailureReason(inventoryResponse.getMessage());
                throw new InventoryFailedException(orderId, "Inventory reservation failed");
            }

            logger.info("Inventory reserved for order: {}", orderId);
            state.addCompletedStep(WorkflowStep.INVENTORY);

            // ===== PAYMENT STEP =====
            state.setCurrentStep(WorkflowStep.PAYMENT);
            logger.info("Processing payment for order: {}", orderId);

            // Activity to publish payment request
            activities.publishPaymentRequest(orderId);
            saga.addCompensation(() -> compensationActivities.compensatePayment(orderId));

            boolean paymentReceived = Workflow.await(
                    WorkflowOptionsConfig.SIGNAL_WAIT_TIMEOUT,
                    () -> paymentResponse != null &&
                          (paymentResponse.getPaymentStatus().equals(PAYMENT_COMPLETED) ||
                           paymentResponse.getPaymentStatus().equals(PAYMENT_FAILED)));

            if (!paymentReceived) {
                logger.error("Payment timeout for order: {}", orderId);
                throw new PaymentFailedException(orderId, "Payment processing timeout");
            }

            if (paymentResponse.getPaymentStatus().equals(PAYMENT_FAILED)) {
                logger.warn("Payment failed for order: {}, compensating", orderId);
                state.setStatus(OrderStatus.FAILED);
                state.setFailureReason(paymentResponse.getMessage());
                throw new PaymentFailedException(orderId, "Payment failed");
            }

            logger.info("Payment completed for order: {}", orderId);
            state.addCompletedStep(WorkflowStep.PAYMENT);

            // ===== SHIPPING STEP =====
            state.setCurrentStep(WorkflowStep.SHIPPING);
            logger.info("Processing shipping for order: {}", orderId);

            // Activity to publish shipping request
            activities.publishShippingRequest(orderId);
            saga.addCompensation(() -> compensationActivities.compensateShipping(orderId));

            boolean shippingReceived = Workflow.await(
                    WorkflowOptionsConfig.SIGNAL_WAIT_TIMEOUT,
                    () -> shippingResponse != null &&
                          (shippingResponse.getShippingStatus().equals(SHIPPING_COMPLETED) ||
                           shippingResponse.getShippingStatus().equals(SHIPPING_FAILED)));

            if (!shippingReceived) {
                logger.error("Shipping timeout for order: {}", orderId);
                throw new ShippingFailedException(orderId, "Shipping processing timeout");
            }

            if (shippingResponse.getShippingStatus().equals(SHIPPING_FAILED)) {
                logger.warn("Shipping failed for order: {}, compensating", orderId);
                state.setStatus(OrderStatus.FAILED);
                state.setFailureReason(shippingResponse.getMessage());
                throw new ShippingFailedException(orderId, "Shipping failed");
            }

            logger.info("Shipping completed for order: {}", orderId);
            state.addCompletedStep(WorkflowStep.SHIPPING);

            // ===== ORDER COMPLETED =====
            state.setStatus(OrderStatus.COMPLETED);
            logger.info("Order workflow completed successfully for order: {}", orderId);

        } catch (OrderFailedException | PaymentFailedException | InventoryFailedException | ShippingFailedException e) {
            // Business failures - compensate if needed
            logger.error("Order workflow failed for order {}: {}", orderId, e.getMessage());
            state.setStatus(OrderStatus.COMPENSATING);
            state.setFailureReason(e.getReason());
            saga.compensate();
            state.setStatus(OrderStatus.COMPENSATED);

        } catch (Exception e) {
            // Unexpected failures
            logger.error("Unexpected error in order workflow for order: {}", orderId, e);
            state.setStatus(OrderStatus.COMPENSATING);
            state.setFailureReason("Unexpected error: " + e.getMessage());
            saga.compensate();
            state.setStatus(OrderStatus.FAILED);
            throw e;
        }
    }

    // ===== SIGNAL METHODS (Event-driven pattern) =====

    @Override
    public void orderReply(OrderResponse orderResponse) {
        logger.info("Signal received: Order response with status: {}", orderResponse.getOrderStatus());
        this.orderResponse = orderResponse;
    }

    @Override
    public void inventoryReply(InventoryResponse inventoryResponse) {
        logger.info("Signal received: Inventory response with status: {}", inventoryResponse.getInventoryStatus());
        this.inventoryResponse = inventoryResponse;
    }

    @Override
    public void paymentReply(PaymentResponse paymentResponse) {
        logger.info("Signal received: Payment response with status: {}", paymentResponse.getPaymentStatus());
        this.paymentResponse = paymentResponse;
    }

    @Override
    public void shippingReply(ShippingResponse shippingResponse) {
        logger.info("Signal received: Shipping response with status: {}", shippingResponse.getShippingStatus());
        this.shippingResponse = shippingResponse;
    }

    // ===== QUERY METHODS (Read-only state inspection) =====

    @Override
    public OrderWorkflowState getState() {
        return state;
    }

    @Override
    public String getStatus() {
        return state.getStatus().toString();
    }
}

