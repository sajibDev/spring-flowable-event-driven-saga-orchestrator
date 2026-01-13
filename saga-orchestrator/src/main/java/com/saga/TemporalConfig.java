package com.saga;

import com.saga.activities.OrderActivitiesImpl;
import io.temporal.worker.WorkerOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

/**
 * Temporal Configuration following best practices.
 * 
 * Key improvements:
 * 1. Registers Activity implementations
 * 2. Configures worker options for better performance
 * 3. Proper dependency injection of activities
 */
@Configuration
public class TemporalConfig {
    
    @Autowired
    private OrderActivitiesImpl orderActivities;

    @Bean
    public WorkerFactory workerFactory(WorkflowClient workflowClient) {
        WorkerFactory factory = WorkerFactory.newInstance(workflowClient);
        
        // Configure worker with proper options
        WorkerOptions workerOptions = WorkerOptions.newBuilder()
                .setMaxConcurrentActivityExecutionSize(20)  // Allow up to 20 concurrent activities
                .setMaxConcurrentWorkflowTaskExecutionSize(10)  // Allow up to 10 concurrent workflow tasks
                .build();
        
        Worker worker = factory.newWorker("ORDER_TASK_QUEUE", workerOptions);

        // Register workflow implementation
        worker.registerWorkflowImplementationTypes(OrderWorkflowImpl.class);
        
        // Register activity implementations (CRITICAL for best practices)
        // Activities must be registered so the worker can execute them
        worker.registerActivitiesImplementations(orderActivities);

        factory.start();
        return factory;
    }

    @Bean
    public WorkflowServiceStubs workflowServiceStubs() {
        String temporalTarget = System.getenv().getOrDefault("TEMPORAL_TARGET", "localhost:7233");
        return WorkflowServiceStubs.newServiceStubs(
            WorkflowServiceStubsOptions.newBuilder()
                .setTarget(temporalTarget)
                .build());
    }

    @Bean
    public WorkflowClient workflowClient(WorkflowServiceStubs workflowServiceStubs) {
        return WorkflowClient.newInstance(workflowServiceStubs,
            WorkflowClientOptions.newBuilder()
                .setNamespace("default")
                .build());
    }
}

