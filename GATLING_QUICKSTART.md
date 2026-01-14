# 🚀 Gatling Quick Start Guide

## Setup (One-Time)

```bash
# 1. Make scripts executable (already done)
chmod +x setup-gatling.sh run-gatling-tests.sh

# 2. Run setup (downloads dependencies)
./setup-gatling.sh
```

## Running Tests

### Option 1: Quick Test (Recommended First Run)
```bash
# 2-minute smoke test
./run-gatling-tests.sh QuickSagaSimulation
```

### Option 2: Full Async Saga Test
```bash
# 15-minute comprehensive test with async tracking
./run-gatling-tests.sh AsyncSagaSimulation
```

### Option 3: Stress Test
```bash
# 12-minute stress test to find breaking points
./run-gatling-tests.sh StressSagaSimulation
```

### Option 4: Run All Tests
```bash
# Runs all 3 simulations sequentially (~30 minutes)
./run-gatling-tests.sh all
```

## Manual Gatling Commands

```bash
# From project root
cd gatling-tests

# Run specific simulation
./gradlew gatlingRun-AsyncSagaSimulation

# Run with custom base URL
./gradlew gatlingRun-AsyncSagaSimulation -DBASE_URL=http://my-server:8080

# List all simulations
./gradlew gatlingRun

# Clean results
./gradlew clean
```

## Environment Variables

```bash
# Customize test behavior
export BASE_URL=http://localhost:8080     # Target server
export MAX_POLL_ATTEMPTS=30               # Max status checks per saga
export POLL_INTERVAL=1                    # Seconds between polls
export THINK_TIME_MIN=1                   # Min pause between requests
export THINK_TIME_MAX=3                   # Max pause between requests
export MAX_USERS=100                      # Stress test peak users

# Then run test
./run-gatling-tests.sh AsyncSagaSimulation
```

## View Results

After test completion, HTML report location will be shown:
```bash
# Example output:
📊 View Results:
   HTML Reports: file:///path/to/results/gatling/asyncsagasimulation-20260114123456/index.html
```

Open in browser to see:
- Response time charts
- Requests per second
- Success/failure rates
- Detailed percentiles (p50, p95, p99)
- Request distribution over time

## What Each Test Does

### 1. QuickSagaSimulation ⚡
- **Duration:** 2 minutes
- **Load:** 5-10 users
- **Purpose:** Quick validation
- **Async:** Polls status 10 times
- **Use When:** Before deployments, in CI/CD

### 2. AsyncSagaSimulation 🎯
- **Duration:** 15 minutes
- **Load:** 5 → 25 → 50 users (ramping)
- **Purpose:** Comprehensive async saga testing
- **Async:** Full polling until completion (max 30 attempts)
- **Measures:**
  - End-to-end saga duration
  - Compensation flow rate
  - Async success percentage
  - Polling attempts needed
- **Use When:** Performance benchmarking, regression testing

### 3. StressSagaSimulation 💥
- **Duration:** 12 minutes
- **Load:** 20 → 50 → 100 users (aggressive)
- **Purpose:** Find breaking points
- **Async:** Quick status checks (not full polling)
- **Monitors:**
  - System degradation under stress
  - RabbitMQ queue saturation
  - Compensation trigger frequency
  - Error rates at peak load
- **Use When:** Capacity planning, finding limits

## Troubleshooting

### "Connection refused"
```bash
# Check if services are running
curl http://localhost:8080/actuator/health

# Start services
./start.sh
```

### "Gradle not found"
```bash
cd gatling-tests
# Use wrapper
./gradlew --version

# Or install Gradle
sdk install gradle 8.5
```

### "Simulation not found"
```bash
# List available simulations
cd gatling-tests
./gradlew gatlingRun

# Check file exists
ls src/gatling/scala/
```

### Clean and rebuild
```bash
cd gatling-tests
./gradlew clean build
```

## Key Gatling Features Used

1. **Async Polling with `asLongAs`**
   ```scala
   .asLongAs(session => session("status").as[String] == "PENDING") {
     exec(http("Poll").get(...))
     .pause(1.second)
   }
   ```

2. **Correlation Tracking**
   ```scala
   .check(jsonPath("$.correlationId").saveAs("id"))
   // Later use: session("id").as[String]
   ```

3. **Custom Metrics**
   ```scala
   .exec { session =>
     println(s"✅ Saga completed in ${attempts} polls")
     session
   }
   ```

4. **Load Patterns**
   ```scala
   rampUsers(50).during(2.minutes)           // Gradual increase
   constantUsersPerSec(10).during(5.minutes) // Sustained load
   atOnceUsers(5)                            // Instant spike
   ```

## Next Steps

1. **Run Quick Test First**
   ```bash
   ./run-gatling-tests.sh QuickSagaSimulation
   ```

2. **Review Results**
   - Open HTML report
   - Check response times
   - Verify saga completion rates

3. **Customize if Needed**
   - Edit [gatling-tests/src/gatling/scala/AsyncSagaSimulation.scala](gatling-tests/src/gatling/scala/AsyncSagaSimulation.scala)
   - Adjust user counts, durations, think times

4. **Compare with k6**
   - Read [GATLING_VS_K6.md](GATLING_VS_K6.md)
   - See why Gatling is better for async systems

5. **Integrate into CI/CD**
   ```yaml
   # .github/workflows/load-test.yml
   - name: Run Gatling Tests
     run: |
       ./start.sh
       ./run-gatling-tests.sh QuickSagaSimulation
   ```

## Pro Tips 💡

1. **Start services before testing**
   ```bash
   ./start.sh
   ```

2. **Monitor during tests**
   ```bash
   # Watch RabbitMQ
   open http://localhost:15672
   
   # Watch logs
   docker-compose logs -f saga-orchestrator
   ```

3. **Compare runs**
   - Save HTML reports: `results/gatling/*/index.html`
   - Compare response times across versions

4. **Adjust for your needs**
   - More users? Edit `maxUsers` in simulations
   - Longer tests? Adjust `duration` values
   - Different endpoints? Modify HTTP requests

## Support

- 📖 Full docs: [gatling-tests/README.md](gatling-tests/README.md)
- 🆚 k6 comparison: [GATLING_VS_K6.md](GATLING_VS_K6.md)
- 🌐 Gatling docs: https://gatling.io/docs/
- 🐛 Issues: Check simulation output and logs
