package com.saga.orchestrator.delegate;

import static com.saga.orchestrator.common.constants.RabbitMQConstants.ORDER_CANCEL_ROUTING_KEY;
import static com.saga.orchestrator.common.constants.RabbitMQConstants.ORDER_EXCHANGE;
import static com.saga.orchestrator.util.constant.AppConstant.VAR_CORRELATION_ID;
import static com.saga.orchestrator.util.constant.AppConstant.VAR_FAILURE_REASON;
import static com.saga.orchestrator.util.constant.AppConstant.VAR_ORDER_ID;

import com.saga.orchestrator.common.constants.RabbitMQConstants;
import com.saga.orchestrator.common.events.CancelOrderCommand;
import com.saga.orchestrator.config.RabbitMQConfig;
import com.saga.orchestrator.tracking.SagaTimingTracker;
import com.saga.orchestrator.util.constant.AppConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CancelOrderCommandDelegate implements JavaDelegate {

  private final RabbitTemplate rabbitTemplate;
  private final SagaTimingTracker sagaTimingTracker;

  @Override
  public void execute(DelegateExecution execution) {
    String correlationId = (String) execution.getVariable(VAR_CORRELATION_ID);
    String orderId = (String) execution.getVariable(VAR_ORDER_ID);
    String failureReason = (String) execution.getVariable(VAR_FAILURE_REASON);

    log.info("Sending CancelOrderCommand for order: {}, reason: {}", orderId, failureReason);

    // Track order cancellation event
    sagaTimingTracker.recordSagaEvent(correlationId, "ORDER_CANCELLED", true);

    CancelOrderCommand command = CancelOrderCommand.builder()
        .correlationId(correlationId)
        .orderId(orderId)
        .reason(failureReason != null ? failureReason : "Order saga failed")
        .build();

    rabbitTemplate.convertAndSend(
        ORDER_EXCHANGE,
        ORDER_CANCEL_ROUTING_KEY,
        command
    );

    log.info("CancelOrderCommand sent for orderId: {}", orderId);
    
    // Mark saga as completed with failure
    sagaTimingTracker.recordSagaCompletion(correlationId, false);
  }
}