#!/bin/bash

# Saga test script - test N orders with concurrent VUs and view timing report
# Usage: ./saga-test.sh [number_of_orders] [fail_rate_%] [wait_time_seconds] [virtual_users]
#
# Parameters:
#   number_of_orders  - Number of orders to send (default: 1)
#   fail_rate_%       - Percentage of orders to fail 0-100 (default: 0)
#   wait_time_seconds - Time to wait before showing report (default: auto-calculated)
#   virtual_users     - Number of concurrent workers/VUs (default: 1)
#
# Examples:
#   ./saga-test.sh                  # 1 order, 0% fail, auto wait, 1 VU
#   ./saga-test.sh 5                # 5 orders, 0% fail, auto wait, 1 VU
#   ./saga-test.sh 10 20            # 10 orders, 20% fail rate, auto wait, 1 VU
#   ./saga-test.sh 10 0 30          # 10 orders, 0% fail, wait 30s, 1 VU
#   ./saga-test.sh 20 0 30 5        # 20 orders, 0% fail, wait 30s, 5 VUs (concurrent)

BASE_URL="${BASE_URL:-http://localhost:18080}"
NUM_ORDERS="${1:-1}"
FAIL_RATE="${2:-0}"
WAIT_TIME="${3:-0}"
NUM_VUS="${4:-1}"

# Ensure VUs don't exceed orders
if [ "$NUM_VUS" -gt "$NUM_ORDERS" ]; then
    NUM_VUS=$NUM_ORDERS
fi

# Auto-calculate wait time if not provided (shorter with more VUs)
if [ "$WAIT_TIME" -eq 0 ]; then
    # With VUs, orders complete in parallel, so divide by VUs
    WAIT_TIME=$(( (NUM_ORDERS / NUM_VUS) * 3 + 10 ))
fi

echo "=========================================="
echo "  SAGA LOAD TEST"
echo "=========================================="
echo "  Orders:    $NUM_ORDERS"
echo "  Fail Rate: $FAIL_RATE%"
echo "  VUs:       $NUM_VUS (concurrent)"
echo "  Wait Time: ${WAIT_TIME}s"
echo "=========================================="
echo ""

# Check service
echo "Checking service..."
if ! curl -s "$BASE_URL/actuator/health" > /dev/null 2>&1; then
    echo "ERROR: Service not running at $BASE_URL"
    exit 1
fi
echo "✓ Service is running"
echo ""

# Clear previous timing data
echo "Clearing previous timing data..."
CLEAR_RESPONSE=$(curl -s -X POST "$BASE_URL/api/saga-timing/clear" 2>/dev/null)
if echo "$CLEAR_RESPONSE" | grep -q "success" 2>/dev/null; then
    echo "✓ Timing data cleared"
else
    echo "⚠ Could not clear timing data (continuing anyway)"
fi
echo ""

# Calculate number of orders to fail
NUM_FAIL=$((NUM_ORDERS * FAIL_RATE / 100))
echo "Sending $NUM_ORDERS order(s) with $NUM_VUS VU(s) ($NUM_FAIL will be configured to fail)..."

# Create temp file for results
RESULTS_FILE=$(mktemp)
trap "rm -f $RESULTS_FILE" EXIT

# Function to send a single order
send_order() {
    local order_num=$1
    local should_fail=$2
    
    if [ "$should_fail" -eq 1 ]; then
        QUANTITY=0
        PRICE=-1
        FAIL_MARKER=" [FAIL]"
    else
        QUANTITY=1
        PRICE=99.99
        FAIL_MARKER=""
    fi
    
    RESPONSE=$(curl -s -X POST "$BASE_URL/api/orders" \
        -H "Content-Type: application/json" \
        -d "{
            \"customerId\": \"TEST-$(printf '%03d' $order_num)\",
            \"shippingAddress\": \"$order_num Test Street\",
            \"items\": [{
                \"productId\": \"PROD-$(printf '%03d' $order_num)\",
                \"productName\": \"Test Product $order_num\",
                \"quantity\": $QUANTITY,
                \"price\": $PRICE
            }]
        }")
    
    CORRELATION_ID=$(echo "$RESPONSE" | grep -o '"correlationId":"[^"]*"' | cut -d'"' -f4)
    echo "  Order $order_num: $CORRELATION_ID$FAIL_MARKER"
}

# Track start time
START_TIME=$(date +%s.%N)

# Send orders concurrently with VU limit
ACTIVE_JOBS=0
for i in $(seq 1 $NUM_ORDERS); do
    # Determine if this order should fail
    if [ "$i" -le "$NUM_FAIL" ]; then
        SHOULD_FAIL=1
    else
        SHOULD_FAIL=0
    fi
    
    # Send order in background
    send_order $i $SHOULD_FAIL &
    ACTIVE_JOBS=$((ACTIVE_JOBS + 1))
    
    # If we've hit our VU limit, wait for one to complete
    if [ "$ACTIVE_JOBS" -ge "$NUM_VUS" ]; then
        wait -n 2>/dev/null || wait  # wait -n waits for any one job (bash 4.3+)
        ACTIVE_JOBS=$((ACTIVE_JOBS - 1))
    fi
done

# Wait for all remaining jobs
wait

# Calculate send duration
END_TIME=$(date +%s.%N)
SEND_DURATION=$(echo "$END_TIME - $START_TIME" | bc 2>/dev/null || echo "N/A")
echo ""
echo "✓ All $NUM_ORDERS orders sent in ${SEND_DURATION}s"

# Wait for saga completion
echo "Waiting ${WAIT_TIME}s for saga(s) to complete..."
sleep $WAIT_TIME

# Show stats
echo ""
echo "=========================================="
echo "  STATS"
echo "=========================================="
curl -s "$BASE_URL/api/saga-timing/stats" | python3 -m json.tool 2>/dev/null

# Show report
echo ""
echo "=========================================="
echo "  TIMING REPORT"
echo "=========================================="
curl -s "$BASE_URL/api/saga-timing/report"

echo ""
echo "Done!"
