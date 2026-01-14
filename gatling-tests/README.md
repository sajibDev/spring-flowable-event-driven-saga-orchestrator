# Gatling Load Testing for Async Saga Orchestrator

## 🎯 Overview

Gatling-based load testing suite specifically designed for **async saga orchestration** with proper end-to-end saga completion tracking.

## 📁 Structure

```
gatling-tests/
├── build.gradle                    # Gatling Gradle configuration
├── src/gatling/
│   ├── scala/
│   │   ├── AsyncSagaSimulation.scala      # Full async saga test with polling
│   │   ├── QuickSagaSimulation.scala      # Quick validation test
│   │   └── StressSagaSimulation.scala     # Stress/breaking point test
│   └── resources/
│       ├── gatling.conf                   # Gatling configuration
│       └── logback-test.xml               # Logging configuration
```

## 🚀 Quick Start

### 1. Start Your Services
```bash
# Make sure saga orchestrator and all services are running
./start.sh
```

### 2. Run Tests

#### Quick Test (2 minutes)
```bash
./run-gatling-tests.sh QuickSagaSimulation
```

#### Full Async Saga Test (15 minutes)
```bash
./run-gatling-tests.sh AsyncSagaSimulation
```

#### Stress Test (12 minutes)
```bash
./run-gatling-tests.sh StressSagaSimulation
```

#### Run All Tests
```bash
./run-gatling-tests.sh all
```

### 3. View Results
After test completion, open the HTML report:
```bash
# Report location will be shown in console output
# Example: file:///path/to/results/gatling/asyncsagasimulation-timestamp/index.html
```

## 📊 Test Scenarios

### 1. **AsyncSagaSimulation** (Comprehensive)
- **Duration**: ~15 minutes
- **Phases**:
  1. Smoke test: 5 users at once
  2. Ramp up: 20 users over 2 minutes
  3. Sustained: 10 users/second for 5 minutes
  4. Stress: 50 users over 2 minutes, then 25/second for 3 minutes
- **Features**:
  - ✅ Full async saga tracking with polling
  - ✅ Measures end-to-end saga completion time
  - ✅ Tracks PENDING → COMPLETED/FAILED transitions
  - ✅ Polls up to 30 times (configurable)
  - ✅ Identifies compensation flows

### 2. **QuickSagaSimulation** (Fast Validation)
- **Duration**: ~2 minutes
- **Users**: 5-10 concurrent
- **Purpose**: Quick validation before deployments
- **Features**:
  - Fast smoke test
  - Simple 10-poll status check
  - Ideal for CI/CD pipelines

### 3. **StressSagaSimulation** (Breaking Point)
- **Duration**: ~12 minutes
- **Peak Load**: Up to 100 concurrent users, 50 requests/second
- **Purpose**: Find system limits and compensation behavior
- **Features**:
  - Aggressive load ramping
  - Minimal think time
  - Tracks compensation flows under stress
  - Tests RabbitMQ queue saturation

## 🔧 Configuration

### Environment Variables
```bash
# Base URL for saga orchestrator
export BASE_URL=http://localhost:8080

# Polling configuration
export MAX_POLL_ATTEMPTS=30    # Max status checks per saga
export POLL_INTERVAL=1          # Seconds between polls

# Stress test configuration
export MAX_USERS=100            # Peak concurrent users
export THINK_TIME_MIN=1         # Min pause between requests (seconds)
export THINK_TIME_MAX=3         # Max pause between requests (seconds)
```

### Run with Custom Config
```bash
BASE_URL=http://my-server:8080 MAX_POLL_ATTEMPTS=50 ./run-gatling-tests.sh AsyncSagaSimulation
```

## 📈 Key Metrics Tracked

### Async-Specific Metrics
1. **Saga Completion Time**: End-to-end time from order creation to final status
2. **Polling Attempts**: Number of status checks needed per saga
3. **Async Success Rate**: Percentage of sagas reaching COMPLETED
4. **Compensation Rate**: Percentage triggering rollback (FAILED/COMPENSATED)
5. **Timeout Rate**: Sagas still PENDING after max polls

### Standard HTTP Metrics
- Response times (p50, p75, p95, p99)
- Requests per second
- Success/failure rates
- Connection errors

## 🎨 What Makes This Different from k6

| Feature | Gatling | k6 |
|---------|---------|-----|
| **Async Polling** | ✅ Built-in with `asLongAs` | ❌ Manual implementation |
| **Saga Completion Tracking** | ✅ Native support | ⚠️ Limited |
| **Real-time HTML Reports** | ✅ Beautiful dashboards | ⚠️ Basic |
| **Correlation Tracking** | ✅ Easy session variables | ⚠️ Possible but manual |
| **JVM Integration** | ✅ Native (same as Spring Boot) | ❌ Go-based |
| **Scala DSL** | ✅ Type-safe, powerful | ❌ JavaScript |

## 📊 Sample Output

```
[Saga] Created order with correlationId: 123e4567-e89b-12d3-a456-426614174000
[Saga] Poll attempt 1 - Status: PENDING
[Saga] Poll attempt 2 - Status: PENDING
[Saga] Poll attempt 3 - Status: COMPLETED
✅ [Saga] SUCCESS - Completed in 3 polls
```

## 🐛 Troubleshooting

### Services Not Running
```bash
# Check if orchestrator is up
curl http://localhost:8080/actuator/health

# Start services
./start.sh
```

### Test Failures
```bash
# View detailed Gatling logs
cd gatling-tests
./gradlew gatlingRun-AsyncSagaSimulation --info

# Check simulation output
tail -f results/gatling/*/simulation.log
```

### Gradle Issues
```bash
# Clean and rebuild
cd gatling-tests
./gradlew clean

# Download dependencies
./gradlew dependencies
```

## 📝 Creating Custom Simulations

Create a new file in `gatling-tests/src/gatling/scala/`:

```scala
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class MyCustomSimulation extends Simulation {
  val httpProtocol = http.baseUrl("http://localhost:8080")
  
  val myScenario = scenario("My Test")
    .exec(http("Create Order")
      .post("/api/orders")
      .body(StringBody("""{"customerId":"TEST"}"""))
      .check(jsonPath("$.correlationId").saveAs("id"))
    )
    .pause(2.seconds)
    .exec(http("Check Status")
      .get(session => s"/api/orders/${session("id").as[String]}/status")
    )
  
  setUp(myScenario.inject(atOnceUsers(10))).protocols(httpProtocol)
}
```

Run it:
```bash
./run-gatling-tests.sh MyCustomSimulation
```

## 🔗 Useful Links

- [Gatling Documentation](https://gatling.io/docs/gatling/)
- [Gatling Gradle Plugin](https://github.com/gatling/gatling-gradle-plugin)
- [Async Testing Guide](https://gatling.io/docs/gatling/tutorials/advanced/)

## 💡 Best Practices

1. **Start Small**: Run QuickSagaSimulation first
2. **Monitor Resources**: Watch RabbitMQ and service memory during tests
3. **Baseline Performance**: Run tests on isolated environment first
4. **Compare Results**: Save reports to track performance over time
5. **Gradual Load**: Use ramp-up patterns, don't shock the system

## 🎯 Next Steps

After load testing, analyze:
- RabbitMQ queue depths during peak load
- Database connection pool usage
- Flowable async executor performance
- Compensation flow timing
- Event processing latency
