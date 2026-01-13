package com.saga.orchestrator.payment.listener;

import com.saga.orchestrator.common.constants.RabbitMQConstants;
import com.saga.orchestrator.common.events.*;
import com.saga.orchestrator.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

  private final PaymentService paymentService;

  @RabbitListener(queues = RabbitMQConstants.PAYMENT_PROCESS_COMMAND_QUEUE)
  public void handlePaymentProcess(ProcessPaymentCommand processPaymentCommand) {
    log.info("Received payment process request. CorrelationId: {}, OrderId: {}",
        processPaymentCommand.getCorrelationId(), processPaymentCommand.getOrderId());
    paymentService.processPayment(processPaymentCommand);
  }

  @RabbitListener(queues = RabbitMQConstants.PAYMENT_REFUND_COMMAND_QUEUE)
  public void handlePaymentRefund(RefundPaymentCommand refundPaymentCommand) {
    log.info("Received payment refund request. CorrelationId: {}, OrderId: {}",
        refundPaymentCommand.getCorrelationId(), refundPaymentCommand.getOrderId());
    paymentService.refundPayment(refundPaymentCommand);
  }
}