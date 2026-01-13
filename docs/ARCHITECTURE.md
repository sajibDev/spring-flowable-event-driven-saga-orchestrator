# Architecture Diagram

## System Architecture

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                          CLIENT APPLICATION                                   │
│                                                                               │
│                      POST /api/orders (JSON)                                  │
└────────────────────────────────┬─────────────────────────────────────────────┘
                                 │
                                 ▼
┌────────────────────────────────────────────────────────────────────────────────┐
│                    SAGA ORCHESTRATOR (Port 8080)                               │
│  ┌─────────────────────────────────────────────────────────────────────┐      │
│  │              Flowable BPMN Engine (order-saga-process)              │      │
│  │                                                                      │      │
│  │  [Start] → [Create Order] → [Reserve Inventory] → [Process Payment] │      │
│  │           → [Create Shipment] → [End]                               │      │
│  │                                                                      │      │
│  │  Compensation:                                                       │      │
│  │  [Inventory Failed] → [Cancel Order] → [End]                        │      │
│  │  [Payment Failed] → [Release Inventory] → [Cancel Order] → [End]    │      │
│  └─────────────────────────────────────────────────────────────────────┘      │
│                                                                                │
│  PostgreSQL: saga_orchestrator_db (localhost:5432)                             │
└────────────────┬───────────────────────────────────────────────┬──────────────┘
                 │                                               │
                 │         RabbitMQ Message Broker               │
                 │         (localhost:5672)                      │
                 │                                               │
    ┌────────────▼────────────┐                     ┌───────────▼────────────┐
    │   Commands (Send)       │                     │   Events (Receive)     │
    ├─────────────────────────┤                     ├────────────────────────┤
    │ order.create            │                     │ inventory.reserved     │
    │ order.cancel            │                     │ payment.processed      │
    │ inventory.reserve       │                     │ shipment.created       │
    │ inventory.release       │                     │                        │
    │ payment.process         │                     │                        │
    │ payment.refund          │                     │                        │
    │ shipping.create         │                     │                        │
    └────────────┬────────────┘                     └────────────┬───────────┘
                 │                                               │
      ┌──────────┴──────────┬──────────┬──────────┬─────────────┘
      │                     │          │          │
      ▼                     ▼          ▼          ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│ Order Service│  │   Inventory  │  │   Payment    │  │   Shipping   │
│  (Port 8081) │  │    Service   │  │   Service    │  │   Service    │
│              │  │  (Port 8082) │  │ (Port 8083)  │  │ (Port 8084)  │
│              │  │              │  │              │  │              │
│ Listens:     │  │ Listens:     │  │ Listens:     │  │ Listens:     │
│ - order.     │  │ - inventory. │  │ - payment.   │  │ - shipping.  │
│   create     │  │   reserve    │  │   process    │  │   create     │
│ - order.     │  │ - inventory. │  │ - payment.   │  │              │
│   cancel     │  │   release    │  │   refund     │  │              │
│              │  │              │  │              │  │              │
│ Actions:     │  │ Actions:     │  │ Actions:     │  │ Actions:     │
│ - Create     │  │ - Reserve    │  │ - Process    │  │ - Create     │
│   order      │  │   inventory  │  │   payment    │  │   shipment   │
│ - Cancel     │  │ - Release    │  │ - Refund     │  │              │
│   order      │  │   inventory  │  │   payment    │  │              │
│              │  │              │  │              │  │              │
│ Publishes:   │  │ Publishes:   │  │ Publishes:   │  │ Publishes:   │
│ - None       │  │ - inventory. │  │ - payment.   │  │ - shipment.  │
│              │  │   reserved   │  │   processed  │  │   created    │
│              │  │   (90% ✓)    │  │   (85% ✓)    │  │   (100% ✓)   │
│              │  │   (10% ✗)    │  │   (15% ✗)    │  │              │
└──────┬───────┘  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘
       │                 │                 │                 │
       ▼                 ▼                 ▼                 ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│ PostgreSQL   │  │ PostgreSQL   │  │ PostgreSQL   │  │ PostgreSQL   │
│ order_db     │  │ inventory_db │  │ payment_db   │  │ shipping_db  │
│ (Port 5433)  │  │ (Port 5434)  │  │ (Port 5435)  │  │ (Port 5436)  │
│              │  │              │  │              │  │              │
│ Tables:      │  │ Tables:      │  │ Tables:      │  │ Tables:      │
│ - orders     │  │ - inventory_ │  │ - payment_   │  │ - shipments  │
│ - order_items│  │   reservations│ │   transactions│ │              │
└──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘
```

## Message Flow Example - Successful Order

```
1. Client → POST /api/orders → Saga Orchestrator
2. Saga Orchestrator → Starts BPMN Process → Flowable Engine
3. Flowable → Sends "Create Order" → RabbitMQ (order.create)
4. Order Service ← Receives message ← RabbitMQ
5. Order Service → Saves to order_db → Returns
6. Flowable → Sends "Reserve Inventory" → RabbitMQ (inventory.reserve)
7. Inventory Service ← Receives message ← RabbitMQ
8. Inventory Service → Saves to inventory_db → Publishes "inventory.reserved" (success)
9. Saga Orchestrator ← Receives "inventory.reserved" ← RabbitMQ
10. Flowable → Continues to next step → "Process Payment"
11. Flowable → Sends "Process Payment" → RabbitMQ (payment.process)
12. Payment Service ← Receives message ← RabbitMQ
13. Payment Service → Saves to payment_db → Publishes "payment.processed" (success)
14. Saga Orchestrator ← Receives "payment.processed" ← RabbitMQ
15. Flowable → Continues to next step → "Create Shipment"
16. Flowable → Sends "Create Shipment" → RabbitMQ (shipping.create)
17. Shipping Service ← Receives message ← RabbitMQ
18. Shipping Service → Saves to shipping_db → Publishes "shipment.created" (success)
19. Saga Orchestrator ← Receives "shipment.created" ← RabbitMQ
20. Flowable → Completes BPMN Process → End Event
21. Client ← Returns order status ← Saga Orchestrator
```

## Message Flow Example - Inventory Failure Compensation

```
1-5. [Same as successful order]
6. Flowable → Sends "Reserve Inventory" → RabbitMQ (inventory.reserve)
7. Inventory Service ← Receives message ← RabbitMQ
8. Inventory Service → Inventory check fails → Publishes "inventory.reserved" (failure)
9. Saga Orchestrator ← Receives "inventory.reserved" (failure) ← RabbitMQ
10. Flowable → Takes failure path → Event-based gateway
11. Flowable → Sends "Cancel Order" → RabbitMQ (order.cancel)
12. Order Service ← Receives message ← RabbitMQ
13. Order Service → Updates order status to CANCELLED → Returns
14. Flowable → Completes BPMN Process → End Event (Failed)
15. Client ← Returns order status (FAILED) ← Saga Orchestrator
```

## Message Flow Example - Payment Failure Compensation

```
1-9. [Same as successful order - inventory reserved successfully]
10. Flowable → Continues to "Process Payment"
11. Flowable → Sends "Process Payment" → RabbitMQ (payment.process)
12. Payment Service ← Receives message ← RabbitMQ
13. Payment Service → Payment fails → Publishes "payment.processed" (failure)
14. Saga Orchestrator ← Receives "payment.processed" (failure) ← RabbitMQ
15. Flowable → Takes failure path → Event-based gateway
16. Flowable → COMPENSATE: Sends "Release Inventory" → RabbitMQ (inventory.release)
17. Inventory Service ← Receives message ← RabbitMQ
18. Inventory Service → Releases reserved inventory → Returns
19. Flowable → Sends "Cancel Order" → RabbitMQ (order.cancel)
20. Order Service ← Receives message ← RabbitMQ
21. Order Service → Updates order status to CANCELLED → Returns
22. Flowable → Completes BPMN Process → End Event (Failed)
23. Client ← Returns order status (FAILED) ← Saga Orchestrator
```

## Technology Stack Distribution

```
┌─────────────────────────────────────────────────────────────┐
│ Layer              │ Technology                             │
├─────────────────────────────────────────────────────────────┤
│ API Layer          │ Spring Boot REST Controllers           │
│ Orchestration      │ Flowable BPMN 7.2.0                    │
│ Messaging          │ RabbitMQ 3 + Spring AMQP               │
│ Business Logic     │ Spring Services + Repositories         │
│ Persistence        │ Spring Data JPA + Hibernate            │
│ Database           │ PostgreSQL 17                          │
│ Build Tool         │ Gradle 9.2.1                           │
│ Language           │ Java 21                                │
│ Framework          │ Spring Boot 3.5.8                      │
│ Infrastructure     │ Docker Compose                         │
└─────────────────────────────────────────────────────────────┘
```
