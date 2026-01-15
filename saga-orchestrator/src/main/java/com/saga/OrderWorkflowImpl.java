package com.saga;

import com.saga.activities.OrderActivities;
import com.saga.config.WorkflowOptionsConfig;
import com.saga.dto.CreateOrderRequest;
import com.saga.exceptions.InventoryFailedException;
import com.saga.exceptions.OrderFailedException;
import com.saga.exceptions.PaymentFailedException;
import com.saga.exceptions.ShippingFailedException;
import com.saga.model.OrderWorkflowState;
import com.saga.model.OrderWorkflowState.OrderStatus;
import com.saga.model.OrderWorkflowState.WorkflowStep;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.Saga;
import io.temporal.workflow.CompletablePromise;
import org.slf4j.Logger;

/**
 * Order Workflow Implementation following Temporal best practices.
 * 
 * Key improvements:
 * 1. Uses Activities for all side effects (Kafka publishing)
 * 2. Keeps Signals for external events (durable, event-driven)
 * 3. Adds timeouts to prevent infinite waiting
 * 4. Real compensations via Activities
 * 5. Structured state tracking for queries
 * 6. Proper error handling with custom exceptions
 */
public class OrderWorkflowImpl implements OrderWorkflow {
    
    private static final Logger logger = Workflow.getLogger(OrderWorkflowImpl.class);
    
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
    
    // Promises for signal-based coordination (event-driven pattern)
    private final CompletablePromise<Void> orderCompleted = Workflow.newPromise();
    private final CompletablePromise<Void> orderFailed = Workflow.newPromise();
    private final CompletablePromise<Void> paymentCompleted = Workflow.newPromise();
    private final CompletablePromise<Void> paymentFailed = Workflow.newPromise();
    private final CompletablePromise<Void> inventoryReserved = Workflow.newPromise();
    private final CompletablePromise<Void> inventoryFailed = Workflow.newPromise();
    private final CompletablePromise<Void> shippingCompleted = Workflow.newPromise();
    private final CompletablePromise<Void> shippingFailed = Workflow.newPromise();
    
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
            // ===== Order STEP =====
            state.setCurrentStep(WorkflowStep.ORDER);
            logger.info("Processing order: {}", orderId);

            //activites to publish order request

            activities.publishOrderCreatedEvent(orderId);

            //Register compensation for order
            saga.addCompensation(() -> compensationActivities.compensateOrder(orderId));

            boolean orderCreated = Workflow.await(
                    WorkflowOptionsConfig.SIGNAL_WAIT_TIMEOUT,
                    () -> orderCompleted.isCompleted() || orderFailed.isCompleted());

            if (!orderCreated) {
                logger.info("Order timeout for order: {}", orderId);
                throw new OrderFailedException(orderId, "Order creation timeout");
            }

            if (orderFailed.isCompleted()) {
                logger.warn("Order failed: {}, no compensation needed", orderId);
                state.setStatus(OrderStatus.FAILED);
                state.setFailureReason("Order failed");
                return;
            }

            logger.info("Order created for order: {}", orderId);
            state.addCompletedStep(WorkflowStep.ORDER);

            // ===== INVENTORY STEP =====
            state.setCurrentStep(WorkflowStep.INVENTORY);
            logger.info("Processing inventory reservation for order: {}", orderId);

            // Use Activity to publish inventory request
            activities.publishInventoryRequest(orderId);

            // Wait for signal with timeout
            boolean inventoryReceived = Workflow.await(
                    WorkflowOptionsConfig.SIGNAL_WAIT_TIMEOUT,
                    () -> inventoryReserved.isCompleted() || inventoryFailed.isCompleted()
            );

            if (!inventoryReceived) {
                logger.error("Inventory timeout for order: {}", orderId);
                throw new InventoryFailedException(orderId, "Inventory reservation timeout");
            }

            if (inventoryFailed.isCompleted()) {
                logger.warn("Inventory reservation failed for order: {}, compensating payment", orderId);
                state.setStatus(OrderStatus.COMPENSATING);
                state.setFailureReason("Inventory reservation failed");
                saga.compensate();
                state.setStatus(OrderStatus.COMPENSATED);
                return;
            }

            logger.info("Inventory reserved for order: {}", orderId);
            state.addCompletedStep(WorkflowStep.INVENTORY);

            // Register compensation for inventory (after success)
            saga.addCompensation(() -> compensationActivities.compensateInventory(orderId));

            // ===== Payment STEP =====
            state.setCurrentStep(WorkflowStep.PAYMENT);
            // Use Activity to publish payment request (deterministic)
            activities.publishPaymentRequest(orderId);
            
            // Register compensation for payment
            saga.addCompensation(() -> compensationActivities.compensatePayment(orderId));
            
            // Wait for signal with timeout (event-driven + resilient)
            boolean paymentReceived = Workflow.await(
                WorkflowOptionsConfig.SIGNAL_WAIT_TIMEOUT,
                () -> paymentCompleted.isCompleted() || paymentFailed.isCompleted()
            );
            
            if (!paymentReceived) {
                logger.error("Payment timeout for order: {}", orderId);
                throw new PaymentFailedException(orderId, "Payment processing timeout");
            }
            
            if (paymentFailed.isCompleted()) {
                logger.warn("Payment failed for order: {}, no compensation needed", orderId);
                state.setStatus(OrderStatus.COMPENSATING);
                state.setFailureReason("Payment failed");
                saga.compensate();
                state.setStatus(OrderStatus.COMPENSATED);
                return;
            }
            
            logger.info("Payment completed for order: {}", orderId);
            state.addCompletedStep(WorkflowStep.PAYMENT);

            // ===== SHIPPING STEP =====
            state.setCurrentStep(WorkflowStep.SHIPPING);
            logger.info("Processing shipping for order: {}", orderId);
            
            // Use Activity to publish shipping request
            activities.publishShippingRequest(orderId);

            saga.addCompensation(() -> compensationActivities.compensateShipping(orderId));
            // Wait for signal with timeout
            boolean shippingReceived = Workflow.await(
                WorkflowOptionsConfig.SIGNAL_WAIT_TIMEOUT,
                () -> shippingCompleted.isCompleted() || shippingFailed.isCompleted()
            );
            
            if (!shippingReceived) {
                logger.error("Shipping timeout for order: {}", orderId);
                throw new ShippingFailedException(orderId, "Shipping processing timeout");
            }
            
            if (shippingFailed.isCompleted()) {
                logger.warn("Shipping failed for order: {}, compensating inventory and payment", orderId);
                state.setStatus(OrderStatus.COMPENSATING);
                state.setFailureReason("Shipping failed");
                saga.compensate();
                state.setStatus(OrderStatus.COMPENSATED);
                return;
            }
            
            logger.info("Shipping completed for order: {}", orderId);
            state.addCompletedStep(WorkflowStep.SHIPPING);
            
            // ===== ORDER COMPLETED =====
            state.setStatus(OrderStatus.COMPLETED);
            logger.info("Order workflow completed successfully for order: {}", orderId);
            
        } catch (PaymentFailedException | InventoryFailedException | ShippingFailedException e) {
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

    @Override
    public void onOrderCreated() {
        logger.info("Order created event received");
        orderCompleted.complete(null);
    }

    @Override
    public void onOrderFailed() {
        logger.warn("Order failed event received");
        orderFailed.complete(null);
    }

    // ===== SIGNAL METHODS (Event-driven pattern) =====
    
    @Override
    public void onPaymentCompleted() {
        logger.info("Signal received: Payment completed");
        paymentCompleted.complete(null);
    }

    @Override
    public void onPaymentFailed() {
        logger.warn("Signal received: Payment failed");
        paymentFailed.complete(null);
    }

    @Override
    public void onInventoryReserved() {
        logger.info("Signal received: Inventory reserved");
        inventoryReserved.complete(null);
    }

    @Override
    public void onInventoryFailed() {
        logger.warn("Signal received: Inventory failed");
        inventoryFailed.complete(null);
    }

    @Override
    public void onShippingCompleted() {
        logger.info("Signal received: Shipping completed");
        shippingCompleted.complete(null);
    }

    @Override
    public void onShippingFailed() {
        logger.warn("Signal received: Shipping failed");
        shippingFailed.complete(null);
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

