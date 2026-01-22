package com.saga.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PaymentResponse implements Serializable {
    private String orderId;
    private String transactionId;
    private String paymentStatus;
    private String message;
}
