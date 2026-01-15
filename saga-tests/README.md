# Saga Orchestration Test Suite

Automated test scripts for measuring saga orchestration timing and performance.

## Prerequisites

1. **Service Running**: Ensure the saga-orchestrator is running:
   ```bash
   docker compose up -d
   ```

2. **Dependencies**: The scripts require:
   - `curl` - for HTTP requests
   - `bc` - for calculations
   - `python3` - for JSON formatting (optional)

## Test Types

| Test | Orders | Concurrency | Delay | Purpose |
|------|--------|-------------|-------|---------|
| **single** | 1 | 1 | 0ms | Verify system works |
| **smoke** | 5 | 2 | 500ms | Light functional test |
| **quick** | 20 | 5 | 200ms | Moderate load test |
| **stress** | 100 | 20 | 50ms | Heavy load test |

## Usage

### Quick Start
```bash
cd saga-tests

# Run single order test
./single-test.sh

# Run smoke test
./smoke-test.sh

# Run quick test
./quick-test.sh

# Run stress test
./stress-test.sh
```

### Using Main Runner
```bash
# Single order test
./run-tests.sh single

# Smoke test  
./run-tests.sh smoke

# Quick test
./run-tests.sh quick

# Stress test
./run-tests.sh stress

# Custom test (50 orders, 10 concurrent, 100ms delay)
./run-tests.sh custom 50 10 100

# Just get the timing report
./run-tests.sh report

# Get quick stats
./run-tests.sh stats
```

### Environment Variables
```bash
# Use a different base URL
BASE_URL=http://localhost:9090 ./run-tests.sh single
```

## Output

### Console Output
The test shows real-time progress:
```
╔══════════════════════════════════════════════════════════════════════╗
║           SAGA ORCHESTRATION TIMING TEST SUITE                       ║
╚══════════════════════════════════════════════════════════════════════╝

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  Running Single Order Test
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  Orders: 1 | Concurrency: 1 | Delay: 0ms

Sending orders...
  Sent: 1 / 1
✓ All orders sent in 45ms

Waiting for 1 saga(s) to complete (max 180s)...
  Progress: 1/1 completed, 0 active (2s elapsed)
✓ All sagas completed!

Test completed in 2156ms (2.15s)
```

### Timing Report
After each test, a detailed timing report is displayed:

```
╔══════════════════════════════════════════════════════════════════════╗
║           SAGA ORCHESTRATION DETAILED TIMING REPORT                  ║
╚══════════════════════════════════════════════════════════════════════╝

┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
┃  SAGA #1                                                             ┃
┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫
┃  CorrelationId  : abc-123-def-456                                    ┃
┃  CustomerId     : CUST-001                                           ┃
┃  Status         : ✓ SUCCESS                                          ┃
┃  Total Duration : 1444 ms (1.444 seconds)                            ┃
┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫
┃  EVENT TIMELINE:                                                     ┃
┣──────────────────────────────────────────────────────────────────────┫
┃  Step │ Event              │ Status │ Elapsed (ms) │ Timestamp       ┃
┣──────────────────────────────────────────────────────────────────────┫
┃  0    │ SAGA_INITIATED     │ ✓      │ 0            │ 10:30:00.123    ┃
┃  1    │ ORDER_CREATED      │ ✓      │ 245 (+245)   │ 10:30:00.368    ┃
┃  2    │ INVENTORY_RESERVED │ ✓      │ 512 (+267)   │ 10:30:00.635    ┃
┃  3    │ PAYMENT_PROCESSED  │ ✓      │ 934 (+422)   │ 10:30:01.057    ┃
┃  4    │ SHIPMENT_CREATED   │ ✓      │ 1444 (+510)  │ 10:30:01.567    ┃
┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛

╔══════════════════════════════════════════════════════════════════════╗
║                        SUMMARY STATISTICS                            ║
╠══════════════════════════════════════════════════════════════════════╣
║  Total Sagas Completed   │ 20                                        ║
║  Successful              │ 20 (100.0%)                                ║
║  Minimum Duration        │ 892 ms (0.892 sec)                         ║
║  Maximum Duration        │ 2341 ms (2.341 sec)                        ║
║  Average Duration        │ 1456.30 ms (1.456 sec)                     ║
║  95th Percentile (P95)   │ 2100 ms (2.100 sec)                        ║
╚══════════════════════════════════════════════════════════════════════╝
```

## Log Files

Timing data is also saved to files in the project root:
- `saga-timing-logs/saga-timing.log` - Real-time event log
- `saga-timing-logs/saga-detailed-report.txt` - Full detailed report

## REST Endpoints

You can also access timing data directly via REST:

```bash
# Get detailed report
curl http://localhost:8080/api/saga-timing/report

# Get JSON stats
curl http://localhost:8080/api/saga-timing/stats
```

## Tips

1. **First Run**: Start with `single` test to verify everything works
2. **Warm Up**: Run `smoke` test before `stress` to warm up JVM
3. **Clean Data**: Restart the service to clear timing data between test runs
4. **Monitor**: Watch RabbitMQ management UI at http://localhost:15672 during tests
