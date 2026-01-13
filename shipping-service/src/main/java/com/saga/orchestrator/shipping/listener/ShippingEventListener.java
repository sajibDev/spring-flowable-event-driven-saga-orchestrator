package com.saga.orchestrator.shipping.listener;

import com.saga.orchestrator.common.constants.RabbitMQConstants;
import com.saga.orchestrator.common.events.*;
import com.saga.orchestrator.shipping.service.ShippingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShippingEventListener {

  private final ShippingService shippingService;

  @RabbitListener(queues = RabbitMQConstants.SHIPPING_CREATE_COMMAND_QUEUE)
  public void handleShippingCreate(CreateShipmentCommand createShipmentCommand) {
    log.info("Received create shipping command. CorrelationId: {}, OrderId: {}",
        createShipmentCommand.getCorrelationId(), createShipmentCommand.getOrderId());
    shippingService.createShipment(createShipmentCommand);
  }

  @RabbitListener(queues = RabbitMQConstants.SHIPPING_CANCEL_COMMAND_QUEUE)
  public void handleShippingCancel(CancelShipmentCommand cancelShipmentCommand) {
    log.info("Received cancel shipping command. CorrelationId: {}, OrderId: {}",
        cancelShipmentCommand.getCorrelationId(), cancelShipmentCommand.getOrderId());
    shippingService.cancelShipment(cancelShipmentCommand);
  }
}