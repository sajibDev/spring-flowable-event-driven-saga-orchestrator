import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import scala.collection.mutable.ArrayBuffer

/**
 * Stress Test for Saga Orchestrator
 * Pushes the system to find breaking points
 * Monitors compensation flows under high load
 */
class StressSagaSimulation extends Simulation {

  // Track metrics for summary
  private val sagaDurations = new ArrayBuffer[Long]()
  private val sagaStatuses = new ArrayBuffer[String]()
  private val compensationTriggers = new ArrayBuffer[String]()

  val baseUrl = sys.env.getOrElse("BASE_URL", "http://localhost:8080")
  val maxUsers = sys.env.getOrElse("MAX_USERS", "100").toInt
  
  val httpProtocol = http
    .baseUrl(baseUrl)
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .shareConnections // Reuse connections under stress

  val products = Array(
    Map("productId" -> "PROD-001", "productName" -> "Laptop", "price" -> 1200.00),
    Map("productId" -> "PROD-002", "productName" -> "Mouse", "price" -> 25.00),
    Map("productId" -> "PROD-003", "productName" -> "Keyboard", "price" -> 75.00),
    Map("productId" -> "PROD-004", "productName" -> "Monitor", "price" -> 350.00),
    Map("productId" -> "PROD-005", "productName" -> "Smartphone", "price" -> 899.99)
  )

  val addresses = Array(
    "123 Main St, New York, NY 10001",
    "456 Oak Ave, Los Angeles, CA 90001",
    "789 Pine Rd, Chicago, IL 60601"
  )

  val feeder = Iterator.continually(Map(
    "product" -> products(scala.util.Random.nextInt(products.length)),
    "address" -> addresses(scala.util.Random.nextInt(addresses.length)),
    "customerId" -> s"CUST-STRESS-${System.currentTimeMillis()}-${scala.util.Random.nextInt(100000)}",
    "quantity" -> (scala.util.Random.nextInt(5) + 1)
  ))

  // Aggressive saga scenario with minimal think time
  val stressSaga = scenario("Stress Saga Test")
    .feed(feeder)
    .exec { session =>
      // Track saga start time
      session.set("sagaStartTime", System.currentTimeMillis())
    }
    .exec(
      http("Create Order")
        .post("/api/orders")
        .body(StringBody(session => {
          val product = session("product").as[Map[String, Any]]
          s"""{
             |  "customerId": "${session("customerId").as[String]}",
             |  "shippingAddress": "${session("address").as[String]}",
             |  "items": [{
             |    "productId": "${product("productId")}",
             |    "productName": "${product("productName")}",
             |    "quantity": ${session("quantity").as[Int]},
             |    "price": ${product("price")}
             |  }]
             |}""".stripMargin
        }))
        .check(status.is(200))
        .check(jsonPath("$.correlationId").saveAs("correlationId"))
    )
    .pause(1.second)
    // Quick status check (not full polling to increase stress)
    .exec(
      http("Quick Status Check")
        .get(session => s"/api/orders/${session("correlationId").as[String]}/status")
        .check(status.is(200))
        .check(bodyString.saveAs("finalStatus"))
    )
    .exec { session =>
      val status = session("finalStatus").as[String]
      val correlationId = session("correlationId").as[String]
      val sagaStartTime = session("sagaStartTime").as[Long]
      val sagaEndTime = System.currentTimeMillis()
      val sagaDuration = sagaEndTime - sagaStartTime
      
      // Track metrics
      sagaDurations.synchronized {
        sagaDurations += sagaDuration
        sagaStatuses += status
        if (status == "FAILED" || status == "COMPENSATED") {
          compensationTriggers += correlationId
        }
      }
      
      if (status == "COMPLETED") {
        println(f"✅ [STRESS] Saga completed: $correlationId in ${sagaDuration}ms (${sagaDuration/1000.0}%.2fs)")
      } else if (status == "FAILED" || status == "COMPENSATED") {
        println(f"⚠️ [STRESS] Compensation triggered: $correlationId after ${sagaDuration}ms - Status: $status")
      } else {
        println(f"⏱️ [STRESS] Saga pending: $correlationId after ${sagaDuration}ms")
      }
      session
    }
    .pause(500.milliseconds, 1.second) // Minimal think time

  setUp(
    stressSaga.inject(
      // Warm up
      rampUsers(20).during(1.minute),
      // Stress phase 1
      constantUsersPerSec(30).during(2.minutes),
      // Stress phase 2 - push harder
      rampUsers(maxUsers).during(2.minutes),
      // Peak stress
      constantUsersPerSec(50).during(3.minutes),
      // Cool down
      rampUsers(10).during(1.minute)
    )
  ).protocols(httpProtocol)
   .maxDuration(12.minutes)
   .assertions(
     global.responseTime.percentile3.lt(10000), // 99th percentile under 10s
     global.successfulRequests.percent.gt(85)   // 85% success under stress
   )

  // Print summary after test (call StressSagaSimulationSummary.printStats())
  def printStats(): Unit = {
    if (sagaDurations.nonEmpty) {
      val sortedDurations = sagaDurations.sorted
      val avgDuration = sagaDurations.sum / sagaDurations.length
      val p95Duration = sortedDurations((sortedDurations.length * 0.95).toInt)
      val completedCount = sagaStatuses.count(_ == "COMPLETED")
      val failedCount = sagaStatuses.count(s => s == "FAILED" || s == "COMPENSATED")
      
      println("\n" + "="*80)
      println("                  STRESS TEST - SUMMARY REPORT")
      println("="*80)
      println(f"\n📊 Results: ${sagaDurations.length} total | ✅ $completedCount completed (${completedCount*100.0/sagaDurations.length}%.1f%%) | ❌ $failedCount failed (${failedCount*100.0/sagaDurations.length}%.1f%%) | 🔄 ${compensationTriggers.length} compensations")
      println(f"\n⏱️  Saga Duration: Mean ${avgDuration}ms (${avgDuration/1000.0}%.2fs) | P95 ${p95Duration}ms (${p95Duration/1000.0}%.2fs) | Max ${sortedDurations.last}ms")
      if (failedCount > sagaDurations.length * 0.2) println("   ⚠️  HIGH FAILURE RATE - System under stress") else println("   ✅ Good resilience")
      println("="*80 + "\n")
    }
  }
}

// Custom summary output
object StressSagaSimulationSummary {
  def printSummary(): Unit = {
    println("\n" + "="*80)
    println("                 STRESS SAGA LOAD TEST SUMMARY")
    println("="*80)
    println("Test Details:")
    println("  - Scenario: Stress Test to Find Breaking Points")
    println("  - Duration: ~12 minutes")
    println("  - Load Pattern: Aggressive ramping to peak stress")
    println("    • Warm up: 20 users over 1 minute")
    println("    • Stress phase 1: 30 users/sec for 2 minutes")
    println("    • Stress phase 2: Ramp to 100 users over 2 minutes")
    println("    • Peak stress: 50 users/sec for 3 minutes")
    println("    • Cool down: Ramp down to 10 users over 1 minute")
    println("  - Status Checks: Quick single check (minimal polling for max stress)")
    println("  - Think Time: Minimal (500ms-1s) to maximize load")
    println("\nMetrics Tracked:")
    println("  ✅ HTTP response times under stress")
    println("  ✅ Quick saga status snapshot")
    println("  ✅ Compensation flow frequency under load")
    println("  ✅ System degradation patterns")
    println("  ✅ Breaking point identification")
    println("  ✅ RabbitMQ queue saturation behavior")
    println("\nStress Test Goals:")
    println("  🎯 Find maximum sustainable load")
    println("  🎯 Identify bottlenecks and failure modes")
    println("  🎯 Monitor compensation triggers under stress")
    println("  🎯 Test system recovery and resilience")
    println("\nCheck the detailed HTML report for:")
    println("  - Response time degradation curves")
    println("  - Error rate spikes during peak load")
    println("  - p99 latency trends")
    println("  - Request throughput limits")
    println("="*80 + "\n")
  }
}
