package com.saga.exceptions;

public class OrderFailedException extends SagaException{
    public OrderFailedException(String orderId, String reason) {
        super(orderId, reason);
    }
}
