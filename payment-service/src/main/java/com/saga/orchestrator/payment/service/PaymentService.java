package com.saga.orchestrator.payment.service;

import static com.saga.orchestrator.common.constants.RabbitMQConstants.ORDER_EXCHANGE;
import static com.saga.orchestrator.common.constants.RabbitMQConstants.PAYMENT_PROCESSED_ROUTING_KEY;
import static com.saga.orchestrator.common.constants.RabbitMQConstants.PAYMENT_REFUNDED_ROUTING_KEY;

import java.util.Random;
import java.util.UUID;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.saga.orchestrator.common.events.PaymentProcessedEvent;
import com.saga.orchestrator.common.events.ProcessPaymentCommand;
import com.saga.orchestrator.common.events.RefundPaymentCommand;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

  private final RabbitTemplate rabbitTemplate;
  private final Random random = new Random();

  // Failure rate: 10% (0.10) - configurable via application.yml
  @Value("${payment.failure.rate:0.10}")
  private double failureRate;

  public void processPayment(ProcessPaymentCommand processPaymentCommand) {
    log.info("Started processing payment. CorrelationId: {}, OrderId: {}",
        processPaymentCommand.getCorrelationId(), processPaymentCommand.getOrderId());

    try {
      String transactionId = UUID.randomUUID().toString();

      // Simulate 10% failure rate for load testing
      boolean shouldFail = random.nextDouble() < failureRate;

      if (shouldFail) {
        log.warn("Simulated payment failure. CorrelationId: {}, OrderId: {}",
            processPaymentCommand.getCorrelationId(), processPaymentCommand.getOrderId());

        publishPaymentProcessedEvent(
            processPaymentCommand.getCorrelationId(),
            transactionId,
            false,
            "Simulated payment failure for load testing");
        return;
      }

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

      Thread.sleep(250);

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

  public void refundPayment(RefundPaymentCommand refundPaymentCommand) {
    log.info("Refunding payment. CorrelationId: {}, OrderId: {}",
        refundPaymentCommand.getCorrelationId(), refundPaymentCommand.getOrderId());

    try {
      Thread.sleep(250);
    } catch (InterruptedException e) {
      // Handle interruption
    }

    PaymentProcessedEvent paymentProcessedEvent = PaymentProcessedEvent.builder()
        .correlationId(refundPaymentCommand.getCorrelationId())
        .transactionId(refundPaymentCommand.getTransactionId())
        .success(true)
        .message("Payment refunded successfully")
        .build();

    rabbitTemplate.convertAndSend(
        ORDER_EXCHANGE,
        PAYMENT_REFUNDED_ROUTING_KEY,
        paymentProcessedEvent);
  }

  private boolean processPaymentGateway(String customerId, java.math.BigDecimal amount) {
    return true;
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