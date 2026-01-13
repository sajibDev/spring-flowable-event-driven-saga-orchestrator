package com.saga.orchestrator.delegate;

import static com.saga.orchestrator.common.constants.RabbitMQConstants.ORDER_CREATE_ROUTING_KEY;
import static com.saga.orchestrator.common.constants.RabbitMQConstants.ORDER_EXCHANGE;
import static com.saga.orchestrator.util.constant.AppConstant.VAR_CORRELATION_ID;
import static com.saga.orchestrator.util.constant.AppConstant.VAR_CREATE_ORDER_REQUEST;

import com.saga.orchestrator.common.events.CreateOrderCommand;
import com.saga.orchestrator.config.RabbitMQConfig;
import com.saga.orchestrator.dto.CreateOrderRequest;
import java.math.BigDecimal;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreateOrderCommandDelegate implements JavaDelegate {

  private final RabbitTemplate rabbitTemplate;

  @Override
  public void execute(DelegateExecution execution) {
    String correlationId = (String) execution.getVariable(VAR_CORRELATION_ID);
    log.info("Sending CreateOrderCommand to the order service. correlationId: {}", correlationId);

    CreateOrderRequest createOrderRequest = (CreateOrderRequest) execution.getVariable(
        VAR_CREATE_ORDER_REQUEST);
    BigDecimal totalAmount = createOrderRequest.getItems().stream()
        .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    CreateOrderCommand createOrderCommand = convertToCreateOrderCommand(
        correlationId,
        createOrderRequest,
        totalAmount
    );
    log.debug("Current ProcessInstanceId: {}, ExecutionId: {}, ActivityId: {}",
        execution.getProcessInstanceId(), execution.getId(), execution.getCurrentActivityId());

    rabbitTemplate.convertAndSend(
        ORDER_EXCHANGE,
        ORDER_CREATE_ROUTING_KEY,
        createOrderCommand
    );

    log.info("Sent CreateOrderCommand to the order service. correlationId: {}, CustomerId: {}",
        correlationId, createOrderRequest.getCustomerId());
  }

  private CreateOrderCommand convertToCreateOrderCommand(
      String correlationId,
      CreateOrderRequest request,
      BigDecimal totalAmount
  ) {
    return CreateOrderCommand.builder()
        .correlationId(correlationId)
        .customerId(request.getCustomerId())
        .totalAmount(totalAmount)
        .shippingAddress(request.getShippingAddress())
        .items(request.getItems().stream()
            .map(item -> CreateOrderCommand.OrderItem.builder()
                .productId(item.getProductId())
                .productName(item.getProductName())
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .build())
            .collect(Collectors.toList()))
        .build();
  }
}