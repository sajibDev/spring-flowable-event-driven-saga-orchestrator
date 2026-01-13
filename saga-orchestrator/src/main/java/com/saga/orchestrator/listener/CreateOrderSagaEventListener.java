package com.saga.orchestrator.listener;

import com.saga.orchestrator.common.constants.RabbitMQConstants;
import com.saga.orchestrator.common.events.*;
import com.saga.orchestrator.util.constant.AppConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreateOrderSagaEventListener {

  private final RuntimeService runtimeService;

  @RabbitListener(queues = RabbitMQConstants.ORDER_CREATED_EVENT_QUEUE)
  public void handleOrderCreated(OrderCreatedEvent orderCreatedEvent) {
    log.info("Received order created event from Order service. CorrelationId: {}, IsSuccess: {}",
        orderCreatedEvent.getCorrelationId(), orderCreatedEvent.isSuccess());

    if (orderCreatedEvent.isSuccess()) {
      setMessageEventToResumeOrchestration(orderCreatedEvent.getCorrelationId(),
          AppConstant.EVENT_ORDER_CREATED,
          orderCreatedEvent.getOrderId(), AppConstant.VAR_ORDER_ID);
    } else {
      setMessageEventToResumeOrchestration(orderCreatedEvent.getCorrelationId(),
          AppConstant.EVENT_ORDER_CREATION_FAILED,
          orderCreatedEvent.getMessage(), AppConstant.VAR_FAILURE_REASON);
    }
  }

  @RabbitListener(queues = RabbitMQConstants.INVENTORY_RESERVED_EVENT_QUEUE)
  public void handleInventoryReserved(InventoryReservedEvent event) {
    log.info(
        "Received inventory reserved event from Inventory service. CorrelationId: {}, IsSuccess: {}",
        event.getCorrelationId(), event.isSuccess());

    if (event.isSuccess()) {
      setMessageEventToResumeOrchestration(event.getCorrelationId(),
          AppConstant.EVENT_INVENTORY_RESERVED_SUCCESS,
          event.getReservationId(), AppConstant.VAR_RESERVATION_ID);
    } else {
      setMessageEventToResumeOrchestration(event.getCorrelationId(),
          AppConstant.EVENT_INVENTORY_RESERVED_FAILURE,
          event.getMessage(), AppConstant.VAR_FAILURE_REASON);
    }
  }

  @RabbitListener(queues = RabbitMQConstants.PAYMENT_PROCESSED_EVENT_QUEUE)
  public void handlePaymentProcessed(PaymentProcessedEvent event) {
    log.info(
        "Received payment processed event from Payment service. CorrelationId: {}, IsSuccess: {}",
        event.getCorrelationId(), event.isSuccess());

    if (event.isSuccess()) {
      setMessageEventToResumeOrchestration(event.getCorrelationId(),
          AppConstant.EVENT_PAYMENT_PROCESSED_SUCCESS,
          event.getTransactionId(), AppConstant.VAR_TRANSACTION_ID);
    } else {
      setMessageEventToResumeOrchestration(event.getCorrelationId(),
          AppConstant.EVENT_PAYMENT_PROCESSED_FAILURE,
          event.getMessage(), AppConstant.VAR_FAILURE_REASON);
    }
  }

  @RabbitListener(queues = RabbitMQConstants.SHIPMENT_CREATED_EVENT_QUEUE)
  public void handleShipmentCreated(ShipmentCreatedEvent event) {
    log.info(
        "Received shipment created event from Shipping service. CorrelationId: {}, IsSuccess: {}",
        event.getCorrelationId(), event.isSuccess());

    if (event.isSuccess()) {
      setMessageEventToResumeOrchestration(event.getCorrelationId(),
          AppConstant.EVENT_SHIPMENT_CREATED_SUCCESS,
          event.getShipmentId(), AppConstant.VAR_SHIPMENT_ID);
    }
  }

  private void setMessageEventToResumeOrchestration(
      String correlationId,
      String messageEventName,
      Object variableValue,
      String variableName
  ) {
    AtomicBoolean messageDelivered = new AtomicBoolean(false);
    int maxRetries = 3;
    int retryDelayMs = 200;

    for (int attempt = 1; attempt <= maxRetries && !messageDelivered.get(); attempt++) {
      if (attempt > 1) {
        log.debug("Retry attempt {}/{} for event {} after {}ms delay", attempt, maxRetries,
            messageEventName, retryDelayMs);
        try {
          Thread.sleep(retryDelayMs);
          retryDelayMs = Math.min(retryDelayMs * 2, 1000);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          log.warn("Retry interrupted for event {}: {}", messageEventName, e.getMessage());
          break;
        }
      }

      List<Execution> executions = findCurrentWaitingExecution(correlationId, messageEventName);

      if (!executions.isEmpty()) {
        executions.forEach(execution -> {
          try {
            runtimeService.setVariable(
                execution.getProcessInstanceId(),
                variableName,
                variableValue
            );
            runtimeService.messageEventReceived(
                messageEventName,
                execution.getId()
            );

            messageDelivered.set(true);
            log.info("Saga Orchestration processed successfully for the current event. CorrelationId: {}, EventName: {}",
                correlationId, messageEventName);
          } catch (Exception e) {
            log.error(
                "Error occurred while processing Saga Orchestration. CorrelationId: {}, EventName: {}",
                correlationId, messageEventName,
                e);
          }
        });
      }
    }

    if (!messageDelivered.get()) {
      log.error(
          "Failed to move the Saga Orchestration further, retries exhausted. CorrelationId: {}, EventName: {}",
          correlationId, messageEventName);
    }
  }

  private List<Execution> findCurrentWaitingExecution(String correlationId, String eventName) {
    log.debug("Started searching for waiting execution to resume. CorrelationId: {}, EventName: {}",
        correlationId,
        eventName);

    List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery()
        .processInstanceBusinessKey(correlationId)
        .list();

    if (processInstances.isEmpty()) {
      log.debug("No process instance found. CorrelationId: {}, EventName: {}", correlationId,
          eventName);
      return Collections.emptyList();
    }

    ProcessInstance processInstance = processInstances.getFirst();
    log.debug(
        "Found the current process instance. CorrelationId: {}, EventName: {}, ProcessInstanceId: {}, Status: {}",
        correlationId, eventName, processInstance.getId(),
        processInstance.isEnded() ? "ENDED" : "ACTIVE");

    List<Execution> allExecutions = runtimeService.createExecutionQuery()
        .processInstanceId(processInstance.getId())
        .list();

    List<Execution> executions = runtimeService.createExecutionQuery()
        .processInstanceBusinessKey(correlationId)
        .messageEventSubscriptionName(eventName)
        .list();

    if (!executions.isEmpty()) {
      log.debug(
          "Found waiting executions using message subscription query. CorrelationId: {}, EventName: {}, NumberOfExecutions: {}",
          correlationId, eventName, executions.size());
      return executions;
    }

    if (processInstance != null) {
      executions = runtimeService.createExecutionQuery()
          .processInstanceId(processInstance.getId())
          .activityId(getActivityIdForEvent(eventName))
          .list();

      if (!executions.isEmpty()) {
        log.debug(
            "Found waiting executions using process instance ID query. CorrelationId: {}, EventName: {}, NumberOfExecutions: {}",
            correlationId, eventName, executions.size());
        return executions;
      }

      executions = allExecutions.stream()
          .filter(exec -> getActivityIdForEvent(eventName).equals(exec.getActivityId()))
          .collect(Collectors.toList());

      if (!executions.isEmpty()) {
        log.debug("Found waiting executions using manual filter query. CorrelationId: {}, EventName: {}, NumberOfExecutions: {}",
            correlationId, eventName, executions.size());
        return executions;
      }
    }

    log.debug(
        "No waiting executions found for the current event after all type of queries. CorrelationId: {}, EventName: {}",
        correlationId, eventName);
    return executions;
  }

  private String getActivityIdForEvent(String eventName) {
    switch (eventName) {
      case AppConstant.EVENT_ORDER_CREATED:
        return AppConstant.ACTIVITY_ORDER_CREATED_EVENT;
      case AppConstant.EVENT_ORDER_CREATION_FAILED:
        return AppConstant.ACTIVITY_ORDER_CREATION_FAILED_EVENT;
      case AppConstant.EVENT_INVENTORY_RESERVED_SUCCESS:
        return AppConstant.ACTIVITY_INVENTORY_RESERVED_SUCCESS;
      case AppConstant.EVENT_INVENTORY_RESERVED_FAILURE:
        return AppConstant.ACTIVITY_INVENTORY_RESERVED_FAILURE;
      case AppConstant.EVENT_PAYMENT_PROCESSED_SUCCESS:
        return AppConstant.ACTIVITY_PAYMENT_PROCESSED_SUCCESS;
      case AppConstant.EVENT_PAYMENT_PROCESSED_FAILURE:
        return AppConstant.ACTIVITY_PAYMENT_PROCESSED_FAILURE;
      case AppConstant.EVENT_SHIPMENT_CREATED_SUCCESS:
        return AppConstant.ACTIVITY_SHIPMENT_CREATED_SUCCESS;
      default:
        return eventName;
    }
  }
}