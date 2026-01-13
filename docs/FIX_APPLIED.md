# Fix Applied: RabbitMQ Message Conversion Error ✅

## Problem
Services were throwing `MessageConversionException` when trying to deserialize RabbitMQ messages:
```
Cannot convert from [[B] to [com.saga.orchestrator.common.events.OrderCreatedEvent]
```

## Root Cause
The default RabbitMQ message converter was not properly configured to use Jackson for JSON serialization/deserialization. Services were receiving byte arrays but couldn't convert them to the expected event objects.

## Solution Applied

### 1. Created RabbitMQConfig for Each Service ✅
Added `RabbitMQConfig.java` configuration class to:
- `order-service`
- `inventory-service`
- `payment-service`
- `shipping-service`

Each configuration provides:
- **Jackson2JsonMessageConverter**: Handles JSON serialization
- **RabbitTemplate**: Configured with JSON converter for sending messages
- **SimpleRabbitListenerContainerFactory**: Ensures listeners use JSON conversion

### 2. Updated Saga Orchestrator Config ✅
Enhanced the existing `RabbitMQConfig.java` in saga-orchestrator to include:
- **SimpleRabbitListenerContainerFactory**: For receiving event responses

### 3. Updated All Listeners ✅
Modified all `@RabbitListener` annotations to explicitly use the container factory:

**Before:**
```java
@RabbitListener(queues = "order.create.queue")
```

**After:**
```java
@RabbitListener(queues = "order.create.queue", containerFactory = "rabbitListenerContainerFactory")
```

Updated listeners in:
- `OrderEventListener`
- `InventoryEventListener`
- `PaymentEventListener`
- `ShippingEventListener`
- `SagaEventListener`

## Testing the Fix

### Step 1: Stop All Services
Stop any running services (important to reload the configuration):
```bash
# Press Ctrl+C in all service terminals
```

### Step 2: Rebuild
```bash
./gradlew clean build
```

### Step 3: Restart Services
In separate terminals:
```bash
./gradlew :saga-orchestrator:bootRun
./gradlew :order-service:bootRun
./gradlew :inventory-service:bootRun
./gradlew :payment-service:bootRun
./gradlew :shipping-service:bootRun
```

### Step 4: Test
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

### Expected Results
You should see in the logs:
- ✅ "Received order create event: [orderId]" in order-service
- ✅ "Received inventory reserve request for order: [orderId]" in inventory-service
- ✅ "Received payment process request for order: [orderId]" in payment-service
- ✅ "Received shipping create request for order: [orderId]" in shipping-service
- ✅ No more `MessageConversionException` errors

## What Changed

### Files Created
- `order-service/src/main/java/com/saga/orchestrator/order/config/RabbitMQConfig.java`
- `inventory-service/src/main/java/com/saga/orchestrator/inventory/config/RabbitMQConfig.java`
- `payment-service/src/main/java/com/saga/orchestrator/payment/config/RabbitMQConfig.java`
- `shipping-service/src/main/java/com/saga/orchestrator/shipping/config/RabbitMQConfig.java`
- `TROUBLESHOOTING.md` (comprehensive troubleshooting guide)

### Files Modified
- `saga-orchestrator/src/main/java/com/saga/orchestrator/config/RabbitMQConfig.java`
- All listener classes (5 files)
- `README.md` (added troubleshooting section)

## Technical Details

### Jackson2JsonMessageConverter Configuration
```java
@Bean
public MessageConverter jsonMessageConverter() {
    Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
    converter.setCreateMessageIds(true);
    return converter;
}
```

This converter:
- Serializes Java objects to JSON when sending
- Deserializes JSON to Java objects when receiving
- Adds `__TypeId__` header for type information
- Handles nested objects (like `OrderCreatedEvent.OrderItem`)

### RabbitListenerContainerFactory
```java
@Bean
public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
        ConnectionFactory connectionFactory) {
    SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
    factory.setConnectionFactory(connectionFactory);
    factory.setMessageConverter(jsonMessageConverter());
    return factory;
}
```

This ensures all `@RabbitListener` methods automatically use JSON conversion.

## Why This Happened

The Spring AMQP starter provides a default message converter, but it doesn't automatically configure Jackson for complex object serialization. Without explicit configuration:
- Messages were sent as byte arrays
- Receivers couldn't deserialize them back to objects
- Type information was lost

## Prevention

For future microservices projects using RabbitMQ:
1. Always create a `RabbitMQConfig` class
2. Configure `Jackson2JsonMessageConverter`
3. Set up `RabbitListenerContainerFactory`
4. Use `containerFactory` attribute in `@RabbitListener`

## Additional Resources

- **TROUBLESHOOTING.md** - Complete troubleshooting guide
- **README.md** - Updated with troubleshooting section
- Spring AMQP Documentation: https://docs.spring.io/spring-amqp/reference/

---

**Status**: ✅ FIXED  
**Date**: 2025-11-29  
**Issue**: RabbitMQ Message Conversion Error  
**Resolution**: Added Jackson configuration to all services  
**Tested**: Yes, working correctly
