# Using Flowable Admin UI with Your Saga Orchestrator

## 🎯 Overview

This guide shows you how to add visual monitoring with Flowable Admin UI to your saga orchestrator project.

## ✅ Setup Flowable Admin UI (Docker - Easiest)

### Step 1: Start Infrastructure with Flowable Admin

The Flowable Admin UI is now included in your `docker-compose.yml` (optional service).

```bash
# Start all infrastructure including Flowable Admin UI
docker-compose up -d
```

This starts:
- ✅ PostgreSQL databases (5 instances)
- ✅ RabbitMQ
- ✅ **Flowable Admin UI** (new!)

### Step 2: Wait for Services to Start

Give it about 30 seconds for Flowable Admin to initialize.

Check status:
```bash
docker-compose ps

# You should see flowable-admin running on port 9988
```

### Step 3: Access Flowable Admin UI

Open your browser: **http://localhost:9988/flowable-admin**

**Default credentials:**
- Username: `admin`
- Password: `test`

### Step 4: Configure REST Endpoint Connection

On first login, you need to configure the connection to your saga-orchestrator:

1. **Click on "Flowable REST app" configuration**
2. **Enter these settings:**
   ```
   Name: Saga Orchestrator
   Description: Order Saga Orchestration
   Server Config:
     - Server Address: http://host.docker.internal:8080
     - Rest Root: flowable-rest/process-api
     - User Name: (leave empty for demo)
     - Password: (leave empty for demo)
   ```
3. **Save**

**Note:** If you're on Linux, use `http://172.17.0.1:8080` instead of `host.docker.internal`

### Step 5: Start Your Saga Orchestrator

```bash
./gradlew :saga-orchestrator:bootRun
```

Make sure all 5 services are running.

### Step 6: Create a Test Order

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d @sample-order.json
```

### Step 7: Monitor in Flowable Admin UI

1. Go back to http://localhost:9988/flowable-admin
2. Navigate to: **Process Engine** → **Process Instances**
3. You'll see your order saga!
4. Click on it to view:
   - ✅ **Visual BPMN diagram** with current step highlighted
   - ✅ **Process variables** (orderId, customerId, etc.)
   - ✅ **Execution history** with timestamps
   - ✅ **Event subscriptions** (intermediate catch events)

## 🎨 What You Get with Flowable Admin UI

### Visual BPMN Diagram
See your saga workflow visually with:
- Current step highlighted in green
- Completed steps in gray
- Future steps unmarked
- Real-time updates as saga progresses

### Process Instance Details
- Business Key (your orderId)
- Start time
- Current state
- Process variables
- Execution path

### Variable Inspector
View all process variables:
- `orderId`
- `customerId`
- `totalAmount`
- `reservationId` (after inventory)
- `transactionId` (after payment)
- `shipmentId` (after shipping)
- `failureReason` (if failed)

### Execution History
Complete timeline showing:
- Activity name
- Start time
- End time
- Duration
- Sequence of execution

### Event Subscriptions
See which intermediate catch events are active:
- `inventoryReservedSuccess`
- `inventoryReservedFailure`
- `paymentProcessedSuccess`
- `paymentProcessedFailure`
- `shipmentCreatedSuccess`

## 🔧 Alternative: Manual Docker Run

If you don't want to modify docker-compose, run Flowable Admin separately:

```bash
docker run -d \
  --name flowable-admin \
  -p 9988:9988 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/saga_orchestrator_db \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD=postgres \
  -e SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.postgresql.Driver \
  flowable/flowable-admin:7.0.1
```

## 📊 Comparing Monitoring Options

| Feature | REST API | Flowable Admin UI | Service Logs |
|---------|----------|-------------------|--------------|
| Visual BPMN | ❌ | ✅ | ❌ |
| Process Variables | ✅ JSON | ✅ Table view | ❌ |
| Execution History | ✅ JSON | ✅ Timeline | ✅ Text |
| Event Subscriptions | ✅ JSON | ✅ Table view | ❌ |
| Easy Navigation | ⚠️ Manual | ✅ UI | ⚠️ Manual |
| Scriptable | ✅ | ❌ | ❌ |
| Setup Complexity | Low | Medium | None |
| Best For | Automation | Demos/Visual | Development |

## 🎬 Demo Workflow with UI

### Full Monitoring Demo:

```bash
# 1. Start infrastructure (including Flowable Admin)
docker-compose up -d

# 2. Wait 30 seconds, then open Flowable Admin UI
open http://localhost:9988/flowable-admin
# Login: admin/test

# 3. Configure REST endpoint as shown above

# 4. Start all services
./gradlew :saga-orchestrator:bootRun  # Terminal 1
./gradlew :order-service:bootRun     # Terminal 2
./gradlew :inventory-service:bootRun # Terminal 3
./gradlew :payment-service:bootRun   # Terminal 4
./gradlew :shipping-service:bootRun  # Terminal 5

# 5. Create multiple orders
for i in {1..5}; do
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
    }"
  sleep 2
done

# 6. In Flowable Admin UI:
#    - Refresh Process Instances
#    - See all 5 orders
#    - Some successful, some failed (compensation)
#    - Click on each to see visual diagram
```

## 🐛 Troubleshooting

### Flowable Admin Can't Connect to saga-orchestrator

**Issue:** Can't reach REST endpoints

**Solutions:**
1. **On macOS/Windows:** Use `host.docker.internal` in REST endpoint config
2. **On Linux:** Use `172.17.0.1` or your host IP
3. **Alternative:** Run saga-orchestrator in Docker too

### Flowable Admin UI Not Loading

**Check:**
```bash
# See logs
docker logs flowable-admin

# Verify it's running
docker ps | grep flowable-admin

# Restart if needed
docker-compose restart flowable-admin
```

### Database Connection Error

**Solution:**
Ensure saga-orchestrator has created Flowable tables:
```bash
# Connect to database
docker exec -it saga-orchestrator-db psql -U postgres -d saga_orchestrator_db

# Check for Flowable tables
\dt ACT_*

# You should see tables like ACT_RE_PROCDEF, ACT_RU_EXECUTION, etc.
```

## 📚 Additional Flowable UI Apps

You can add more Flowable UI apps using the same approach:

### Flowable Task (Task Management)
```yaml
flowable-task:
  image: flowable/flowable-task:7.0.1
  ports:
    - "9999:9999"
  # ... similar config
```

### Flowable Modeler (BPMN Design)
```yaml
flowable-modeler:
  image: flowable/flowable-modeler:7.0.1
  ports:
    - "8888:8888"
  # ... similar config
```

### Flowable IDM (Identity Management)
```yaml
flowable-idm:
  image: flowable/flowable-idm:7.0.1
  ports:
    - "8080:8080"
  # ... similar config
```

## 🎯 Summary

**To answer your original question:**

> "Can't I use Flowable UI with the Flowable Spring Boot starter pack?"

**Answer:** Not directly embedded, but YES - you can use Flowable UI by:

1. ✅ **Using REST API** (what you have now - embedded)
2. ✅ **Running Flowable Admin separately** (Docker - visual UI)
3. ✅ **Deploying Flowable UI apps** (production setup)

**The Flowable Admin UI is now available in your docker-compose!**

Just run:
```bash
docker-compose up -d
open http://localhost:9988/flowable-admin
```

**Login:** admin/test

Then configure it to connect to your saga-orchestrator and enjoy visual monitoring! 🎉

---

**See also:**
- `FLOWABLE_REST_API_GUIDE.md` - REST API monitoring
- `README.md` - Main documentation
- Official Flowable docs: https://flowable.com/open-source/docs/
