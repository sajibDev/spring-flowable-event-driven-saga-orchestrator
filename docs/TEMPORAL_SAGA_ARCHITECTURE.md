# Temporal Saga Orchestrator - Architecture Diagram

## System Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                               CLIENT APPLICATION                                         │
│                                                                                          │
│                          POST /api/orders (JSON Payload)                                 │
└─────────────────────────────────────┬───────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                        SAGA ORCHESTRATOR (Spring Boot - Port 8085)                       │
│  ┌────────────────────────────────────────────────────────────────────────────────────┐ │
│  │                    OrderWorkflowController (REST API)                              │ │
│  │    - Receives CreateOrderRequest                                                   │ │
│  │    - Generates workflowId (UUID)                                                   │ │
│  │    - Starts Temporal Workflow via WorkflowClient                                   │ │
│  └────────────────────────────────────────┬───────────────────────────────────────────┘ │
│                                           │                                              │
│  ┌────────────────────────────────────────▼───────────────────────────────────────────┐ │
│  │                      TEMPORAL WORKER (ORDER_TASK_QUEUE)                            │ │
│  │  ┌───────────────────────────────────────────────────────────────────────────────┐ │ │
│  │  │                     OrderWorkflowImpl (Workflow Definition)                   │ │ │
│  │  │                                                                               │ │ │
│  │  │  ┌─────────────────────────────────────────────────────────────────────────┐  │ │ │
│  │  │  │ STEP 1: ORDER                                                           │  │ │ │
│  │  │  │ • Activity: publishOrderCreatedEvent(orderId)                           │  │ │ │
│  │  │  │ • Wait for Signal: onOrderCreated() / onOrderFailed()                   │  │ │ │
│  │  │  │ • Compensation: compensateOrder(orderId)                                │  │ │ │
│  │  │  └─────────────────────────────────────────────────────────────────────────┘  │ │ │
│  │  │                                    ▼                                          │ │ │
│  │  │  ┌─────────────────────────────────────────────────────────────────────────┐  │ │ │
│  │  │  │ STEP 2: INVENTORY                                                       │  │ │ │
│  │  │  │ • Activity: publishInventoryRequest(orderId)                            │  │ │ │
│  │  │  │ • Wait for Signal: onInventoryReserved() / onInventoryFailed()          │  │ │ │
│  │  │  │ • Compensation: compensateInventory(orderId)                            │  │ │ │
│  │  │  └─────────────────────────────────────────────────────────────────────────┘  │ │ │
│  │  │                                    ▼                                          │ │ │
│  │  │  ┌─────────────────────────────────────────────────────────────────────────┐  │ │ │
│  │  │  │ STEP 3: PAYMENT (10% Failure Rate)                                      │  │ │ │
│  │  │  │ • Activity: publishPaymentRequest(orderId)                              │  │ │ │
│  │  │  │ • Wait for Signal: onPaymentCompleted() / onPaymentFailed()             │  │ │ │
│  │  │  │ • Compensation: compensatePayment(orderId)                              │  │ │ │
│  │  │  └─────────────────────────────────────────────────────────────────────────┘  │ │ │
│  │  │                                    ▼                                          │ │ │
│  │  │  ┌─────────────────────────────────────────────────────────────────────────┐  │ │ │
│  │  │  │ STEP 4: SHIPPING (20% Failure Rate)                                     │  │ │ │
│  │  │  │ • Activity: publishShippingRequest(orderId)                             │  │ │ │
│  │  │  │ • Wait for Signal: onShippingCompleted() / onShippingFailed()           │  │ │ │
│  │  │  │ • Compensation: compensateShipping(orderId)                             │  │ │ │
│  │  │  └─────────────────────────────────────────────────────────────────────────┘  │ │ │
│  │  │                                    ▼                                          │ │ │
│  │  │                           ┌───────────────┐                                   │ │ │
│  │  │                           │ ORDER_COMPLETE│                                   │ │ │
│  │  │                           └───────────────┘                                   │ │ │
│  │  └───────────────────────────────────────────────────────────────────────────────┘ │ │
│  │                                                                                    │ │
│  │  ┌───────────────────────────────────────────────────────────────────────────────┐ │ │
│  │  │                 OrderActivitiesImpl (Activity Implementations)                │ │ │
│  │  │  • Publishes commands to RabbitMQ via RabbitTemplate                          │ │ │
│  │  │  • Handles compensations (refunds, cancellations, releases)                   │ │ │
│  │  └───────────────────────────────────────────────────────────────────────────────┘ │ │
│  └────────────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                          │
│  ┌────────────────────────────────────────────────────────────────────────────────────┐ │
│  │                         SagaEventListener (RabbitMQ Consumer)                      │ │
│  │  • Listens to event queues from microservices                                      │ │
│  │  • Sends Signals to running Temporal Workflows                                     │ │
│  │    - OrderCreatedEvent → onOrderCreated()                                          │ │
│  │    - InventoryReservedEvent → onInventoryReserved() / onInventoryFailed()          │ │
│  │    - PaymentProcessedEvent → onPaymentCompleted() / onPaymentFailed()              │ │
│  │    - ShipmentCreatedEvent → onShippingCompleted() / onShippingFailed()             │ │
│  └────────────────────────────────────────────────────────────────────────────────────┘ │
└───────────────────────────────┬────────────────────────────────────┬────────────────────┘
                                │                                    │
           ┌────────────────────┘                                    └────────────────────┐
           │ Commands (RabbitMQ)                                      Events (RabbitMQ)   │
           ▼                                                                              ▼
┌─────────────────────────────────────────────────────────────────────────────────────────────┐
│                            RABBITMQ MESSAGE BROKER (Port 5672)                              │
│  ┌─────────────────────────────────────────────────────────────────────────────────────┐   │
│  │                              ORDER_EXCHANGE (Topic Exchange)                         │   │
│  └─────────────────────────────────────────────────────────────────────────────────────┘   │
│         │                    │                    │                    │                    │
│  ┌──────▼──────┐     ┌───────▼───────┐    ┌──────▼──────┐     ┌───────▼───────┐           │
│  │order.create │     │inventory.     │    │payment.     │     │shipping.      │           │
│  │order.cancel │     │  reserve      │    │  process    │     │  create       │           │
│  │             │     │  release      │    │  refund     │     │  cancel       │           │
│  └──────┬──────┘     └───────┬───────┘    └──────┬──────┘     └───────┬───────┘           │
│         │                    │                    │                    │                    │
│  ┌──────▼──────┐     ┌───────▼───────┐    ┌──────▼──────┐     ┌───────▼───────┐           │
│  │order.created│     │inventory.     │    │payment.     │     │shipment.      │           │
│  │             │     │  reserved     │    │  processed  │     │  created      │           │
│  └──────┬──────┘     └───────┬───────┘    └──────┬──────┘     └───────┬───────┘           │
└─────────┼────────────────────┼────────────────────┼────────────────────┼───────────────────┘
          │                    │                    │                    │
          ▼                    ▼                    ▼                    ▼
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│  ORDER SERVICE  │  │INVENTORY SERVICE│  │ PAYMENT SERVICE │  │SHIPPING SERVICE │
│   (Port 8081)   │  │   (Port 8082)   │  │   (Port 8083)   │  │   (Port 8084)   │
│                 │  │                 │  │                 │  │                 │
│ ┌─────────────┐ │  │ ┌─────────────┐ │  │ ┌─────────────┐ │  │ ┌─────────────┐ │
│ │ OrderService│ │  │ │InventorySvc│ │  │ │PaymentSvc   │ │  │ │ShippingSvc  │ │
│ │             │ │  │ │             │ │  │ │             │ │  │ │             │ │
│ │• createOrder│ │  │ │• reserve    │ │  │ │• process    │ │  │ │• create     │ │
│ │• cancelOrder│ │  │ │  Inventory  │ │  │ │  Payment    │ │  │ │  Shipment   │ │
│ │             │ │  │ │• release    │ │  │ │• refund     │ │  │ │• cancel     │ │
│ │             │ │  │ │  Inventory  │ │  │ │  Payment    │ │  │ │  Shipment   │ │
│ └─────────────┘ │  │ └─────────────┘ │  │ └─────────────┘ │  │ └─────────────┘ │
│                 │  │                 │  │                 │  │                 │
│ Listens:        │  │ Listens:        │  │ Listens:        │  │ Listens:        │
│ • order.create  │  │ • inventory.    │  │ • payment.      │  │ • shipping.     │
│ • order.cancel  │  │   reserve       │  │   process       │  │   create        │
│                 │  │ • inventory.    │  │ • payment.      │  │ • shipping.     │
│                 │  │   release       │  │   refund        │  │   cancel        │
│                 │  │                 │  │                 │  │                 │
│ Publishes:      │  │ Publishes:      │  │ Publishes:      │  │ Publishes:      │
│ • order.created │  │ • inventory.    │  │ • payment.      │  │ • shipment.     │
│   (100% ✓)      │  │   reserved      │  │   processed     │  │   created       │
│                 │  │   (100% ✓)      │  │   (90% ✓)       │  │   (80% ✓)       │
│                 │  │                 │  │   (10% ✗)       │  │   (20% ✗)       │
└─────────────────┘  └─────────────────┘  └─────────────────┘  └─────────────────┘
```

## Temporal Server Infrastructure

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                              TEMPORAL SERVER (Port 7233)                                 │
│                                                                                          │
│  ┌─────────────────────────────────────────────────────────────────────────────────────┐│
│  │                              Temporal Frontend Service                               ││
│  │  • Receives workflow start requests                                                  ││
│  │  • Routes signals to correct workflow executions                                     ││
│  │  • Handles queries for workflow state                                                ││
│  └─────────────────────────────────────────────────────────────────────────────────────┘│
│                                          │                                               │
│  ┌─────────────────────────────────────────────────────────────────────────────────────┐│
│  │                              Temporal History Service                                ││
│  │  • Maintains workflow execution history                                              ││
│  │  • Enables workflow replay for recovery                                              ││
│  │  • Stores signals, activities, and timers                                            ││
│  └─────────────────────────────────────────────────────────────────────────────────────┘│
│                                          │                                               │
│  ┌─────────────────────────────────────────────────────────────────────────────────────┐│
│  │                              Temporal Matching Service                               ││
│  │  • Routes tasks to appropriate workers                                               ││
│  │  • Manages ORDER_TASK_QUEUE                                                          ││
│  │  • Load balances across workers                                                      ││
│  └─────────────────────────────────────────────────────────────────────────────────────┘│
│                                          │                                               │
│  ┌─────────────────────────────────────────────────────────────────────────────────────┐│
│  │                              PostgreSQL Database                                     ││
│  │  • Stores workflow state                                                             ││
│  │  • Stores activity execution history                                                 ││
│  │  • Enables durable workflow execution                                                ││
│  └─────────────────────────────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                              TEMPORAL UI (Port 8233)                                     │
│                                                                                          │
│  • Visual workflow monitoring                                                            │
│  • View running/completed workflows                                                      │
│  • Inspect workflow history and signals                                                  │
│  • Debug failed executions                                                               │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

## Workflow Execution Flow - Success Path

```
┌───────────────────────────────────────────────────────────────────────────────────────────┐
│                              SUCCESS FLOW SEQUENCE                                         │
└───────────────────────────────────────────────────────────────────────────────────────────┘

   Client              Orchestrator           Temporal           RabbitMQ          Services
     │                     │                    │                   │                 │
     │ POST /api/orders    │                    │                   │                 │
     ├────────────────────►│                    │                   │                 │
     │                     │ Start Workflow     │                   │                 │
     │                     ├───────────────────►│                   │                 │
     │                     │                    │ Store History     │                 │
     │ 202 Accepted        │                    │                   │                 │
     │◄────────────────────│                    │                   │                 │
     │                     │                    │                   │                 │
     │                     │ Execute Activity   │                   │                 │
     │                     │◄───────────────────│                   │                 │
     │                     │                    │                   │                 │
     │                     │ publishOrderCreatedEvent               │                 │
     │                     ├────────────────────────────────────────►│ CreateOrderCmd │
     │                     │                    │                   ├────────────────►│
     │                     │                    │                   │ OrderCreatedEvt │
     │                     │◄───────────────────────────────────────┤◄────────────────│
     │                     │ Signal: onOrderCreated                 │                 │
     │                     ├───────────────────►│                   │                 │
     │                     │                    │                   │                 │
     │                     │ publishInventoryRequest                │                 │
     │                     ├────────────────────────────────────────►│ ReserveInvCmd  │
     │                     │                    │                   ├────────────────►│
     │                     │                    │                   │ InvReservedEvt  │
     │                     │◄───────────────────────────────────────┤◄────────────────│
     │                     │ Signal: onInventoryReserved            │                 │
     │                     ├───────────────────►│                   │                 │
     │                     │                    │                   │                 │
     │                     │ publishPaymentRequest                  │                 │
     │                     ├────────────────────────────────────────►│ ProcessPmtCmd  │
     │                     │                    │                   ├────────────────►│
     │                     │                    │                   │ PmtProcessedEvt │
     │                     │◄───────────────────────────────────────┤◄────────────────│
     │                     │ Signal: onPaymentCompleted             │                 │
     │                     ├───────────────────►│                   │                 │
     │                     │                    │                   │                 │
     │                     │ publishShippingRequest                 │                 │
     │                     ├────────────────────────────────────────►│ CreateShipCmd  │
     │                     │                    │                   ├────────────────►│
     │                     │                    │                   │ ShipCreatedEvt  │
     │                     │◄───────────────────────────────────────┤◄────────────────│
     │                     │ Signal: onShippingCompleted            │                 │
     │                     ├───────────────────►│                   │                 │
     │                     │                    │ Workflow Complete │                 │
     │                     │◄───────────────────│                   │                 │
     │                     │                    │                   │                 │
```

## Workflow Execution Flow - Compensation (Payment Failure)

```
┌───────────────────────────────────────────────────────────────────────────────────────────┐
│                     COMPENSATION FLOW (Payment Failure - 10% of requests)                  │
└───────────────────────────────────────────────────────────────────────────────────────────┘

   Orchestrator           Temporal           RabbitMQ           Payment          Inventory
       │                    │                   │                  │                 │
       │ ... (Order & Inventory steps succeed)  │                  │                 │
       │                    │                   │                  │                 │
       │ publishPaymentRequest                  │                  │                 │
       ├────────────────────────────────────────►│ ProcessPmtCmd   │                 │
       │                    │                   ├─────────────────►│                 │
       │                    │                   │                  │ 10% Failure     │
       │                    │                   │ PmtProcessedEvt  │ (simulated)     │
       │◄───────────────────────────────────────┤◄─────────────────│                 │
       │                    │                   │ (success=false)  │                 │
       │ Signal: onPaymentFailed                │                  │                 │
       ├───────────────────►│                   │                  │                 │
       │                    │                   │                  │                 │
       │ ┌──────────────────────────────────────────────────────────────────────────┐
       │ │                    SAGA COMPENSATION STARTS                              │
       │ └──────────────────────────────────────────────────────────────────────────┘
       │                    │                   │                  │                 │
       │ compensateInventory│                   │                  │                 │
       ├────────────────────────────────────────►│CompensateInvCmd │                 │
       │                    │                   ├─────────────────────────────────────►│
       │                    │                   │                  │    Release      │
       │                    │                   │                  │    Inventory    │
       │                    │                   │                  │                 │
       │ compensateOrder    │                   │                  │                 │
       ├────────────────────────────────────────►│ CancelOrderCmd  │                 │
       │                    │                   │                  │                 │
       │                    │ Workflow Complete │                  │                 │
       │                    │ (COMPENSATED)     │                  │                 │
       │◄───────────────────│                   │                  │                 │
```

## Workflow Execution Flow - Compensation (Shipping Failure)

```
┌───────────────────────────────────────────────────────────────────────────────────────────┐
│                     COMPENSATION FLOW (Shipping Failure - 20% of requests)                 │
└───────────────────────────────────────────────────────────────────────────────────────────┘

   Orchestrator           Temporal           RabbitMQ           Shipping         Other Svcs
       │                    │                   │                   │                │
       │ ... (Order, Inventory & Payment steps succeed)            │                │
       │                    │                   │                   │                │
       │ publishShippingRequest                 │                   │                │
       ├────────────────────────────────────────►│ CreateShipCmd   │                │
       │                    │                   ├──────────────────►│                │
       │                    │                   │                   │ 20% Failure    │
       │                    │                   │ ShipCreatedEvt   │ (simulated)    │
       │◄───────────────────────────────────────┤◄──────────────────│                │
       │                    │                   │ (success=false)   │                │
       │ Signal: onShippingFailed               │                   │                │
       ├───────────────────►│                   │                   │                │
       │                    │                   │                   │                │
       │ ┌──────────────────────────────────────────────────────────────────────────┐
       │ │                    SAGA COMPENSATION STARTS                              │
       │ └──────────────────────────────────────────────────────────────────────────┘
       │                    │                   │                   │                │
       │ compensatePayment (Refund)             │                   │                │
       ├────────────────────────────────────────►│ RefundPmtCmd    │      Payment   │
       │                    │                   ├──────────────────────────────────►│
       │                    │                   │                   │                │
       │ compensateInventory│                   │                   │                │
       ├────────────────────────────────────────►│CompensateInvCmd │     Inventory  │
       │                    │                   ├──────────────────────────────────►│
       │                    │                   │                   │                │
       │ compensateOrder    │                   │                   │                │
       ├────────────────────────────────────────►│ CancelOrderCmd  │        Order   │
       │                    │                   ├──────────────────────────────────►│
       │                    │                   │                   │                │
       │                    │ Workflow Complete │                   │                │
       │                    │ (COMPENSATED)     │                   │                │
       │◄───────────────────│                   │                   │                │
```

## Technology Stack

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                              TECHNOLOGY STACK                                            │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│  Layer                  │ Technology                                                    │
├─────────────────────────┼───────────────────────────────────────────────────────────────┤
│  API Layer              │ Spring Boot REST Controllers                                  │
│  Orchestration          │ Temporal Workflow Engine 1.24.2                               │
│  Workflow Pattern       │ Saga Pattern with Activities & Signals                        │
│  Messaging              │ RabbitMQ 3 + Spring AMQP                                      │
│  Business Logic         │ Spring Services                                               │
│  Database (Temporal)    │ PostgreSQL 17                                                 │
│  Language               │ Java 21                                                       │
│  Framework              │ Spring Boot 3.x                                               │
│  Infrastructure         │ Docker Compose                                                │
│  Monitoring             │ Temporal UI (Port 8233)                                       │
└─────────────────────────┴───────────────────────────────────────────────────────────────┘
```

## Failure Rate Configuration (Load Testing)

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                              FAILURE RATE CONFIGURATION                                  │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│  Service         │ Success Rate │ Failure Rate │ Configuration Property                │
├──────────────────┼──────────────┼──────────────┼───────────────────────────────────────┤
│  Order Service   │    100%      │     0%       │ N/A (always succeeds)                 │
│  Inventory Svc   │    100%      │     0%       │ N/A (always succeeds)                 │
│  Payment Service │     90%      │    10%       │ payment.failure.rate=0.10             │
│  Shipping Svc    │     80%      │    20%       │ shipping.failure.rate=0.20            │
├──────────────────┼──────────────┼──────────────┼───────────────────────────────────────┤
│  TOTAL           │    ~72%      │   ~28%       │ Combined failure probability          │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

## Port Mapping

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                              SERVICE PORT MAPPING                                        │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│  Service              │ Internal Port │ External Port │ Description                     │
├───────────────────────┼───────────────┼───────────────┼─────────────────────────────────┤
│  Saga Orchestrator    │    8085       │    8085       │ REST API + Temporal Worker      │
│  Order Service        │    8081       │    8081       │ Order processing                │
│  Inventory Service    │    8082       │    8082       │ Inventory management            │
│  Payment Service      │    8083       │    8083       │ Payment processing              │
│  Shipping Service     │    8084       │    8084       │ Shipment handling               │
│  Temporal Server      │    7233       │    7233       │ Temporal gRPC                   │
│  Temporal UI          │    8080       │    8233       │ Workflow monitoring             │
│  RabbitMQ             │    5672       │    5672       │ AMQP messaging                  │
│  RabbitMQ Management  │    15672      │    15672      │ RabbitMQ UI                     │
│  PostgreSQL           │    5432       │    5432       │ Temporal database               │
└───────────────────────┴───────────────┴───────────────┴─────────────────────────────────┘
```

## Key Components Summary

| Component | File Location | Purpose |
|-----------|---------------|---------|
| `OrderWorkflowController` | `saga-orchestrator/src/main/java/com/saga/controller/` | REST API endpoint for creating orders |
| `OrderWorkflow` | `saga-orchestrator/src/main/java/com/saga/` | Workflow interface definition |
| `OrderWorkflowImpl` | `saga-orchestrator/src/main/java/com/saga/` | Workflow implementation with saga steps |
| `OrderActivities` | `saga-orchestrator/src/main/java/com/saga/activities/` | Activity interface for side effects |
| `OrderActivitiesImpl` | `saga-orchestrator/src/main/java/com/saga/activities/` | Activity implementation (RabbitMQ publishing) |
| `SagaEventListener` | `saga-orchestrator/src/main/java/com/saga/` | RabbitMQ consumer that signals workflows |
| `TemporalConfig` | `saga-orchestrator/src/main/java/com/saga/` | Temporal worker and client configuration |
| `OrderService` | `order-service/src/main/java/.../service/` | Order service business logic |
| `InventoryService` | `inventory-service/src/main/java/.../service/` | Inventory service business logic |
| `PaymentService` | `payment-service/src/main/java/.../service/` | Payment service with 10% failure rate |
| `ShippingService` | `shipping-service/src/main/java/.../service/` | Shipping service with 20% failure rate |
