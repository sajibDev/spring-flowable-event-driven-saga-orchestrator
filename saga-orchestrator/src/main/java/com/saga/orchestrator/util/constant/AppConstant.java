package com.saga.orchestrator.util.constant;

public class AppConstant {

  // Process Definition Keys
  public static final String CREATE_ORDER_SAGA_PROCESS_DEFINITION_KEY = "orderSagaProcess";

  // Process Variable Names
  public static final String VAR_CORRELATION_ID = "correlationId";
  public static final String VAR_ORDER_ID = "orderId";
  public static final String VAR_CUSTOMER_ID = "customerId";
  public static final String VAR_TOTAL_AMOUNT = "totalAmount";
  public static final String VAR_SHIPPING_ADDRESS = "shippingAddress";
  public static final String VAR_RESERVATION_ID = "reservationId";
  public static final String VAR_TRANSACTION_ID = "transactionId";
  public static final String VAR_SHIPMENT_ID = "shipmentId";
  public static final String VAR_FAILURE_REASON = "failureReason";
  public static final String VAR_ORDER_CREATION_TIMED_OUT = "orderCreationTimedOut";
  public static final String VAR_INVENTORY_TIMED_OUT = "inventoryTimedOut";
  public static final String VAR_PAYMENT_TIMED_OUT = "paymentTimedOut";
  public static final String VAR_SHIPMENT_TIMED_OUT = "shipmentTimedOut";
  public static final String VAR_CREATE_ORDER_REQUEST = "createOrderRequest";

  // Event Names (Message Event Names in BPMN)
  public static final String EVENT_ORDER_CREATED = "orderCreated";
  public static final String EVENT_ORDER_CREATION_FAILED = "orderCreationFailed";
  public static final String EVENT_INVENTORY_RESERVED_SUCCESS = "inventoryReservedSuccess";
  public static final String EVENT_INVENTORY_RESERVED_FAILURE = "inventoryReservedFailure";
  public static final String EVENT_PAYMENT_PROCESSED_SUCCESS = "paymentProcessedSuccess";
  public static final String EVENT_PAYMENT_PROCESSED_FAILURE = "paymentProcessedFailure";
  public static final String EVENT_SHIPMENT_CREATED_SUCCESS = "shipmentCreatedSuccess";

  // Process Activity IDs (Element IDs in BPMN)
  public static final String ACTIVITY_ORDER_CREATED_EVENT = "evOrderCreated";
  public static final String ACTIVITY_ORDER_CREATION_FAILED_EVENT = "evOrderCreationFailed";
  public static final String ACTIVITY_INVENTORY_RESERVED_SUCCESS = "evInventoryReservedSuccess";
  public static final String ACTIVITY_INVENTORY_RESERVED_FAILURE = "evInventoryReservedFailure";
  public static final String ACTIVITY_PAYMENT_PROCESSED_SUCCESS = "evPaymentProcessedSuccess";
  public static final String ACTIVITY_PAYMENT_PROCESSED_FAILURE = "evPaymentProcessedFailure";
  public static final String ACTIVITY_SHIPMENT_CREATED_SUCCESS = "evShipmentCreatedSuccess";
}