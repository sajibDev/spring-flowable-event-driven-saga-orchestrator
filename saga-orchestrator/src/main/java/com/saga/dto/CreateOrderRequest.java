package com.saga.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

public class CreateOrderRequest implements Serializable {
    private String customerId;
    private List<OrderItemDto> items;
    private String shippingAddress;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemDto implements Serializable {
        private String productId;
        private String productName;
        private Integer quantity;
        private BigDecimal price;
    }
}
