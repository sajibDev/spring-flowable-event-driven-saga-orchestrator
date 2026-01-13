package com.saga.orchestrator.order.listener;

import com.saga.orchestrator.common.constants.RabbitMQConstants;
import com.saga.orchestrator.common.events.*;
import com.saga.orchestrator.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

  private final OrderService orderService;

  @RabbitListener(queues = RabbitMQConstants.ORDER_CREATE_COMMAND_QUEUE)
  public void handleOrderCreate(CreateOrderCommand command) {
    log.info("Received create order command. CorrelationId: {}", command.getCorrelationId());
    orderService.createOrder(command);
  }

  @RabbitListener(queues = RabbitMQConstants.ORDER_CANCEL_COMMAND_QUEUE)
  public void handleOrderCancel(CancelOrderCommand command) {
    log.info("Received cancel order command. CorrelationId: {}", command.getCorrelationId());
    orderService.cancelOrder(command);
  }
}