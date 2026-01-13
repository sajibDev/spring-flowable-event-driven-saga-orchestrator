# Flowable UI Monitoring Guide

## 🎯 Overview

This guide explains how to monitor your Order Saga Orchestration using the Flowable Admin UI, giving you complete visibility into the saga execution process.

## 📊 Accessing Flowable Admin UI

### URL
**http://localhost:8080/flowable-admin**

### Credentials
- **Username**: `admin`
- **Password**: `admin`

### First-Time Setup
1. After starting the saga-orchestrator service, the admin user is automatically created
2. Navigate to http://localhost:8080/flowable-admin
3. Log in with the credentials above
4. You'll see the Flowable Admin dashboard

## 🔍 Main Monitoring Sections

### 1. Process Definitions
**Path**: Process Engine → Definitions → Process Definitions

**What you'll see:**
- **Process Key**: `orderSagaProcess`
- **Process Name**: Order Saga Process
- **Version**: Current deployed version
- **Deployment Time**: When the BPMN was deployed

**Actions you can take:**
- View the BPMN diagram
- See all instances of this process
- Check process definition details

### 2. Process Instances
**Path**: Process Engine → Instances → Process Instances

**What you'll see:**
- All running and completed saga instances
- Process Instance ID
- Business Key (your orderId)
- Start Time
- Current State (Running/Completed/Failed)

**Key Information:**
- **Business Key**: This is your `orderId`, making it easy to find specific orders
- **State**: Shows if the saga is still running or completed
- **Variables**: All process variables like customerId, totalAmount, reservationId, etc.

**To monitor a specific order:**
1. Click on a process instance
2. View the **Diagram** tab to see the current position in the workflow
3. Check **Variables** to see all data (orderId, reservationId, transactionId, etc.)
4. View **Activities** to see the execution history

### 3. Real-Time Process Visualization

**How to view a running saga:**
1. Create an order via the REST API
2. Immediately go to Process Instances
3. Find your order by Business Key (orderId)
4. Click on it and select the **Diagram** tab
5. You'll see the BPMN diagram with the current activity highlighted in green

**What the colors mean:**
- 🟢 **Green**: Current active step
- ⚫ **Gray**: Not yet executed
- ✅ **Completed**: Already executed steps

### 4. Process Variables

**Path**: Process Instance → Variables tab

**Variables you'll find:**
- `orderId`: The unique order identifier
- `customerId`: Customer who placed the order
- `totalAmount`: Total order amount
- `shippingAddress`: Delivery address
- `orderData`: Complete order event object
- `reservationId`: Inventory reservation ID (set after inventory step)
- `transactionId`: Payment transaction ID (set after payment step)
- `shipmentId`: Shipment ID (set after shipping step)
- `failureReason`: Reason for failure (if saga failed)

**Why this matters:**
- Debug issues by inspecting variable values
- Trace the flow of data through the saga
- Understand why a saga failed

### 5. Execution History

**Path**: Process Instance → Activities tab

**What you'll see:**
- Chronological list of all executed activities
- Start and end times for each step
- Duration of each activity
- Sequence of execution

**Example for successful order:**
```
1. Start Event (startEvent)
2. Create Order (createOrderTask)
3. Reserve Inventory (reserveInventoryTask)
4. Wait for Inventory Response (waitForInventoryResponse)
5. Inventory Reserved Success (inventoryReservedSuccess)
6. Process Payment (processPaymentTask)
7. Wait for Payment Response (waitForPaymentResponse)
8. Payment Processed Success (paymentProcessedSuccess)
9. Create Shipment (createShipmentTask)
10. Wait for Shipment Response (waitForShipmentResponse)
11. Shipment Created Success (shipmentCreatedSuccess)
12. End Event Success (endEventSuccess)
```

### 6. Jobs Monitoring

**Path**: Process Engine → Jobs → Jobs

**What you'll see:**
- Async jobs (if any)
- Job execution times
- Failed jobs
- Retry counts

**Important for:**
- Monitoring asynchronous operations
- Debugging failed jobs
- Understanding job retry mechanisms

### 7. Intermediate Catch Events

**Path**: Process Engine → Jobs → Event Subscriptions

**What you'll see:**
- Active message event subscriptions
- Event names waiting for signals:
  - `inventoryReservedSuccess`
  - `inventoryReservedFailure`
  - `paymentProcessedSuccess`
  - `paymentProcessedFailure`
  - `shipmentCreatedSuccess`

**Why this is important:**
- Shows which sagas are waiting for events
- Helps debug if events aren't being received
- Confirms event subscriptions are active

## 🎬 Step-by-Step Monitoring Workflow

### Scenario: Monitoring a New Order

#### Step 1: Create an Order
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d @sample-order.json
```

Note the `orderId` returned in the response.

#### Step 2: Open Flowable Admin
1. Navigate to http://localhost:8080/flowable-admin
2. Log in with admin/admin

#### Step 3: Find Your Order
1. Go to **Process Engine** → **Instances** → **Process Instances**
2. Look for your `orderId` in the **Business Key** column
3. Click on the row to open details

#### Step 4: Monitor Execution
1. Click the **Diagram** tab
   - See the visual BPMN with current step highlighted
2. Click the **Variables** tab
   - Inspect all process variables
3. Click the **Activities** tab
   - View execution history

#### Step 5: Track Event Responses
1. Watch as the process moves through each step
2. After "Reserve Inventory", it will wait at the event-based gateway
3. When the inventory service responds, the process continues
4. Repeat for payment and shipping steps

## 🔍 Debugging Failed Sagas

### If Inventory Reservation Fails

**What you'll see in Flowable:**
1. Process stops at `inventoryReservedFailure` intermediate catch event
2. Process takes the failure path
3. Executes `cancelOrderTask`
4. Ends at `endEventFailure`

**Variables to check:**
- `failureReason`: Will contain "Insufficient inventory" or error message

**Activities tab will show:**
```
1. Start Event
2. Create Order
3. Reserve Inventory
4. Wait for Inventory Response
5. Inventory Reserved Failure ← Failed here
6. Cancel Order
7. End Event Failure
```

### If Payment Processing Fails

**What you'll see in Flowable:**
1. Process successfully completes inventory reservation
2. Process stops at `paymentProcessedFailure` intermediate catch event
3. Process takes the compensation path
4. Executes `compensateInventoryTask` (releases inventory)
5. Executes `cancelOrderTask`
6. Ends at `endEventFailure`

**Variables to check:**
- `reservationId`: Shows inventory was reserved
- `failureReason`: Will contain payment error message
- `transactionId`: Will be null or show failed transaction

**Activities tab will show compensation:**
```
1-7. [Normal flow until payment]
8. Payment Processed Failure ← Failed here
9. Compensate Inventory ← Rollback
10. Cancel Order
11. End Event Failure
```

## 📈 Advanced Monitoring

### Filter Process Instances
You can filter by:
- **State**: Running, Completed, Suspended
- **Business Key**: Search by orderId
- **Start Date**: Time range filters

### Search by Business Key
1. In Process Instances page
2. Use the search box
3. Enter your orderId
4. Click search

### Export History
You can export process instance history for reporting or auditing.

## 🛠️ Additional Flowable Endpoints

### Process Instance Query API
```bash
# Get all process instances
curl -u admin:admin http://localhost:8080/process-api/runtime/process-instances

# Get specific instance by business key (orderId)
curl -u admin:admin "http://localhost:8080/process-api/runtime/process-instances?businessKey={orderId}"
```

### Historical Process Instances
```bash
curl -u admin:admin http://localhost:8080/process-api/history/historic-process-instances
```

## 📊 Monitoring Best Practices

1. **Use Business Keys**: Always use orderId as business key for easy lookup
2. **Check Variables Early**: Inspect variables to understand saga state
3. **Monitor Event Subscriptions**: Ensure intermediate catch events are registered
4. **Review Failed Jobs**: Check Jobs section for any failures
5. **Analyze Execution Times**: Use Activities tab to find performance bottlenecks

## 🎯 Quick Reference

| **What to Monitor** | **Where to Find It** | **Why It Matters** |
|---------------------|---------------------|-------------------|
| Running Sagas | Process Instances | See active orders |
| Saga Progress | Instance → Diagram | Visual workflow state |
| Order Data | Instance → Variables | Debug data issues |
| Execution Path | Instance → Activities | Understand flow |
| Waiting Events | Event Subscriptions | Check async events |
| Failed Operations | Jobs → Failed Jobs | Debug failures |
| Process Definition | Definitions → orderSagaProcess | Verify BPMN deployment |

## 🚨 Common Issues and Solutions

### Issue: Can't see any process instances
**Solution**: Make sure you've created at least one order via the REST API

### Issue: Process is stuck at event-based gateway
**Solution**: 
- Check RabbitMQ to ensure events are being published
- Verify event subscription in Event Subscriptions section
- Check service logs to ensure they're processing messages

### Issue: Variables are missing
**Solution**: Check the delegate classes to ensure they're setting variables correctly

### Issue: Can't log in to Flowable Admin
**Solution**: 
- Verify saga-orchestrator is running
- Check logs for "Flowable admin user created" message
- Try clearing browser cache

## 📚 Additional Resources

- **Flowable Documentation**: https://www.flowable.com/open-source/docs
- **BPMN 2.0 Guide**: https://www.omg.org/spec/BPMN/2.0/
- **Process Engine API**: http://localhost:8080/process-api (when running)

---

**Happy Monitoring! 🎭**
