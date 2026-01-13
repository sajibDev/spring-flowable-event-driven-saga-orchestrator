# Saga Event Handling Fix ✅

## 🎯 **Issue Description**

The saga orchestrator was receiving OrderCreatedEvent messages but couldn't find any executions waiting for these events. This was causing the workflow to stall after the initial CreateOrderCommand.

## 🔍 **Root Cause Analysis**

After thorough investigation, we identified the root cause:

1. **BPMN Element ID vs Message Name Mismatch**: 
   - In the BPMN file, intermediate catch events have element IDs (e.g., `orderCreatedEvent`) that are different from their message names (e.g., `orderCreated`)
   - The query logic in `SagaEventListener` was incorrectly using the message name as the activity ID when querying for executions

2. **Query Strategy Issue**:
   - The `findExecutionsByEvent` method was using `activityId(eventName)` where `eventName` was the message name
   - However, Flowable's execution query requires the actual BPMN element ID, not the message name

## 🛠️ **Solution Implemented**

### 1. **Added Activity ID Mapping Method**

Created a new method `getActivityIdForEvent(String eventName)` that correctly maps message event names to their corresponding BPMN element IDs:

```java
private String getActivityIdForEvent(String eventName) {
    switch (eventName) {
        case "orderCreated":
            return "orderCreatedEvent";
        case "orderCreationFailed":
            return "orderCreationFailedEvent";
        case "inventoryReservedSuccess":
            return "inventoryReservedSuccess";
        case "inventoryReservedFailure":
            return "inventoryReservedFailure";
        case "paymentProcessedSuccess":
            return "paymentProcessedSuccess";
        case "paymentProcessedFailure":
            return "paymentProcessedFailure";
        case "shipmentCreatedSuccess":
            return "shipmentCreatedSuccess";
        default:
            return eventName;
    }
}
```

### 2. **Updated Query Logic**

Modified the execution query strategies to use the correct activity IDs:

```java
// Method 1: Direct query by process instance ID and activity ID
executions = runtimeService.createExecutionQuery()
        .processInstanceId(processInstance.getId())
        .activityId(getActivityIdForEvent(eventName))
        .list();

// Method 2: Query all executions and filter manually
executions = allExecutions.stream()
        .filter(exec -> getActivityIdForEvent(eventName).equals(exec.getActivityId()))
        .collect(Collectors.toList());
```

## 📋 **Verification Steps**

1. ✅ Compilation successful
2. ✅ All existing query strategies preserved
3. ✅ New mapping method handles all current message events
4. ✅ Default fallback for unknown events maintains backward compatibility

## 🚀 **Expected Behavior**

With this fix, the saga orchestrator should now correctly:

1. Receive OrderCreatedEvent messages
2. Find the execution waiting at the `orderCreatedEvent` intermediate catch event
3. Successfully deliver the message event to progress the workflow
4. Continue to the next steps in the saga (inventory reservation, payment processing, etc.)

## 📚 **BPMN Reference**

For future reference, here are the current BPMN element IDs and their corresponding message names:

| BPMN Element ID | Message Name |
|----------------|--------------|
| orderCreatedEvent | orderCreated |
| orderCreationFailedEvent | orderCreationFailed |
| inventoryReservedSuccess | inventoryReservedSuccess |
| inventoryReservedFailure | inventoryReservedFailure |
| paymentProcessedSuccess | paymentProcessedSuccess |
| paymentProcessedFailure | paymentProcessedFailure |
| shipmentCreatedSuccess | shipmentCreatedSuccess |

Any future modifications to the BPMN should update the `getActivityIdForEvent` method accordingly.