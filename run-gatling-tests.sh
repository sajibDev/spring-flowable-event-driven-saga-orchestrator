#!/bin/bash

# Gatling Load Test Runner for Async Saga Orchestrator
# Usage: ./run-gatling-tests.sh [simulation-name] [options]

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default configuration
BASE_URL="${BASE_URL:-http://localhost:8080}"
SIMULATION="${1:-QuickSagaSimulation}"

echo -e "${BLUE}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║  Gatling Load Test - Async Saga Orchestrator              ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Check if services are running
echo -e "${YELLOW}🔍 Checking if saga orchestrator is available...${NC}"
if curl -s -f "${BASE_URL}/actuator/health" > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Saga orchestrator is running at ${BASE_URL}${NC}"
else
    echo -e "${RED}❌ ERROR: Saga orchestrator is not responding at ${BASE_URL}${NC}"
    echo -e "${YELLOW}💡 Make sure to start the services first:${NC}"
    echo -e "   ${YELLOW}./start.sh${NC}"
    exit 1
fi

echo ""
echo -e "${YELLOW}📋 Test Configuration:${NC}"
echo -e "   Simulation: ${GREEN}${SIMULATION}${NC}"
echo -e "   Base URL: ${GREEN}${BASE_URL}${NC}"
echo -e "   Max Poll Attempts: ${GREEN}${MAX_POLL_ATTEMPTS:-30}${NC}"
echo -e "   Poll Interval: ${GREEN}${POLL_INTERVAL:-1}s${NC}"
echo ""

# Display load criteria based on simulation type
case "$SIMULATION" in
    "QuickSagaSimulation")
        echo -e "${BLUE}📈 Load Criteria - Quick Smoke Test:${NC}"
        echo -e "   Duration: ${GREEN}~2 minutes${NC}"
        echo -e "   Load Pattern: ${GREEN}5 instant users + ramp to 15 users over 1 minute${NC}"
        echo -e "   Expected Throughput: ${GREEN}~3 requests/second${NC}"
        echo -e "   Max Concurrent Users: ${GREEN}15${NC}"
        echo -e "   Poll Strategy: ${GREEN}Up to 10 polls per saga (1s interval)${NC}"
        echo -e "   Objective: ${GREEN}Quick validation & baseline metrics${NC}"
        ;;
    "AsyncSagaSimulation")
        echo -e "${BLUE}📈 Load Criteria - Comprehensive Multi-Phase Test:${NC}"
        echo -e "   Duration: ${GREEN}~15 minutes${NC}"
        echo -e "   Load Phases:"
        echo -e "     ${GREEN}1. Smoke${NC} - 5 instant users (quick validation)"
        echo -e "     ${GREEN}2. Ramp${NC} - 20 users over 2 minutes (gradual increase)"
        echo -e "     ${GREEN}3. Sustained${NC} - 10 users/sec for 5 minutes (steady state)"
        echo -e "     ${GREEN}4. Stress${NC} - 50 users + 25 users/sec for 5 minutes (peak load)"
        echo -e "   Expected Peak Throughput: ${GREEN}~25 requests/second${NC}"
        echo -e "   Max Concurrent Users: ${GREEN}50+${NC}"
        echo -e "   Poll Strategy: ${GREEN}Up to 30 polls per saga (1s interval)${NC}"
        echo -e "   Objective: ${GREEN}Full end-to-end validation & performance analysis${NC}"
        ;;
    "StressSagaSimulation")
        echo -e "${BLUE}📈 Load Criteria - Stress Test (Find Breaking Points):${NC}"
        echo -e "   Duration: ${GREEN}~12 minutes${NC}"
        echo -e "   Load Phases:"
        echo -e "     ${GREEN}1. Warm-up${NC} - 20 users over 1 minute"
        echo -e "     ${GREEN}2. Stress 1${NC} - 30 users/sec for 2 minutes"
        echo -e "     ${GREEN}3. Stress 2${NC} - Ramp to 100 users over 2 minutes"
        echo -e "     ${GREEN}4. Peak${NC} - 50 users/sec for 3 minutes"
        echo -e "     ${GREEN}5. Cool-down${NC} - Ramp down to 10 users over 1 minute"
        echo -e "   Expected Peak Throughput: ${GREEN}~50 requests/second${NC}"
        echo -e "   Max Concurrent Users: ${GREEN}100${NC}"
        echo -e "   Poll Strategy: ${GREEN}Single quick check (minimal for max stress)${NC}"
        echo -e "   Objective: ${GREEN}Identify bottlenecks, breaking points & compensation triggers${NC}"
        ;;
    "all")
        echo -e "${BLUE}📈 Load Criteria - Running All Tests Sequentially:${NC}"
        echo -e "   Total Duration: ${GREEN}~30 minutes${NC}"
        echo -e "   Tests: ${GREEN}Quick → Async → Stress${NC}"
        echo -e "   Coverage: ${GREEN}Smoke test → Full validation → Stress analysis${NC}"
        ;;
    *)
        echo -e "${YELLOW}📈 Load Criteria: See simulation file for details${NC}"
        ;;
esac
echo ""

# Create results directory
mkdir -p results/gatling

# Run Gatling test
echo -e "${YELLOW}🚀 Starting Gatling test...${NC}"
echo ""

cd gatling-tests

if [ "$SIMULATION" == "all" ]; then
    echo -e "${BLUE}Running all simulations sequentially...${NC}"
    echo ""
    
    echo -e "${YELLOW}╔══════════════════════════════════════════════════════════╗${NC}"
    echo -e "${YELLOW}║  1/3 - Quick Smoke Test (2 min, 15 users)               ║${NC}"
    echo -e "${YELLOW}╚══════════════════════════════════════════════════════════╝${NC}"
    ./gradlew gatlingRun --simulation=QuickSagaSimulation
    
    echo -e "${GREEN}✓ Quick test completed. Cooling down for 5 seconds...${NC}"
    sleep 5
    
    echo -e "${YELLOW}╔══════════════════════════════════════════════════════════╗${NC}"
    echo -e "${YELLOW}║  2/3 - Full Async Saga Test (15 min, 50+ users)         ║${NC}"
    echo -e "${YELLOW}╚══════════════════════════════════════════════════════════╝${NC}"
    ./gradlew gatlingRun --simulation=AsyncSagaSimulation
    
    echo -e "${GREEN}✓ Async test completed. Cooling down for 5 seconds...${NC}"
    sleep 5
    
    echo -e "${YELLOW}╔══════════════════════════════════════════════════════════╗${NC}"
    echo -e "${YELLOW}║  3/3 - Stress Test (12 min, 100 users, 50 req/s)        ║${NC}"
    echo -e "${YELLOW}╚══════════════════════════════════════════════════════════╝${NC}"
    ./gradlew gatlingRun --simulation=StressSagaSimulation
else
    ./gradlew gatlingRun --simulation=${SIMULATION}
fi

cd ..

echo ""
echo -e "${GREEN}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║  ✅ Gatling Test Completed                                 ║${NC}"
echo -e "${GREEN}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${YELLOW}📊 View Results:${NC}"
echo -e "   HTML Reports: ${GREEN}file://$(pwd)/results/gatling/${NC}"
echo ""
echo -e "${YELLOW}💡 Quick Tests:${NC}"
echo -e "   Quick:  ${GREEN}./run-gatling-tests.sh QuickSagaSimulation${NC}"
echo -e "   Full:   ${GREEN}./run-gatling-tests.sh AsyncSagaSimulation${NC}"
echo -e "   Stress: ${GREEN}./run-gatling-tests.sh StressSagaSimulation${NC}"
echo -e "   All:    ${GREEN}./run-gatling-tests.sh all${NC}"
echo ""
