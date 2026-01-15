package com.saga.orchestrator.shipping.service;

import static com.saga.orchestrator.common.constants.RabbitMQConstants.ORDER_EXCHANGE;
import static com.saga.orchestrator.common.constants.RabbitMQConstants.SHIPMENT_CREATED_ROUTING_KEY;

import java.util.UUID;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import com.saga.orchestrator.common.events.CreateShipmentCommand;
import com.saga.orchestrator.common.events.ShipmentCreatedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShippingService {

    private final RabbitTemplate rabbitTemplate;

    public void createShipment(CreateShipmentCommand createShipmentCommand) {
        log.info("Started creating shipment. CorrelationId: {}, OrderId: {}",
                createShipmentCommand.getCorrelationId(), createShipmentCommand.getOrderId());

        try {
            String shipmentId = UUID.randomUUID().toString();

            // Simulate 30% failure rate
            if (Math.random() < 0.3) {
                log.error("Shipment creation failed (simulated). CorrelationId: {}, OrderId: {}",
                        createShipmentCommand.getCorrelationId(), createShipmentCommand.getOrderId());
                
                publishShipmentCreatedEvent(
                        createShipmentCommand.getCorrelationId(),
                        null,
                        false,
                        "Shipment service unavailable");
                return;
            }

            Thread.sleep(500);

            log.info("Shipment created successfully. CorrelationId: {}, OrderId: {}",
                    createShipmentCommand.getCorrelationId(), createShipmentCommand.getOrderId());

            publishShipmentCreatedEvent(
                    createShipmentCommand.getCorrelationId(),
                    shipmentId,
                    true,
                    "Shipment created successfully");

        } catch (Exception e) {
            log.error("Error occurred while creating shipment. CorrelationId: {}, OrderId: {}",
                    createShipmentCommand.getCorrelationId(), createShipmentCommand.getOrderId(), e);

            publishShipmentCreatedEvent(
                    createShipmentCommand.getCorrelationId(),
                    null,
                    false,
                    "Error creating shipment: " + e.getMessage());
        }
    }

    private void publishShipmentCreatedEvent(
            String correlationId,
            String shipmentId,
            boolean success,
            String message) {
        ShipmentCreatedEvent event = ShipmentCreatedEvent.builder()
                .correlationId(correlationId)
                .shipmentId(shipmentId)
                .success(success)
                .message(message)
                .build();

        rabbitTemplate.convertAndSend(
                ORDER_EXCHANGE,
                SHIPMENT_CREATED_ROUTING_KEY,
                event);

        log.info("Successfully published shipment created event. CorrelationId: {}, Success: {}",
                correlationId, success);
    }
}