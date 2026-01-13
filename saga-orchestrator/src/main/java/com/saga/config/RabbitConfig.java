package com.saga.config;

import static com.saga.orchestrator.common.constants.RabbitMQConstants.*;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    // Exchange Bean - Using ORDER_EXCHANGE from RabbitMQConstants
    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE);
    }

    // Command Queues (for sending commands to services)
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

    @Bean
    public Queue shippingCancelCommandQueue() {
        return new Queue(SHIPPING_CANCEL_COMMAND_QUEUE, true);
    }

    // Event Queues (for receiving events from services - saga listeners)
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

    // Command Queue Bindings
    @Bean
    public Binding orderCreateCommandBinding() {
        return BindingBuilder.bind(orderCreateCommandQueue()).to(orderExchange()).with(ORDER_CREATE_ROUTING_KEY);
    }

    @Bean
    public Binding orderCancelCommandBinding() {
        return BindingBuilder.bind(orderCancelCommandQueue()).to(orderExchange()).with(ORDER_CANCEL_ROUTING_KEY);
    }

    @Bean
    public Binding inventoryReserveCommandBinding() {
        return BindingBuilder.bind(inventoryReserveCommandQueue()).to(orderExchange()).with(INVENTORY_RESERVE_ROUTING_KEY);
    }

    @Bean
    public Binding inventoryReleaseCommandBinding() {
        return BindingBuilder.bind(inventoryReleaseCommandQueue()).to(orderExchange()).with(INVENTORY_RELEASE_ROUTING_KEY);
    }

    @Bean
    public Binding paymentProcessCommandBinding() {
        return BindingBuilder.bind(paymentProcessCommandQueue()).to(orderExchange()).with(PAYMENT_PROCESS_ROUTING_KEY);
    }

    @Bean
    public Binding paymentRefundCommandBinding() {
        return BindingBuilder.bind(paymentRefundCommandQueue()).to(orderExchange()).with(PAYMENT_REFUND_ROUTING_KEY);
    }

    @Bean
    public Binding shippingCreateCommandBinding() {
        return BindingBuilder.bind(shippingCreateCommandQueue()).to(orderExchange()).with(SHIPPING_CREATE_ROUTING_KEY);
    }

    @Bean
    public Binding shippingCancelCommandBinding() {
        return BindingBuilder.bind(shippingCancelCommandQueue()).to(orderExchange()).with(SHIPPING_CANCEL_ROUTING_KEY);
    }

    // Event Queue Bindings (for saga to listen to service events)
    @Bean
    public Binding orderCreatedEventBinding() {
        return BindingBuilder.bind(orderCreatedEventQueue()).to(orderExchange()).with(ORDER_CREATED_ROUTING_KEY);
    }

    @Bean
    public Binding inventoryReservedEventBinding() {
        return BindingBuilder.bind(inventoryReservedEventQueue()).to(orderExchange()).with(INVENTORY_RESERVED_ROUTING_KEY);
    }

    @Bean
    public Binding inventoryCompensatedEventBinding() {
        return BindingBuilder.bind(inventoryCompensatedEventQueue()).to(orderExchange()).with(INVENTORY_COMPENSATED_ROUTING_KEY);
    }

    @Bean
    public Binding paymentProcessedEventBinding() {
        return BindingBuilder.bind(paymentProcessedEventQueue()).to(orderExchange()).with(PAYMENT_PROCESSED_ROUTING_KEY);
    }

    @Bean
    public Binding paymentRefundedEventBinding() {
        return BindingBuilder.bind(paymentRefundedEventQueue()).to(orderExchange()).with(PAYMENT_REFUNDED_ROUTING_KEY);
    }

    @Bean
    public Binding shipmentCreatedEventBinding() {
        return BindingBuilder.bind(shipmentCreatedEventQueue()).to(orderExchange()).with(SHIPMENT_CREATED_ROUTING_KEY);
    }

    // Message Converter
    @Bean
    public MessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        converter.setCreateMessageIds(true);
        return converter;
    }

    // RabbitTemplate
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        rabbitTemplate.setExchange(ORDER_EXCHANGE);
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

