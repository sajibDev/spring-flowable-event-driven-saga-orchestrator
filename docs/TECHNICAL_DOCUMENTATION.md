# Technical Documentation: Temporal Event-Driven Saga Orchestrator

## Table of Contents
1. [Executive Summary](#executive-summary)
2. [Architecture Overview](#architecture-overview)
3. [Core Technologies](#core-technologies)
4. [Workflow Implementation Deep Dive](#workflow-implementation-deep-dive)
5. [Event-Driven Architecture (EDA)](#event-driven-architecture-eda)
6. [Saga Orchestration Pattern](#saga-orchestration-pattern)
7. [Code Analysis](#code-analysis)
8. [Technical Complexity Assessment](#technical-complexity-assessment)
9. [Strengths](#strengths)
10. [Weaknesses & Areas for Improvement](#weaknesses--areas-for-improvement)
11. [Best Practices Evaluation](#best-practices-evaluation)
12. [Performance Considerations](#performance-considerations)
13. [Recommendations](#recommendations)

---

## Executive Summary

This project implements a **distributed order management system** using the **Saga Orchestration Pattern** with **Temporal** as the workflow engine and **RabbitMQ** as the message broker. The system coordinates transactions across four microservices: Order, Inventory, Payment, and Shipping.

### Key Characteristics
| Aspect | Implementation |
|--------|----------------|
| **Pattern** | Saga Orchestration (Centralized Coordinator) |
| **Workflow Engine** | Temporal 1.20.0 |
| **Message Broker** | RabbitMQ 3.x |
| **Framework** | Spring Boot 3.5.8 |
| **Language** | Java 21 |
| **Communication** | Asynchronous (Signal-based Event-Driven) |

---

## Architecture Overview

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              CLIENT                                          │
│                         POST /api/orders                                     │
└─────────────────────────────────┬───────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                     SAGA ORCHESTRATOR (Port 8085)                            │
│  ┌─────────────────────────────────────────────────────────────────────────┐│
│  │                    Spring Boot REST Controller                          ││
│  │                    OrderWorkflowController                              ││
│  └────────────────────────────────┬────────────────────────────────────────┘│
│                                   │                                          │
│  ┌────────────────────────────────▼────────────────────────────────────────┐│
│  │                      TEMPORAL WORKER                                    ││
│  │  ┌──────────────────────────────────────────────────────────────────┐   ││
│  │  │              OrderWorkflowImpl (Workflow Definition)             │   ││
│  │  │                                                                  │   ││
│  │  │  Step 1: ORDER → Step 2: INVENTORY → Step 3: PAYMENT → Step 4: SHIPPING
│  │  │                                                                  │   ││
│  │  │  Uses: CompletablePromise + Workflow.await() for Signal waiting  │   ││
│  │  │  Uses: Saga object for compensation registration                 │   ││
│  │  └──────────────────────────────────────────────────────────────────┘   ││
│  │                                                                          ││
│  │  ┌──────────────────────────────────────────────────────────────────┐   ││
│  │  │           OrderActivitiesImpl (Side Effects Handler)             │   ││
│  │  │  • publishOrderCreatedEvent()    • compensateOrder()             │   ││
│  │  │  • publishInventoryRequest()     • compensateInventory()         │   ││
│  │  │  • publishPaymentRequest()       • compensatePayment()           │   ││
│  │  │  • publishShippingRequest()      • compensateShipping()          │   ││
│  │  └──────────────────────────────────────────────────────────────────┘   ││
│  └──────────────────────────────────────────────────────────────────────────┘│
│                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────────┐│
│  │                    SagaEventListener (RabbitMQ Consumer)                ││
│  │  Receives events from services → Signals running workflows              ││
│  └──────────────────────────────────────────────────────────────────────────┘│
└───────────────────────────────┬─────────────────────────────────────────────┘
                                │
                    ┌───────────┴───────────┐
                    ▼                       ▼
            ┌───────────────┐       ┌───────────────┐
            │   TEMPORAL    │       │   RABBITMQ    │
            │   SERVER      │       │   BROKER      │
            │  (Port 7233)  │       │  (Port 5672)  │
            └───────────────┘       └───────┬───────┘
                                           │
            ┌──────────────┬───────────────┼───────────────┬──────────────┐
            ▼              ▼               ▼               ▼              │
    ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐  │
    │ORDER SERVICE │ │INVENTORY SVC │ │PAYMENT SVC   │ │SHIPPING SVC  │  │
    │  (8081)      │ │  (8082)      │ │  (8083)      │ │  (8084)      │  │
    │              │ │              │ │ 10% Failure  │ │ 20% Failure  │  │
    │              │ │              │ │ 5% Slow      │ │ 5% Slow      │  │
    └──────────────┘ └──────────────┘ └──────────────┘ └──────────────┘  │
            │              │               │               │              │
            └──────────────┴───────────────┴───────────────┴──────────────┘
                                    Events back to Orchestrator
```

### Project Structure

```
spring-flowable-event-driven-saga-orchestrator/
├── saga-orchestrator/          # Main orchestration service
│   ├── OrderWorkflow.java      # Temporal workflow interface
│   ├── OrderWorkflowImpl.java  # Workflow implementation
│   ├── SagaEventListener.java  # RabbitMQ → Temporal signal bridge
│   ├── TemporalConfig.java     # Temporal worker configuration
│   ├── activities/
│   │   ├── OrderActivities.java      # Activity interface
│   │   └── OrderActivitiesImpl.java  # RabbitMQ publishing
│   ├── config/
│   │   └── WorkflowOptionsConfig.java # Timeouts & retry policies
│   ├── controller/
│   │   └── OrderWorkflowController.java # REST API
│   └── model/
│       └── OrderWorkflowState.java    # Workflow state for queries
├── order-service/              # Order microservice
├── inventory-service/          # Inventory microservice
├── payment-service/            # Payment microservice (10% failure rate)
├── shipping-service/           # Shipping microservice (20% failure rate)
├── common-events/              # Shared DTOs and constants
└── docker-compose.yml          # Infrastructure setup
```

---

## Core Technologies

### 1. Temporal Workflow Engine (v1.20.0)

**Purpose**: Durable workflow orchestration with built-in state management, retries, and timeouts.

**Key Features Used**:
| Feature | Usage in Project |
|---------|------------------|
| `@WorkflowInterface` | Defines `OrderWorkflow` contract |
| `@WorkflowMethod` | `placeOrder()` - main saga entry point |
| `@SignalMethod` | 8 signal methods for async event handling |
| `@QueryMethod` | `getState()`, `getStatus()` for observability |
| `CompletablePromise` | Wait for external signals |
| `Workflow.await()` | Timeout-based waiting |
| `Saga` class | Compensation registration and execution |
| `ActivityOptions` | Retry policies and timeouts |

### 2. RabbitMQ Message Broker

**Purpose**: Asynchronous communication between orchestrator and microservices.

**Topology**:
```
ORDER_EXCHANGE (Topic Exchange)
    │
    ├── Commands (Orchestrator → Services)
    │   ├── order.create → ORDER_CREATE_COMMAND_QUEUE
    │   ├── order.cancel → ORDER_CANCEL_COMMAND_QUEUE
    │   ├── inventory.reserve → INVENTORY_RESERVE_COMMAND_QUEUE
    │   ├── inventory.release → INVENTORY_RELEASE_COMMAND_QUEUE
    │   ├── payment.process → PAYMENT_PROCESS_COMMAND_QUEUE
    │   ├── payment.refund → PAYMENT_REFUND_COMMAND_QUEUE
    │   ├── shipping.create → SHIPPING_CREATE_COMMAND_QUEUE
    │   └── shipping.cancel → SHIPPING_CANCEL_COMMAND_QUEUE
    │
    └── Events (Services → Orchestrator)
        ├── order.created → ORDER_CREATED_EVENT_QUEUE
        ├── inventory.reserved → INVENTORY_RESERVED_EVENT_QUEUE
        ├── payment.processed → PAYMENT_PROCESSED_EVENT_QUEUE
        └── shipment.created → SHIPMENT_CREATED_EVENT_QUEUE
```

### 3. Spring Boot 3.5.8

**Key Integrations**:
- `spring-boot-starter-web` - REST API
- `spring-boot-starter-amqp` - RabbitMQ integration
- `spring-boot-starter-actuator` - Health checks and metrics

---

## Workflow Implementation Deep Dive

### Signal-Based Event-Driven Pattern

The workflow uses a **hybrid pattern** combining:
1. **Activities** for outbound commands (deterministic, retryable)
2. **Signals** for inbound events (durable, async)
3. **CompletablePromise** for coordination

```java
// Workflow waits for external signal with timeout
private final CompletablePromise<Void> paymentCompleted = Workflow.newPromise();
private final CompletablePromise<Void> paymentFailed = Workflow.newPromise();

// Activity publishes command to RabbitMQ
activities.publishPaymentRequest(orderId);

// Wait for signal (with 30-minute timeout)
boolean paymentReceived = Workflow.await(
    WorkflowOptionsConfig.SIGNAL_WAIT_TIMEOUT,  // 30 minutes
    () -> paymentCompleted.isCompleted() || paymentFailed.isCompleted()
);
```

### Workflow Execution Flow

```
┌─────────┐    ┌──────────────┐    ┌─────────────┐    ┌──────────────┐    ┌──────────┐
│  START  │───►│ ORDER STEP   │───►│ INVENTORY   │───►│ PAYMENT STEP │───►│ SHIPPING │───► COMPLETED
└─────────┘    │              │    │ STEP        │    │              │    │ STEP     │
               │ Activity:    │    │ Activity:   │    │ Activity:    │    │ Activity:│
               │ publish      │    │ publish     │    │ publish      │    │ publish  │
               │ OrderCmd     │    │ InvCmd      │    │ PaymentCmd   │    │ ShipCmd  │
               │              │    │             │    │              │    │          │
               │ Wait Signal: │    │ Wait Signal:│    │ Wait Signal: │    │ Wait:    │
               │ onOrderCreated│   │ onInventory │    │ onPayment    │    │ onShipping
               │ /onOrderFailed│   │ Reserved/   │    │ Completed/   │    │ Completed/
               │              │    │ Failed      │    │ Failed       │    │ Failed   │
               └──────────────┘    └─────────────┘    └──────────────┘    └──────────┘
                     │                   │                   │                  │
                     │ Failure?          │ Failure?          │ Failure?         │ Failure?
                     ▼                   ▼                   ▼                  ▼
               ┌─────────────────────────────────────────────────────────────────────┐
               │                    SAGA COMPENSATION                                │
               │  saga.compensate() → Executes registered compensations in reverse   │
               │  compensateShipping → compensatePayment → compensateInventory →     │
               │  compensateOrder                                                    │
               └─────────────────────────────────────────────────────────────────────┘
```

### State Machine

```
                    ┌──────────────┐
                    │  PROCESSING  │
                    └──────┬───────┘
                           │
           ┌───────────────┼───────────────┐
           │               │               │
           ▼               ▼               ▼
    ┌──────────────┐ ┌──────────┐ ┌───────────────┐
    │   FAILED     │ │ COMPLETED│ │ COMPENSATING  │
    │ (Order step) │ │          │ │               │
    └──────────────┘ └──────────┘ └───────┬───────┘
                                          │
                                          ▼
                                   ┌──────────────┐
                                   │ COMPENSATED  │
                                   └──────────────┘
```

---

## Event-Driven Architecture (EDA)

### Communication Pattern

```
┌─────────────────┐              ┌─────────────────┐              ┌─────────────────┐
│   ORCHESTRATOR  │              │    RABBITMQ     │              │    SERVICE      │
│                 │              │                 │              │                 │
│  Activity       │──Command────►│  Command Queue  │──Consume────►│  Process        │
│  (publish)      │              │                 │              │  Business Logic │
│                 │              │                 │              │                 │
│  Signal Handler │◄──Event──────│  Event Queue    │◄──Publish────│  Publish Result │
│  (receive)      │              │                 │              │                 │
└─────────────────┘              └─────────────────┘              └─────────────────┘
```

### Event Types

| Category | Event/Command | Direction | Purpose |
|----------|---------------|-----------|---------|
| **Commands** | `CreateOrderCommand` | Orchestrator → Order | Create order |
| | `CancelOrderCommand` | Orchestrator → Order | Compensate order |
| | `ReserveInventoryCommand` | Orchestrator → Inventory | Reserve stock |
| | `CompensateInventoryCommand` | Orchestrator → Inventory | Release stock |
| | `ProcessPaymentCommand` | Orchestrator → Payment | Process payment |
| | `RefundPaymentCommand` | Orchestrator → Payment | Refund payment |
| | `CreateShipmentCommand` | Orchestrator → Shipping | Create shipment |
| | `CancelShipmentCommand` | Orchestrator → Shipping | Cancel shipment |
| **Events** | `OrderCreatedEvent` | Order → Orchestrator | Order result |
| | `InventoryReservedEvent` | Inventory → Orchestrator | Reservation result |
| | `PaymentProcessedEvent` | Payment → Orchestrator | Payment result |
| | `ShipmentCreatedEvent` | Shipping → Orchestrator | Shipment result |

### Correlation Strategy

All messages use `correlationId` (UUID) to link:
- Workflow instance (`workflowId = correlationId`)
- Commands sent to services
- Events received from services

```java
// In OrderActivitiesImpl
CreateOrderCommand command = CreateOrderCommand.builder()
    .correlationId(orderId)  // Same as workflowId
    .build();

// In SagaEventListener
String workflowId = event.getCorrelationId();  // Used to signal correct workflow
signalWorkflow(workflowId, OrderWorkflow::onOrderCreated, "Order completion");
```

---

## Saga Orchestration Pattern

### Implementation Approach

This project implements **Orchestration-based Saga** where:
- **Central Coordinator** (Temporal Workflow) manages the entire transaction
- **Compensation Actions** are registered and executed in reverse order on failure
- **State is durable** in Temporal's event history

### Compensation Registration & Execution

```java
// CURRENT IMPLEMENTATION (has timing issues - see Weaknesses section)
activities.publishOrderCreatedEvent(orderId);
saga.addCompensation(() -> compensationActivities.compensateOrder(orderId));  // Registered before confirmation

boolean orderCreated = Workflow.await(...);  // Wait for confirmation

if (orderFailed.isCompleted()) {
    return;  // No saga.compensate() called - compensation not needed
}

// ... more steps ...

// On failure in later steps:
saga.compensate();  // Executes: compensateShipping → compensatePayment → compensateInventory → compensateOrder
```

### Failure Scenarios

| Failure Point | Compensation Actions |
|---------------|---------------------|
| Order Step fails | None (nothing committed) |
| Inventory fails | `compensateOrder` |
| Payment fails | `compensateInventory` → `compensateOrder` |
| Shipping fails | `compensatePayment` → `compensateInventory` → `compensateOrder` |
| Timeout | Full compensation chain |

---

## Code Analysis

### Key Classes Analysis

#### 1. OrderWorkflowImpl.java (282 lines)

**Responsibilities**:
- Orchestrates 4-step saga (Order → Inventory → Payment → Shipping)
- Manages workflow state via `OrderWorkflowState`
- Handles signal reception via `CompletablePromise`
- Registers and executes compensations

**Complexity Score**: **High**
- 8 `CompletablePromise` fields for signal coordination
- Nested try-catch with multiple exception types
- State management interleaved with business logic

#### 2. OrderActivitiesImpl.java (206 lines)

**Responsibilities**:
- Publishes commands to RabbitMQ
- Executes compensation commands
- Logs workflow timestamps

**Complexity Score**: **Medium**
- Straightforward RabbitMQ operations
- Builder pattern for command creation
- Consistent error handling

#### 3. SagaEventListener.java (160 lines)

**Responsibilities**:
- Consumes events from RabbitMQ queues
- Bridges events to Temporal signals
- Handles workflow-not-found gracefully

**Complexity Score**: **Low-Medium**
- Clear routing logic (success/failure → signal)
- Reusable `signalWorkflow` method

#### 4. WorkflowOptionsConfig.java (66 lines)

**Responsibilities**:
- Centralizes timeout configurations
- Defines retry policies for activities and compensations

**Complexity Score**: **Low**
- Clean separation of concerns
- Static configuration methods

---

## Technical Complexity Assessment

### Complexity Matrix

| Dimension | Score (1-5) | Notes |
|-----------|-------------|-------|
| **Workflow Logic** | 4/5 | Signal-based coordination with timeouts |
| **Distributed Coordination** | 5/5 | 4 services + Temporal + RabbitMQ |
| **Failure Handling** | 4/5 | Saga compensation + timeout handling |
| **State Management** | 3/5 | Temporal handles persistence automatically |
| **Testing Complexity** | 4/5 | Requires full infrastructure for E2E tests |
| **Operational Complexity** | 4/5 | Multiple moving parts (Temporal, RabbitMQ, 5 services) |

**Overall Technical Complexity: HIGH**

### Lines of Code Summary

| Module | Lines | Percentage |
|--------|-------|------------|
| saga-orchestrator | ~1,200 | 40% |
| order-service | ~300 | 10% |
| inventory-service | ~350 | 12% |
| payment-service | ~400 | 13% |
| shipping-service | ~350 | 12% |
| common-events | ~400 | 13% |
| **Total** | **~3,000** | 100% |

---

## Strengths

### 1. ✅ Durable Workflow Execution
- Temporal provides automatic state persistence
- Workflows survive service restarts
- Event history enables debugging and replay

### 2. ✅ Clean Separation of Concerns
```
Workflow Logic (OrderWorkflowImpl)
    ↓
Side Effects (OrderActivitiesImpl)
    ↓
Message Broker (RabbitMQ)
    ↓
Business Services (Order, Inventory, Payment, Shipping)
```

### 3. ✅ Configurable Timeouts & Retries
```java
// Centralized configuration
public static final Duration WORKFLOW_EXECUTION_TIMEOUT = Duration.ofMinutes(30);
public static final Duration SIGNAL_WAIT_TIMEOUT = Duration.ofMinutes(30);

// Different retry policies for normal vs compensation activities
ACTIVITY_RETRY_OPTIONS (3 attempts)
COMPENSATION_RETRY_OPTIONS (5 attempts)
```

### 4. ✅ Observable Workflow State
```java
@QueryMethod
OrderWorkflowState getState();  // Query current step, status, completed steps

@QueryMethod
String getStatus();  // Quick status check
```

### 5. ✅ Configurable Failure Simulation
```yaml
# application.yml
payment:
  failure:
    rate: 0.10  # 10% failure rate
shipping:
  failure:
    rate: 0.20  # 20% failure rate
  slow:
    response:
      rate: 0.05  # 5% slow response
      delay:
        ms: 600000  # 10 minutes
```

### 6. ✅ Load Test Ready
- k6 load test scripts included
- Configurable ramp-up/ramp-down stages
- Performance thresholds defined

### 7. ✅ Shared Event Library
- `common-events` module prevents duplication
- Consistent event/command DTOs across services

### 8. ✅ Docker Compose Infrastructure
- Complete environment setup
- Elasticsearch integration for Temporal visibility
- Health checks for dependent services

---

## Weaknesses & Areas for Improvement

### 1. ❌ Compensation Registration Timing Issue

**Problem**: Compensations are registered BEFORE confirming success.

```java
// CURRENT (PROBLEMATIC)
activities.publishOrderCreatedEvent(orderId);
saga.addCompensation(() -> compensationActivities.compensateOrder(orderId));  // Too early!
boolean orderCreated = Workflow.await(...);  // Confirmation comes later
```

**Impact**: If the activity fails or times out, compensation is registered for an operation that may never have succeeded.

**Fix**:
```java
// CORRECT
activities.publishOrderCreatedEvent(orderId);
boolean orderCreated = Workflow.await(...);

if (orderCompleted.isCompleted()) {
    saga.addCompensation(() -> compensationActivities.compensateOrder(orderId));  // After success
}
```

### 2. ❌ Order Failed Signal Not Sent

**Problem**: In `SagaEventListener`, order failure doesn't signal the workflow.

```java
// CURRENT (PROBLEMATIC)
if (event.isSuccess()) {
    signalWorkflow(workflowId, OrderWorkflow::onOrderCreated, "Order completion");
} else {
    timestampLogger.logOrderEventReceived(workflowId, false);  // No signal sent!
}
```

**Impact**: Workflow waits for 30 minutes before timing out on order failure.

**Fix**:
```java
if (event.isSuccess()) {
    signalWorkflow(workflowId, OrderWorkflow::onOrderCreated, "Order completion");
} else {
    signalWorkflow(workflowId, OrderWorkflow::onOrderFailed, "Order failure");  // Signal failure
}
```

### 3. ❌ orderRequest Parameter Not Used

**Problem**: `CreateOrderRequest` is passed to workflow but ignored.

```java
public void placeOrder(String orderId, CreateOrderRequest orderRequest) {
    // orderRequest is never used!
    // Activities use hardcoded values:
    CreateOrderCommand command = CreateOrderCommand.builder()
        .customerId("CUSTOMER-001")  // Hardcoded!
        .totalAmount(new BigDecimal("100.00"))  // Hardcoded!
        .build();
}
```

**Impact**: Actual order data is lost; all orders have same customer and amount.

### 4. ❌ No Data in Signals

**Problem**: Signals pass no data, losing important context.

```java
// CURRENT
@SignalMethod
void onPaymentCompleted();  // No data

// BETTER
@SignalMethod
void onPaymentCompleted(PaymentResult result);  // Include transactionId, etc.
```

### 5. ❌ Single Consumer Per Queue (Default)

**Problem**: Without explicit concurrency configuration, one slow request blocks all messages.

**Current Shipping Service Fix** (already applied):
```java
@RabbitListener(queues = SHIPPING_CREATE_COMMAND_QUEUE, concurrency = "10-50")
```

**Other services still use default (1 consumer).**

### 6. ❌ No Dead Letter Queue (DLQ) Configuration

**Problem**: Failed messages are lost; no retry mechanism at message level.

### 7. ❌ Limited Error Context in Workflow State

**Problem**: `failureReason` is a simple string; no structured error details.

```java
state.setFailureReason("Payment failed");  // No error code, stack trace, etc.
```

### 8. ❌ No Idempotency Handling

**Problem**: Duplicate signals could cause issues (though `CompletablePromise.complete()` is idempotent for single completion).

### 9. ❌ Missing Unit Tests

**Problem**: No workflow tests visible in the codebase.

---

## Best Practices Evaluation

| Best Practice | Status | Notes |
|---------------|--------|-------|
| Deterministic workflow code | ✅ | Side effects in Activities only |
| Activity retry policies | ✅ | Configured with exponential backoff |
| Signal timeout | ✅ | 30-minute timeout with `Workflow.await()` |
| Compensation registration | ⚠️ | Timing issue (register before success) |
| Structured logging | ✅ | SLF4J with correlation IDs |
| Centralized configuration | ✅ | `WorkflowOptionsConfig` |
| Shared DTOs | ✅ | `common-events` module |
| Health checks | ✅ | Docker Compose health checks |
| Query methods | ✅ | `getState()` and `getStatus()` |
| Workflow versioning | ❌ | Not implemented |
| Dead letter queues | ❌ | Not configured |
| Circuit breaker | ❌ | Not implemented |
| Distributed tracing | ❌ | Not implemented |

---

## Performance Considerations

### Current Configuration

| Parameter | Value | Impact |
|-----------|-------|--------|
| Workflow Execution Timeout | 30 min | Max workflow duration |
| Signal Wait Timeout | 30 min | Max wait for each service response |
| Activity Start-to-Close | 30 sec | Max activity execution time |
| Activity Retry Attempts | 3 | Retry count before failure |
| Worker Concurrent Activities | 200 | Max parallel activities |
| Worker Concurrent Workflows | 100 | Max parallel workflow tasks |

### Bottlenecks Identified

1. **RabbitMQ Consumer Concurrency**
   - Default: 1 consumer per queue
   - Slow responses block entire queue
   - Fix: Configure `concurrency = "10-50"` per listener

2. **Prefetch Count**
   - Default: 250 messages prefetched
   - One slow consumer holds 250 messages
   - Fix: Set `prefetch = 1` for slow-processing queues

3. **Sequential Saga Steps**
   - Steps execute sequentially (Order → Inventory → Payment → Shipping)
   - Total latency = sum of all step latencies
   - Optimization: Parallel steps where possible (e.g., Inventory + Payment)

---

## Recommendations

### Immediate Fixes (High Priority)

1. **Fix Compensation Registration Timing**
   - Move `saga.addCompensation()` after success confirmation

2. **Fix Order Failed Signal**
   - Signal `onOrderFailed()` when order creation fails

3. **Use orderRequest Data**
   - Pass actual order data to activities

### Short-Term Improvements

4. **Add Dead Letter Queues**
   - Configure DLQ for failed message handling

5. **Implement Circuit Breaker**
   - Use Resilience4j for service call protection

6. **Add Distributed Tracing**
   - Integrate OpenTelemetry for end-to-end tracing

7. **Add Consumer Concurrency to All Services**
   - Configure `concurrency` on all `@RabbitListener` methods

### Long-Term Enhancements

8. **Implement Workflow Versioning**
   - Use `Workflow.getVersion()` for backward compatibility

9. **Add Comprehensive Tests**
   - Temporal's `TestWorkflowEnvironment` for unit tests
   - Integration tests with Testcontainers

10. **Consider Signal with Data**
    - Pass event payloads through signals for richer context

11. **Implement Metrics & Dashboards**
    - Temporal metrics + Grafana dashboards

12. **Evaluate Parallel Steps**
    - Run Inventory and Payment in parallel where business logic permits

---

## Conclusion

This project demonstrates a **well-architected** implementation of the Saga Orchestration pattern using Temporal and RabbitMQ. The codebase shows good understanding of:

- ✅ Temporal workflow concepts (Activities, Signals, Queries, Saga)
- ✅ Event-driven architecture principles
- ✅ Spring Boot best practices
- ✅ Distributed system patterns

**Key Strengths**: Durable workflows, clean separation, observable state, load test ready

**Areas Needing Attention**: Compensation timing, signal data, consumer concurrency, missing tests

The foundation is solid for a production system with the recommended fixes applied.

---

## References

- [Temporal Documentation](https://docs.temporal.io/)
- [Saga Pattern - Microservices.io](https://microservices.io/patterns/data/saga.html)
- [Spring AMQP Reference](https://docs.spring.io/spring-amqp/reference/)
- [RabbitMQ Documentation](https://www.rabbitmq.com/documentation.html)
