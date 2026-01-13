package com.saga.orchestrator.config;

import static com.saga.orchestrator.common.constants.RabbitMQConstants.INVENTORY_COMPENSATED_EVENT_QUEUE;
import static com.saga.orchestrator.common.constants.RabbitMQConstants.INVENTORY_COMPENSATED_ROUTING_KEY;
import static com.saga.orchestrator.common.constants.RabbitMQConstants.INVENTORY_RELEASE_COMMAND_QUEUE;
import static com.saga.orchestrator.common.constants.RabbitMQConstants.INVENTORY_RELEASE_ROUTING_KEY;
import static com.saga.orchestrator.common.constants.RabbitMQConstants.INVENTORY_RESERVED_EVENT_QUEUE;
import static com.saga.orchestrator.common.constants.RabbitMQConstants.INVENTORY_RESERVED_ROUTING_KEY;
import static com.saga.orchestrator.common.constants.RabbitMQConstants.INVENTORY_RESERVE_COMMAND_QUEUE;
import static com.saga.orchestrator.common.constants.RabbitMQConstants.INVENTORY_RESERVE_ROUTING_KEY;
import static com.saga.orchestrator.common.constants.RabbitMQConstants.ORDER_CANCEL_COMMAND_QUEUE;
import static com.saga.orchestrator.common.constants.RabbitMQConstants.ORDER_CANCEL_ROUTING_KEY;
import static com.saga.orchestrator.common.constants.RabbitMQConstants.ORDER_CREATED_EVENT_QUEUE;
import static com.saga.orchestrator.common.constants.RabbitMQConstants.ORDER_CREATED_ROUTING_KEY;
import static com.saga.orchestrator.common.constants.RabbitMQConstants.ORDER_CREATE_COMMAND_QUEUE;
import static com.saga.orchestrator.common.constants.RabbitMQConstants.ORDER_CREATE_ROUTING_KEY;
import static com.saga.orchestrator.common.constants.RabbitMQConstants.ORDER_EXCHANGE;
import static com.saga.orchestrator.common.constants.RabbitMQConstants.PAYMENT_PROCESSED_EVENT_QUEUE;
import static com.saga.orchestrator.common.constants.RabbitMQConstants.PAYMENT_PROCESSED_ROUTING_KEY;
import static com.saga.orchestrator.common.constants.RabbitMQConstants.PAYMENT_PROCESS_COMMAND_QUEUE;
import static com.saga.orchestrator.common.constants.RabbitMQConstants.PAYMENT_PROCESS_ROUTING_KEY;
import static com.saga.orchestrator.common.constants.RabbitMQConstants.PAYMENT_REFUNDED_EVENT_QUEUE;
import static com.saga.orchestrator.common.constants.RabbitMQConstants.PAYMENT_REFUNDED_ROUTING_KEY;
import static com.saga.orchestrator.common.constants.RabbitMQConstants.PAYMENT_REFUND_COMMAND_QUEUE;
import static com.saga.orchestrator.common.constants.RabbitMQConstants.PAYMENT_REFUND_ROUTING_KEY;
import static com.saga.orchestrator.common.constants.RabbitMQConstants.SHIPMENT_CREATED_EVENT_QUEUE;
import static com.saga.orchestrator.common.constants.RabbitMQConstants.SHIPMENT_CREATED_ROUTING_KEY;
import static com.saga.orchestrator.common.constants.RabbitMQConstants.SHIPPING_CREATE_COMMAND_QUEUE;
import static com.saga.orchestrator.common.constants.RabbitMQConstants.SHIPPING_CREATE_ROUTING_KEY;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

  @Bean
  public TopicExchange orderExchange() {
    return new TopicExchange(ORDER_EXCHANGE);
  }

  // Command Queues
  @Bean
  public Queue orderCreateCommandQueue() {
    return new Queue(ORDER_CREATE_COMMAND_QUEUE, true);
  }

  @Bean
  public Queue orderCancelCommandQueue() {
    return new Queue(ORDER_CANCEL_COMMAND_QUEUE, true);
  }

  @Bean
  public Queue inventoryReserveCommandQueue() {
    return new Queue(INVENTORY_RESERVE_COMMAND_QUEUE, true);
  }

  @Bean
  public Queue inventoryReleaseCommandQueue() {
    return new Queue(INVENTORY_RELEASE_COMMAND_QUEUE, true);
  }

  @Bean
  public Queue paymentProcessCommandQueue() {
    return new Queue(PAYMENT_PROCESS_COMMAND_QUEUE, true);
  }

  @Bean
  public Queue paymentRefundCommandQueue() {
    return new Queue(PAYMENT_REFUND_COMMAND_QUEUE, true);
  }

  @Bean
  public Queue shippingCreateCommandQueue() {
    return new Queue(SHIPPING_CREATE_COMMAND_QUEUE, true);
  }

  // Event Queues (Saga Listeners)
  @Bean
  public Queue orderCreatedEventQueue() {
    return new Queue(ORDER_CREATED_EVENT_QUEUE, true);
  }

  @Bean
  public Queue inventoryReservedEventQueue() {
    return new Queue(INVENTORY_RESERVED_EVENT_QUEUE, true);
  }

  @Bean
  public Queue inventoryCompensatedEventQueue() {
    return new Queue(INVENTORY_COMPENSATED_EVENT_QUEUE, true);
  }

  @Bean
  public Queue paymentProcessedEventQueue() {
    return new Queue(PAYMENT_PROCESSED_EVENT_QUEUE, true);
  }

  @Bean
  public Queue paymentRefundedEventQueue() {
    return new Queue(PAYMENT_REFUNDED_EVENT_QUEUE, true);
  }

  @Bean
  public Queue shipmentCreatedEventQueue() {
    return new Queue(SHIPMENT_CREATED_EVENT_QUEUE, true);
  }

  // Bindings
  @Bean
  public Binding orderCreateBinding() {
    return BindingBuilder.bind(orderCreateCommandQueue()).to(orderExchange())
        .with(ORDER_CREATE_ROUTING_KEY);
  }

  @Bean
  public Binding orderCreatedBinding() {
    return BindingBuilder.bind(orderCreatedEventQueue()).to(orderExchange())
        .with(ORDER_CREATED_ROUTING_KEY);
  }

  @Bean
  public Binding orderCancelBinding() {
    return BindingBuilder.bind(orderCancelCommandQueue()).to(orderExchange())
        .with(ORDER_CANCEL_ROUTING_KEY);
  }

  @Bean
  public Binding inventoryReserveBinding() {
    return BindingBuilder.bind(inventoryReserveCommandQueue()).to(orderExchange())
        .with(INVENTORY_RESERVE_ROUTING_KEY);
  }

  @Bean
  public Binding inventoryReleaseBinding() {
    return BindingBuilder.bind(inventoryReleaseCommandQueue()).to(orderExchange())
        .with(INVENTORY_RELEASE_ROUTING_KEY);
  }

  @Bean
  public Binding inventoryReservedBinding() {
    return BindingBuilder.bind(inventoryReservedEventQueue()).to(orderExchange())
        .with(INVENTORY_RESERVED_ROUTING_KEY);
  }

  @Bean
  public Binding inventoryCompensatedBinding() {
    return BindingBuilder.bind(inventoryCompensatedEventQueue()).to(orderExchange())
        .with(INVENTORY_COMPENSATED_ROUTING_KEY);
  }

  @Bean
  public Binding paymentProcessBinding() {
    return BindingBuilder.bind(paymentProcessCommandQueue()).to(orderExchange())
        .with(PAYMENT_PROCESS_ROUTING_KEY);
  }

  @Bean
  public Binding paymentRefundBinding() {
    return BindingBuilder.bind(paymentRefundCommandQueue()).to(orderExchange())
        .with(PAYMENT_REFUND_ROUTING_KEY);
  }

  @Bean
  public Binding paymentProcessedBinding() {
    return BindingBuilder.bind(paymentProcessedEventQueue()).to(orderExchange())
        .with(PAYMENT_PROCESSED_ROUTING_KEY);
  }

  @Bean
  public Binding paymentRefundedBinding() {
    return BindingBuilder.bind(paymentRefundedEventQueue()).to(orderExchange())
        .with(PAYMENT_REFUNDED_ROUTING_KEY);
  }

  @Bean
  public Binding shippingCreateBinding() {
    return BindingBuilder.bind(shippingCreateCommandQueue()).to(orderExchange())
        .with(SHIPPING_CREATE_ROUTING_KEY);
  }

  @Bean
  public Binding shipmentCreatedBinding() {
    return BindingBuilder.bind(shipmentCreatedEventQueue()).to(orderExchange())
        .with(SHIPMENT_CREATED_ROUTING_KEY);
  }

  @Bean
  public MessageConverter jsonMessageConverter() {
    Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
    converter.setCreateMessageIds(true);
    return converter;
  }

  @Bean
  public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
    RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
    rabbitTemplate.setMessageConverter(jsonMessageConverter());
    return rabbitTemplate;
  }

  @Bean
  public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
      ConnectionFactory connectionFactory) {
    SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
    factory.setConnectionFactory(connectionFactory);
    factory.setMessageConverter(jsonMessageConverter());
    return factory;
  }
}