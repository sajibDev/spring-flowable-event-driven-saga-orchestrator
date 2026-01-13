package com.saga.orchestrator.shipping.config;

import static com.saga.orchestrator.common.constants.RabbitMQConstants.*;

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
    public Queue shippingCreateCommandQueue() {
        return new Queue(SHIPPING_CREATE_COMMAND_QUEUE, true);
    }

    @Bean
    public Queue shippingCancelCommandQueue() {
        return new Queue(SHIPPING_CANCEL_COMMAND_QUEUE, true);
    }

    // Event Queues (Saga Listeners)
    @Bean
    public Queue shipmentCreatedEventQueue() {
        return new Queue(SHIPMENT_CREATED_EVENT_QUEUE, true);
    }
    
    // Bindings
    @Bean
    public Binding shippingCreateBinding() {
        return BindingBuilder.bind(shippingCreateCommandQueue()).to(orderExchange()).with(SHIPPING_CREATE_ROUTING_KEY);
    }

    @Bean
    public Binding shippingCancelBinding() {
        return BindingBuilder.bind(shippingCancelCommandQueue()).to(orderExchange()).with(SHIPPING_CANCEL_ROUTING_KEY);
    }

    @Bean
    public Binding shipmentCreatedBinding() {
        return BindingBuilder.bind(shipmentCreatedEventQueue()).to(orderExchange()).with(SHIPMENT_CREATED_ROUTING_KEY);
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
