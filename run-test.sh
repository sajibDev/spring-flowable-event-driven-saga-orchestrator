#!/bin/bash

# Simple Load Test & Analysis Automation
# 1. Clear previous saga data
# 2. Run k6 load test


set -e

# Configuration
CONTAINER_NAME="saga-orchestrator"

echo "=========================================="
echo "  Saga Load Test"
echo "=========================================="
echo ""

# Step 1: Clear previous saga log file from Docker
echo "[1/5] Clearing previous saga log file from container..."
docker exec ${CONTAINER_NAME} sh -c "rm -f /app/saga-timing-logs/saga-timing.log && mkdir -p /app/saga-timing-logs"
echo "✓ Previous log file cleared"
echo ""

# Step 2: Run k6 load test
echo "[2/5] Running k6 load test..."
echo ""
k6 run load-test.js
echo ""
echo "✓ Load test completed"
echo ""
