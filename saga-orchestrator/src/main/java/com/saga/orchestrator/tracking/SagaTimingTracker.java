package com.saga.orchestrator.tracking;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Tracks saga orchestration timing from initiation to completion.
 * Logs timing data to a file for analysis and reporting.
 */
@Slf4j
@Component
public class SagaTimingTracker {

    private static final String LOG_DIR = "saga-timing-logs";
    private static final String LOG_FILE = "saga-timing.log";
    private static final String REPORT_FILE = "saga-timing-report.txt";
    private static final String DETAILED_REPORT_FILE = "saga-detailed-report.txt";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    // Thread-safe map to store saga records by correlationId
    private final Map<String, SagaTimingRecord> activeSagas = new ConcurrentHashMap<>();
    
    // Completed saga records for detailed reporting
    private final Map<String, SagaTimingRecord> completedSagas = new ConcurrentHashMap<>();

    private PrintWriter logWriter;
    private Path logFilePath;
    private Path reportFilePath;
    private Path detailedReportFilePath;

    @PostConstruct
    public void init() {
        try {
            Path logDir = Paths.get(LOG_DIR);
            if (!Files.exists(logDir)) {
                Files.createDirectories(logDir);
            }
            
            logFilePath = logDir.resolve(LOG_FILE);
            reportFilePath = logDir.resolve(REPORT_FILE);
            detailedReportFilePath = logDir.resolve(DETAILED_REPORT_FILE);
            
            // Append mode for continuous logging
            logWriter = new PrintWriter(new BufferedWriter(new FileWriter(logFilePath.toFile(), true)), true);
            
            logWriter.println("=".repeat(100));
            logWriter.println("SAGA TIMING TRACKER INITIALIZED - " + formatTimestamp(System.currentTimeMillis()));
            logWriter.println("=".repeat(100));
            
            log.info("SagaTimingTracker initialized. Log file: {}", logFilePath.toAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to initialize SagaTimingTracker log file", e);
        }
    }

    @PreDestroy
    public void cleanup() {
        if (logWriter != null) {
            generateDetailedReport();
            logWriter.println("=".repeat(100));
            logWriter.println("SAGA TIMING TRACKER SHUTDOWN - " + formatTimestamp(System.currentTimeMillis()));
            logWriter.println("=".repeat(100));
            logWriter.close();
        }
    }

    /**
     * Records the start of a saga orchestration.
     */
    public void recordSagaStart(String correlationId, String customerId) {
        long startTime = System.currentTimeMillis();
        SagaTimingRecord record = new SagaTimingRecord(correlationId, customerId, startTime);
        activeSagas.put(correlationId, record);
        
        String logEntry = String.format(
            "[START] CorrelationId: %s | CustomerId: %s | StartTime: %s | Timestamp: %d",
            correlationId, customerId, formatTimestamp(startTime), startTime
        );
        
        writeToLog(logEntry);
        log.debug("Saga timing started: {}", correlationId);
    }

    /**
     * Records an intermediate event in the saga.
     */
    public void recordSagaEvent(String correlationId, String eventName, boolean isSuccess) {
        long eventTime = System.currentTimeMillis();
        SagaTimingRecord record = activeSagas.get(correlationId);
        
        String logEntry;
        if (record != null) {
            long elapsedMs = eventTime - record.getStartTime();
            
            // Add event to the record's event list
            record.addEvent(new SagaEvent(eventName, eventTime, isSuccess, elapsedMs));
            
            logEntry = String.format(
                "[EVENT] CorrelationId: %s | Event: %s | Success: %s | ElapsedMs: %d | Timestamp: %s",
                correlationId, eventName, isSuccess, elapsedMs, formatTimestamp(eventTime)
            );
        } else {
            logEntry = String.format(
                "[EVENT] CorrelationId: %s | Event: %s | Success: %s | Timestamp: %s | (No start record found)",
                correlationId, eventName, isSuccess, formatTimestamp(eventTime)
            );
        }
        
        writeToLog(logEntry);
    }

    /**
     * Records the completion of a saga (when shipment created event is received).
     */
    public void recordSagaCompletion(String correlationId, boolean isSuccess) {
        long endTime = System.currentTimeMillis();
        SagaTimingRecord record = activeSagas.remove(correlationId);
        
        if (record != null) {
            long durationMs = endTime - record.getStartTime();
            record.setEndTime(endTime);
            record.setTotalDurationMs(durationMs);
            record.setSuccess(isSuccess);
            
            // Add completion event
            record.addEvent(new SagaEvent("SHIPMENT_CREATED", endTime, isSuccess, durationMs));
            
            // Store completed saga for detailed reporting
            completedSagas.put(correlationId, record);
            
            String logEntry = String.format(
                "[COMPLETE] CorrelationId: %s | CustomerId: %s | Success: %s | DurationMs: %d | StartTime: %s | EndTime: %s",
                correlationId, record.getCustomerId(), isSuccess, durationMs,
                formatTimestamp(record.getStartTime()), formatTimestamp(endTime)
            );
            
            writeToLog(logEntry);
            
            log.info("Saga completed - CorrelationId: {}, Duration: {}ms", correlationId, durationMs);
        } else {
            String logEntry = String.format(
                "[COMPLETE] CorrelationId: %s | Success: %s | EndTime: %s | (No start record found - cannot calculate duration)",
                correlationId, isSuccess, formatTimestamp(endTime)
            );
            writeToLog(logEntry);
            log.warn("Saga completion recorded without start time: {}", correlationId);
        }
    }

    /**
     * Generates a detailed report for each correlationId plus summary.
     */
    public String generateDetailedReport() {
        StringBuilder report = new StringBuilder();
        
        report.append("\n");
        report.append("╔").append("═".repeat(98)).append("╗\n");
        report.append("║").append(centerText("SAGA ORCHESTRATION DETAILED TIMING REPORT", 98)).append("║\n");
        report.append("║").append(centerText("Generated: " + formatTimestamp(System.currentTimeMillis()), 98)).append("║\n");
        report.append("╚").append("═".repeat(98)).append("╝\n");

        if (completedSagas.isEmpty()) {
            report.append("\nNo completed sagas to report.\n");
            writeReportToFile(report.toString());
            return report.toString();
        }

        // ═══════════════════════════════════════════════════════════════════════════════
        // SECTION 1: Individual Saga Details
        // ═══════════════════════════════════════════════════════════════════════════════
        report.append("\n");
        report.append("┌").append("─".repeat(98)).append("┐\n");
        report.append("│").append(centerText("INDIVIDUAL SAGA DETAILS", 98)).append("│\n");
        report.append("└").append("─".repeat(98)).append("┘\n");

        int sagaNumber = 1;
        for (Map.Entry<String, SagaTimingRecord> entry : completedSagas.entrySet()) {
            SagaTimingRecord record = entry.getValue();
            report.append(generateSingleSagaReport(sagaNumber++, record));
        }

        // ═══════════════════════════════════════════════════════════════════════════════
        // SECTION 2: Summary Statistics
        // ═══════════════════════════════════════════════════════════════════════════════
        report.append(generateSummarySection());

        String reportStr = report.toString();
        writeToLog(reportStr);
        writeReportToFile(reportStr);

        return reportStr;
    }

    /**
     * Generates detailed report for a single saga.
     */
    private String generateSingleSagaReport(int number, SagaTimingRecord record) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("\n");
        sb.append("┏").append("━".repeat(98)).append("┓\n");
        sb.append("┃  SAGA #").append(number).append(String.format(" %-88s┃%n", ""));
        sb.append("┣").append("━".repeat(98)).append("┫\n");
        
        sb.append(String.format("┃  %-20s: %-74s┃%n", "CorrelationId", record.getCorrelationId()));
        sb.append(String.format("┃  %-20s: %-74s┃%n", "CustomerId", record.getCustomerId()));
        sb.append(String.format("┃  %-20s: %-74s┃%n", "Status", record.isSuccess() ? "✓ SUCCESS" : "✗ FAILED"));
        sb.append(String.format("┃  %-20s: %-74s┃%n", "Start Time", formatTimestamp(record.getStartTime())));
        sb.append(String.format("┃  %-20s: %-74s┃%n", "End Time", formatTimestamp(record.getEndTime())));
        sb.append(String.format("┃  %-20s: %-74s┃%n", "Total Duration", 
            String.format("%d ms (%.3f seconds)", record.getTotalDurationMs(), record.getTotalDurationMs() / 1000.0)));
        
        sb.append("┣").append("━".repeat(98)).append("┫\n");
        sb.append("┃  EVENT TIMELINE:").append(String.format("%-80s┃%n", ""));
        sb.append("┣").append("─".repeat(98)).append("┫\n");
        sb.append(String.format("┃  %-5s │ %-25s │ %-10s │ %-15s │ %-25s ┃%n", 
            "Step", "Event", "Status", "Elapsed (ms)", "Timestamp"));
        sb.append("┣").append("─".repeat(98)).append("┫\n");
        
        // Add START event
        sb.append(String.format("┃  %-5s │ %-25s │ %-10s │ %-15s │ %-25s ┃%n",
            "0", "SAGA_INITIATED", "✓", "0", formatTimestamp(record.getStartTime())));
        
        int step = 1;
        long previousElapsed = 0;
        for (SagaEvent event : record.getEvents()) {
            String statusIcon = event.isSuccess() ? "✓" : "✗";
            long stepDuration = event.getElapsedFromStart() - previousElapsed;
            sb.append(String.format("┃  %-5s │ %-25s │ %-10s │ %-15s │ %-25s ┃%n",
                step++,
                event.getEventName(),
                statusIcon,
                String.format("%d (+%d)", event.getElapsedFromStart(), stepDuration),
                formatTimestamp(event.getTimestamp())
            ));
            previousElapsed = event.getElapsedFromStart();
        }
        
        sb.append("┗").append("━".repeat(98)).append("┛\n");
        
        return sb.toString();
    }

    /**
     * Generates summary statistics section.
     */
    private String generateSummarySection() {
        StringBuilder sb = new StringBuilder();
        
        List<Long> durations = completedSagas.values().stream()
            .map(SagaTimingRecord::getTotalDurationMs)
            .sorted()
            .collect(Collectors.toList());

        DoubleSummaryStatistics stats = durations.stream()
            .mapToDouble(Long::doubleValue)
            .summaryStatistics();

        long successCount = completedSagas.values().stream().filter(SagaTimingRecord::isSuccess).count();
        long failureCount = completedSagas.size() - successCount;

        long median = durations.get(durations.size() / 2);
        int p90Index = (int) (durations.size() * 0.90);
        int p95Index = (int) (durations.size() * 0.95);
        int p99Index = (int) (durations.size() * 0.99);
        long p90 = durations.get(Math.min(p90Index, durations.size() - 1));
        long p95 = durations.get(Math.min(p95Index, durations.size() - 1));
        long p99 = durations.get(Math.min(p99Index, durations.size() - 1));

        sb.append("\n");
        sb.append("╔").append("═".repeat(98)).append("╗\n");
        sb.append("║").append(centerText("SUMMARY STATISTICS", 98)).append("║\n");
        sb.append("╠").append("═".repeat(98)).append("╣\n");
        
        sb.append(String.format("║  %-35s │ %-58s║%n", "Total Sagas Completed", completedSagas.size()));
        sb.append(String.format("║  %-35s │ %-58s║%n", "Successful", successCount + " (" + String.format("%.1f%%", (successCount * 100.0 / completedSagas.size())) + ")"));
        sb.append(String.format("║  %-35s │ %-58s║%n", "Failed", failureCount + " (" + String.format("%.1f%%", (failureCount * 100.0 / completedSagas.size())) + ")"));
        
        sb.append("╠").append("─".repeat(98)).append("╣\n");
        sb.append("║").append(centerText("DURATION METRICS", 98)).append("║\n");
        sb.append("╠").append("─".repeat(98)).append("╣\n");
        
        sb.append(String.format("║  %-35s │ %10.0f ms  │  %10.3f seconds %16s║%n", "Minimum Duration", stats.getMin(), stats.getMin() / 1000.0, ""));
        sb.append(String.format("║  %-35s │ %10.0f ms  │  %10.3f seconds %16s║%n", "Maximum Duration", stats.getMax(), stats.getMax() / 1000.0, ""));
        sb.append(String.format("║  %-35s │ %10.2f ms  │  %10.3f seconds %16s║%n", "Average Duration", stats.getAverage(), stats.getAverage() / 1000.0, ""));
        sb.append(String.format("║  %-35s │ %10d ms  │  %10.3f seconds %16s║%n", "Median Duration", median, median / 1000.0, ""));
        
        sb.append("╠").append("─".repeat(98)).append("╣\n");
        sb.append("║").append(centerText("PERCENTILES", 98)).append("║\n");
        sb.append("╠").append("─".repeat(98)).append("╣\n");
        
        sb.append(String.format("║  %-35s │ %10d ms  │  %10.3f seconds %16s║%n", "90th Percentile (P90)", p90, p90 / 1000.0, ""));
        sb.append(String.format("║  %-35s │ %10d ms  │  %10.3f seconds %16s║%n", "95th Percentile (P95)", p95, p95 / 1000.0, ""));
        sb.append(String.format("║  %-35s │ %10d ms  │  %10.3f seconds %16s║%n", "99th Percentile (P99)", p99, p99 / 1000.0, ""));
        
        sb.append("╠").append("─".repeat(98)).append("╣\n");
        sb.append("║").append(centerText("THROUGHPUT", 98)).append("║\n");
        sb.append("╠").append("─".repeat(98)).append("╣\n");
        
        double avgDurationSec = stats.getAverage() / 1000.0;
        double theoreticalThroughput = avgDurationSec > 0 ? 1.0 / avgDurationSec : 0;
        sb.append(String.format("║  %-35s │ %-58s║%n", "Avg Throughput (single thread)", String.format("%.2f sagas/second", theoreticalThroughput)));
        
        sb.append("╚").append("═".repeat(98)).append("╝\n");
        
        return sb.toString();
    }

    private void writeReportToFile(String report) {
        try (PrintWriter reportWriter = new PrintWriter(new FileWriter(detailedReportFilePath.toFile()))) {
            reportWriter.print(report);
            log.info("Detailed report written to: {}", detailedReportFilePath.toAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to write detailed report file", e);
        }
    }

    private String centerText(String text, int width) {
        int padding = (width - text.length()) / 2;
        return String.format("%" + padding + "s%s%" + (width - padding - text.length()) + "s", "", text, "");
    }

    /**
     * Generates a quick summary report.
     */
    public String generateSummaryReport() {
        return generateDetailedReport();
    }

    private synchronized void writeToLog(String entry) {
        if (logWriter != null) {
            logWriter.println(entry);
            logWriter.flush();
        }
    }

    private String formatTimestamp(long millis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())
            .format(TIMESTAMP_FORMAT);
    }

    /**
     * Returns the number of currently active (in-progress) sagas.
     */
    public int getActiveSagaCount() {
        return activeSagas.size();
    }

    /**
     * Returns the number of completed sagas.
     */
    public int getCompletedSagaCount() {
        return completedSagas.size();
    }

    /**
     * Gets the log file path.
     */
    public String getLogFilePath() {
        return logFilePath != null ? logFilePath.toAbsolutePath().toString() : "Not initialized";
    }

    /**
     * Gets the detailed report file path.
     */
    public String getDetailedReportFilePath() {
        return detailedReportFilePath != null ? detailedReportFilePath.toAbsolutePath().toString() : "Not initialized";
    }

    /**
     * Inner class to hold saga timing record with all events.
     */
    private static class SagaTimingRecord {
        private final String correlationId;
        private final String customerId;
        private final long startTime;
        private long endTime;
        private long totalDurationMs;
        private boolean success;
        private final List<SagaEvent> events = new ArrayList<>();

        public SagaTimingRecord(String correlationId, String customerId, long startTime) {
            this.correlationId = correlationId;
            this.customerId = customerId;
            this.startTime = startTime;
        }

        public String getCorrelationId() { return correlationId; }
        public String getCustomerId() { return customerId; }
        public long getStartTime() { return startTime; }
        public long getEndTime() { return endTime; }
        public void setEndTime(long endTime) { this.endTime = endTime; }
        public long getTotalDurationMs() { return totalDurationMs; }
        public void setTotalDurationMs(long totalDurationMs) { this.totalDurationMs = totalDurationMs; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public List<SagaEvent> getEvents() { return events; }
        public void addEvent(SagaEvent event) { events.add(event); }
    }

    /**
     * Inner class to hold individual saga event.
     */
    private static class SagaEvent {
        private final String eventName;
        private final long timestamp;
        private final boolean success;
        private final long elapsedFromStart;

        public SagaEvent(String eventName, long timestamp, boolean success, long elapsedFromStart) {
            this.eventName = eventName;
            this.timestamp = timestamp;
            this.success = success;
            this.elapsedFromStart = elapsedFromStart;
        }

        public String getEventName() { return eventName; }
        public long getTimestamp() { return timestamp; }
        public boolean isSuccess() { return success; }
        public long getElapsedFromStart() { return elapsedFromStart; }
    }

    public void clearAllData() {
        int activeCount = activeSagas.size();
        int completedCount = completedSagas.size();
        
        activeSagas.clear();
        completedSagas.clear();
        
        String logEntry = String.format(
            "[CLEAR] All saga timing data cleared at %s | Cleared: %d active, %d completed sagas",
            formatTimestamp(System.currentTimeMillis()), activeCount, completedCount
        );
        writeToLog(logEntry);
        
        log.info("Saga timing data cleared: {} active, {} completed sagas removed", activeCount, completedCount);
    }
}
