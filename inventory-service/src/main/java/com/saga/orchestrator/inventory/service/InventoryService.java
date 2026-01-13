package com.saga.orchestrator.inventory.service;

import static com.saga.orchestrator.common.constants.RabbitMQConstants.INVENTORY_COMPENSATED_ROUTING_KEY;
import static com.saga.orchestrator.common.constants.RabbitMQConstants.INVENTORY_RESERVED_ROUTING_KEY;
import static com.saga.orchestrator.common.constants.RabbitMQConstants.ORDER_EXCHANGE;

import com.saga.orchestrator.common.events.*;
import com.saga.orchestrator.inventory.entity.InventoryReservation;
import com.saga.orchestrator.inventory.entity.ReservationStatus;
import com.saga.orchestrator.inventory.repository.InventoryReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

  private final InventoryReservationRepository reservationRepository;
  private final RabbitTemplate rabbitTemplate;

  @Transactional
  public void reserveInventory(ReserveInventoryCommand reserveInventoryCommand) {
    log.info("Started reserving inventory. CorrelationId: {}, OrderId: {}",
        reserveInventoryCommand.getCorrelationId(), reserveInventoryCommand.getOrderId());

    try {
      String reservationId = UUID.randomUUID().toString();
      boolean allAvailable = true;
      StringBuilder failureMessage = new StringBuilder();

      for (String productId : reserveInventoryCommand.getProductIdList()) {
        boolean inventoryAvailable = checkInventoryAvailability(productId, reserveInventoryCommand.getQuantity());
        
        if (!inventoryAvailable) {
          allAvailable = false;
          failureMessage.append("Insufficient inventory for product: ").append(productId).append("; ");
        }
      }

      if (!allAvailable) {
        log.warn("Insufficient inventory for some products: {}", failureMessage);

        publishInventoryReservedEvent(
            reserveInventoryCommand.getCorrelationId(),
            null,
            false,
            failureMessage.toString()
        );

        return;
      }

      for (String productId : reserveInventoryCommand.getProductIdList()) {
        InventoryReservation reservation = InventoryReservation.builder()
            .reservationId(reservationId)
            .orderId(reserveInventoryCommand.getOrderId())
            .productId(productId)
            .quantity(reserveInventoryCommand.getQuantity())
            .status(ReservationStatus.RESERVED)
            .build();

        reservationRepository.save(reservation);
      }

      log.info("Completed reserving inventory. CorrelationId: {}, OrderId: {}",
          reserveInventoryCommand.getCorrelationId(), reserveInventoryCommand.getOrderId());
      publishInventoryReservedEvent(reserveInventoryCommand.getCorrelationId(), reservationId, true,
          "Inventory reserved successfully");

    } catch (Exception e) {
      log.error("Error occurred while reserving inventory. CorrelationId: {}, OrderId: {}",
          reserveInventoryCommand.getCorrelationId(), reserveInventoryCommand.getOrderId(), e);
      publishInventoryReservedEvent(
          reserveInventoryCommand.getCorrelationId(),
          null,
          false,
          "Error reserving inventory: " + e.getMessage()
      );
    }
  }

  @Transactional
  public void releaseInventory(CompensateInventoryCommand compensateInventoryCommand) {
    log.info("Started releasing inventory. CorrelationId: {}, OrderId: {}",
        compensateInventoryCommand.getCorrelationId(), compensateInventoryCommand.getOrderId());

    List<InventoryReservation> reservations = reservationRepository.findByOrderId(
        compensateInventoryCommand.getOrderId());

    for (InventoryReservation reservation : reservations) {
      reservation.setStatus(ReservationStatus.RELEASED);
      reservationRepository.save(reservation);
    }

    log.info("Inventory released for order: {}", compensateInventoryCommand.getCorrelationId());

    InventoryCompensatedEvent event = InventoryCompensatedEvent.builder()
        .orderId(compensateInventoryCommand.getOrderId())
        .build();

    rabbitTemplate.convertAndSend(
        ORDER_EXCHANGE,
        INVENTORY_COMPENSATED_ROUTING_KEY,
        event
    );
  }

  private boolean checkInventoryAvailability(String productId, Integer quantity) {
    return true;
  }

  private void publishInventoryReservedEvent(
      String correlationId,
      String reservationId,
      boolean success,
      String message
  ) {
    InventoryReservedEvent inventoryReservedEvent = InventoryReservedEvent.builder()
        .correlationId(correlationId)
        .reservationId(reservationId)
        .success(success)
        .message(message)
        .build();

    rabbitTemplate.convertAndSend(
        ORDER_EXCHANGE,
        INVENTORY_RESERVED_ROUTING_KEY,
        inventoryReservedEvent
    );

    log.info("Published inventoryReservedEvent. CorrelationId: {}, IsSuccess: {}", correlationId,
        success);
  }
}