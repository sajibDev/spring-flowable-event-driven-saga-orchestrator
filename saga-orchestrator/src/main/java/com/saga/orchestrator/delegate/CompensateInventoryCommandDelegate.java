package com.saga.orchestrator.delegate;

import static com.saga.orchestrator.common.constants.RabbitMQConstants.INVENTORY_RELEASE_ROUTING_KEY;
import static com.saga.orchestrator.common.constants.RabbitMQConstants.ORDER_EXCHANGE;
import static com.saga.orchestrator.util.constant.AppConstant.VAR_CORRELATION_ID;
import static com.saga.orchestrator.util.constant.AppConstant.VAR_ORDER_ID;
import static com.saga.orchestrator.util.constant.AppConstant.VAR_RESERVATION_ID;

import com.saga.orchestrator.common.events.CompensateInventoryCommand;
import com.saga.orchestrator.tracking.SagaTimingTracker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CompensateInventoryCommandDelegate implements JavaDelegate {

  private final RabbitTemplate rabbitTemplate;
  private final SagaTimingTracker sagaTimingTracker;

  @Override
  public void execute(DelegateExecution execution) {
    String correlationId = (String) execution.getVariable(VAR_CORRELATION_ID);
    String orderId = (String) execution.getVariable(VAR_ORDER_ID);
    String reservationId = (String) execution.getVariable(VAR_RESERVATION_ID);

    log.info(
        "Compensate inventory flow triggered which means make payment has been failed. So starting "
            + "to send compensate inventory command to release inventory. After this step, the process will "
            + "continue to cancel the order. CorrelationId: {}", correlationId);

    // Track compensation event
    sagaTimingTracker.recordSagaEvent(correlationId, "INVENTORY_COMPENSATION", true);

    CompensateInventoryCommand command = CompensateInventoryCommand.builder()
        .correlationId(correlationId)
        .orderId(orderId)
        .reservationId(reservationId)
        .build();

    rabbitTemplate.convertAndSend(
        ORDER_EXCHANGE,
        INVENTORY_RELEASE_ROUTING_KEY,
        command
    );

    log.info(
        "CompensateInventoryCommand sent successfully. The process will now continue to cancel the order. correlationId: {}",
        correlationId);
  }
}