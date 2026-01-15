#!/bin/bash

# Saga Report Generation
# 1. Copy logs from Docker container
# 2. Generate Python analysis report

set -e

# Configuration
CONTAINER_NAME="saga-orchestrator"

echo "=========================================="
echo "  Saga Log Analysis"
echo "=========================================="
echo ""

# Step 1: Copy logs from Docker container
echo "[1/2] Copying logs from container..."
docker cp ${CONTAINER_NAME}:/app/saga-timing-logs ./saga-timing-logs
echo "✓ Logs copied to ./saga-timing-logs/"
echo ""

# Step 2: Generate Python report
echo "[2/2] Generating analysis report..."
echo ""
python3 analyze-saga-logs.py --detailed --limit 10
echo ""

echo "=========================================="
echo "  ✓ Process Complete!"
echo "=========================================="
echo ""
echo "View results:"
echo "  cat saga-timing-logs/python-summary-report.txt"
echo "  cat saga-timing-logs/python-detailed-report.txt"
