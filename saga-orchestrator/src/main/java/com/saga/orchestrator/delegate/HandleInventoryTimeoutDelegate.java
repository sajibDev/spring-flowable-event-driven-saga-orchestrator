package com.saga.orchestrator.delegate;

import static com.saga.orchestrator.util.constant.AppConstant.VAR_CORRELATION_ID;

import com.saga.orchestrator.util.constant.AppConstant;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Slf4j
@Component("handleInventoryTimeoutDelegate")
public class HandleInventoryTimeoutDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
        String correlationId = (String) execution.getVariable(VAR_CORRELATION_ID);
        log.warn("Inventory reservation timeout occurred for correlationId: {}", correlationId);
        
        // Set a process variable to indicate timeout occurred
        execution.setVariable(AppConstant.VAR_INVENTORY_TIMED_OUT, true);
        
        // Log timeout details
        log.info("Setting inventory timeout flag for correlationId: {}", correlationId);
    }
}