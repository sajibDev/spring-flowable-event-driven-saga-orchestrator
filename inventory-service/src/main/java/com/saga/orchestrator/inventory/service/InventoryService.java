package com.saga.orchestrator.inventory.service;

import static com.saga.orchestrator.common.constants.RabbitMQConstants.INVENTORY_COMPENSATED_ROUTING_KEY;
import static com.saga.orchestrator.common.constants.RabbitMQConstants.INVENTORY_RESERVED_ROUTING_KEY;
import static com.saga.orchestrator.common.constants.RabbitMQConstants.ORDER_EXCHANGE;

import java.util.UUID;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import com.saga.orchestrator.common.events.CompensateInventoryCommand;
import com.saga.orchestrator.common.events.InventoryCompensatedEvent;
import com.saga.orchestrator.common.events.InventoryReservedEvent;
import com.saga.orchestrator.common.events.ReserveInventoryCommand;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

  private final RabbitTemplate rabbitTemplate;

  public void reserveInventory(ReserveInventoryCommand reserveInventoryCommand) {
    log.info("Started reserving inventory. CorrelationId: {}, OrderId: {}",
        reserveInventoryCommand.getCorrelationId(), reserveInventoryCommand.getOrderId());

    try {
      String reservationId = UUID.randomUUID().toString();
      boolean allAvailable = true;
      StringBuilder failureMessage = new StringBuilder();

//      for (String productId : reserveInventoryCommand.getProductIdList()) {
//        boolean inventoryAvailable = checkInventoryAvailability(productId, reserveInventoryCommand.getQuantity());
//
//        if (!inventoryAvailable) {
//          allAvailable = false;
//          failureMessage.append("Insufficient inventory for product: ").append(productId).append("; ");
//        }
//      }

      if (!allAvailable) {
        log.warn("Insufficient inventory for some products: {}", failureMessage);

        publishInventoryReservedEvent(
            reserveInventoryCommand.getCorrelationId(),
            null,
            false,
            failureMessage.toString());

        return;
      }

      Thread.sleep(250);

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
          "Error reserving inventory: " + e.getMessage());
    }
  }

  public void releaseInventory(CompensateInventoryCommand compensateInventoryCommand) {
    log.info("Started releasing inventory. CorrelationId: {}, OrderId: {}",
        compensateInventoryCommand.getCorrelationId(), compensateInventoryCommand.getOrderId());

    try {
      Thread.sleep(250);
    } catch (InterruptedException e) {
      // Handle interruption
    }

    log.info("Inventory released for order: {}", compensateInventoryCommand.getCorrelationId());

    InventoryCompensatedEvent event = InventoryCompensatedEvent.builder()
        .correlationId(compensateInventoryCommand.getCorrelationId())
        .orderId(compensateInventoryCommand.getOrderId())
        .reservationId(compensateInventoryCommand.getReservationId())
        .success(true)
        .message("Inventory released successfully")
        .build();

    rabbitTemplate.convertAndSend(
        ORDER_EXCHANGE,
        INVENTORY_COMPENSATED_ROUTING_KEY,
        event);
  }

  private boolean checkInventoryAvailability(String productId, Integer quantity) {
    return true;
  }

  private void publishInventoryReservedEvent(
      String correlationId,
      String reservationId,
      boolean success,
      String message) {
    InventoryReservedEvent inventoryReservedEvent = InventoryReservedEvent.builder()
        .correlationId(correlationId)
        .reservationId(reservationId)
        .success(success)
        .message(message)
        .build();

    rabbitTemplate.convertAndSend(
        ORDER_EXCHANGE,
        INVENTORY_RESERVED_ROUTING_KEY,
        inventoryReservedEvent);

    log.info("Published inventoryReservedEvent. CorrelationId: {}, IsSuccess: {}", correlationId,
        success);
  }
}