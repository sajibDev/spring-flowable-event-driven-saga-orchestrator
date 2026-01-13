package com.saga.orchestrator.order.config;

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
    public Queue orderCreateCommandQueue() {
        return new Queue(ORDER_CREATE_COMMAND_QUEUE, true);
    }

    @Bean
    public Queue orderCancelCommandQueue() {
        return new Queue(ORDER_CANCEL_COMMAND_QUEUE, true);
    }

    // Event Queues
    @Bean
    public Queue orderCreatedEventQueue() {
        return new Queue(ORDER_CREATED_EVENT_QUEUE, true);
    }

    // Bindings
    @Bean
    public Binding orderCreateCommandBinding() {
        return BindingBuilder.bind(orderCreateCommandQueue()).to(orderExchange()).with(ORDER_CREATE_ROUTING_KEY);
    }

    @Bean
    public Binding orderCancelCommandBinding() {
        return BindingBuilder.bind(orderCancelCommandQueue()).to(orderExchange()).with(ORDER_CANCEL_ROUTING_KEY);
    }

    @Bean
    public Binding orderCreatedEventBinding() {
        return BindingBuilder.bind(orderCreatedEventQueue()).to(orderExchange()).with(ORDER_CREATED_ROUTING_KEY);
    }

    @Bean
    public Binding orderCancelledEventBinding() {
        return BindingBuilder.bind(orderCreatedEventQueue()).to(orderExchange()).with(ORDER_CANCELLED_ROUTING_KEY);
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
