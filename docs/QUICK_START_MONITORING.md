# Quick Start: Monitoring Your Saga with Flowable UI

## 🚀 Quick Setup (2 Minutes)

### Step 1: Start Everything
```bash
# Start infrastructure and build
./start.sh

# In 5 separate terminals, start each service:
./gradlew :saga-orchestrator:bootRun
./gradlew :order-service:bootRun
./gradlew :inventory-service:bootRun
./gradlew :payment-service:bootRun
./gradlew :shipping-service:bootRun
```

Wait for all services to start (watch for "Started [ServiceName]Application" in logs).

### Step 2: Open Flowable Admin UI
1. Open your browser
2. Navigate to: **http://localhost:8080/flowable-admin**
3. Log in:
   - Username: `admin`
   - Password: `admin`

### Step 3: Create a Test Order
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST-001",
    "shippingAddress": "123 Main St, New York, NY 10001",
    "items": [
      {
        "productId": "PROD-001",
        "productName": "Laptop",
        "quantity": 1,
        "price": 1200.00
      }
    ]
  }'
```

Copy the `orderId` from the response (e.g., `"orderId": "a1b2c3d4-e5f6-..."`)

### Step 4: Watch Your Saga in Action! 🎬

1. **In Flowable Admin**, navigate to:
   ```
   Process Engine → Instances → Process Instances
   ```

2. **Find your order** by the Business Key (your orderId)

3. **Click on the row** to open the process instance details

4. **Switch to the "Diagram" tab** - You'll see:
   - Your BPMN workflow diagram
   - Current step highlighted in GREEN
   - Real-time updates as the saga progresses

5. **Switch to the "Variables" tab** - You'll see:
   - `orderId`: Your order ID
   - `customerId`: CUST-001
   - `totalAmount`: 1200.00
   - `reservationId`: (appears after inventory step)
   - `transactionId`: (appears after payment step)
   - `shipmentId`: (appears after shipping step)

6. **Switch to the "Activities" tab** - You'll see:
   - Complete execution history
   - Timestamps for each step
   - Duration of each activity

## 🎯 What You'll See

### Successful Order Flow
```
┌─────────────────────────────────────────────┐
│  BPMN Diagram (Live Updates)                │
│                                             │
│  [Start] → [Create Order] → [Reserve Inv]  │
│            🟢 Currently here                │
│                                             │
│  → [Process Payment] → [Ship] → [End] ✅   │
└─────────────────────────────────────────────┘
```

### When Inventory Fails (10% chance)
```
┌─────────────────────────────────────────────┐
│  BPMN Diagram                               │
│                                             │
│  [Start] → [Create Order] → [Reserve Inv]  │
│                              ↓              │
│                         [Inv Failed] 🔴     │
│                              ↓              │
│                         [Cancel Order]      │
│                              ↓              │
│                           [End] ❌          │
└─────────────────────────────────────────────┘
```

### When Payment Fails (15% chance)
```
┌─────────────────────────────────────────────┐
│  BPMN Diagram with Compensation             │
│                                             │
│  [Create Order] → [Reserve Inv] ✅          │
│                        ↓                    │
│                   [Process Payment] ❌      │
│                        ↓                    │
│                   [Compensate Inv] ⚠️       │
│                        ↓                    │
│                   [Cancel Order]            │
│                        ↓                    │
│                     [End] ❌                │
└─────────────────────────────────────────────┘
```

## 📊 Key Monitoring Points

### 1. Process Instances Page
- **Location**: Process Engine → Instances → Process Instances
- **Shows**: All your orders (running and completed)
- **Business Key**: Your orderId for easy searching
- **State**: Running/Completed

### 2. Process Instance Diagram
- **Location**: Click instance → Diagram tab
- **Shows**: Visual BPMN with current position
- **Colors**: 
  - 🟢 Green = Current step
  - ⚫ Gray = Not yet executed
  - ✅ Completed = Already done

### 3. Variables Inspector
- **Location**: Click instance → Variables tab
- **Shows**: All process data
- **Useful for**: Debugging, tracing data flow

### 4. Execution History
- **Location**: Click instance → Activities tab
- **Shows**: Complete activity log with timestamps
- **Useful for**: Understanding flow, finding bottlenecks

### 5. Event Subscriptions
- **Location**: Process Engine → Jobs → Event Subscriptions
- **Shows**: Intermediate catch events waiting for messages
- **Look for**:
  - `inventoryReservedSuccess`
  - `paymentProcessedSuccess`
  - `shipmentCreatedSuccess`

## 🎬 Live Demo Script

Try this to see the full saga in action:

```bash
# Create multiple orders to see different outcomes
for i in {1..10}; do
  curl -X POST http://localhost:8080/api/orders \
    -H "Content-Type: application/json" \
    -d "{
      \"customerId\": \"CUST-$i\",
      \"shippingAddress\": \"$i Main St\",
      \"items\": [{
        \"productId\": \"PROD-001\",
        \"productName\": \"Laptop\",
        \"quantity\": 1,
        \"price\": 1200.00
      }]
    }" && echo ""
  sleep 2
done
```

Then in Flowable Admin:
1. Refresh the Process Instances page
2. You'll see 10 orders
3. Some will complete successfully ✅
4. Some will fail at inventory (10% chance) ❌
5. Some will fail at payment (15% chance) ❌
6. Click on each to see how compensation works!

## 🔍 Pro Tips

### Finding a Specific Order
1. In Process Instances page
2. Use the "Business Key" search box
3. Enter your orderId
4. Click search

### Watching Real-Time
1. Keep the Diagram tab open
2. Refresh every few seconds
3. Watch the green indicator move through the workflow

### Debugging Failures
1. Look at the Variables tab for `failureReason`
2. Check Activities tab to see where it failed
3. Verify event subscriptions are active

### Understanding Compensation
1. Create orders until you get a payment failure
2. Open that process instance
3. In Activities tab, you'll see:
   - Reserve Inventory ✅
   - Payment Failed ❌
   - **Compensate Inventory** ⚠️ (rollback!)
   - Cancel Order

## 📚 Learn More

- **Full Guide**: See `FLOWABLE_MONITORING_GUIDE.md`
- **Architecture**: See `ARCHITECTURE.md`
- **Project Overview**: See `README.md`

## ⚡ Quick Commands Reference

```bash
# Start everything
./start.sh

# Start saga-orchestrator (with Flowable UI)
./gradlew :saga-orchestrator:bootRun

# Create test order
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d @sample-order.json

# Access Flowable Admin
open http://localhost:8080/flowable-admin

# Access RabbitMQ Management
open http://localhost:15672

# Stop everything
./stop.sh
```

## 🎉 That's It!

You now have full visibility into your saga orchestration. Watch as orders flow through the system, see compensation in action when failures occur, and debug issues with complete process history.

**Happy Monitoring! 🎭**
