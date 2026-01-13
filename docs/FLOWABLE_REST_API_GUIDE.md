# Flowable REST API Monitoring Guide

## 🎯 Overview

This guide explains how to monitor your Order Saga Orchestration using the Flowable REST API, giving you programmatic access to process instances, variables, and execution history.

## 📡 Accessing Flowable REST API

### Base URL
**http://localhost:8080/flowable-rest**

### API Documentation
The Flowable REST API follows REST principles and provides comprehensive endpoints for monitoring and managing workflow processes.

## 🔍 Key Monitoring Endpoints

### 1. Get All Process Instances

**Endpoint**: `GET /flowable-rest/process-api/runtime/process-instances`

```bash
curl http://localhost:8080/flowable-rest/process-api/runtime/process-instances
```

**Response**: List of all active process instances with details like:
- Process instance ID
- Business key (your orderId)
- Process definition
- Start time
- Current state

### 2. Find Order by Business Key

**Endpoint**: `GET /flowable-rest/process-api/runtime/process-instances?businessKey={orderId}`

```bash
curl "http://localhost:8080/flowable-rest/process-api/runtime/process-instances?businessKey=a1b2c3d4-e5f6-..."
```

**Use this to**: Find a specific order by its orderId

### 3. Get Process Variables

**Endpoint**: `GET /flowable-rest/process-api/runtime/process-instances/{instanceId}/variables`

```bash
# First get the instance ID from the list, then:
curl http://localhost:8080/flowable-rest/process-api/runtime/process-instances/12345/variables
```

**Response**: All process variables including:
- `orderId`
- `customerId`
- `totalAmount`
- `reservationId`
- `transactionId`
- `shipmentId`
- `failureReason` (if failed)

### 4. Get Historical Process Instances

**Endpoint**: `GET /flowable-rest/process-api/history/historic-process-instances`

```bash
curl http://localhost:8080/flowable-rest/process-api/history/historic-process-instances
```

**Use this to**: View completed saga instances and their outcomes

### 5. Get Event Subscriptions (Intermediate Catch Events)

**Endpoint**: `GET /flowable-rest/process-api/runtime/event-subscriptions`

```bash
curl http://localhost:8080/flowable-rest/process-api/runtime/event-subscriptions
```

**Response**: Active message subscriptions showing which processes are waiting for events like:
- `inventoryReservedSuccess`
- `paymentProcessedSuccess`
- `shipmentCreatedSuccess`

### 6. Get Process Activities

**Endpoint**: `GET /flowable-rest/process-api/history/historic-activity-instances?processInstanceId={instanceId}`

```bash
curl "http://localhost:8080/flowable-rest/process-api/history/historic-activity-instances?processInstanceId=12345"
```

**Use this to**: View complete execution history with timestamps

## 🎬 Step-by-Step Monitoring Workflow

### Scenario: Monitoring a New Order

#### Step 1: Create an Order
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d @sample-order.json
```

Note the `orderId` returned (e.g., `a1b2c3d4-e5f6-...`)

#### Step 2: Find the Process Instance
```bash
curl "http://localhost:8080/flowable-rest/process-api/runtime/process-instances?businessKey=a1b2c3d4-e5f6-..."
```

Note the `id` field in the response (this is your process instance ID)

#### Step 3: Monitor Variables
```bash
curl http://localhost:8080/flowable-rest/process-api/runtime/process-instances/INSTANCE_ID/variables
```

Watch as variables like `reservationId`, `transactionId`, and `shipmentId` get populated

#### Step 4: Check Execution History
```bash
curl "http://localhost:8080/flowable-rest/process-api/history/historic-activity-instances?processInstanceId=INSTANCE_ID"
```

See the complete timeline of activities

## 🔧 Using with Postman

### Import Collection
1. Open Postman
2. Create a new collection "Flowable Saga Monitoring"
3. Add requests for each endpoint above
4. Set base URL: `http://localhost:8080/flowable-rest`

### Example Requests

**Get All Process Instances:**
```
GET http://localhost:8080/flowable-rest/process-api/runtime/process-instances
```

**Get Specific Order:**
```
GET http://localhost:8080/flowable-rest/process-api/runtime/process-instances?businessKey={{orderId}}
```

**Get Variables:**
```
GET http://localhost:8080/flowable-rest/process-api/runtime/process-instances/{{instanceId}}/variables
```

## 🐛 Debugging Failed Sagas

### Check for Failures
```bash
# Get all process instances and filter by ended
curl "http://localhost:8080/flowable-rest/process-api/history/historic-process-instances?finished=true"
```

### Inspect Failure Reason
```bash
# Get variables to see failureReason
curl http://localhost:8080/flowable-rest/process-api/runtime/process-instances/INSTANCE_ID/variables | grep failureReason
```

### View Compensation Steps
```bash
# Get activities to see compensation flow
curl "http://localhost:8080/flowable-rest/process-api/history/historic-activity-instances?processInstanceId=INSTANCE_ID" | grep -E "compensate|cancel"
```

## 📊 Monitoring Script Example

Create a simple monitoring script:

```bash
#!/bin/bash
# monitor-order.sh

ORDER_ID=$1

if [ -z "$ORDER_ID" ]; then
    echo "Usage: ./monitor-order.sh ORDER_ID"
    exit 1
fi

echo "Monitoring order: $ORDER_ID"
echo "================================"

# Get process instance
INSTANCE=$(curl -s "http://localhost:8080/flowable-rest/process-api/runtime/process-instances?businessKey=$ORDER_ID")
echo "$INSTANCE" | jq '.'

# Extract instance ID
INSTANCE_ID=$(echo "$INSTANCE" | jq -r '.data[0].id')

if [ -n "$INSTANCE_ID" ]; then
    echo ""
    echo "Process Variables:"
    echo "=================="
    curl -s "http://localhost:8080/flowable-rest/process-api/runtime/process-instances/$INSTANCE_ID/variables" | jq '.'
    
    echo ""
    echo "Execution History:"
    echo "=================="
    curl -s "http://localhost:8080/flowable-rest/process-api/history/historic-activity-instances?processInstanceId=$INSTANCE_ID" | jq '.data[] | {name, startTime, endTime}'
fi
```

Make it executable:
```bash
chmod +x monitor-order.sh
./monitor-order.sh YOUR_ORDER_ID
```

## 🎯 Advanced Monitoring

### Real-time Polling
Create a watch script:
```bash
#!/bin/bash
watch -n 2 'curl -s http://localhost:8080/flowable-rest/process-api/runtime/process-instances | jq ".data[] | {businessKey, id, ended: .ended}"'
```

### Export to JSON
```bash
# Export all process instances to file
curl -s http://localhost:8080/flowable-rest/process-api/runtime/process-instances > process-instances.json
```

## 🌐 Alternative: Flowable UI Applications

For visual monitoring with diagrams and UI:

1. **Download Flowable Distribution** from https://flowable.com/open-source/downloads/
2. **Run Flowable Admin** as separate application
3. **Configure connection** to your saga-orchestrator database
4. **Benefits**: Visual BPMN diagrams, easier navigation, no coding required

See the official Flowable documentation for setup instructions.

## 📚 Additional REST API Endpoints

### Process Definitions
```bash
# List all deployed processes
curl http://localhost:8080/flowable-rest/process-api/repository/process-definitions

# Get specific definition
curl http://localhost:8080/flowable-rest/process-api/repository/process-definitions/{definitionId}
```

### Jobs
```bash
# List all jobs
curl http://localhost:8080/flowable-rest/process-api/management/jobs

# Failed jobs
curl http://localhost:8080/flowable-rest/process-api/management/deadletter-jobs
```

### Tasks
```bash
# List all tasks (if any user tasks exist)
curl http://localhost:8080/flowable-rest/process-api/runtime/tasks
```

## 🔍 Quick Reference

| **What to Monitor** | **Endpoint** | **Method** |
|---------------------|-------------|-----------|
| All Processes | `/process-api/runtime/process-instances` | GET |
| Find by Order ID | `/process-api/runtime/process-instances?businessKey={orderId}` | GET |
| Process Variables | `/process-api/runtime/process-instances/{id}/variables` | GET |
| Execution History | `/process-api/history/historic-activity-instances?processInstanceId={id}` | GET |
| Event Subscriptions | `/process-api/runtime/event-subscriptions` | GET |
| Historical Instances | `/process-api/history/historic-process-instances` | GET |

## 📞 Need Help?

- **Flowable REST API Docs**: https://www.flowable.com/open-source/docs/bpmn/ch15-REST/
- **Service Logs**: Check saga-orchestrator logs for detailed process execution
- **RabbitMQ UI**: http://localhost:15672 for message flow

---

**Happy Monitoring via REST API! 🎭**
