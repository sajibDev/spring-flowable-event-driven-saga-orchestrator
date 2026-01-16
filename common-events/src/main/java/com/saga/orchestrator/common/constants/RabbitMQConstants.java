package com.saga.orchestrator.common.constants;

public class RabbitMQConstants {
    // Exchange names
    public static final String ORDER_EXCHANGE = "order.exchange";
    
    // Queue names for commands
    public static final String ORDER_CREATE_COMMAND_QUEUE = "order.create.command.queue";
    public static final String ORDER_CANCEL_COMMAND_QUEUE = "order.cancel.command.queue";
    public static final String INVENTORY_RESERVE_COMMAND_QUEUE = "inventory.reserve.command.queue";
    public static final String INVENTORY_RELEASE_COMMAND_QUEUE = "inventory.release.command.queue";
    public static final String PAYMENT_PROCESS_COMMAND_QUEUE = "payment.process.command.queue";
    public static final String PAYMENT_REFUND_COMMAND_QUEUE = "payment.refund.command.queue";
    public static final String SHIPPING_CREATE_COMMAND_QUEUE = "shipping.create.command.queue";

    
    // Queue names for events (saga responses)
    public static final String ORDER_CREATED_EVENT_QUEUE = "order.created.event.queue";
    public static final String INVENTORY_RESERVED_EVENT_QUEUE = "inventory.reserved.event.queue";
    public static final String INVENTORY_COMPENSATED_EVENT_QUEUE = "inventory.compensated.event.queue";
    public static final String PAYMENT_PROCESSED_EVENT_QUEUE = "payment.processed.event.queue";
    public static final String PAYMENT_REFUNDED_EVENT_QUEUE = "payment.refunded.event.queue";
    public static final String SHIPMENT_CREATED_EVENT_QUEUE = "shipment.created.event.queue";
    
    // Routing keys
    public static final String ORDER_CREATE_ROUTING_KEY = "order.create";
    public static final String ORDER_CREATED_ROUTING_KEY = "order.created";
    public static final String ORDER_CANCEL_ROUTING_KEY = "order.cancel";
    public static final String ORDER_CANCELLED_ROUTING_KEY = "order.cancelled";
    public static final String INVENTORY_RESERVE_ROUTING_KEY = "inventory.reserve";
    public static final String INVENTORY_RELEASE_ROUTING_KEY = "inventory.release";
    public static final String INVENTORY_RESERVED_ROUTING_KEY = "inventory.reserved";
    public static final String INVENTORY_COMPENSATED_ROUTING_KEY = "inventory.compensated";
    public static final String PAYMENT_PROCESS_ROUTING_KEY = "payment.process";
    public static final String PAYMENT_REFUND_ROUTING_KEY = "payment.refund";
    public static final String PAYMENT_PROCESSED_ROUTING_KEY = "payment.processed";
    public static final String PAYMENT_REFUNDED_ROUTING_KEY = "payment.refunded";
    public static final String SHIPPING_CREATE_ROUTING_KEY = "shipping.create";
    public static final String SHIPMENT_CREATED_ROUTING_KEY = "shipment.created";
}