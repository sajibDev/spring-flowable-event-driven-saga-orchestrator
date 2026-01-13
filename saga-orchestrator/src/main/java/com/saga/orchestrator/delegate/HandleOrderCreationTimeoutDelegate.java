package com.saga.orchestrator.delegate;

import static com.saga.orchestrator.util.constant.AppConstant.VAR_CORRELATION_ID;

import com.saga.orchestrator.util.constant.AppConstant;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Slf4j
@Component("handleOrderCreationTimeoutDelegate")
public class HandleOrderCreationTimeoutDelegate implements JavaDelegate {

  @Override
  public void execute(DelegateExecution execution) {
    String correlationId = (String) execution.getVariable(VAR_CORRELATION_ID);
    log.error(
        "Order creation timeout occurred. No response from the order service within the threshold. CorrelationId: {}",
        correlationId);

    execution.setVariable(AppConstant.VAR_ORDER_CREATION_TIMED_OUT, true);
  }
}