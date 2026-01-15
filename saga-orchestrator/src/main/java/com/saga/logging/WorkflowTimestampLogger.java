package com.saga.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Workflow Timestamp Logger - logs all workflow events with timestamps to a separate log file.
 * This logger uses a dedicated logger name "WORKFLOW_TIMESTAMP" which is configured
 * in logback-spring.xml to write to a separate file.
 */
@Component
public class WorkflowTimestampLogger {

    private static final Logger workflowLogger = LoggerFactory.getLogger("WORKFLOW_TIMESTAMP");
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    public void logWorkflowStart(String workflowId) {
        String timestamp = getCurrentTimestamp();
        workflowLogger.info("[WORKFLOW_START] workflowId={}, timestamp={}", workflowId, timestamp);
    }

    public void logWorkflowEnd(String workflowId, String status) {
        String timestamp = getCurrentTimestamp();
        workflowLogger.info("[WORKFLOW_END] workflowId={}, status={}, timestamp={}", workflowId, status, timestamp);
    }

    public void logOrderEventReceived(String workflowId, boolean success) {
        String timestamp = getCurrentTimestamp();
        workflowLogger.info("[ORDER_EVENT_RECEIVED] workflowId={}, success={}, timestamp={}",
            workflowId, success, timestamp);
    }

    public void logPaymentEventReceived(String workflowId, boolean success) {
        String timestamp = getCurrentTimestamp();
        workflowLogger.info("[PAYMENT_EVENT_RECEIVED] workflowId={}, success={}, timestamp={}",
            workflowId, success, timestamp);
    }

    public void logInventoryEventReceived(String workflowId, boolean success) {
        String timestamp = getCurrentTimestamp();
        workflowLogger.info("[INVENTORY_EVENT_RECEIVED] workflowId={}, success={}, timestamp={}",
            workflowId, success, timestamp);
    }

    public void logShippingEventReceived(String workflowId, boolean success) {
        String timestamp = getCurrentTimestamp();
        workflowLogger.info("[SHIPPING_EVENT_RECEIVED] workflowId={}, success={}, timestamp={}",
            workflowId, success, timestamp);
    }

    public void logOrderRequestPublished(String workflowId) {
        String timestamp = getCurrentTimestamp();
        workflowLogger.info("[ORDER_REQUEST_PUBLISHED] workflowId={}, timestamp={}", workflowId, timestamp);
    }

    public void logPaymentRequestPublished(String workflowId) {
        String timestamp = getCurrentTimestamp();
        workflowLogger.info("[PAYMENT_REQUEST_PUBLISHED] workflowId={}, timestamp={}", workflowId, timestamp);
    }

    public void logInventoryRequestPublished(String workflowId) {
        String timestamp = getCurrentTimestamp();
        workflowLogger.info("[INVENTORY_REQUEST_PUBLISHED] workflowId={}, timestamp={}", workflowId, timestamp);
    }

    public void logShippingRequestPublished(String workflowId) {
        String timestamp = getCurrentTimestamp();
        workflowLogger.info("[SHIPPING_REQUEST_PUBLISHED] workflowId={}, timestamp={}", workflowId, timestamp);
    }

    public void logCompensationStarted(String workflowId, String step) {
        String timestamp = getCurrentTimestamp();
        workflowLogger.info("[COMPENSATION_STARTED] workflowId={}, step={}, timestamp={}",
            workflowId, step, timestamp);
    }

    public void logCompensationCompleted(String workflowId) {
        String timestamp = getCurrentTimestamp();
        workflowLogger.info("[COMPENSATION_COMPLETED] workflowId={}, timestamp={}", workflowId, timestamp);
    }

    private String getCurrentTimestamp() {
        return LocalDateTime.now().format(formatter);
    }
}

