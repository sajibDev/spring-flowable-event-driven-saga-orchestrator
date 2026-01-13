package com.saga.orchestrator.controller;

import com.saga.orchestrator.dto.CreateOrderRequest;
import com.saga.orchestrator.dto.OrderResponse;
import com.saga.orchestrator.service.OrderSagaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderSagaService orderSagaService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody CreateOrderRequest request) {
        log.info("Received order creation request for customer: {}", request.getCustomerId());
        OrderResponse response = orderSagaService.initiateOrderSaga(request);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{orderId}/status")
    public ResponseEntity<String> getOrderStatus(@PathVariable String orderId) {
        String status = orderSagaService.getOrderStatus(orderId);
        return ResponseEntity.ok(status);
    }
}
