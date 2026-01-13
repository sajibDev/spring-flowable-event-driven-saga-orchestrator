package com.saga.orchestrator.delegate;

import static com.saga.orchestrator.common.constants.RabbitMQConstants.ORDER_EXCHANGE;
import static com.saga.orchestrator.common.constants.RabbitMQConstants.PAYMENT_PROCESS_ROUTING_KEY;
import static com.saga.orchestrator.util.constant.AppConstant.VAR_CORRELATION_ID;
import static com.saga.orchestrator.util.constant.AppConstant.VAR_ORDER_ID;
import static com.saga.orchestrator.util.constant.AppConstant.VAR_TOTAL_AMOUNT;

import com.saga.orchestrator.common.events.ProcessPaymentCommand;
import com.saga.orchestrator.config.RabbitMQConfig;
import com.saga.orchestrator.util.constant.AppConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProcessPaymentCommandDelegate implements JavaDelegate {

  private final RabbitTemplate rabbitTemplate;

  @Override
  public void execute(DelegateExecution execution) {
    String correlationId = (String) execution.getVariable(VAR_CORRELATION_ID);
    String orderId = (String) execution.getVariable(VAR_ORDER_ID);
    String totalAmountStr = (String) execution.getVariable(VAR_TOTAL_AMOUNT);

    log.info(
        "Sending ProcessPaymentCommand to the payment service to initiate payment for the order items. CorrelationId: {}, OrderId: {}, TotalAmount: {} taka.",
        correlationId, orderId, totalAmountStr);
    log.debug("Current ProcessInstanceId: {}, ExecutionId: {}, ActivityId: {}",
        execution.getProcessInstanceId(), execution.getId(), execution.getCurrentActivityId());

    ProcessPaymentCommand processPaymentCommand = ProcessPaymentCommand.builder()
        .correlationId(correlationId)
        .orderId(orderId)
        .amount(new BigDecimal(totalAmountStr))
        .build();

    rabbitTemplate.convertAndSend(
        ORDER_EXCHANGE,
        PAYMENT_PROCESS_ROUTING_KEY,
        processPaymentCommand
    );

    log.info(
        "Sent ProcessPaymentCommand to the payment service to initiate payment for the order items. CorrelationId: {}, OrderId: {}, TotalAmount: {} taka.",
        correlationId, orderId, totalAmountStr);
  }
}