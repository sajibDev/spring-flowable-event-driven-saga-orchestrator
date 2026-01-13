package com.saga.orchestrator.delegate;

import static com.saga.orchestrator.common.constants.RabbitMQConstants.INVENTORY_RESERVE_ROUTING_KEY;
import static com.saga.orchestrator.common.constants.RabbitMQConstants.ORDER_EXCHANGE;
import static com.saga.orchestrator.util.constant.AppConstant.VAR_CORRELATION_ID;
import static com.saga.orchestrator.util.constant.AppConstant.VAR_CREATE_ORDER_REQUEST;
import static com.saga.orchestrator.util.constant.AppConstant.VAR_ORDER_ID;

import com.saga.orchestrator.common.events.ReserveInventoryCommand;
import com.saga.orchestrator.dto.CreateOrderRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReserveInventoryCommandDelegate implements JavaDelegate {

  private final RabbitTemplate rabbitTemplate;

  @Override
  public void execute(DelegateExecution execution) {
    String correlationId = (String) execution.getVariable(VAR_CORRELATION_ID);
    String orderId = (String) execution.getVariable(VAR_ORDER_ID);
    CreateOrderRequest createOrderRequest = (CreateOrderRequest) execution.getVariable(
        VAR_CREATE_ORDER_REQUEST);

    log.info(
        "Sending ReserveInventoryCommand to the inventory service to reserve inventory for the order items. correlationId: {}",
        correlationId);
    log.debug("Current ProcessInstanceId: {}, ExecutionId: {}, ActivityId: {}",
        execution.getProcessInstanceId(), execution.getId(), execution.getCurrentActivityId());

    try {
      ReserveInventoryCommand reserveInventoryCommand = ReserveInventoryCommand.builder()
          .correlationId(correlationId)
          .orderId(orderId)
          .customerId(createOrderRequest.getCustomerId())
          .productIdList(
              createOrderRequest.getItems().stream()
                  .map(CreateOrderRequest.OrderItemDto::getProductId).toList())
          .quantity(createOrderRequest.getItems().stream()
              .mapToInt(CreateOrderRequest.OrderItemDto::getQuantity).sum())
          .build();

      rabbitTemplate.convertAndSend(
          ORDER_EXCHANGE,
          INVENTORY_RESERVE_ROUTING_KEY,
          reserveInventoryCommand
      );

      log.info(
          "Sent ReserveInventoryCommand to the inventory service to reserve inventory for the order items. correlationId: {}",
          correlationId);
    } catch (Exception e) {
      log.error(
          "Error occurred while sending ReserveInventoryCommand to the inventory service to reserve inventory for the order items. correlationId: {}",
          correlationId, e);
      throw e;
    }

    log.info(
        "ReserveInventoryCommandDelegate.execute() completed. Delegate will now return. correlationId: {}",
        correlationId);
  }
}