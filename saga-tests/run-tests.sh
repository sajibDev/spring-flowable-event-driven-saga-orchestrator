#!/bin/bash

# ============================================================================
# SAGA ORCHESTRATION TEST RUNNER
# ============================================================================
# This script provides different test profiles for the saga orchestrator
# Usage: ./run-tests.sh [single|smoke|quick|stress]
# ============================================================================

set -e

# Configuration
BASE_URL="${BASE_URL:-http://localhost:8080}"
ORDER_ENDPOINT="$BASE_URL/api/orders"
TIMING_REPORT_ENDPOINT="$BASE_URL/api/saga-timing/report"
TIMING_STATS_ENDPOINT="$BASE_URL/api/saga-timing/stats"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Test configurations
declare -A TEST_CONFIGS
TEST_CONFIGS[single_orders]=1
TEST_CONFIGS[single_concurrent]=1
TEST_CONFIGS[single_delay]=0

TEST_CONFIGS[smoke_orders]=5
TEST_CONFIGS[smoke_concurrent]=2
TEST_CONFIGS[smoke_delay]=500

TEST_CONFIGS[quick_orders]=20
TEST_CONFIGS[quick_concurrent]=5
TEST_CONFIGS[quick_delay]=200

TEST_CONFIGS[stress_orders]=100
TEST_CONFIGS[stress_concurrent]=20
TEST_CONFIGS[stress_delay]=50

# Print banner
print_banner() {
    echo -e "${CYAN}"
    echo "╔══════════════════════════════════════════════════════════════════════╗"
    echo "║           SAGA ORCHESTRATION TIMING TEST SUITE                       ║"
    echo "╚══════════════════════════════════════════════════════════════════════╝"
    echo -e "${NC}"
}

# Print section header
print_section() {
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${YELLOW}  $1${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
}

# Check if service is running
check_service() {
    echo -e "${CYAN}Checking if saga-orchestrator is running...${NC}"
    if curl -s --connect-timeout 5 "$BASE_URL/actuator/health" > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Service is running at $BASE_URL${NC}"
        return 0
    else
        echo -e "${RED}✗ Service is not running at $BASE_URL${NC}"
        echo -e "${YELLOW}  Please start the service first: docker compose up -d${NC}"
        return 1
    fi
}

# Generate random order JSON
generate_order() {
    local customer_id="CUST-$(printf '%04d' $((RANDOM % 1000)))"
    local product_id="PROD-$(printf '%03d' $((RANDOM % 100)))"
    local quantity=$((RANDOM % 5 + 1))
    local price=$(echo "scale=2; $((RANDOM % 1000 + 100))" | bc)
    
    cat <<EOF
{
  "customerId": "$customer_id",
  "shippingAddress": "$((RANDOM % 999 + 1)) Test Street, City $((RANDOM % 50)), State $((RANDOM % 50))",
  "items": [
    {
      "productId": "$product_id",
      "productName": "Product-$((RANDOM % 100))",
      "quantity": $quantity,
      "price": $price
    }
  ]
}
EOF
}

# Send a single order and capture correlation ID
send_order() {
    local order_json="$1"
    local response
    
    response=$(curl -s -X POST "$ORDER_ENDPOINT" \
        -H "Content-Type: application/json" \
        -d "$order_json" 2>/dev/null)
    
    # Extract correlationId from response
    echo "$response" | grep -o '"correlationId":"[^"]*"' | cut -d'"' -f4
}

# Wait for all sagas to complete with progress
wait_for_completion() {
    local expected_count=$1
    local max_wait=${2:-120}  # Default 120 seconds
    local check_interval=2
    local elapsed=0
    
    echo -e "${CYAN}Waiting for $expected_count saga(s) to complete (max ${max_wait}s)...${NC}"
    
    while [ $elapsed -lt $max_wait ]; do
        local stats
        stats=$(curl -s "$TIMING_STATS_ENDPOINT" 2>/dev/null)
        local completed=$(echo "$stats" | grep -o '"completedSagas":[0-9]*' | cut -d':' -f2)
        local active=$(echo "$stats" | grep -o '"activeSagas":[0-9]*' | cut -d':' -f2)
        
        completed=${completed:-0}
        active=${active:-0}
        
        echo -ne "\r  ${YELLOW}Progress: ${completed}/${expected_count} completed, ${active} active (${elapsed}s elapsed)${NC}    "
        
        if [ "$completed" -ge "$expected_count" ]; then
            echo -e "\n${GREEN}✓ All sagas completed!${NC}"
            return 0
        fi
        
        sleep $check_interval
        elapsed=$((elapsed + check_interval))
    done
    
    echo -e "\n${RED}✗ Timeout waiting for saga completion${NC}"
    return 1
}

# Run load test with given parameters
run_load_test() {
    local test_name=$1
    local total_orders=$2
    local concurrent=$3
    local delay_ms=$4
    
    print_section "Running $test_name Test"
    echo -e "  ${CYAN}Orders: $total_orders | Concurrency: $concurrent | Delay: ${delay_ms}ms${NC}"
    echo ""
    
    local start_time=$(date +%s%3N)
    local correlation_ids=()
    local sent=0
    local failed=0
    
    echo -e "${CYAN}Sending orders...${NC}"
    
    # Send orders with concurrency control
    for ((i=1; i<=total_orders; i++)); do
        local order_json=$(generate_order)
        
        # Send order in background if we haven't hit concurrency limit
        {
            local correlation_id=$(send_order "$order_json")
            if [ -n "$correlation_id" ]; then
                echo "$correlation_id" >> /tmp/saga_test_correlations.txt
            fi
        } &
        
        # Control concurrency
        if [ $((i % concurrent)) -eq 0 ]; then
            wait
        fi
        
        # Progress indicator
        echo -ne "\r  ${YELLOW}Sent: $i / $total_orders${NC}"
        
        # Add delay between batches
        if [ "$delay_ms" -gt 0 ] && [ $((i % concurrent)) -eq 0 ]; then
            sleep $(echo "scale=3; $delay_ms/1000" | bc)
        fi
    done
    
    wait  # Wait for all background jobs to complete
    echo ""
    
    local send_end_time=$(date +%s%3N)
    local send_duration=$((send_end_time - start_time))
    
    echo -e "${GREEN}✓ All orders sent in ${send_duration}ms${NC}"
    echo ""
    
    # Wait for sagas to complete
    wait_for_completion $total_orders 180
    
    local end_time=$(date +%s%3N)
    local total_duration=$((end_time - start_time))
    
    echo ""
    echo -e "${GREEN}Test completed in ${total_duration}ms ($(echo "scale=2; $total_duration/1000" | bc)s)${NC}"
    
    # Cleanup temp file
    rm -f /tmp/saga_test_correlations.txt
    
    return 0
}

# Fetch and display timing report
display_report() {
    print_section "SAGA TIMING REPORT"
    echo ""
    
    # Fetch the detailed report
    local report
    report=$(curl -s "$TIMING_REPORT_ENDPOINT" 2>/dev/null)
    
    if [ -n "$report" ]; then
        echo "$report"
    else
        echo -e "${RED}Failed to fetch timing report${NC}"
    fi
    
    echo ""
}

# Display quick stats
display_stats() {
    print_section "Quick Stats"
    local stats
    stats=$(curl -s "$TIMING_STATS_ENDPOINT" 2>/dev/null)
    echo "$stats" | python3 -m json.tool 2>/dev/null || echo "$stats"
}

# Single Order Test
run_single_test() {
    print_banner
    print_section "SINGLE ORDER TEST"
    echo -e "  ${CYAN}Testing with 1 order to verify the system works${NC}"
    echo ""
    
    check_service || exit 1
    
    run_load_test "Single Order" 1 1 0
    
    # Small delay to ensure report is ready
    sleep 2
    
    display_report
}

# Smoke Test
run_smoke_test() {
    print_banner
    print_section "SMOKE TEST"
    echo -e "  ${CYAN}Light test with 5 orders to verify basic functionality${NC}"
    echo ""
    
    check_service || exit 1
    
    run_load_test "Smoke" 5 2 500
    
    sleep 2
    display_report
}

# Quick Test
run_quick_test() {
    print_banner
    print_section "QUICK TEST"
    echo -e "  ${CYAN}Moderate load test with 20 orders${NC}"
    echo ""
    
    check_service || exit 1
    
    run_load_test "Quick" 20 5 200
    
    sleep 2
    display_report
}

# Stress Test
run_stress_test() {
    print_banner
    print_section "STRESS TEST"
    echo -e "  ${CYAN}Heavy load test with 100 orders, high concurrency${NC}"
    echo ""
    
    check_service || exit 1
    
    run_load_test "Stress" 100 20 50
    
    sleep 3
    display_report
}

# Custom test with user-defined parameters
run_custom_test() {
    local orders=${1:-10}
    local concurrent=${2:-5}
    local delay=${3:-100}
    
    print_banner
    print_section "CUSTOM TEST"
    echo -e "  ${CYAN}Custom test with $orders orders, $concurrent concurrent, ${delay}ms delay${NC}"
    echo ""
    
    check_service || exit 1
    
    run_load_test "Custom" "$orders" "$concurrent" "$delay"
    
    sleep 2
    display_report
}

# Show usage
show_usage() {
    print_banner
    echo "Usage: $0 <test-type> [options]"
    echo ""
    echo "Test Types:"
    echo "  single    - Single order test (1 order)"
    echo "  smoke     - Smoke test (5 orders, low concurrency)"
    echo "  quick     - Quick test (20 orders, medium concurrency)"
    echo "  stress    - Stress test (100 orders, high concurrency)"
    echo "  custom    - Custom test (specify parameters)"
    echo "  report    - Just fetch and display the timing report"
    echo "  stats     - Display quick stats"
    echo ""
    echo "Custom Test Options:"
    echo "  $0 custom <orders> <concurrent> <delay_ms>"
    echo "  Example: $0 custom 50 10 100"
    echo ""
    echo "Environment Variables:"
    echo "  BASE_URL  - Base URL of the service (default: http://localhost:8080)"
    echo ""
}

# Main
main() {
    case "${1:-}" in
        single)
            run_single_test
            ;;
        smoke)
            run_smoke_test
            ;;
        quick)
            run_quick_test
            ;;
        stress)
            run_stress_test
            ;;
        custom)
            run_custom_test "${2:-10}" "${3:-5}" "${4:-100}"
            ;;
        report)
            print_banner
            check_service || exit 1
            display_report
            ;;
        stats)
            print_banner
            check_service || exit 1
            display_stats
            ;;
        *)
            show_usage
            exit 1
            ;;
    esac
}

main "$@"
