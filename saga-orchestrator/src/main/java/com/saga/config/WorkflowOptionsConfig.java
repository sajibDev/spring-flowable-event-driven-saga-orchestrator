package com.saga.config;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Centralized configuration for Temporal workflow and activity options.
 * Defines timeouts, retry policies, and other execution parameters.
 */
@Configuration
public class WorkflowOptionsConfig {
    
    // Workflow execution timeout - maximum time a workflow can run
    public static final Duration WORKFLOW_EXECUTION_TIMEOUT = Duration.ofMinutes(30);
    
    // Activity timeouts
    public static final Duration ACTIVITY_START_TO_CLOSE_TIMEOUT = Duration.ofSeconds(30);
    public static final Duration ACTIVITY_SCHEDULE_TO_CLOSE_TIMEOUT = Duration.ofMinutes(5);
    
    // Retry policy for activities
    public static final RetryOptions ACTIVITY_RETRY_OPTIONS = RetryOptions.newBuilder()
            .setInitialInterval(Duration.ofSeconds(1))
            .setMaximumInterval(Duration.ofSeconds(10))
            .setBackoffCoefficient(2.0)
            .setMaximumAttempts(3)
            .build();
    
    // Retry policy for critical activities (like compensations)
    public static final RetryOptions COMPENSATION_RETRY_OPTIONS = RetryOptions.newBuilder()
            .setInitialInterval(Duration.ofSeconds(2))
            .setMaximumInterval(Duration.ofSeconds(30))
            .setBackoffCoefficient(2.0)
            .setMaximumAttempts(5) // More attempts for compensations
            .build();
    
    // Signal wait timeout - how long to wait for external signals
    public static final Duration SIGNAL_WAIT_TIMEOUT = Duration.ofMinutes(5);
    
    /**
     * Default activity options for standard operations (publishing events).
     */
    public static ActivityOptions getDefaultActivityOptions() {
        return ActivityOptions.newBuilder()
                .setStartToCloseTimeout(ACTIVITY_START_TO_CLOSE_TIMEOUT)
                .setScheduleToCloseTimeout(ACTIVITY_SCHEDULE_TO_CLOSE_TIMEOUT)
                .setRetryOptions(ACTIVITY_RETRY_OPTIONS)
                .build();
    }
    
    /**
     * Activity options for compensation operations.
     * More retries because compensations are critical for data consistency.
     */
    public static ActivityOptions getCompensationActivityOptions() {
        return ActivityOptions.newBuilder()
                .setStartToCloseTimeout(ACTIVITY_START_TO_CLOSE_TIMEOUT)
                .setScheduleToCloseTimeout(ACTIVITY_SCHEDULE_TO_CLOSE_TIMEOUT)
                .setRetryOptions(COMPENSATION_RETRY_OPTIONS)
                .build();
    }
}

