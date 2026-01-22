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
public class InventoryResponse implements Serializable {
    private String orderId;
    private String reservationId;
    private String inventoryStatus;
    private String message;
}
