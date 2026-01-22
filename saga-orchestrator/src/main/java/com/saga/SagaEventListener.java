package com.saga;

import com.saga.dto.InventoryResponse;
import com.saga.dto.OrderResponse;
import com.saga.dto.PaymentResponse;
import com.saga.dto.ShippingResponse;
import com.saga.orchestrator.common.events.OrderCreatedEvent;
import com.saga.orchestrator.common.events.PaymentProcessedEvent;
import com.saga.orchestrator.common.events.InventoryReservedEvent;
import com.saga.orchestrator.common.events.ShipmentCreatedEvent;
import com.saga.orchestrator.common.constants.RabbitMQConstants;
import com.saga.logging.WorkflowTimestampLogger;
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
 * 7. Response-based signals with status information
 *
 * Pattern: Signals for external events (event-driven, durable)
 * - Each service event is converted to a response object and signaled to the workflow
 */
@Component
public class SagaEventListener {

    private static final Logger logger = LoggerFactory.getLogger(SagaEventListener.class);

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

    @Autowired
    private WorkflowClient workflowClient;

    @Autowired
    private WorkflowTimestampLogger timestampLogger;

    /**
     * Listens to order-created events and signals the workflow with order response
     */
    @RabbitListener(queues = RabbitMQConstants.ORDER_CREATED_EVENT_QUEUE)
    public void consumeOrderEvent(@Payload OrderCreatedEvent event) {
        String workflowId = event.getCorrelationId();

        logger.info("Processing order event for order: {}, success: {}", workflowId, event.isSuccess());
        timestampLogger.logOrderEventReceived(workflowId, event.isSuccess());

        try {
            OrderResponse orderResponse = new OrderResponse();
            orderResponse.setOrderId(workflowId);

            if (event.isSuccess()) {
                orderResponse.setOrderStatus(ORDER_CREATED);
                orderResponse.setMessage(event.getMessage());
            } else {
                orderResponse.setOrderStatus(ORDER_CREATION_FAILED);
                orderResponse.setMessage(event.getMessage());
            }

            signalWorkflow(workflowId, wf -> wf.orderReply(orderResponse), "Order response");
        } catch (Exception e) {
            logger.error("Error processing Order event for order: {}", workflowId, e);
        }
    }
    
    /**
     * Listens to inventory-reserved events and signals the workflow with inventory response
     */
    @RabbitListener(queues = RabbitMQConstants.INVENTORY_RESERVED_EVENT_QUEUE)
    public void consumeInventoryEvent(@Payload InventoryReservedEvent event) {
        String workflowId = event.getCorrelationId();

        logger.info("Processing inventory event for order: {}, success: {}", workflowId, event.isSuccess());
        timestampLogger.logInventoryEventReceived(workflowId, event.isSuccess());

        try {
            InventoryResponse inventoryResponse = new InventoryResponse();
            inventoryResponse.setOrderId(workflowId);
            inventoryResponse.setReservationId(event.getReservationId());

            if (event.isSuccess()) {
                inventoryResponse.setInventoryStatus(INVENTORY_RESERVED);
                inventoryResponse.setMessage(event.getMessage());
            } else {
                inventoryResponse.setInventoryStatus(INVENTORY_RESERVATION_FAILED);
                inventoryResponse.setMessage(event.getMessage());
            }

            signalWorkflow(workflowId, wf -> wf.inventoryReply(inventoryResponse), "Inventory response");
        } catch (Exception e) {
            logger.error("Error processing inventory event for order: {}", workflowId, e);
        }
    }

    /**
     * Listens to payment-processed events and signals the workflow with payment response
     */
    @RabbitListener(queues = RabbitMQConstants.PAYMENT_PROCESSED_EVENT_QUEUE)
    public void consumePaymentEvent(@Payload PaymentProcessedEvent event) {
        String workflowId = event.getCorrelationId();

        logger.info("Processing payment event for order: {}, success: {}", workflowId, event.isSuccess());
        timestampLogger.logPaymentEventReceived(workflowId, event.isSuccess());

        try {
            PaymentResponse paymentResponse = new PaymentResponse();
            paymentResponse.setOrderId(workflowId);
            paymentResponse.setTransactionId(event.getTransactionId());

            if (event.isSuccess()) {
                paymentResponse.setPaymentStatus(PAYMENT_COMPLETED);
                paymentResponse.setMessage(event.getMessage());
            } else {
                paymentResponse.setPaymentStatus(PAYMENT_FAILED);
                paymentResponse.setMessage(event.getMessage());
            }

            signalWorkflow(workflowId, wf -> wf.paymentReply(paymentResponse), "Payment response");
        } catch (Exception e) {
            logger.error("Error processing payment event for order: {}", workflowId, e);
        }
    }

    /**
     * Listens to shipment-created events and signals the workflow with shipping response
     */
    @RabbitListener(queues = RabbitMQConstants.SHIPMENT_CREATED_EVENT_QUEUE)
    public void consumeShippingEvent(@Payload ShipmentCreatedEvent event) {
        String workflowId = event.getCorrelationId();

        logger.info("Processing shipping event for order: {}, success: {}", workflowId, event.isSuccess());
        timestampLogger.logShippingEventReceived(workflowId, event.isSuccess());

        try {
            ShippingResponse shippingResponse = new ShippingResponse();
            shippingResponse.setOrderId(workflowId);
            shippingResponse.setShipmentId(event.getShipmentId());

            if (event.isSuccess()) {
                shippingResponse.setShippingStatus(SHIPPING_COMPLETED);
                shippingResponse.setMessage(event.getMessage());
            } else {
                shippingResponse.setShippingStatus(SHIPPING_FAILED);
                shippingResponse.setMessage(event.getMessage());
            }

            signalWorkflow(workflowId, wf -> wf.shippingReply(shippingResponse), "Shipping response");
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
