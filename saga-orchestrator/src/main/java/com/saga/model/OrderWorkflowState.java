package com.saga.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the state of an order workflow.
 * Used for querying workflow status externally.
 */
public class OrderWorkflowState {
    
    public enum OrderStatus {
        PROCESSING,
        COMPLETED,
        FAILED,
        COMPENSATING,
        COMPENSATED
    }
    
    public enum WorkflowStep {
        ORDER,
        PAYMENT,
        INVENTORY,
        SHIPPING
    }
    
    private String orderId;
    private WorkflowStep currentStep;
    private List<WorkflowStep> completedSteps;
    private OrderStatus status;
    private String failureReason;
    private Instant startTime;
    private Instant endTime;
    
    public OrderWorkflowState() {
        this.completedSteps = new ArrayList<>();
        this.status = OrderStatus.PROCESSING;
        this.startTime = Instant.now();
    }
    
    public OrderWorkflowState(String orderId) {
        this();
        this.orderId = orderId;
    }
    
    // Getters and setters
    public String getOrderId() {
        return orderId;
    }
    
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
    
    public WorkflowStep getCurrentStep() {
        return currentStep;
    }
    
    public void setCurrentStep(WorkflowStep currentStep) {
        this.currentStep = currentStep;
    }
    
    public List<WorkflowStep> getCompletedSteps() {
        return completedSteps;
    }
    
    public void setCompletedSteps(List<WorkflowStep> completedSteps) {
        this.completedSteps = completedSteps;
    }
    
    public void addCompletedStep(WorkflowStep step) {
        if (!this.completedSteps.contains(step)) {
            this.completedSteps.add(step);
        }
    }
    
    public OrderStatus getStatus() {
        return status;
    }
    
    public void setStatus(OrderStatus status) {
        this.status = status;
    }
    
    public String getFailureReason() {
        return failureReason;
    }
    
    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }
    
    public Instant getStartTime() {
        return startTime;
    }
    
    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }
    
    public Instant getEndTime() {
        return endTime;
    }
    
    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }
    
    @Override
    public String toString() {
        return String.format("OrderWorkflowState{orderId='%s', status=%s, currentStep=%s, completedSteps=%s, failureReason='%s'}",
                orderId, status, currentStep, completedSteps, failureReason);
    }
}

