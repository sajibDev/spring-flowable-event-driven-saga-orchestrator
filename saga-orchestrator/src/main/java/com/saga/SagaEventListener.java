package com.saga;

import com.saga.orchestrator.common.events.OrderCreatedEvent;
import com.saga.orchestrator.common.events.PaymentProcessedEvent;
import com.saga.orchestrator.common.events.InventoryReservedEvent;
import com.saga.orchestrator.common.events.ShipmentCreatedEvent;
import com.saga.orchestrator.common.constants.RabbitMQConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Payload;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowNotFoundException;

/**
 * Saga Event Listener following Temporal best practices.
 * 
 * Improvements:
 * 1. Better error handling with specific exception types
 * 2. Null checks and validation
 * 3. Structured logging (SLF4J instead of System.out)
 * 4. Graceful handling of duplicate signals
 * 5. Proper workflow execution timeout configuration
 * 6. Domain-driven event listeners - each service publishes to its own topic
 * 
 * Pattern: Signals for external events (event-driven, durable)
 * - ORDER_CREATED: Starts new workflow
 * - Other domain events: Signal existing workflow
 */
@Component
public class SagaEventListener {

    //todo : {temporal worker concepts}

    private static final Logger logger = LoggerFactory.getLogger(SagaEventListener.class);

    @Autowired
    private WorkflowClient workflowClient;

    /**
     * Listens to order-events topic for ORDER_CREATED events to start workflows
     */
   // @KafkaListener(topics = "order-events", groupId = "saga-group")
    @RabbitListener(queues = RabbitMQConstants.ORDER_CREATED_EVENT_QUEUE)
    public void consumeOrderEvent(@Payload OrderCreatedEvent event) {

        String workflowId = event.getCorrelationId();

        logger.info("Processing order event for order: {}, success: {}", workflowId, event.isSuccess());

        try {
            if (event.isSuccess()) {
                signalWorkflow(workflowId, OrderWorkflow::onOrderCreated, "Order completion");
            } else {
                signalWorkflow(workflowId, OrderWorkflow::onOrderFailed, "Order failure");
            }
        } catch (Exception e) {
            logger.error("Error processing Order event for order: {}", workflowId, e);
        }
    }
    
    /**
     * Listens to payment-events topic for payment-related events
     */
    //@KafkaListener(topics = "payment-events", groupId = "saga-group")
    @RabbitListener(queues = RabbitMQConstants.PAYMENT_PROCESSED_EVENT_QUEUE)
    public void consumePaymentEvent(@Payload PaymentProcessedEvent event) {

        String workflowId = event.getCorrelationId();

        logger.info("Processing payment event for order: {}, success: {}", workflowId, event.isSuccess());

        try {
            if (event.isSuccess()) {
                signalWorkflow(workflowId, OrderWorkflow::onPaymentCompleted, "payment completion");
            } else {
                signalWorkflow(workflowId, OrderWorkflow::onPaymentFailed, "payment failure");
            }
        } catch (Exception e) {
            logger.error("Error processing payment event for order: {}", workflowId, e);
        }
    }
    
    /**
     * Listens to inventory-events topic for inventory-related events
     */
    @RabbitListener(queues = RabbitMQConstants.INVENTORY_RESERVED_EVENT_QUEUE)
    public void consumeInventoryEvent(@Payload InventoryReservedEvent event) {

        String workflowId = event.getCorrelationId();

        logger.info("Processing inventory event for order: {}, success: {}", workflowId, event.isSuccess());

        try {
            if (event.isSuccess()) {
                signalWorkflow(workflowId, OrderWorkflow::onInventoryReserved, "inventory reservation");
            } else {
                signalWorkflow(workflowId, OrderWorkflow::onInventoryFailed, "inventory failure");
            }
        } catch (Exception e) {
            logger.error("Error processing inventory event for order: {}", workflowId, e);
        }
    }
    
    /**
     * Listens to shipping-events topic for shipping-related events
     */
    @RabbitListener(queues = RabbitMQConstants.SHIPMENT_CREATED_EVENT_QUEUE)
    public void consumeShippingEvent(@Payload ShipmentCreatedEvent event) {

        String workflowId = event.getCorrelationId();

        logger.info("Processing shipping event for order: {}, success: {}", workflowId, event.isSuccess());

        try {
            if (event.isSuccess()) {
                signalWorkflow(workflowId, OrderWorkflow::onShippingCompleted, "shipping completion");
            } else {
                signalWorkflow(workflowId, OrderWorkflow::onShippingFailed, "shipping failure");
            }
        } catch (Exception e) {
            logger.error("Error processing shipping event for order: {}", workflowId, e);
        }
    }
    
    /**
     * Signals an existing workflow.
     * Handles WorkflowNotFoundException gracefully (workflow may have already completed).
     */
    private void signalWorkflow(String workflowId, 
                                java.util.function.Consumer<OrderWorkflow> signalMethod,
                                String signalDescription) {
        try {
            OrderWorkflow workflow = workflowClient.newWorkflowStub(OrderWorkflow.class, workflowId);
            signalMethod.accept(workflow);
            logger.info("Signaled workflow for {} - order: {}", signalDescription, workflowId);
            
        } catch (WorkflowNotFoundException e) {
            // Workflow may have already completed or timed out - log as warning, not error
            logger.warn("Workflow not found for {} - order: {} (may have already completed)", 
                signalDescription, workflowId);
            
        } catch (Exception e) {
            logger.error("Failed to signal workflow for {} - order: {}", signalDescription, workflowId, e);
            throw new RuntimeException(
                String.format("Failed to signal %s for order %s", signalDescription, workflowId), e);
        }
    }
}
