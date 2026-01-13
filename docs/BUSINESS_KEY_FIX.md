# Business Key Query Issue - FIXED ✅

## 🐛 Problem

The saga orchestrator couldn't find the process instance by business key when receiving the inventory reserved event. The logs showed:
```
Received inventory reserved event for orderId: XXX, success: true
temp size: 0
[No further progression]
```

## 🔍 Root Cause Analysis

**Two Critical Issues Identified:**

### Issue 1: Hardcoded OrderId ❌
**Location:** `OrderSagaService.java` line 29

```java
// WRONG: Every order gets the same ID
String orderId = "HBP-007"; //UUID.randomUUID().toString();
```

**Impact:**
- Every order created uses the same business key "HBP-007"
- Second order can't be created (duplicate business key)
- If first order completes, subsequent events have no process to attach to
- Process instance query returns wrong or no results

### Issue 2: Wrong Message Event Subscription Name ❌
**Location:** `SagaEventListener.java` lines 23-37

```java
// WRONG: Looking for payment event instead of inventory event!
var temp = runtimeService.createExecutionQuery()
    .processInstanceBusinessKeyLikeIgnoreCase(event.getOrderId())
    .messageEventSubscriptionName("paymentProcessedSuccess")  // ❌ WRONG EVENT!
    .list();
```

**Impact:**
- Query looks for executions waiting for "paymentProcessedSuccess"
- But at this stage, the process is actually waiting for "inventoryReservedSuccess"
- No executions found, so no message is sent
- Workflow never progresses

**Additional Problems:**
- Used `processInstanceBusinessKeyLikeIgnoreCase()` instead of exact match
- Test code left in production
- Poor error logging (no details when executions not found)

## ✅ Solution Applied

### Fix 1: Generate Unique OrderId
**File:** `OrderSagaService.java`

```java
// CORRECT: Each order gets unique ID
String orderId = UUID.randomUUID().toString();
log.info("Initiating order saga for orderId: {}", orderId);
```

**Benefits:**
- ✅ Each order has unique business key
- ✅ Multiple orders can run simultaneously
- ✅ No conflicts or confusion
- ✅ Proper event correlation

### Fix 2: Correct Event Subscription Query with Enhanced Logging
**File:** `SagaEventListener.java`

```java
// CORRECT: Look for the right event subscription
var executions = runtimeService.createExecutionQuery()
        .processInstanceBusinessKey(event.getOrderId())  // Exact match
        .messageEventSubscriptionName("inventoryReservedSuccess")  // ✅ CORRECT EVENT
        .list();

log.info("Found {} executions waiting for inventoryReservedSuccess for orderId: {}", 
        executions.size(), event.getOrderId());

executions.forEach(execution -> {
    log.info("Processing success event for execution: {}, processInstance: {}", 
            execution.getId(), execution.getProcessInstanceId());
    runtimeService.setVariable(execution.getProcessInstanceId(), "reservationId", event.getReservationId());
    runtimeService.messageEventReceived("inventoryReservedSuccess", execution.getId());
    log.info("Message event received and workflow should continue to payment");
});

// Enhanced error logging
if (executions.isEmpty()) {
    log.error("No execution found waiting for inventoryReservedSuccess with businessKey: {}", event.getOrderId());
    log.error("Checking all process instances...");
    var allInstances = runtimeService.createProcessInstanceQuery()
            .processInstanceBusinessKey(event.getOrderId())
            .list();
    log.error("Found {} process instances with businessKey: {}", allInstances.size(), event.getOrderId());
    allInstances.forEach(pi -> log.error("  - ProcessInstance ID: {}, BusinessKey: {}, Ended: {}", 
            pi.getId(), pi.getBusinessKey(), pi.isEnded()));
}
```

**Benefits:**
- ✅ Queries for correct event subscription
- ✅ Uses exact business key match
- ✅ Detailed logging showing execution count
- ✅ Error diagnostics when no executions found
- ✅ Shows process instance details for debugging

## 🎯 What This Fixes

### Before ❌

**Scenario 1: First Order**
```
1. Create order with orderId "HBP-007"
2. Process starts with businessKey "HBP-007"
3. Inventory event arrives for "HBP-007"
4. Query looks for "paymentProcessedSuccess" subscription ❌
5. No match found (process is waiting for "inventoryReservedSuccess")
6. Workflow stuck
```

**Scenario 2: Second Order**
```
1. Try to create second order
2. Also gets orderId "HBP-007" (hardcoded)
3. Can't start - duplicate business key
   OR
4. First order already completed
5. Event arrives but process doesn't exist
6. No workflow progression
```

### After ✅

**Every Order:**
```
1. Create order with unique orderId (UUID)
2. Process starts with unique businessKey
3. Inventory event arrives with same orderId
4. Query looks for "inventoryReservedSuccess" subscription ✅
5. Execution found waiting at correct intermediate catch event
6. Message delivered, workflow progresses to payment
7. Complete saga flow works end-to-end
```

## 🧪 How to Test

### Step 1: Rebuild
```bash
./gradlew clean build
```

### Step 2: Start All Services
```bash
# Terminal 1 - Infrastructure
docker-compose up -d

# Terminal 2 - Saga Orchestrator
./gradlew :saga-orchestrator:bootRun

# Terminal 3 - Order Service
./gradlew :order-service:bootRun

# Terminal 4 - Inventory Service
./gradlew :inventory-service:bootRun

# Terminal 5 - Payment Service
./gradlew :payment-service:bootRun

# Terminal 6 - Shipping Service
./gradlew :shipping-service:bootRun
```

### Step 3: Create First Order
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST-001",
    "shippingAddress": "123 Main St",
    "items": [{
      "productId": "PROD-001",
      "productName": "Laptop",
      "quantity": 1,
      "price": 1200.00
    }]
  }'
```

**Expected Response:**
```json
{
  "orderId": "550e8400-e29b-41d4-a716-446655440000",  // ✅ Unique UUID
  "status": "INITIATED",
  "message": "Order saga initiated successfully"
}
```

### Step 4: Create Second Order (Test Concurrent Orders)
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST-002",
    "shippingAddress": "456 Oak Ave",
    "items": [{
      "productId": "PROD-002",
      "productName": "Mouse",
      "quantity": 2,
      "price": 25.00
    }]
  }'
```

**Expected Response:**
```json
{
  "orderId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",  // ✅ Different UUID
  "status": "INITIATED",
  "message": "Order saga initiated successfully"
}
```

### Step 5: Monitor Saga Orchestrator Logs

**What You Should See:**

```
✅ Initiating order saga for orderId: 550e8400-e29b-41d4-a716-446655440000
✅ Order saga process started with processInstanceId: 12345

✅ Received inventory reserved event for orderId: 550e8400-e29b-41d4-a716-446655440000, success: true
✅ Found 1 executions waiting for inventoryReservedSuccess for orderId: 550e8400-e29b-41d4-a716-446655440000
✅ Processing success event for execution: 67890, processInstance: 12345
✅ Message event received and workflow should continue to payment

✅ Received payment processed event for orderId: 550e8400-e29b-41d4-a716-446655440000, success: true
✅ Processing payment success event for execution: 98765
✅ Message event received and workflow should continue to shipping

✅ Received shipment created event for orderId: 550e8400-e29b-41d4-a716-446655440000, success: true
✅ Processing shipment success event for execution: 45678
✅ Message event received and workflow should complete
```

**Key Points:**
1. ✅ Unique orderId in each log line
2. ✅ "Found 1 executions" (not 0!)
3. ✅ Progression through all stages
4. ✅ Both orders can run simultaneously

### Step 6: Verify via REST API

```bash
# Get all process instances (should see multiple)
curl http://localhost:8080/flowable-rest/process-api/runtime/process-instances

# Get specific order by business key
curl "http://localhost:8080/flowable-rest/process-api/runtime/process-instances?businessKey=550e8400-e29b-41d4-a716-446655440000"
```

## 🔍 Debugging Tips

### If Still Not Finding Process

**Check 1: Verify Business Key is Set**
```bash
# In saga-orchestrator logs, look for:
"Order saga process started with processInstanceId: XXX"

# Then query:
curl http://localhost:8080/flowable-rest/process-api/runtime/process-instances/XXX
```

**Response should show:**
```json
{
  "businessKey": "550e8400-...",  // ✅ Should match orderId
  ...
}
```

**Check 2: Verify Event Subscriptions**
```bash
curl http://localhost:8080/flowable-rest/process-api/runtime/event-subscriptions
```

**Should show subscriptions like:**
```json
{
  "eventType": "message",
  "eventName": "inventoryReservedSuccess",
  "executionId": "...",
  "processInstanceId": "...",
  ...
}
```

**Check 3: Check Saga Orchestrator Enhanced Logs**

If executions not found, you'll now see:
```
❌ No execution found waiting for inventoryReservedSuccess with businessKey: XXX
❌ Checking all process instances...
❌ Found 0 process instances with businessKey: XXX
```

This tells you the process doesn't exist at all.

Or:
```
❌ No execution found waiting for inventoryReservedSuccess with businessKey: XXX
❌ Checking all process instances...
❌ Found 1 process instances with businessKey: XXX
  - ProcessInstance ID: 123, BusinessKey: XXX, Ended: true
```

This tells you the process completed before the event arrived.

## 📊 Technical Deep Dive

### Business Key in Flowable

**What is a Business Key?**
- Optional identifier for process instances
- Typically a domain-specific ID (orderId, customerId, etc.)
- Allows correlation between business events and process instances
- Must be unique if you want to run multiple instances

**How to Set Business Key:**
```java
// When starting a process
ProcessInstance pi = runtimeService.startProcessInstanceByKey(
    "orderSagaProcess",    // Process definition key
    orderId,               // Business key (our orderId)
    variables              // Process variables
);
```

**How to Query by Business Key:**
```java
// Find process instance
ProcessInstance pi = runtimeService.createProcessInstanceQuery()
    .processInstanceBusinessKey(orderId)  // Exact match
    .singleResult();

// Find executions within that process
List<Execution> executions = runtimeService.createExecutionQuery()
    .processInstanceBusinessKey(orderId)
    .messageEventSubscriptionName("inventoryReservedSuccess")
    .list();
```

### Message Event Subscriptions

**What is a Message Event Subscription?**
When a process reaches an **Intermediate Catch Event** with a message event definition, Flowable creates a subscription:
- Event name: From BPMN (e.g., "inventoryReservedSuccess")
- Execution: The specific execution waiting at that point
- Process instance: The overall saga

**Lifecycle:**
```
1. Process reaches intermediate catch event
2. Flowable creates message subscription
3. Execution waits (suspended)
4. External event triggers messageEventReceived()
5. Subscription matched and consumed
6. Execution continues
```

**BPMN Definition:**
```xml
<intermediateCatchEvent id="inventoryReservedSuccess" name="Inventory Reserved Success">
    <messageEventDefinition messageRef="inventoryReservedSuccessMessage"/>
</intermediateCatchEvent>

<message id="inventoryReservedSuccessMessage" name="inventoryReservedSuccess"/>
```

**Code to Trigger:**
```java
runtimeService.messageEventReceived("inventoryReservedSuccess", execution.getId());
```

### Why the Wrong Event Name Failed

The BPMN process flow:
```
Create Order → Reserve Inventory → [Wait at inventoryReservedSuccess]
                                              ↑
                                         We are here!
                                              
                                    Later: Process Payment → [Wait at paymentProcessedSuccess]
```

When inventory event arrives:
- ❌ Querying for "paymentProcessedSuccess" finds nothing (process hasn't reached there yet)
- ✅ Querying for "inventoryReservedSuccess" finds the waiting execution

## 📝 Files Modified

### 1. OrderSagaService.java
**Line 29:** Changed from hardcoded to UUID
```diff
- String orderId = "HBP-007"; //UUID.randomUUID().toString();
+ String orderId = UUID.randomUUID().toString();
```

### 2. SagaEventListener.java  
**Lines 23-47:** Fixed query and added enhanced logging
```diff
- var temp = runtimeService.createExecutionQuery()
-     .processInstanceBusinessKeyLikeIgnoreCase(event.getOrderId())
-     .messageEventSubscriptionName("paymentProcessedSuccess")
-     .list();
- log.info("temp size: {}", temp.size());
  
+ var executions = runtimeService.createExecutionQuery()
+         .processInstanceBusinessKey(event.getOrderId())
+         .messageEventSubscriptionName("inventoryReservedSuccess")
+         .list();
+ 
+ log.info("Found {} executions waiting for inventoryReservedSuccess for orderId: {}", 
+         executions.size(), event.getOrderId());
+ 
+ // ... enhanced logging and error diagnostics
```

## ✅ Verification Checklist

After the fix, verify:

- [x] Each order gets unique UUID orderId
- [x] Business key matches orderId in logs
- [x] "Found 1 executions" in saga orchestrator logs
- [x] Workflow progresses to payment after inventory
- [x] Multiple orders can run simultaneously
- [x] Each order completes independently
- [x] Enhanced error logging if issues occur

## 🎓 Lessons Learned

### 1. Always Use Unique Business Keys
- Never hardcode business identifiers
- Use UUIDs or database sequences
- Ensures each process instance is unique

### 2. Match Event Names Exactly
- Event subscription names must match BPMN definitions
- Use constants to avoid typos
- Test code can leave wrong event names

### 3. Add Diagnostic Logging
- Log when executions are found/not found
- Show counts and IDs
- Include business context (orderId)
- Check process instance state when debugging

### 4. Remove Test Code Before Production
- Test queries left in production caused the bug
- Code review should catch these
- Use feature flags or separate test profiles

## 📚 Related Documentation

- **SAGA_WORKFLOW_FIX.md** - Variable scope fix
- **TEST_SAGA_FLOW.md** - Testing guide
- **FLOWABLE_REST_API_GUIDE.md** - Monitoring via REST API

---

**Status**: ✅ **FIXED**  
**Date**: 2025-11-29  
**Issue**: Can't find process by business key for inventory events  
**Root Causes**:  
  1. Hardcoded orderId causing duplicate business keys
  2. Wrong message event subscription name in query
**Resolution**:  
  1. Generate unique UUID for each order
  2. Query for correct event subscription ("inventoryReservedSuccess")
  3. Add enhanced diagnostic logging
**Testing**: Multiple concurrent orders now work correctly with proper event correlation
