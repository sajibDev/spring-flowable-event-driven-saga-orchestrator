package com.saga.controller;

import com.saga.OrderWorkflow;
import com.saga.SagaEventListener;
import com.saga.config.WorkflowOptionsConfig;
import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowExecutionAlreadyStarted;
import io.temporal.client.WorkflowOptions;
import io.temporal.common.RetryOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Random;

@RestController
public class OrderWorkflowController {

    private static final Logger logger = LoggerFactory.getLogger(OrderWorkflowController.class);
    private static final String TASK_QUEUE = "ORDER_TASK_QUEUE";

    @Autowired
    private WorkflowClient workflowClient;


    @PostMapping("/api/order-workflow/start")
    public void startOrderWorkflow() {
        System.out.println("Starting workflow for order: ");
        Random random = new Random();

        String workflowId = "workflow-" + random.nextInt(100000);
        try {
            OrderWorkflow workflow = workflowClient.newWorkflowStub(
                    OrderWorkflow.class,
                    WorkflowOptions.newBuilder()
                            .setWorkflowId(workflowId)
                            .setTaskQueue(TASK_QUEUE)
                            .setWorkflowExecutionTimeout(WorkflowOptionsConfig.WORKFLOW_EXECUTION_TIMEOUT)
                            .setRetryOptions(RetryOptions.newBuilder()
                                    .setInitialInterval(Duration.ofSeconds(1))
                                    .setMaximumInterval(Duration.ofSeconds(10))
                                    .setBackoffCoefficient(2.0)
                                    .setMaximumAttempts(3)
                                    .build())
                            .build()
            );

            WorkflowExecution execution = WorkflowClient.start(workflow::placeOrder, workflowId);
            logger.info("Started workflow for order: {} with execution: {}",
                    workflowId, execution.getWorkflowId());

        } catch (WorkflowExecutionAlreadyStarted e) {
            // This is expected if the event is replayed - handle gracefully
            logger.warn("Workflow already started for order: {} (duplicate ORDER_CREATED event)", workflowId);

        } catch (Exception e) {
            logger.error("Failed to start workflow for order: {}", workflowId, e);
            throw new RuntimeException("Failed to start workflow for order " + workflowId, e);
        }

    }
}
