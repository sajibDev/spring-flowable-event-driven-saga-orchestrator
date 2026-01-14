import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import scala.collection.mutable.ArrayBuffer

class AsyncSagaSimulation extends Simulation {

  // Track metrics for summary
  private val sagaDurations = new ArrayBuffer[Long]()
  private val pollCounts = new ArrayBuffer[Int]()
  private val sagaStatuses = new ArrayBuffer[String]()

  // Configuration
  val baseUrl = sys.env.getOrElse("BASE_URL", "http://localhost:8080")
  val thinkTimeMin = sys.env.getOrElse("THINK_TIME_MIN", "1").toInt
  val thinkTimeMax = sys.env.getOrElse("THINK_TIME_MAX", "3").toInt
  val maxPollAttempts = sys.env.getOrElse("MAX_POLL_ATTEMPTS", "30").toInt
  val pollInterval = sys.env.getOrElse("POLL_INTERVAL", "1").toInt

  // HTTP protocol configuration
  val httpProtocol = http
    .baseUrl(baseUrl)
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .userAgentHeader("Gatling-Async-Saga-Load-Test")

  // Test data - Products
  val products = Array(
    Map("productId" -> "PROD-001", "productName" -> "Laptop", "price" -> 1200.00),
    Map("productId" -> "PROD-002", "productName" -> "Mouse", "price" -> 25.00),
    Map("productId" -> "PROD-003", "productName" -> "Keyboard", "price" -> 75.00),
    Map("productId" -> "PROD-004", "productName" -> "Monitor", "price" -> 350.00),
    Map("productId" -> "PROD-005", "productName" -> "Smartphone", "price" -> 899.99),
    Map("productId" -> "PROD-006", "productName" -> "Tablet", "price" -> 599.99),
    Map("productId" -> "PROD-007", "productName" -> "Smartwatch", "price" -> 299.99),
    Map("productId" -> "PROD-008", "productName" -> "Headphones", "price" -> 149.99)
  )

  // Test data - Addresses
  val addresses = Array(
    "123 Main St, New York, NY 10001",
    "456 Oak Ave, Los Angeles, CA 90001",
    "789 Pine Rd, Chicago, IL 60601",
    "321 Elm St, Houston, TX 77001",
    "654 Maple Ave, Phoenix, AZ 85001"
  )

  // Random data feeders
  val productFeeder = Iterator.continually(Map(
    "product" -> products(scala.util.Random.nextInt(products.length))
  ))

  val addressFeeder = Iterator.continually(Map(
    "address" -> addresses(scala.util.Random.nextInt(addresses.length))
  ))

  // Scenario: Complete Async Saga Flow with Polling
  val asyncSagaScenario = scenario("Async Saga Orchestration")
    .feed(productFeeder)
    .feed(addressFeeder)
    .exec { session =>
      val timestamp = System.currentTimeMillis()
      val virtualUser = session.userId
      session
        .set("customerId", s"CUST-GATLING-$virtualUser-$timestamp")
        .set("quantity", scala.util.Random.nextInt(3) + 1)
        .set("sagaStartTime", timestamp)  // Track saga start time
    }
    .exec(
      http("Create Order")
        .post("/api/orders")
        .body(StringBody(session => {
          val product = session("product").as[Map[String, Any]]
          s"""{
             |  "customerId": "${session("customerId").as[String]}",
             |  "shippingAddress": "${session("address").as[String]}",
             |  "items": [
             |    {
             |      "productId": "${product("productId")}",
             |      "productName": "${product("productName")}",
             |      "quantity": ${session("quantity").as[Int]},
             |      "price": ${product("price")}
             |    }
             |  ]
             |}""".stripMargin
        }))
        .check(status.is(200))
        .check(jsonPath("$.correlationId").saveAs("correlationId"))
        .check(jsonPath("$.message").optional.saveAs("message"))
    )
    .exec { session =>
      println(s"[Saga] Created order with correlationId: ${session("correlationId").as[String]}")
      session
    }
    // Wait for async processing to start
    .pause(2.seconds)
    // Poll for saga completion (this is what makes it async-aware!)
    .asLongAs(session => {
      val status = session("sagaStatus").asOption[String].getOrElse("PENDING")
      val attempts = session("pollAttempts").asOption[Int].getOrElse(0)
      status == "PENDING" && attempts < maxPollAttempts
    }) {
      exec { session =>
        session.set("pollAttempts", session("pollAttempts").asOption[Int].getOrElse(0) + 1)
      }
      .exec(
        http("Check Saga Status")
          .get(session => s"/api/orders/${session("correlationId").as[String]}/status")
          .check(status.is(200))
          .check(bodyString.saveAs("sagaStatus"))
      )
      .exec { session =>
        val currentStatus = session("sagaStatus").as[String]
        val attempts = session("pollAttempts").as[Int]
        println(s"[Saga] Poll attempt $attempts - Status: $currentStatus")
        session
      }
      .doIf(session => session("sagaStatus").as[String] == "PENDING") {
        pause(pollInterval.seconds)
      }
    }
    // Validate final status and calculate saga duration
    .exec { session =>
      val finalStatus = session("sagaStatus").as[String]
      val attempts = session("pollAttempts").as[Int]
      val sagaStartTime = session("sagaStartTime").as[Long]
      val sagaEndTime = System.currentTimeMillis()
      val sagaDuration = sagaEndTime - sagaStartTime
      val correlationId = session("correlationId").as[String]
      
      // Track metrics
      sagaDurations.synchronized {
        sagaDurations += sagaDuration
        pollCounts += attempts
        sagaStatuses += finalStatus
      }
      
      if (finalStatus == "COMPLETED") {
        println(f"✅ [SAGA COMPLETE] CorrelationId: $correlationId")
        println(f"   Status: $finalStatus")
        println(f"   ⏱️  Total Duration: ${sagaDuration}ms (${sagaDuration/1000.0}%.2f seconds)")
        println(f"   📊 Polls needed: $attempts")
        println("   " + "="*70)
      } else if (finalStatus == "FAILED" || finalStatus == "COMPENSATED") {
        println(f"❌ [SAGA COMPENSATED] CorrelationId: $correlationId")
        println(f"   Status: $finalStatus")
        println(f"   ⏱️  Total Duration: ${sagaDuration}ms (${sagaDuration/1000.0}%.2f seconds)")
        println(f"   📊 Polls needed: $attempts")
        println("   " + "="*70)
      } else {
        println(f"⏱️ [SAGA TIMEOUT] CorrelationId: $correlationId")
        println(f"   Status: Still PENDING after ${sagaDuration}ms (${sagaDuration/1000.0}%.2f seconds)")
        println(f"   Max polls ($maxPollAttempts) reached - saga may still be processing")
        println("   " + "="*70)
      }
      session
    }
    .pause(thinkTimeMin.seconds, thinkTimeMax.seconds)

  // Load Test Scenarios - Multi-phase injection
  setUp(
    asyncSagaScenario.inject(
      // Phase 1: Smoke Test (Quick validation)
      atOnceUsers(5),
      nothingFor(10.seconds),
      
      // Phase 2: Ramp Up Load Test
      rampUsers(20).during(2.minutes),
      nothingFor(5.seconds),
      
      // Phase 3: Sustained Load
      constantUsersPerSec(10).during(5.minutes),
      nothingFor(5.seconds),
      
      // Phase 4: Stress Test
      rampUsers(50).during(2.minutes),
      constantUsersPerSec(25).during(3.minutes)
    ).protocols(httpProtocol)
  )
   .assertions(
     global.responseTime.max.lt(30000), // Max response time including polling
     global.successfulRequests.percent.gt(95) // 95% success rate
   )

  // Print summary after test (call AsyncSagaSimulationSummary.printStats())
  def printStats(): Unit = {
    if (sagaDurations.nonEmpty) {
      val sortedDurations = sagaDurations.sorted
      val avgDuration = sagaDurations.sum / sagaDurations.length
      val p95Duration = sortedDurations((sortedDurations.length * 0.95).toInt)
      val completedCount = sagaStatuses.count(_ == "COMPLETED")
      val failedCount = sagaStatuses.count(s => s == "FAILED" || s == "COMPENSATED")
      
      println("\n" + "="*80)
      println("                   ASYNC SAGA TEST - SUMMARY REPORT")
      println("="*80)
      println(f"\n📊 Test Summary: ${sagaDurations.length} total | ✅ $completedCount completed (${completedCount*100.0/sagaDurations.length}%.1f%%) | ❌ $failedCount failed (${failedCount*100.0/sagaDurations.length}%.1f%%)")
      println(f"\n⏱️  Saga Duration: Mean ${avgDuration}ms (${avgDuration/1000.0}%.2fs) | P95 ${p95Duration}ms (${p95Duration/1000.0}%.2fs) | Max ${sortedDurations.last}ms")
      println(f"🔄 Polls: Avg ${pollCounts.sum.toDouble/pollCounts.length}%.1f | Min ${pollCounts.min} | Max ${pollCounts.max}")
      println("="*80 + "\n")
    }
  }
}

// Custom summary output
object AsyncSagaSimulationSummary {
  def printSummary(): Unit = {
    println("\n" + "="*80)
    println("                 ASYNC SAGA LOAD TEST SUMMARY")
    println("="*80)
    println("Test Details:")
    println("  - Scenario: Full Async Saga Orchestration with Complete Polling")
    println("  - Duration: ~15 minutes")
    println("  - Load Pattern: Multiple phases (smoke → ramp → sustained → stress)")
    println("    • Phase 1: 5 instant users (smoke)")
    println("    • Phase 2: 20 users over 2 minutes (ramp)")
    println("    • Phase 3: 10 users/sec for 5 minutes (sustained)")
    println("    • Phase 4: 50 users + 25 users/sec for 5 minutes (stress)")
    println("  - Max Polls: 30 attempts per saga")
    println("  - Poll Interval: 1 second")
    println("\nMetrics Tracked:")
    println("  ✅ HTTP response times (order creation)")
    println("  ✅ End-to-end saga orchestration duration")
    println("  ✅ Saga completion status (COMPLETED/FAILED/COMPENSATED/TIMEOUT)")
    println("  ✅ Number of polls needed per saga")
    println("  ✅ Success/failure/compensation rates")
    println("  ✅ Async event processing latency")
    println("\nCheck the detailed HTML report for:")
    println("  - Response time percentiles (p50, p75, p95, p99)")
    println("  - Requests per second trends")
    println("  - Error rates by phase")
    println("  - Timeline and distribution charts")
    println("  - Per-request type breakdown")
    println("="*80 + "\n")
  }
}
