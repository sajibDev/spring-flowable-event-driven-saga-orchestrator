package com.saga.orchestrator.delegate;

import static com.saga.orchestrator.common.constants.RabbitMQConstants.ORDER_EXCHANGE;
import static com.saga.orchestrator.common.constants.RabbitMQConstants.SHIPPING_CREATE_ROUTING_KEY;
import static com.saga.orchestrator.util.constant.AppConstant.VAR_CORRELATION_ID;
import static com.saga.orchestrator.util.constant.AppConstant.VAR_ORDER_ID;
import static com.saga.orchestrator.util.constant.AppConstant.VAR_SHIPPING_ADDRESS;

import com.saga.orchestrator.common.events.CreateShipmentCommand;
import com.saga.orchestrator.config.RabbitMQConfig;
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
public class CreateShipmentCommandDelegate implements JavaDelegate {

  private final RabbitTemplate rabbitTemplate;

  @Override
  public void execute(DelegateExecution execution) {
    String correlationId = (String) execution.getVariable(VAR_CORRELATION_ID);
    String orderId = (String) execution.getVariable(VAR_ORDER_ID);
    String shippingAddress = (String) execution.getVariable(VAR_SHIPPING_ADDRESS);

    log.info(
        "Sending CreateShipmentCommand to the shipment service to initiate shipping for the order. CorrelationId: {}, OrderId: {}, ShippingAddress: {}",
        correlationId, orderId, shippingAddress);
    log.debug("Current ProcessInstanceId: {}, ExecutionId: {}, ActivityId: {}",
        execution.getProcessInstanceId(), execution.getId(), execution.getCurrentActivityId());

    CreateShipmentCommand createShipmentCommand = CreateShipmentCommand.builder()
        .correlationId(correlationId)
        .orderId(orderId)
        .shippingAddress(shippingAddress)
        .build();

    rabbitTemplate.convertAndSend(
        ORDER_EXCHANGE,
        SHIPPING_CREATE_ROUTING_KEY,
        createShipmentCommand
    );

    log.info(
        "Sent CreateShipmentCommand to the shipment service to initiate shipping for the order. CorrelationId: {}, OrderId: {}, ShippingAddress: {}",
        correlationId, orderId, shippingAddress);
  }
}