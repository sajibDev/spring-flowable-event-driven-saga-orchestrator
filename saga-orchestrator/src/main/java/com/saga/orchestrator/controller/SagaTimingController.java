package com.saga.orchestrator.controller;

import com.saga.orchestrator.tracking.SagaTimingTracker;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for accessing saga timing reports.
 */
@RestController
@RequestMapping("/api/saga-timing")
@RequiredArgsConstructor
public class SagaTimingController {

    private final SagaTimingTracker sagaTimingTracker;

    /**
     * Get the current saga timing summary report.
     */
    @GetMapping(value = "/report", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getTimingReport() {
        String report = sagaTimingTracker.generateSummaryReport();
        return ResponseEntity.ok(report);
    }

    /**
     * Get saga timing statistics as JSON.
     */
    @GetMapping(value = "/stats", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getTimingStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("activeSagas", sagaTimingTracker.getActiveSagaCount());
        stats.put("completedSagas", sagaTimingTracker.getCompletedSagaCount());
        stats.put("logFilePath", sagaTimingTracker.getLogFilePath());
        return ResponseEntity.ok(stats);
    }
}
