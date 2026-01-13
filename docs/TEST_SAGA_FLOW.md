# Quick Test Guide - Saga Flow Fix Verification

## 🚀 Quick Start

### 1. Build
```bash
./gradlew clean build
```

### 2. Start Infrastructure
```bash
docker-compose up -d
```

### 3. Start All 5 Services

```bash
# Terminal 1
./gradlew :saga-orchestrator:bootRun

# Terminal 2
./gradlew :order-service:bootRun

# Terminal 3
./gradlew :inventory-service:bootRun

# Terminal 4
./gradlew :payment-service:bootRun

# Terminal 5
./gradlew :shipping-service:bootRun
```

### 4. Test Order Creation
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST-001",
    "shippingAddress": "123 Main St, City, State 12345",
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

## ✅ Expected Results

### Saga Orchestrator Logs (Terminal 1)
```
✅ Creating order via RabbitMQ event...
✅ Received inventory reserved event for orderId: xxx, success: true
✅ Processing success event for execution: yyy
✅ Message event received and workflow should continue to payment  ⭐ KEY FIX
✅ Processing payment via RabbitMQ event...
✅ Received payment processed event for orderId: xxx, success: true
✅ Processing payment success event for execution: zzz
✅ Message event received and workflow should continue to shipping
✅ Creating shipment via RabbitMQ event...
✅ Received shipment created event for orderId: xxx, success: true
✅ Processing shipment success event for execution: aaa
✅ Message event received and workflow should complete
```

### Order Service Logs (Terminal 2)
```
✅ Received order create event
✅ Order created with ID: xxx
✅ Publishing OrderCreatedEvent to RabbitMQ
```

### Inventory Service Logs (Terminal 3)
```
✅ Received inventory reserve event for orderId: xxx
✅ Inventory reserved with reservationId: yyy
✅ Publishing InventoryReservedEvent (success) to RabbitMQ
```

### Payment Service Logs (Terminal 4)
```
✅ Received payment process event for orderId: xxx
✅ Payment processed with transactionId: zzz
✅ Publishing PaymentProcessedEvent (success) to RabbitMQ
```

### Shipping Service Logs (Terminal 5)
```
✅ Received shipment create event for orderId: xxx
✅ Shipment created with shipmentId: aaa
✅ Publishing ShipmentCreatedEvent (success) to RabbitMQ
```

## 🔍 Verify via REST API

### Get Process Instance
```bash
# Replace ORDER_ID with the one from your test
curl "http://localhost:8080/flowable-rest/process-api/runtime/process-instances?businessKey=ORDER_ID"
```

**Expected:** Process should be completed or at final stage

### Get Process Variables
```bash
# Replace INSTANCE_ID from above response
curl http://localhost:8080/flowable-rest/process-api/runtime/process-instances/INSTANCE_ID/variables
```

**Expected Variables:**
- ✅ `orderId`
- ✅ `customerId`
- ✅ `totalAmount`
- ✅ `reservationId` (from inventory)
- ✅ `transactionId` (from payment)
- ✅ `shipmentId` (from shipping)

### Get Execution History
```bash
curl "http://localhost:8080/flowable-rest/process-api/history/historic-activity-instances?processInstanceId=INSTANCE_ID"
```

**Expected Activities (in order):**
1. ✅ Start Event
2. ✅ Create Order
3. ✅ Reserve Inventory
4. ✅ Wait for Inventory Response (gateway)
5. ✅ Inventory Reserved Success (catch event)
6. ✅ Process Payment ⭐ **This should now appear!**
7. ✅ Wait for Payment Response (gateway)
8. ✅ Payment Processed Success (catch event)
9. ✅ Create Shipment
10. ✅ Wait for Shipment Response (gateway)
11. ✅ Shipment Created Success (catch event)
12. ✅ End Event Success

## 🎯 What The Fix Resolves

### Before Fix ❌
```
Order → Inventory → [STUCK HERE - No Payment]
```

### After Fix ✅
```
Order → Inventory → Payment → Shipping → Complete
```

## 🐛 Troubleshooting

### If workflow still doesn't progress:

**Check 1: Are all services running?**
```bash
# Should see 5 services on ports 8080-8084
lsof -i :8080,8081,8082,8083,8084
```

**Check 2: Is RabbitMQ running?**
```bash
docker ps | grep rabbitmq
```

**Check 3: Are messages being published?**
- Open RabbitMQ UI: http://localhost:15672 (guest/guest)
- Check "Queues" tab
- Verify messages are flowing

**Check 4: Check saga-orchestrator logs**
```bash
# Look for this specific line:
"Message event received and workflow should continue to payment"
```

**If missing**, the fix wasn't applied correctly.

**Check 5: Verify database tables**
```bash
docker exec -it saga-orchestrator-db psql -U postgres -d saga_orchestrator_db

# Check active process instances
SELECT * FROM act_ru_execution;

# Check event subscriptions
SELECT * FROM act_ru_event_subscr;
```

## 📊 Monitor with Flowable Admin UI (Optional)

If you have Flowable Admin UI running:

```bash
# Access UI
open http://localhost:9988/flowable-admin
# Login: admin/test

# Navigate to:
Process Engine → Process Instances → Click on your order

# You'll see:
✅ Visual BPMN diagram with current step highlighted
✅ All process variables
✅ Complete execution history
✅ Event subscriptions
```

## 🎬 Demo Script

Run this script to create multiple test orders:

```bash
#!/bin/bash
echo "Creating test orders..."

for i in {1..3}; do
  echo "Creating order $i..."
  curl -s -X POST http://localhost:8080/api/orders \
    -H "Content-Type: application/json" \
    -d "{
      \"customerId\": \"CUST-00$i\",
      \"shippingAddress\": \"$i Main Street, Test City, TC 12345\",
      \"items\": [
        {
          \"productId\": \"PROD-001\",
          \"productName\": \"Laptop\",
          \"quantity\": 1,
          \"price\": 1200.00
        }
      ]
    }" | jq -r '.orderId'
  
  echo "Waiting 3 seconds before next order..."
  sleep 3
done

echo "All orders created. Check logs and Flowable REST API."
```

Save as `create-test-orders.sh`, make executable:
```bash
chmod +x create-test-orders.sh
./create-test-orders.sh
```

## ✅ Success Criteria

Your saga workflow is working correctly when:

1. ✅ All 5 service logs show successful event processing
2. ✅ Saga orchestrator logs show "continue to payment" message
3. ✅ Saga orchestrator logs show "continue to shipping" message
4. ✅ Saga orchestrator logs show "workflow should complete" message
5. ✅ REST API shows process completed
6. ✅ Process variables include reservationId, transactionId, and shipmentId
7. ✅ No errors in any service logs

## 📚 Related Documentation

- **SAGA_WORKFLOW_FIX.md** - Detailed explanation of the fix
- **FLOWABLE_REST_API_GUIDE.md** - How to monitor via REST API
- **FLOWABLE_ADMIN_UI_SETUP.md** - How to use visual monitoring
- **TROUBLESHOOTING.md** - General troubleshooting guide

---

**Happy Testing! 🎉**
