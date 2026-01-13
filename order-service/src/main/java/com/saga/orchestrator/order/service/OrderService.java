package com.saga.orchestrator.order.service;

import static com.saga.orchestrator.common.constants.RabbitMQConstants.ORDER_CANCELLED_ROUTING_KEY;
import static com.saga.orchestrator.common.constants.RabbitMQConstants.ORDER_CREATED_ROUTING_KEY;
import static com.saga.orchestrator.common.constants.RabbitMQConstants.ORDER_EXCHANGE;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import com.saga.orchestrator.common.events.CancelOrderCommand;
import com.saga.orchestrator.common.events.CreateOrderCommand;
import com.saga.orchestrator.common.events.OrderCancelledEvent;
import com.saga.orchestrator.common.events.OrderCreatedEvent;
import com.saga.orchestrator.common.events.OrderCreationFailedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

  private final RabbitTemplate rabbitTemplate;

  public void createOrder(CreateOrderCommand createOrderCommand) {
    log.info("Creating order. CorrelationId: {}", createOrderCommand.getCorrelationId());

    try {
      Thread.sleep(500);
      log.info("Created order successfully. CorrelationId: {}, OrderId: {}",
          createOrderCommand.getCorrelationId(), createOrderCommand.getCorrelationId());

      OrderCreatedEvent orderCreatedEvent = OrderCreatedEvent.builder()
          .correlationId(createOrderCommand.getCorrelationId())
          .orderId(createOrderCommand.getCorrelationId())
          .success(true)
          .message("Order created successfully")
          .build();

      rabbitTemplate.convertAndSend(
          ORDER_EXCHANGE,
          ORDER_CREATED_ROUTING_KEY,
          orderCreatedEvent);
    } catch (Exception e) {
      log.error("Error occurred while creating order. CorrelationId: {}",
          createOrderCommand.getCorrelationId(), e);

      OrderCreationFailedEvent event = OrderCreationFailedEvent.builder()
          .correlationId(createOrderCommand.getCorrelationId())
          .reason(e.getMessage())
          .build();

      rabbitTemplate.convertAndSend(
          ORDER_EXCHANGE,
          ORDER_CREATED_ROUTING_KEY,
          event);
    }
  }

  public void cancelOrder(CancelOrderCommand cancelOrderCommand) {
    log.info("Cancelling order: {}", cancelOrderCommand.getCorrelationId());

    try {
      Thread.sleep(500);
    } catch (InterruptedException e) {
      // Handle interruption
    }

    OrderCancelledEvent event = OrderCancelledEvent.builder()
        .correlationId(cancelOrderCommand.getCorrelationId())
        .orderId(cancelOrderCommand.getOrderId())
        .reason(cancelOrderCommand.getReason())
        .success(true)
        .build();

    rabbitTemplate.convertAndSend(
        ORDER_EXCHANGE,
        ORDER_CANCELLED_ROUTING_KEY,
        event);
  }
}