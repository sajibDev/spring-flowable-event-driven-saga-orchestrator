package com.saga.orchestrator.service;

import static com.saga.orchestrator.util.constant.AppConstant.CREATE_ORDER_SAGA_PROCESS_DEFINITION_KEY;
import static com.saga.orchestrator.util.constant.AppConstant.VAR_CORRELATION_ID;
import static com.saga.orchestrator.util.constant.AppConstant.VAR_CREATE_ORDER_REQUEST;
import static com.saga.orchestrator.util.constant.AppConstant.VAR_CUSTOMER_ID;
import static com.saga.orchestrator.util.constant.AppConstant.VAR_SHIPPING_ADDRESS;
import static com.saga.orchestrator.util.constant.AppConstant.VAR_TOTAL_AMOUNT;

import com.saga.orchestrator.common.events.CreateOrderCommand;
import com.saga.orchestrator.dto.CreateOrderRequest;
import com.saga.orchestrator.dto.OrderResponse;
import com.saga.orchestrator.tracking.SagaTimingTracker;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderSagaService {

  private final RuntimeService runtimeService;
  private final SagaTimingTracker sagaTimingTracker;

  public OrderResponse initiateOrderSaga(CreateOrderRequest createOrderRequest) {
    String correlationId = UUID.randomUUID().toString();
    log.info("Initiating order saga. CorrelationId: {}, CustomerId: {}", correlationId,
        createOrderRequest.getCustomerId());

    Map<String, Object> flowableProcessVariables = new HashMap<>();
    flowableProcessVariables.put(VAR_CORRELATION_ID, correlationId);
    flowableProcessVariables.put(VAR_CREATE_ORDER_REQUEST, createOrderRequest);
    flowableProcessVariables.put(VAR_CUSTOMER_ID, createOrderRequest.getCustomerId());
    flowableProcessVariables.put(VAR_SHIPPING_ADDRESS, createOrderRequest.getShippingAddress());

    BigDecimal totalAmount = createOrderRequest.getItems().stream()
        .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    flowableProcessVariables.put(VAR_TOTAL_AMOUNT, totalAmount.toString());

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
        CREATE_ORDER_SAGA_PROCESS_DEFINITION_KEY,
        correlationId,
        flowableProcessVariables
    );

    log.info(
        "Order saga initiated successfully. CorrelationId: {}, ProcessInstanceId: {}, CustomerId: {} ,timestamp: {}",
        correlationId, processInstance.getId(), createOrderRequest.getCustomerId(), System.currentTimeMillis());

    // Record saga start time for timing tracking
    sagaTimingTracker.recordSagaStart(correlationId, createOrderRequest.getCustomerId());

    return OrderResponse.builder()
        .correlationId(correlationId)
        .status("INITIATED")
        .message("Order saga initiated successfully")
        .build();
  }

  public String getOrderStatus(String correlationId) {
    ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
        .processInstanceBusinessKey(correlationId)
        .singleResult();

    if (processInstance == null) {
      return "UNKNOWN";
    }

    return processInstance.isEnded() ? "COMPLETED" : "IN_PROGRESS";
  }
}