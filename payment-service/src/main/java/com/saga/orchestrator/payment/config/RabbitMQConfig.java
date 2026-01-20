package com.saga.orchestrator.payment.config;

import static com.saga.orchestrator.common.constants.RabbitMQConstants.ORDER_EXCHANGE;
import static com.saga.orchestrator.common.constants.RabbitMQConstants.PAYMENT_PROCESSED_EVENT_QUEUE;
import static com.saga.orchestrator.common.constants.RabbitMQConstants.PAYMENT_PROCESSED_ROUTING_KEY;
import static com.saga.orchestrator.common.constants.RabbitMQConstants.PAYMENT_PROCESS_COMMAND_QUEUE;
import static com.saga.orchestrator.common.constants.RabbitMQConstants.PAYMENT_PROCESS_ROUTING_KEY;
import static com.saga.orchestrator.common.constants.RabbitMQConstants.PAYMENT_REFUNDED_EVENT_QUEUE;
import static com.saga.orchestrator.common.constants.RabbitMQConstants.PAYMENT_REFUND_COMMAND_QUEUE;
import static com.saga.orchestrator.common.constants.RabbitMQConstants.PAYMENT_REFUND_ROUTING_KEY;
import static com.saga.orchestrator.common.constants.RabbitMQConstants.PAYMENT_REFUNDED_ROUTING_KEY;

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
    public Queue paymentProcessCommandQueue() {
        return new Queue(PAYMENT_PROCESS_COMMAND_QUEUE, true);
    }
    
    @Bean
    public Queue paymentRefundCommandQueue() {
        return new Queue(PAYMENT_REFUND_COMMAND_QUEUE, true);
    }
    
    // Event Queues (Saga Listeners)
    @Bean
    public Queue paymentProcessedEventQueue() {
        return new Queue(PAYMENT_PROCESSED_EVENT_QUEUE, true);
    }
    
    @Bean
    public Queue paymentRefundedEventQueue() {
        return new Queue(PAYMENT_REFUNDED_EVENT_QUEUE, true);
    }
    
    // Bindings
    @Bean
    public Binding paymentProcessBinding() {
        return BindingBuilder.bind(paymentProcessCommandQueue()).to(orderExchange()).with(PAYMENT_PROCESS_ROUTING_KEY);
    }
    
    @Bean
    public Binding paymentRefundBinding() {
        return BindingBuilder.bind(paymentRefundCommandQueue()).to(orderExchange()).with(PAYMENT_REFUND_ROUTING_KEY);
    }
    
    @Bean
    public Binding paymentProcessedBinding() {
        return BindingBuilder.bind(paymentProcessedEventQueue()).to(orderExchange()).with(PAYMENT_PROCESSED_ROUTING_KEY);
    }
    
    @Bean
    public Binding paymentRefundedBinding() {
        return BindingBuilder.bind(paymentRefundedEventQueue()).to(orderExchange()).with(PAYMENT_REFUNDED_ROUTING_KEY);
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
        factory.setPrefetchCount(1);
        return factory;
    }
}
