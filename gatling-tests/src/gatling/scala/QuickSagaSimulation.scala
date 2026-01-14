import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import scala.collection.mutable.ArrayBuffer

/**
 * Quick Saga Load Test - Fast validation with async tracking
 * Duration: ~2 minutes
 * Users: 10 concurrent users
 */
class QuickSagaSimulation extends Simulation {

  // Track metrics for summary
  private val sagaDurations = new ArrayBuffer[Long]()
  private val pollCounts = new ArrayBuffer[Int]()

  val baseUrl = sys.env.getOrElse("BASE_URL", "http://localhost:8080")
  
  val httpProtocol = http
    .baseUrl(baseUrl)
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")

  val products = Array(
    Map("productId" -> "PROD-001", "productName" -> "Laptop", "price" -> 1200.00),
    Map("productId" -> "PROD-002", "productName" -> "Mouse", "price" -> 25.00),
    Map("productId" -> "PROD-003", "productName" -> "Keyboard", "price" -> 75.00)
  )

  val addresses = Array(
    "123 Main St, New York, NY 10001",
    "456 Oak Ave, Los Angeles, CA 90001"
  )

  val feeder = Iterator.continually(Map(
    "product" -> products(scala.util.Random.nextInt(products.length)),
    "address" -> addresses(scala.util.Random.nextInt(addresses.length)),
    "customerId" -> s"CUST-QUICK-${System.currentTimeMillis()}-${scala.util.Random.nextInt(10000)}"
  ))

  val quickSaga = scenario("Quick Saga Test")
    .feed(feeder)
    .exec { session =>
      // Track saga start time for end-to-end duration measurement
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
             |    "quantity": 1,
             |    "price": ${product("price")}
             |  }]
             |}""".stripMargin
        }))
        .check(status.is(200))
        .check(jsonPath("$.correlationId").saveAs("correlationId"))
    )
    .pause(2.seconds)
    // Simple polling - check 10 times max
    .repeat(10, "pollCount") {
      exec(
        http("Poll Status")
          .get(session => s"/api/orders/${session("correlationId").as[String]}/status")
          .check(status.is(200))
          .check(bodyString.saveAs("status"))
      )
      .doIf(session => session("status").as[String] == "PENDING") {
        pause(1.second)
      }
      .doIf(session => session("status").as[String] != "PENDING") {
        exec { session =>
          // Calculate total saga orchestration duration
          val sagaStartTime = session("sagaStartTime").as[Long]
          val sagaEndTime = System.currentTimeMillis()
          val sagaDuration = sagaEndTime - sagaStartTime
          val status = session("status").as[String]
          val correlationId = session("correlationId").as[String]
          val pollCount = session("pollCount").as[Int] + 1
          
          // Track metrics
          sagaDurations.synchronized {
            sagaDurations += sagaDuration
            pollCounts += pollCount
          }
          
          println(f"✅ [SAGA COMPLETE] CorrelationId: $correlationId")
          println(f"   Status: $status")
          println(f"   ⏱️  Total Duration: ${sagaDuration}ms (${sagaDuration/1000.0}%.2f seconds)")
          println(f"   📊 Polls needed: $pollCount")
          println("   " + "="*70)
          
          session
        }
      }
    }
    .exec { session =>
      // Handle timeout case - saga still pending after max polls
      if (session("status").asOption[String].getOrElse("PENDING") == "PENDING") {
        val sagaStartTime = session("sagaStartTime").as[Long]
        val sagaEndTime = System.currentTimeMillis()
        val sagaDuration = sagaEndTime - sagaStartTime
        val correlationId = session("correlationId").as[String]
        
        println(f"⏱️ [SAGA TIMEOUT] CorrelationId: $correlationId")
        println(f"   Status: Still PENDING after ${sagaDuration}ms (${sagaDuration/1000.0}%.2f seconds)")
        println(f"   Max polls (10) reached - saga may still be processing")
        println("   " + "="*70)
      }
      session
    }

  setUp(
    quickSaga.inject(
      atOnceUsers(5),
      rampUsers(10).during(1.minute)
    )
  ).protocols(httpProtocol)
   .maxDuration(2.minutes)
   .assertions(
     global.responseTime.max.lt(15000),  // Max response time including polling
     global.successfulRequests.percent.gt(90)  // 90% success rate
   )

  // Print summary after test (call QuickSagaSimulationSummary.printStats())
  def printStats(): Unit = {
    if (sagaDurations.nonEmpty) {
      val sortedDurations = sagaDurations.sorted
      val avgDuration = sagaDurations.sum / sagaDurations.length
      val p95Duration = sortedDurations((sortedDurations.length * 0.95).toInt)
      
      println("\n" + "="*80)
      println("                    QUICK SAGA TEST - SUMMARY REPORT")
      println("="*80)
      println(f"\n📊 Test Summary:")
      println(f"   Total Sagas: ${sagaDurations.length}")
      println(f"\n⏱️  Saga Duration:")
      println(f"   Mean: ${avgDuration}ms (${avgDuration/1000.0}%.2f seconds)")
      println(f"   Min: ${sortedDurations.head}ms")
      println(f"   Max: ${sortedDurations.last}ms")
      println(f"   95th Percentile: ${p95Duration}ms (${p95Duration/1000.0}%.2f seconds)")
      println(f"\n🔄 Polls: Avg ${pollCounts.sum.toDouble/pollCounts.length}%.1f | Min ${pollCounts.min} | Max ${pollCounts.max}")
      println("="*80 + "\n")
    }
  }
}

// Custom summary output
object QuickSagaSimulationSummary {
  def printSummary(): Unit = {
    println("\n" + "="*80)
    println("                 QUICK SAGA LOAD TEST SUMMARY")
    println("="*80)
    println("Test Details:")
    println("  - Scenario: Quick Saga Test with Async Tracking")
    println("  - Duration: ~2 minutes")
    println("  - Load Pattern: 5 instant users + 10 ramped users over 1 minute")
    println("  - Max Polls: 10 attempts per saga")
    println("  - Poll Interval: 1 second")
    println("\nMetrics Tracked:")
    println("  ✅ HTTP response times (order creation)")
    println("  ✅ End-to-end saga orchestration duration")
    println("  ✅ Saga completion status (COMPLETED/FAILED/TIMEOUT)")
    println("  ✅ Number of polls needed per saga")
    println("  ✅ Success/failure rates")
    println("\nCheck the detailed HTML report for:")
    println("  - Response time percentiles (p50, p95, p99)")
    println("  - Requests per second")
    println("  - Error rates and types")
    println("  - Timeline charts")
    println("="*80 + "\n")
  }
}
