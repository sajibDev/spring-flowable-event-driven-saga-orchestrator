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
public class ShippingResponse implements Serializable {
    private String orderId;
    private String shipmentId;
    private String shippingStatus;
    private String message;
}
