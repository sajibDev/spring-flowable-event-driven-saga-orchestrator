package com.saga.orchestrator.inventory.listener;

import com.saga.orchestrator.common.constants.RabbitMQConstants;
import com.saga.orchestrator.common.events.*;
import com.saga.orchestrator.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryEventListener {

  private final InventoryService inventoryService;

  @RabbitListener(queues = RabbitMQConstants.INVENTORY_RESERVE_COMMAND_QUEUE)
  public void handleInventoryReserve(ReserveInventoryCommand reserveInventoryCommand) {
    log.info("Received inventory reserve command. CorrelationId: {}, OrderId: {}",
        reserveInventoryCommand.getCorrelationId(), reserveInventoryCommand.getOrderId());
    inventoryService.reserveInventory(reserveInventoryCommand);
  }

  @RabbitListener(queues = RabbitMQConstants.INVENTORY_RELEASE_COMMAND_QUEUE)
  public void handleInventoryRelease(CompensateInventoryCommand compensateInventoryCommand) {
    log.info("Received inventory release command. CorrelationId: {}, OrderId: {}",
        compensateInventoryCommand.getCorrelationId(), compensateInventoryCommand.getOrderId());
    inventoryService.releaseInventory(compensateInventoryCommand);
  }
}