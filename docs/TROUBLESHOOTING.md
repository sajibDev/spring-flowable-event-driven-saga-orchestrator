# Troubleshooting Guide

## Common Issues and Solutions

### 1. RabbitMQ Message Conversion Error ✅ FIXED

#### Symptom
```
MessageConversionException: Cannot convert from [[B] to [com.saga.orchestrator.common.events.OrderCreatedEvent]
```

#### Root Cause
Services couldn't deserialize RabbitMQ messages because the default message converter was not properly configured with Jackson for JSON serialization.

#### Solution Implemented
Added `RabbitMQConfig` class to each service module with:
1. **Jackson2JsonMessageConverter** bean for JSON serialization
2. **RabbitTemplate** bean configured with the JSON converter
3. **SimpleRabbitListenerContainerFactory** bean to ensure @RabbitListener uses JSON conversion

Updated all `@RabbitListener` annotations to explicitly use the container factory:
```java
@RabbitListener(queues = "order.create.queue", containerFactory = "rabbitListenerContainerFactory")
```

#### Files Modified
- `order-service/src/main/java/com/saga/orchestrator/order/config/RabbitMQConfig.java` ✅ Created
- `inventory-service/src/main/java/com/saga/orchestrator/inventory/config/RabbitMQConfig.java` ✅ Created
- `payment-service/src/main/java/com/saga/orchestrator/payment/config/RabbitMQConfig.java` ✅ Created
- `shipping-service/src/main/java/com/saga/orchestrator/shipping/config/RabbitMQConfig.java` ✅ Created
- `saga-orchestrator/src/main/java/com/saga/orchestrator/config/RabbitMQConfig.java` ✅ Updated
- All listener classes updated to use `containerFactory`

#### Testing After Fix
1. **Stop all running services** (important!)
2. **Rebuild the project**:
   ```bash
   ./gradlew clean build
   ```
3. **Restart all services**:
   ```bash
   ./gradlew :saga-orchestrator:bootRun
   ./gradlew :order-service:bootRun
   ./gradlew :inventory-service:bootRun
   ./gradlew :payment-service:bootRun
   ./gradlew :shipping-service:bootRun
   ```
4. **Test with sample order**:
   ```bash
   curl -X POST http://localhost:8080/api/orders \
     -H "Content-Type: application/json" \
     -d @sample-order.json
   ```

You should now see proper message processing in all service logs without conversion errors.

---

### 2. Service Can't Connect to PostgreSQL

#### Symptom
```
Connection refused: localhost:5432
```

#### Solution
1. Ensure Docker containers are running:
   ```bash
   docker-compose ps
   ```
2. Check if PostgreSQL is running:
   ```bash
   docker logs saga-orchestrator-db
   docker logs order-db
   docker logs inventory-db
   docker logs payment-db
   docker logs shipping-db
   ```
3. Restart Docker Compose if needed:
   ```bash
   docker-compose down
   docker-compose up -d
   ```

---

### 3. Service Can't Connect to RabbitMQ

#### Symptom
```
Connection refused: localhost:5672
```

#### Solution
1. Check RabbitMQ container:
   ```bash
   docker logs rabbitmq
   ```
2. Verify RabbitMQ is healthy:
   ```bash
   curl http://localhost:15672
   ```
3. Restart if needed:
   ```bash
   docker-compose restart rabbitmq
   ```

---

### 4. Flowable Process Not Starting

#### Symptom
BPMN process doesn't start after REST API call

#### Possible Causes & Solutions

**A. BPMN file not deployed**
- Check logs for: "Deploying process definition"
- Ensure `order-saga-process.bpmn20.xml` is in `src/main/resources/processes/`

**B. Database connection issue**
- Verify saga-orchestrator can connect to PostgreSQL
- Check Flowable tables are created: `ACT_RE_*`, `ACT_RU_*`, etc.

**C. Invalid BPMN syntax**
- Validate BPMN file structure
- Check delegate expressions match bean names

---

### 5. Intermediate Catch Events Not Triggering

#### Symptom
Process gets stuck at event-based gateway

#### Possible Causes & Solutions

**A. Message not being published**
- Check service logs to confirm event publication
- Verify RabbitMQ has the message in the queue

**B. Message event subscription not created**
- Check Flowable Admin UI → Jobs → Event Subscriptions
- Should see subscriptions for:
  - `inventoryReservedSuccess`
  - `inventoryReservedFailure`
  - `paymentProcessedSuccess`
  - `paymentProcessedFailure`
  - `shipmentCreatedSuccess`

**C. Incorrect message name in BPMN**
- Verify message names in BPMN match event listener code
- Check `messageEventReceived()` calls use correct message names

---

### 6. Process Variables Not Being Set

#### Symptom
Variables like `reservationId` or `transactionId` are null

#### Solution
- Check delegate classes are setting variables correctly:
  ```java
  runtimeService.setVariable(execution.getId(), "reservationId", value);
  ```
- Verify variable names match between delegates and BPMN
- Check Flowable Admin UI → Variables tab to inspect current values

---

### 7. Compensation Not Executing

#### Symptom
When payment fails, inventory is not released

#### Solution
- Verify failure path is being taken in BPMN diagram
- Check `compensateInventoryTask` is being reached
- Confirm compensation event is being published to correct queue
- Verify inventory service is listening to `inventory.release.queue`

---

### 8. Multiple Instances of Same Message

#### Symptom
Service processes the same message multiple times

#### Solution
- Check for multiple listener instances
- Verify only one instance of each service is running
- Ensure queue names are unique and correctly bound

---

### 9. Gradle Build Fails

#### Symptom
```
Could not resolve com.saga.orchestrator:common-events
```

#### Solution
1. Build common-events first:
   ```bash
   ./gradlew :common-events:build
   ```
2. Then build all:
   ```bash
   ./gradlew clean build
   ```

---

### 10. Port Already in Use

#### Symptom
```
Port 8080 already in use
```

#### Solution
1. Find process using the port:
   ```bash
   lsof -i :8080
   ```
2. Kill the process:
   ```bash
   kill -9 <PID>
   ```
3. Or change port in `application.yml`

---

## Debugging Tips

### 1. Enable DEBUG Logging
Add to `application.yml`:
```yaml
logging:
  level:
    com.saga.orchestrator: DEBUG
    org.flowable: DEBUG
    org.springframework.amqp: DEBUG
```

### 2. Monitor RabbitMQ Queues
Open RabbitMQ Management UI: http://localhost:15672
- Check queue depths
- Verify message flow
- View message payloads

### 3. Use Flowable Admin UI
Open Flowable Admin: http://localhost:8080/flowable-admin
- Monitor process instances
- Check process variables
- View execution history
- Inspect event subscriptions

### 4. Check Service Logs
Look for these key log messages:
- ✅ "Received [event] for order: [orderId]"
- ✅ "Published [event] for order: [orderId]"
- ❌ Exceptions or stack traces

### 5. Verify Message Format
Check RabbitMQ message payload:
- Should be valid JSON
- Should have `__TypeId__` header
- Should match event class structure

---

## Quick Checklist for Testing

Before testing, ensure:
- [ ] Docker Compose is running (PostgreSQL + RabbitMQ)
- [ ] All 5 services are started and running
- [ ] No errors in any service logs
- [ ] RabbitMQ queues are created (check Management UI)
- [ ] Flowable tables exist in saga_orchestrator_db

To test:
- [ ] Create order via REST API
- [ ] Check saga-orchestrator logs for process start
- [ ] Check order-service logs for order creation
- [ ] Check inventory-service logs for reservation
- [ ] Check payment-service logs for payment
- [ ] Check shipping-service logs for shipment
- [ ] View in Flowable Admin UI

---

## Getting Help

If you encounter an issue not covered here:

1. **Check service logs** for error messages
2. **Review Flowable Admin UI** for process state
3. **Inspect RabbitMQ** for message flow
4. **Verify database connections** for all services
5. **Rebuild and restart** services if configuration changed

---

**Last Updated**: 2025-11-29  
**Issue Resolved**: RabbitMQ Message Conversion Error ✅
