# Flowable Saga Orchestrator - Event-Driven Order Management System

A comprehensive multi-module event-driven microservices application that implements the Saga pattern using Flowable BPMN, Spring Boot, RabbitMQ, and PostgreSQL.

## 🏗️ Architecture Overview

This project demonstrates a real-world order processing saga with the following workflow:
1. **Create Order** → 2. **Reserve Inventory** → 3. **Process Payment** → 4. **Create Shipment**

If any step fails, the system automatically compensates (rolls back) all previous successful operations.

### Modules

- **saga-orchestrator**: Orchestrates the entire saga workflow using Flowable BPMN
- **order-service**: Manages order creation and cancellation
- **inventory-service**: Handles inventory reservations and releases
- **payment-service**: Processes payments and refunds
- **shipping-service**: Creates and manages shipments
- **common-events**: Shared event models and DTOs

## 🛠️ Technology Stack

- **Java 21**
- **Spring Boot 3.5.8**
- **Gradle 9.2.1**
- **Flowable 7.2.0** (BPMN workflow engine)
- **PostgreSQL 17** (separate database per service)
- **RabbitMQ** (event-driven messaging)
- **Lombok** (boilerplate reduction)

## 📋 Prerequisites

- Java 21 or higher
- Docker and Docker Compose
- Gradle 9.2.1 (or use Gradle wrapper)

## 🚀 Getting Started

### 1. Start Infrastructure Services

Start PostgreSQL and RabbitMQ using Docker Compose:

```bash
docker-compose up -d
```

This will start:
- 5 PostgreSQL databases (one per service) on ports 5432-5436
- RabbitMQ on port 5672 (AMQP) and 15672 (Management UI)

### 2. Build the Project

```bash
./gradlew clean build
```

### 3. Start the Services

Open 5 separate terminal windows and start each service:

**Terminal 1 - Saga Orchestrator:**
```bash
./gradlew :saga-orchestrator:bootRun
```

**Terminal 2 - Order Service:**
```bash
./gradlew :order-service:bootRun
```

**Terminal 3 - Inventory Service:**
```bash
./gradlew :inventory-service:bootRun
```

**Terminal 4 - Payment Service:**
```bash
./gradlew :payment-service:bootRun
```

**Terminal 5 - Shipping Service:**
```bash
./gradlew :shipping-service:bootRun
```

### Service Ports

| Service | Port |
|---------|------|
| Saga Orchestrator | 8080 |
| Order Service | 8081 |
| Inventory Service | 8082 |
| Payment Service | 8083 |
| Shipping Service | 8084 |

## 📡 Testing the Application

### Create a Successful Order

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "BITS-USER-001",
    "shippingAddress": "Gulshan-1, Dhaka, Bangladesh",
    "items": [
      {
        "productId": "PROD-001",
        "productName": "CRM",
        "quantity": 1,
        "price": 1200.00
      },
      {
        "productId": "PROD-002",
        "productName": "Ticketing System",
        "quantity": 2,
        "price": 5000.00
      }
    ]
  }'
```

### Check Order Status

```bash
curl http://localhost:8080/api/orders/{orderId}/status
```

Replace `{orderId}` with the order ID returned from the create order request.

## 🔄 BPMN Workflow Explanation

The Order Saga process (`order-saga-process.bpmn20.xml`) implements the following flow:

### Happy Path (Success):
1. **Start Event** → Triggers the saga
2. **Create Order** → Sends command to Order Service
3. **Reserve Inventory** → Sends command to Inventory Service
4. **Wait for Inventory Response** → Event-based gateway waits for async response
5. **Inventory Reserved Success** → Intermediate catch event
6. **Process Payment** → Sends command to Payment Service
7. **Wait for Payment Response** → Event-based gateway waits for async response
8. **Payment Processed Success** → Intermediate catch event
9. **Create Shipment** → Sends command to Shipping Service
10. **Wait for Shipment Response** → Event-based gateway waits for async response
11. **Shipment Created Success** → Intermediate catch event
12. **End Event** → Order completed successfully

### Compensation Path (Failure):

#### If Inventory Reservation Fails:
- **Inventory Reserved Failure** → Intermediate catch event
- **Cancel Order** → Sends cancellation to Order Service
- **End Event** → Order failed

#### If Payment Processing Fails:
- **Payment Processed Failure** → Intermediate catch event
- **Compensate Inventory** → Releases reserved inventory
- **Cancel Order** → Sends cancellation to Order Service
- **End Event** → Order failed

## 🎯 Key Features

### 1. Event-Driven Architecture
- All communication between services happens through RabbitMQ
- No direct REST API calls between services during saga execution
- Asynchronous message processing

### 2. BPMN 2.0 Standard Workflow
- Industry-standard BPMN notation
- Visual workflow representation
- Intermediate catch events for async responses
- Event-based gateways for handling success/failure paths
- **Timer boundary events for timeout handling**

### 3. Saga Pattern Implementation
- Automatic compensation on failure
- Distributed transaction management
- Eventual consistency
- **Fault tolerance with timeout handling**

### 4. Database Per Service
- Each microservice has its own PostgreSQL database
- Data isolation and autonomy
- Independent scaling

### 5. Failure Simulation
- Inventory service has 10% random failure rate
- Payment service has 15% random failure rate
- Demonstrates compensation logic in action

## 🔍 Monitoring

### Flowable REST API
Access the Flowable REST API to monitor your saga orchestration at: **http://localhost:8080/flowable-rest**

**What you can monitor via REST API:**
- **Process Instances**: Query all running and completed saga instances
- **Process Definitions**: See your deployed BPMN processes
- **Variables**: Inspect process variables (orderId, customerId, etc.)
- **History**: Analyze completed process instances
- **Event Subscriptions**: View intermediate catch events

**Example API calls:**

```bash
# Get all process instances
curl http://localhost:8080/flowable-rest/process-api/runtime/process-instances

# Get specific instance by business key (orderId)
curl "http://localhost:8080/flowable-rest/process-api/runtime/process-instances?businessKey=YOUR_ORDER_ID"

# Get process variables for an instance
curl http://localhost:8080/flowable-rest/process-api/runtime/process-instances/INSTANCE_ID/variables

# Get historical process instances
curl http://localhost:8080/flowable-rest/process-api/history/historic-process-instances

# Get event subscriptions (intermediate catch events)
curl http://localhost:8080/flowable-rest/process-api/runtime/event-subscriptions
```

**For detailed monitoring**, consider:
1. Using Postman or similar REST client for API calls
2. **Deploying Flowable Admin UI** - Run `docker-compose up -d` to start Flowable Admin UI at http://localhost:9988/flowable-admin (see FLOWABLE_ADMIN_UI_SETUP.md)
3. Checking service logs for detailed event flow

### RabbitMQ Management UI
Access the RabbitMQ management console at: http://localhost:15672
- Username: `guest`
- Password: `guest`

Here you can monitor:
- Queue depths
- Message rates
- Exchanges and bindings
- Consumer connections

### Application Logs
Each service logs detailed information about:
- Received events
- Processing steps
- Published events
- Success/failure outcomes

## 📊 Message Flow

### Queues and Routing Keys

| Queue | Routing Key | Purpose |
|-------|-------------|---------|
| order.create.queue | order.create | Create new order |
| order.cancel.queue | order.cancel | Cancel order |
| inventory.reserve.queue | inventory.reserve | Reserve inventory |
| inventory.release.queue | inventory.release | Release inventory (compensation) |
| inventory.reserved.queue | inventory.reserved | Inventory reservation result |
| payment.process.queue | payment.process | Process payment |
| payment.refund.queue | payment.refund | Refund payment (compensation) |
| payment.processed.queue | payment.processed | Payment processing result |
| shipping.create.queue | shipping.create | Create shipment |
| shipment.created.queue | shipment.created | Shipment creation result |

## 🧪 Testing Scenarios

### Scenario 1: Successful Order Flow
Create an order and observe all services processing successfully. The order will go through all stages and complete.

### Scenario 2: Inventory Failure
Due to the 10% failure rate in inventory service, some orders will fail at inventory reservation. Observe the compensation flow canceling the order.

### Scenario 3: Payment Failure
Due to the 15% failure rate in payment service, some orders will fail at payment processing. Observe the compensation flow releasing inventory and canceling the order.

## 🛑 Stopping the Application

1. Stop all Spring Boot applications (Ctrl+C in each terminal)
2. Stop Docker containers:
```bash
docker-compose down
```

To remove all data volumes:
```bash
docker-compose down -v
```

## 🔧 Troubleshooting

If you encounter any issues, refer to the comprehensive troubleshooting guide:
- **TROUBLESHOOTING.md** - Common issues and solutions
- **PRODUCTION_READY_FIX.md** - Production-ready refactored implementation
- **EXECUTION_QUERY_FIX.md** - Fix for execution query issues
- **MESSAGE_SUBSCRIPTION_FIX.md** - Fix for message subscription query issues
- **DEEP_DIAGNOSTIC_FIX.md** - Deep diagnostic analysis for stuck processes
- **TIMING_ISSUE_RETRY_FIX.md** - Fix for timing issues with inventory events
- **BUSINESS_KEY_FIX.md** - Fix for process not found by business key
- **SAGA_WORKFLOW_FIX.md** - Fix for workflow not progressing after inventory success
- **TEST_SAGA_FLOW.md** - Quick test guide to verify saga flow
- **INVENTORY_RABBITMQ_FIX.md** - Fix for inventory service RabbitMQ configuration
- **BPMN_BOUNDARY_EVENT_FIX.md** - Fix for BPMN boundary event attachment
- **BPMN_POSITIONING_FIX.md** - Fix for BPMN boundary event positioning

**Recent Fixes Applied:**
- ✅ **BPMN Boundary Event Positioning Fix** - Fixed timer boundary event XML positioning
- ✅ **BPMN Boundary Event Attachment Fix** - Fixed timer boundary event positioning
- ✅ **Inventory Service RabbitMQ Fix** - Fixed missing queue definitions and bindings
- ✅ **Saga Event Handling Fix** - Fixed BPMN element ID vs message name mismatch
- ✅ **Production-Ready Refactor** - Clean, reusable event handler implementation
- ✅ **Execution Query Fix** - Added multi-method query approach
- ✅ **Message Subscription Query Fix** - Added fallback to activity-based query
- ✅ **Deep Diagnostic Analysis** - Enhanced diagnostics to identify stuck processes
- ✅ **Timing Issue Retry Fix** - Added retry mechanism for inventory events arriving too fast
- ✅ **Business Key Query Issue** - Fixed hardcoded orderId and wrong event subscription query
- ✅ **Saga Workflow Progression** - Fixed workflow not moving to payment step after inventory success
- ✅ **RabbitMQ Message Conversion** - Fixed JSON deserialization errors with proper Jackson configuration
- ✅ **Gradle Build Dependencies** - Fixed Flowable UI dependency issues

## 📚 **Documentation**

- **README.md** - This file
- **PROJECT_SUMMARY.md** - High-level project overview
- **ARCHITECTURE.md** - Detailed architecture documentation
- **EVENT_DRIVEN_SAGA_REFactor.md** - Refactored event-driven saga implementation
- **EVENT_MESSAGE_NAME_FIX.md** - Fix for event message name mismatch
- **SAGA_EVENT_HANDLING_FIX.md** - Fix for saga event handling issues
- **INVENTORY_RABBITMQ_FIX.md** - Fix for inventory service RabbitMQ configuration
- **BPMN_BOUNDARY_EVENT_FIX.md** - Fix for BPMN boundary event attachment
- **BPMN_POSITIONING_FIX.md** - Fix for BPMN boundary event positioning
- **TIMEOUT_HANDLING.md** - Timeout handling in saga orchestration
- **ENHANCED_PROCESS_TRACKING.md** - Enhanced process execution tracking for debugging
- **PROCESS_EXECUTION_DEBUGGING.md** - Process execution debugging guide
- **TROUBLESHOOTING.md** - Common issues and solutions
- **PRODUCTION_READY_FIX.md** - Production-ready refactored implementation
- **EXECUTION_QUERY_FIX.md** - Fix for execution query issues
- **MESSAGE_SUBSCRIPTION_FIX.md** - Fix for message subscription query issues
- **DEEP_DIAGNOSTIC_FIX.md** - Deep diagnostic analysis for stuck processes
- **TIMING_ISSUE_RETRY_FIX.md** - Fix for timing issues with inventory events
- **BUSINESS_KEY_FIX.md** - Fix for process not found by business key
- **SAGA_WORKFLOW_FIX.md** - Fix for workflow not progressing after inventory success
- **TEST_SAGA_FLOW.md** - Quick test guide to verify saga flow
- **FLOWABLE_REST_API_GUIDE.md** - Guide to Flowable REST API for monitoring
- **FLOWABLE_ADMIN_UI_SETUP.md** - Setup guide for Flowable Admin UI
- **QUICK_START_MONITORING.md** - Quick start guide for monitoring
- **FLOWABLE_MONITORING_GUIDE.md** - Comprehensive monitoring guide
- **FLOWABLE_UI_SCREENS.md** - Screenshots and explanations of Flowable UI

## 📁 Project Structure

```
flowable-saga-orchestrator/
├── saga-orchestrator/          # Flowable BPMN orchestration
│   ├── src/main/resources/processes/
│   │   └── order-saga-process.bpmn20.xml
│   └── src/main/java/com/saga/orchestrator/
│       ├── controller/         # REST API endpoints
│       ├── delegate/           # Flowable service tasks
│       ├── listener/           # RabbitMQ event listeners
│       └── service/            # Business logic
├── order-service/              # Order management
├── inventory-service/          # Inventory management
├── payment-service/            # Payment processing
├── shipping-service/           # Shipping management
├── common-events/              # Shared event models
├── docker-compose.yml          # Infrastructure setup
└── README.md
```

## 🔧 Configuration

All services are configured via `application.yml` files in their respective `src/main/resources` directories.

Key configuration points:
- Database URLs and credentials
- RabbitMQ connection settings
- Server ports
- Logging levels

## 📚 Additional Resources

- [Flowable Documentation](https://www.flowable.com/open-source/docs)
- [BPMN 2.0 Specification](https://www.omg.org/spec/BPMN/2.0/)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [RabbitMQ Tutorials](https://www.rabbitmq.com/tutorials)

## 🤝 Contributing

This is a demonstration project showcasing event-driven saga pattern implementation with Flowable BPMN.

## 📄 License

This project is provided as-is for educational and demonstration purposes.

## 📞 Support

For questions or issues, please refer to the official documentation of the technologies used.

---

**Happy Orchestrating! 🎭**
