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
public class OrderCreationFailedEvent implements Serializable {
    private String correlationId;
    private String reason;
    private boolean success = false;
}