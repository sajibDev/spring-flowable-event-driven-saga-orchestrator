package com.saga.orchestrator.common.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryCompensatedEvent implements Serializable {
    private String orderId;
    private String reservationId;
}
