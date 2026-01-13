package com.saga.orchestrator.payment.service;

import static com.saga.orchestrator.common.constants.RabbitMQConstants.ORDER_EXCHANGE;
import static com.saga.orchestrator.common.constants.RabbitMQConstants.PAYMENT_PROCESSED_ROUTING_KEY;
import static com.saga.orchestrator.common.constants.RabbitMQConstants.PAYMENT_REFUNDED_ROUTING_KEY;

import java.util.UUID;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import com.saga.orchestrator.common.events.PaymentCompensationEvent;
import com.saga.orchestrator.common.events.PaymentProcessedEvent;
import com.saga.orchestrator.common.events.ProcessPaymentCommand;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

  private final RabbitTemplate rabbitTemplate;

  public void processPayment(ProcessPaymentCommand processPaymentCommand) {
    log.info("Started processing payment. CorrelationId: {}, OrderId: {}",
        processPaymentCommand.getCorrelationId(), processPaymentCommand.getOrderId());

    try {
      String transactionId = UUID.randomUUID().toString();
      boolean paymentSuccessful = processPaymentGateway(processPaymentCommand.getCustomerId(),
          processPaymentCommand.getAmount());

      if (!paymentSuccessful) {
        log.error("Payment failed at PGW. CorrelationId: {}, OrderId: {}",
            processPaymentCommand.getCorrelationId(), processPaymentCommand.getOrderId());

        publishPaymentProcessedEvent(
            processPaymentCommand.getCorrelationId(),
            transactionId,
            false,
            "Payment gateway declined");

        return;
      }

      Thread.sleep(500);

      log.info("Payment processed successfully. CorrelationId: {}, OrderId: {}",
          processPaymentCommand.getCorrelationId(), processPaymentCommand.getOrderId());

      publishPaymentProcessedEvent(
          processPaymentCommand.getCorrelationId(),
          transactionId,
          true,
          "Payment processed successfully");
    } catch (Exception e) {
      log.error("Error processing payment for order: {}", processPaymentCommand.getCorrelationId(),
          e);
      publishPaymentProcessedEvent(processPaymentCommand.getCorrelationId(), null, false,
          "Error processing payment: " + e.getMessage());
    }
  }

  public void refundPayment(PaymentCompensationEvent paymentCompensationEvent) {
    log.info("Refunding payment. CorrelationId: {}, OrderId: {}",
        paymentCompensationEvent.getOrderId(), paymentCompensationEvent.getOrderId());

    try {
      Thread.sleep(500);
    } catch (InterruptedException e) {
      // Handle interruption
    }

    PaymentProcessedEvent paymentProcessedEvent = PaymentProcessedEvent.builder()
        .correlationId(paymentCompensationEvent.getOrderId())
        .transactionId(null)
        .success(true)
        .message("Payment refunded successfully")
        .build();

    rabbitTemplate.convertAndSend(
        ORDER_EXCHANGE,
        PAYMENT_REFUNDED_ROUTING_KEY,
        paymentProcessedEvent);
  }

  private boolean processPaymentGateway(String customerId, java.math.BigDecimal amount) {
    return Math.random() > 0.1;
  }

  private void publishPaymentProcessedEvent(
      String correlationId,
      String transactionId,
      boolean success,
      String message) {
    PaymentProcessedEvent paymentProcessedEvent = PaymentProcessedEvent.builder()
        .correlationId(correlationId)
        .transactionId(transactionId)
        .success(success)
        .message(message)
        .build();

    rabbitTemplate.convertAndSend(
        ORDER_EXCHANGE,
        PAYMENT_PROCESSED_ROUTING_KEY,
        paymentProcessedEvent);

    log.info("Successfully published payment processed event. CorrelationId: {}, Success: {}",
        correlationId,
        success);
  }
}