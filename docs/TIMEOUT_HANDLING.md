# Timeout Handling in Saga Orchestration ✅

## 🎯 **Overview**

This document explains how timeout handling has been implemented in the saga orchestration system to make processes fault-tolerant. The system now includes timer boundary events that trigger compensation workflows when services don't respond within a specified time period.

## ⏰ **Timeout Implementation Details**

### 1. **Timer Boundary Events**

Timer boundary events have been added to all event-based gateways in the BPMN workflow:

| Gateway | Timeout Duration | Action on Timeout |
|---------|------------------|-------------------|
| waitForOrderCreatedEvent | 30 seconds | Cancel order |
| waitForInventoryResponse | 30 seconds | Cancel order |
| waitForPaymentResponse | 30 seconds | Compensate inventory & cancel order |
| waitForShipmentResponse | 30 seconds | Mark as completed (no compensation) |

### 2. **Timeout Handling Delegates**

Four new delegate classes have been created to handle timeout events:

1. **HandleOrderCreationTimeoutDelegate** - Handles order creation timeouts
2. **HandleInventoryTimeoutDelegate** - Handles inventory reservation timeouts
3. **HandlePaymentTimeoutDelegate** - Handles payment processing timeouts
4. **HandleShipmentTimeoutDelegate** - Handles shipment creation timeouts

Each delegate sets a specific process variable to indicate which timeout occurred.

### 3. **BPMN Modifications**

The BPMN workflow has been updated with timer boundary events:

```xml
<!-- Timer Boundary Event for Order Creation Timeout -->
<boundaryEvent id="orderCreationTimeout" name="Order Creation Timeout" attachedToRef="waitForOrderCreatedEvent">
    <timerEventDefinition>
        <timeDuration>PT30S</timeDuration> <!-- 30 seconds timeout -->
    </timerEventDefinition>
</boundaryEvent>
```

## 🔄 **Timeout Workflow Behavior**

### Order Creation Timeout
- Triggered if order service doesn't respond within 30 seconds
- Routes to cancel order compensation flow
- Sets `orderCreationTimedOut` process variable

### Inventory Reservation Timeout
- Triggered if inventory service doesn't respond within 30 seconds
- Routes to cancel order compensation flow
- Sets `inventoryTimedOut` process variable

### Payment Processing Timeout
- Triggered if payment service doesn't respond within 30 seconds
- Routes to inventory compensation flow, then cancel order
- Sets `paymentTimedOut` process variable

### Shipment Creation Timeout
- Triggered if shipping service doesn't respond within 30 seconds
- Marks process as completed (no compensation needed)
- Sets `shipmentTimedOut` process variable

## 🛠️ **Customization Options**

### Adjusting Timeout Durations
To change timeout durations, modify the `<timeDuration>` elements in the BPMN:

```xml
<timeDuration>PT60S</timeDuration> <!-- 60 seconds -->
<timeDuration>PT5M</timeDuration>  <!-- 5 minutes -->
<timeDuration>PT1H</timeDuration>  <!-- 1 hour -->
```

### Adding New Timeout Handlers
To add timeout handling for new services:

1. Add a timer boundary event to the appropriate event-based gateway
2. Create a new delegate class extending `JavaDelegate`
3. Add the delegate to the Spring context with `@Component`
4. Connect the timeout flow to the appropriate compensation action

## 📊 **Monitoring Timeouts**

Timeout events can be monitored through:

1. **Application Logs**: WARN level messages indicate timeouts
2. **Process Variables**: Check for timeout indicator variables
3. **Flowable REST API**: Query process instances and their variables

```bash
# Get process variables to check for timeouts
curl http://localhost:8080/flowable-rest/process-api/runtime/process-instances/PROCESS_INSTANCE_ID/variables
```

## 🧪 **Testing Timeout Scenarios**

To test timeout handling:

1. **Temporarily disable a service** to simulate timeout
2. **Add artificial delays** in service processing (>30 seconds)
3. **Monitor logs** for timeout warnings
4. **Verify compensation flows** are triggered correctly

## 🔧 **Extending Timeout Functionality**

Future enhancements could include:

1. **Dynamic timeout configuration** based on business rules
2. **Retry mechanisms** before triggering compensation
3. **Notification systems** for timeout events
4. **Metrics collection** for timeout analysis