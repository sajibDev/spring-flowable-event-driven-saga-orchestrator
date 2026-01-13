package com.saga.orchestrator.common.events;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReserveInventoryCommand implements Serializable {
    private String correlationId;
    private String orderId;
    private String customerId;
    private List<String> productIdList;
    private String productName;
    private Integer quantity;
}