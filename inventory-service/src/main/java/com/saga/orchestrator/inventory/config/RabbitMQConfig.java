package com.saga.orchestrator.inventory.config;

import static com.saga.orchestrator.common.constants.RabbitMQConstants.INVENTORY_COMPENSATED_EVENT_QUEUE;
import static com.saga.orchestrator.common.constants.RabbitMQConstants.INVENTORY_COMPENSATED_ROUTING_KEY;
import static com.saga.orchestrator.common.constants.RabbitMQConstants.INVENTORY_RELEASE_COMMAND_QUEUE;
import static com.saga.orchestrator.common.constants.RabbitMQConstants.INVENTORY_RELEASE_ROUTING_KEY;
import static com.saga.orchestrator.common.constants.RabbitMQConstants.INVENTORY_RESERVED_EVENT_QUEUE;
import static com.saga.orchestrator.common.constants.RabbitMQConstants.INVENTORY_RESERVED_ROUTING_KEY;
import static com.saga.orchestrator.common.constants.RabbitMQConstants.INVENTORY_RESERVE_COMMAND_QUEUE;
import static com.saga.orchestrator.common.constants.RabbitMQConstants.INVENTORY_RESERVE_ROUTING_KEY;
import static com.saga.orchestrator.common.constants.RabbitMQConstants.ORDER_EXCHANGE;

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
    public Queue inventoryReserveCommandQueue() {
        return new Queue(INVENTORY_RESERVE_COMMAND_QUEUE, true);
    }
    
    @Bean
    public Queue inventoryReleaseCommandQueue() {
        return new Queue(INVENTORY_RELEASE_COMMAND_QUEUE, true);
    }
    
    // Event Queues (Saga Listeners)
    @Bean
    public Queue inventoryReservedEventQueue() {
        return new Queue(INVENTORY_RESERVED_EVENT_QUEUE, true);
    }
    
    @Bean
    public Queue inventoryCompensatedEventQueue() {
        return new Queue(INVENTORY_COMPENSATED_EVENT_QUEUE, true);
    }
    
    // Bindings
    @Bean
    public Binding inventoryReserveBinding() {
        return BindingBuilder.bind(inventoryReserveCommandQueue()).to(orderExchange()).with(INVENTORY_RESERVE_ROUTING_KEY);
    }
    
    @Bean
    public Binding inventoryReleaseBinding() {
        return BindingBuilder.bind(inventoryReleaseCommandQueue()).to(orderExchange()).with(INVENTORY_RELEASE_ROUTING_KEY);
    }
    
    @Bean
    public Binding inventoryReservedBinding() {
        return BindingBuilder.bind(inventoryReservedEventQueue()).to(orderExchange()).with(INVENTORY_RESERVED_ROUTING_KEY);
    }
    
    @Bean
    public Binding inventoryCompensatedBinding() {
        return BindingBuilder.bind(inventoryCompensatedEventQueue()).to(orderExchange()).with(INVENTORY_COMPENSATED_ROUTING_KEY);
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
