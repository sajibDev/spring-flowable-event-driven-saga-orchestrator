package com.saga.activities;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.saga.logging.WorkflowTimestampLogger;
import com.saga.orchestrator.common.constants.RabbitMQConstants;
import com.saga.orchestrator.common.events.CreateOrderCommand;
import com.saga.orchestrator.common.events.ProcessPaymentCommand;
import com.saga.orchestrator.common.events.ReserveInventoryCommand;
import com.saga.orchestrator.common.events.CreateShipmentCommand;
import com.saga.orchestrator.common.events.CancelOrderCommand;
import com.saga.orchestrator.common.events.RefundPaymentCommand;
import com.saga.orchestrator.common.events.CompensateInventoryCommand;
import com.saga.orchestrator.common.events.CancelShipmentCommand;

/**
 * Implementation of OrderActivities.
 * Handles all RabbitMQ event publishing for the saga workflow.
 *
 * Key benefits of using Activities:
 * 1. Workflow code remains deterministic (can be replayed safely)
 * 2. Activities can be retried independently with different policies
 * 3. Side effects are isolated from workflow logic
 * 4. Better observability and debugging
 */
@Component
public class OrderActivitiesImpl implements OrderActivities {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderActivitiesImpl.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private WorkflowTimestampLogger timestampLogger;

    @Override
    public void publishOrderCreatedEvent(String orderId) {
        logger.info("Activity: Publishing CREATE_ORDER command for order: {}", orderId);

        try {
            timestampLogger.logOrderRequestPublished(orderId);

            CreateOrderCommand command = CreateOrderCommand.builder()
                    .correlationId(orderId)
                    .customerId("CUSTOMER-001")
                    .totalAmount(new BigDecimal("100.00"))
                    .shippingAddress("123 Main St, City, Country")
                    .build();

            rabbitTemplate.convertAndSend(RabbitMQConstants.ORDER_EXCHANGE, RabbitMQConstants.ORDER_CREATE_ROUTING_KEY, command);
            logger.info("Activity: Successfully published CREATE_ORDER command for order: {}", orderId);
        } catch (Exception e) {
            logger.error("Activity: Failed to publish CREATE_ORDER command for order: {}", orderId, e);
            throw new RuntimeException("Failed to publish order created event", e);
        }
    }

    @Override
    public void publishPaymentRequest(String orderId) {
        logger.info("Activity: Publishing PROCESS_PAYMENT command for order: {}", orderId);
        try {
            timestampLogger.logPaymentRequestPublished(orderId);

            ProcessPaymentCommand command = ProcessPaymentCommand.builder()
                    .correlationId(orderId)
                    .orderId(orderId)
                    .customerId("CUSTOMER-001")
                    .amount(new BigDecimal("100.00"))
                    .build();

            rabbitTemplate.convertAndSend(RabbitMQConstants.ORDER_EXCHANGE, RabbitMQConstants.PAYMENT_PROCESS_ROUTING_KEY, command);
            logger.info("Activity: Successfully published PROCESS_PAYMENT command for order: {}", orderId);
        } catch (Exception e) {
            logger.error("Activity: Failed to publish PROCESS_PAYMENT command for order: {}", orderId, e);
            throw new RuntimeException("Failed to publish payment request", e);
        }
    }
    
    @Override
    public void publishInventoryRequest(String orderId) {
        logger.info("Activity: Publishing RESERVE_INVENTORY command for order: {}", orderId);
        try {
            timestampLogger.logInventoryRequestPublished(orderId);

            ReserveInventoryCommand command = ReserveInventoryCommand.builder()
                    .correlationId(orderId)
                    .orderId(orderId)
                    .customerId("CUSTOMER-001")
                    .productName("Sample Product")
                    .quantity(2)
                    .build();

            rabbitTemplate.convertAndSend(RabbitMQConstants.ORDER_EXCHANGE, RabbitMQConstants.INVENTORY_RESERVE_ROUTING_KEY, command);
            logger.info("Activity: Successfully published RESERVE_INVENTORY command for order: {}", orderId);
        } catch (Exception e) {
            logger.error("Activity: Failed to publish RESERVE_INVENTORY command for order: {}", orderId, e);
            throw new RuntimeException("Failed to publish inventory request", e);
        }
    }
    
    @Override
    public void publishShippingRequest(String orderId) {
        logger.info("Activity: Publishing CREATE_SHIPMENT command for order: {}", orderId);
        try {
            timestampLogger.logShippingRequestPublished(orderId);

            CreateShipmentCommand command = CreateShipmentCommand.builder()
                    .correlationId(orderId)
                    .orderId(orderId)
                    .customerId("CUSTOMER-001")
                    .shippingAddress("123 Main St, City, Country")
                    .build();

            rabbitTemplate.convertAndSend(RabbitMQConstants.ORDER_EXCHANGE, RabbitMQConstants.SHIPPING_CREATE_ROUTING_KEY, command);
            logger.info("Activity: Successfully published CREATE_SHIPMENT command for order: {}", orderId);
        } catch (Exception e) {
            logger.error("Activity: Failed to publish CREATE_SHIPMENT command for order: {}", orderId, e);
            throw new RuntimeException("Failed to publish shipping request", e);
        }
    }

    @Override
    public void compensateOrder(String orderId) {
        logger.warn("Activity: Cancelling order: {}", orderId);
        try {
            timestampLogger.logCompensationStarted(orderId, "ORDER");

            CancelOrderCommand command = CancelOrderCommand.builder()
                    .correlationId(orderId)
                    .orderId(orderId)
                    .reason("Saga compensation")
                    .build();

            rabbitTemplate.convertAndSend(RabbitMQConstants.ORDER_EXCHANGE, RabbitMQConstants.ORDER_CANCEL_ROUTING_KEY, command);
            logger.info("Activity: Successfully published CANCEL_ORDER command for order: {}", orderId);
        } catch (Exception e) {
            logger.error("Activity: Failed to cancel order: {}", orderId, e);
            throw new RuntimeException("Failed to cancel order", e);
        }
    }

    @Override
    public void compensatePayment(String orderId) {
        logger.warn("Activity: Compensating payment for order: {}", orderId);
        try {
            timestampLogger.logCompensationStarted(orderId, "PAYMENT");

            RefundPaymentCommand command = RefundPaymentCommand.builder()
                    .correlationId(orderId)
                    .orderId(orderId)
                    .build();

            rabbitTemplate.convertAndSend(RabbitMQConstants.ORDER_EXCHANGE, RabbitMQConstants.PAYMENT_REFUND_ROUTING_KEY, command);
            logger.info("Activity: Successfully published REFUND_PAYMENT command for order: {}", orderId);
        } catch (Exception e) {
            logger.error("Activity: Failed to compensate payment for order: {}", orderId, e);
            throw new RuntimeException("Failed to compensate payment", e);
        }
    }
    
    @Override
    public void compensateInventory(String orderId) {
        logger.warn("Activity: Compensating inventory reservation for order: {}", orderId);
        try {
            timestampLogger.logCompensationStarted(orderId, "INVENTORY");

            CompensateInventoryCommand command = CompensateInventoryCommand.builder()
                    .correlationId(orderId)
                    .orderId(orderId)
                    .build();

            rabbitTemplate.convertAndSend(RabbitMQConstants.ORDER_EXCHANGE, RabbitMQConstants.INVENTORY_RELEASE_ROUTING_KEY, command);
            logger.info("Activity: Successfully published COMPENSATE_INVENTORY command for order: {}", orderId);
        } catch (Exception e) {
            logger.error("Activity: Failed to compensate inventory for order: {}", orderId, e);
            throw new RuntimeException("Failed to compensate inventory", e);
        }
    }
    
    @Override
    public void compensateShipping(String orderId) {
        logger.warn("Activity: Compensating shipping for order: {}", orderId);
        try {
            timestampLogger.logCompensationStarted(orderId, "SHIPPING");

            CancelShipmentCommand command = CancelShipmentCommand.builder()
                    .correlationId(orderId)
                    .orderId(orderId)
                    .build();

            rabbitTemplate.convertAndSend(RabbitMQConstants.ORDER_EXCHANGE, RabbitMQConstants.SHIPPING_CANCEL_ROUTING_KEY, command);
            logger.info("Activity: Successfully published CANCEL_SHIPMENT command for order: {}", orderId);
        } catch (Exception e) {
            logger.error("Activity: Failed to compensate shipping for order: {}", orderId, e);
            throw new RuntimeException("Failed to compensate shipping", e);
        }
    }
}
