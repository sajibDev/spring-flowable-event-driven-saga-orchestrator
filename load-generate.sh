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

# Step 1: Clear previous log file (auto-recreates on next write)
echo "[1/3] Clearing previous saga log file..."
docker exec ${CONTAINER_NAME} rm -f /app/saga-timing-logs/saga-timing.log
echo "✓ Log file cleared (will auto-recreate on first saga)"
echo ""

# Step 2: Run k6 load test
echo "[2/3] Running k6 load test..."
echo ""
k6 run load-test.js
echo ""
echo "✓ Load test completed"
echo ""
