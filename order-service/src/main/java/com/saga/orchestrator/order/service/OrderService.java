package com.saga.orchestrator.order.service;

import static com.saga.orchestrator.common.constants.RabbitMQConstants.ORDER_CANCELLED_ROUTING_KEY;
import static com.saga.orchestrator.common.constants.RabbitMQConstants.ORDER_CREATED_ROUTING_KEY;
import static com.saga.orchestrator.common.constants.RabbitMQConstants.ORDER_EXCHANGE;

import com.saga.orchestrator.common.constants.RabbitMQConstants;
import com.saga.orchestrator.common.events.*;
import com.saga.orchestrator.order.entity.Order;
import com.saga.orchestrator.order.entity.OrderItem;
import com.saga.orchestrator.order.entity.OrderStatus;
import com.saga.orchestrator.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

  private final OrderRepository orderRepository;
  private final RabbitTemplate rabbitTemplate;

  @Transactional
  public void createOrder(CreateOrderCommand createOrderCommand) {
    log.info("Creating order. CorrelationId: {}", createOrderCommand.getCorrelationId());

    try {
      Order order = Order.builder()
          .orderId(createOrderCommand.getCorrelationId())
          .customerId(createOrderCommand.getCustomerId())
          .status(OrderStatus.CREATED)
          .totalAmount(createOrderCommand.getTotalAmount())
          .shippingAddress(createOrderCommand.getShippingAddress())
          .build();

      var items = createOrderCommand.getItems().stream()
          .map(item -> OrderItem.builder()
              .productId(item.getProductId())
              .productName(item.getProductName())
              .quantity(item.getQuantity())
              .price(item.getPrice())
              .order(order)
              .build())
          .collect(java.util.stream.Collectors.toList());

      order.setItems(items);
      Order createdOrder = orderRepository.save(order);
      log.info("Created order successfully. CorrelationId: {}, OrderId: {}",
          createOrderCommand.getCorrelationId(), createdOrder.getOrderId());

      OrderCreatedEvent orderCreatedEvent = OrderCreatedEvent.builder()
          .correlationId(createOrderCommand.getCorrelationId())
          .orderId(createdOrder.getOrderId())
          .success(true)
          .message("Order created successfully")
          .build();

      rabbitTemplate.convertAndSend(
          ORDER_EXCHANGE,
          ORDER_CREATED_ROUTING_KEY,
          orderCreatedEvent
      );
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
          event
      );
    }
  }

  @Transactional
  public void cancelOrder(CancelOrderCommand cancelOrderCommand) {
    log.info("Cancelling order: {}", cancelOrderCommand.getCorrelationId());

    orderRepository.findById(cancelOrderCommand.getOrderId()).ifPresent(order -> {
      order.setStatus(OrderStatus.CANCELLED);
      order.setCancellationReason(cancelOrderCommand.getReason());
      orderRepository.save(order);
      log.info("Order cancelled. CorrelationId: {}", cancelOrderCommand.getCorrelationId());
    });

    OrderCancelledEvent event = OrderCancelledEvent.builder()
        .orderId(cancelOrderCommand.getOrderId())
        .reason(cancelOrderCommand.getReason())
        .build();

    rabbitTemplate.convertAndSend(
        ORDER_EXCHANGE,
        ORDER_CANCELLED_ROUTING_KEY,
        event
    );
  }
}