package com.saga.logging;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Initializes the logging directory and files on application startup.
 */
@Component
public class LoggingInitializer {

    private static final Logger logger = LoggerFactory.getLogger(LoggingInitializer.class);
    private static final Logger workflowLogger = LoggerFactory.getLogger("WORKFLOW_TIMESTAMP");

    @PostConstruct
    public void init() {
        try {
            // Create logs directory if it doesn't exist
            Path logsDir = Paths.get("./logs");
            if (!Files.exists(logsDir)) {
                Files.createDirectories(logsDir);
                logger.info("Created logs directory: {}", logsDir.toAbsolutePath());
            }

            // Initialize workflow timestamp log file with a startup message
            workflowLogger.info("[SYSTEM_STARTUP] Workflow timestamp logging initialized");
            logger.info("Workflow timestamp log file initialized at: {}/workflow-timestamps.log",
                logsDir.toAbsolutePath());

        } catch (IOException e) {
            logger.error("Failed to initialize logging directory", e);
        }
    }
}

