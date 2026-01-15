#!/bin/bash

# Saga test script - test N orders and view timing report
# Usage: ./saga-test.sh [number_of_orders] [fail_rate_%] [wait_time_seconds]
#
# Parameters:
#   number_of_orders  - Number of orders to send (default: 1)
#   fail_rate_%       - Percentage of orders to fail 0-100 (default: 0)
#   wait_time_seconds - Time to wait before showing report (default: auto-calculated)
#
# Examples:
#   ./saga-test.sh              # 1 order, 0% fail, auto wait
#   ./saga-test.sh 5            # 5 orders, 0% fail, auto wait
#   ./saga-test.sh 10 20        # 10 orders, 20% fail rate, auto wait
#   ./saga-test.sh 10 0 30      # 10 orders, 0% fail, wait 30 seconds

BASE_URL="${BASE_URL:-http://localhost:18080}"
NUM_ORDERS="${1:-1}"
FAIL_RATE="${2:-0}"
WAIT_TIME="${3:-0}"

# Auto-calculate wait time if not provided
if [ "$WAIT_TIME" -eq 0 ]; then
    WAIT_TIME=$((NUM_ORDERS * 3 + 5))
fi

echo "=========================================="
echo "  SAGA TEST"
echo "=========================================="
echo "  Orders:    $NUM_ORDERS"
echo "  Fail Rate: $FAIL_RATE%"
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
echo "Sending $NUM_ORDERS order(s) ($NUM_FAIL will be configured to fail)..."

# Send orders
for i in $(seq 1 $NUM_ORDERS); do
    # Determine if this order should fail (use invalid data to trigger failure)
    if [ "$i" -le "$NUM_FAIL" ]; then
        # Failed order - use quantity 0 or negative price to trigger failure
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
            \"customerId\": \"TEST-$(printf '%03d' $i)\",
            \"shippingAddress\": \"$i Test Street\",
            \"items\": [{
                \"productId\": \"PROD-$(printf '%03d' $i)\",
                \"productName\": \"Test Product $i\",
                \"quantity\": $QUANTITY,
                \"price\": $PRICE
            }]
        }")
    
    CORRELATION_ID=$(echo "$RESPONSE" | grep -o '"correlationId":"[^"]*"' | cut -d'"' -f4)
    echo "  Order $i: $CORRELATION_ID$FAIL_MARKER"
done
echo ""

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
