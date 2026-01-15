# Saga Log Analyzer - Usage Guide

## Overview
Python-based analyzer that reads saga timing logs and generates comprehensive reports with statistics, success/failure rates, and performance metrics.

## Features
- ✅ Parse correlation IDs and track saga execution
- ✅ Calculate success/failure rates
- ✅ Performance metrics (min, max, avg, median, P90, P95, P99)
- ✅ Detailed per-saga reports
- ✅ Export to JSON and CSV
- ✅ Beautiful formatted console output

## Quick Start

### 1. Copy logs from Docker container (if needed)
```bash
docker cp saga-orchestrator:/app/saga-timing-logs ./saga-timing-logs
```

### 2. Run basic summary report
```bash
python3 analyze-saga-logs.py
```

**Output:**
- Console summary with statistics
- `saga-timing-logs/python-summary-report.txt`

### 3. Generate detailed report
```bash
python3 analyze-saga-logs.py --detailed --limit 20
```

Shows top 20 sagas by duration with event timeline.

### 4. Export to JSON and CSV
```bash
python3 analyze-saga-logs.py --export-json --export-csv
```

**Generates:**
- `saga-timing-logs/saga-analysis.json` - For programmatic access
- `saga-timing-logs/saga-analysis.csv` - For Excel/spreadsheet analysis

### 5. Complete analysis (all options)
```bash
python3 analyze-saga-logs.py --detailed --limit 50 --export-json --export-csv
```

## Command Line Options

| Option | Description |
|--------|-------------|
| `--log-file PATH` | Path to saga timing log file (default: `saga-timing-logs/saga-timing.log`) |
| `--detailed` | Show detailed report for individual sagas |
| `--limit N` | Number of sagas to show in detailed report (default: 10) |
| `--export-json` | Export analysis to JSON file |
| `--export-csv` | Export analysis to CSV file |
| `--output-dir DIR` | Output directory for reports (default: `saga-timing-logs`) |

## Report Contents

### Summary Report
```
╔══════════════════════════════════════════════════════════╗
║              SAGA EXECUTION SUMMARY REPORT               ║
╠══════════════════════════════════════════════════════════╣
║                    OVERALL STATISTICS                     ║
╠──────────────────────────────────────────────────────────╣
║  Total Sagas Tracked        │                       1852 ║
║  Completed Sagas            │                         91 ║
║    - Successful             │                 91 (100.0%) ║
║    - Failed                 │                   0 (0.0%) ║
║  In Progress                │                       1761 ║
╠──────────────────────────────────────────────────────────╣
║                   DURATION METRICS                        ║
╠──────────────────────────────────────────────────────────╣
║  Minimum Duration           │     2048 │            2.048 ║
║  Maximum Duration           │    19422 │           19.422 ║
║  Average Duration           │  9490.19 │            9.490 ║
║  Median Duration            │  8583.00 │            8.583 ║
║  90th Percentile (P90)      │    16872 │           16.872 ║
║  95th Percentile (P95)      │    17941 │           17.941 ║
║  99th Percentile (P99)      │    19422 │           19.422 ║
╚══════════════════════════════════════════════════════════╝
```

### Detailed Report (per saga)
- Correlation ID
- Customer ID
- Status (SUCCESS/FAILED/IN_PROGRESS)
- Total duration
- Event timeline with timestamps

### CSV Export
Columns: `CorrelationID`, `CustomerID`, `Status`, `Duration(ms)`, `Duration(s)`, `EventCount`

### JSON Export
Complete saga data in JSON format for programmatic processing.

## Integration with Load Testing

### After running k6 load test:
```bash
# 1. Run load test
k6 run load-test.js

# 2. Wait for sagas to complete
sleep 10

# 3. Copy logs from container
docker cp saga-orchestrator:/app/saga-timing-logs ./saga-timing-logs

# 4. Analyze results
python3 analyze-saga-logs.py --detailed --limit 20 --export-json --export-csv
```

### After running report-generate.sh:
```bash
# 1. Run saga test script
./report-generate.sh 100 30 30 10

# 2. Copy logs (if needed)
docker cp saga-orchestrator:/app/saga-timing-logs ./saga-timing-logs

# 3. Analyze with Python
python3 analyze-saga-logs.py --detailed --export-json --export-csv
```

## Sample Workflow

```bash
# Clear previous data
curl -X POST http://localhost:8080/api/saga-timing/clear

# Run load test
k6 run load-test.js

# Wait for completion
sleep 15

# Copy logs
docker cp saga-orchestrator:/app/saga-timing-logs ./saga-timing-logs

# Generate comprehensive report
python3 analyze-saga-logs.py --detailed --limit 50 --export-json --export-csv

# View summary
cat saga-timing-logs/python-summary-report.txt

# View detailed report
cat saga-timing-logs/python-detailed-report.txt

# Open CSV in Excel
# Open saga-timing-logs/saga-analysis.csv
```

## Log File Format

The analyzer parses these log entry types:

**START:**
```
[START] CorrelationId: xxx | CustomerId: yyy | StartTime: ... | Timestamp: nnn
```

**EVENT:**
```
[EVENT] CorrelationId: xxx | EventName: yyy | Success: true/false | ElapsedMs: nnn
```

**COMPLETE:**
```
[COMPLETE] CorrelationId: xxx | Success: true/false | DurationMs: nnn | Timestamp: nnn
```

## Troubleshooting

### Log file not found
```bash
# Check if saga-orchestrator is running
docker ps | grep saga-orchestrator

# Copy logs from container
docker cp saga-orchestrator:/app/saga-timing-logs ./saga-timing-logs
```

### No completed sagas
Wait longer for sagas to complete, or check if services are running properly.

### Python dependencies
The script uses only standard library modules - no external dependencies needed!

## Output Files

| File | Description |
|------|-------------|
| `python-summary-report.txt` | Summary statistics and metrics |
| `python-detailed-report.txt` | Individual saga details |
| `saga-analysis.json` | Complete data in JSON format |
| `saga-analysis.csv` | Spreadsheet-ready CSV export |

## Tips

1. **Always copy logs from container** before analyzing (logs are written inside Docker)
2. **Use `--limit`** to control detailed report size for large datasets
3. **Export to CSV** for Excel analysis and custom charts
4. **Export to JSON** for programmatic processing or integration with other tools
5. **Run analysis after each test cycle** to track performance trends

## Next Steps

- Import CSV into Excel/Google Sheets for custom visualizations
- Use JSON export for automated reporting systems
- Track metrics over time by saving reports with timestamps
- Integrate with CI/CD pipeline for performance regression testing
