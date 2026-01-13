package com.saga.orchestrator.shipping.service;

import static com.saga.orchestrator.common.constants.RabbitMQConstants.ORDER_EXCHANGE;
import static com.saga.orchestrator.common.constants.RabbitMQConstants.SHIPMENT_CREATED_ROUTING_KEY;

import com.saga.orchestrator.common.events.*;
import com.saga.orchestrator.shipping.entity.Shipment;
import com.saga.orchestrator.shipping.entity.ShipmentStatus;
import com.saga.orchestrator.shipping.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShippingService {

  private final ShipmentRepository shipmentRepository;
  private final RabbitTemplate rabbitTemplate;

  @Transactional
  public void createShipment(CreateShipmentCommand createShipmentCommand) {
    log.info("Started creating shipment. CorrelationId: {}, OrderId: {}",
        createShipmentCommand.getCorrelationId(), createShipmentCommand.getOrderId());

    try {
      String shipmentId = UUID.randomUUID().toString();
      String trackingNumber = "TRK-" + System.currentTimeMillis();

      Shipment shipment = Shipment.builder()
          .shipmentId(shipmentId)
          .orderId(createShipmentCommand.getOrderId())
          .shippingAddress(createShipmentCommand.getShippingAddress())
          .status(ShipmentStatus.CREATED)
          .trackingNumber(trackingNumber)
          .build();

      shipmentRepository.save(shipment);

      log.info("Shipment created successfully. CorrelationId: {}, OrderId: {}",
          createShipmentCommand.getCorrelationId(), createShipmentCommand.getOrderId());

      publishShipmentCreatedEvent(
          createShipmentCommand.getCorrelationId(),
          shipmentId,
          true,
          "Shipment created successfully"
      );

    } catch (Exception e) {
      log.error("Error occurred while creating shipment. CorrelationId: {}, OrderId: {}",
          createShipmentCommand.getCorrelationId(), createShipmentCommand.getOrderId(), e);

      publishShipmentCreatedEvent(
          createShipmentCommand.getCorrelationId(),
          null,
          false,
          "Error creating shipment: " + e.getMessage()
      );
    }
  }

  private void publishShipmentCreatedEvent(
      String correlationId,
      String shipmentId,
      boolean success,
      String message
  ) {
    ShipmentCreatedEvent event = ShipmentCreatedEvent.builder()
        .correlationId(correlationId)
        .shipmentId(shipmentId)
        .success(success)
        .message(message)
        .build();

    rabbitTemplate.convertAndSend(
        ORDER_EXCHANGE,
        SHIPMENT_CREATED_ROUTING_KEY,
        event
    );

    log.info("Successfully published shipment created event. CorrelationId: {}, Success: {}",
        correlationId, success);
  }
}