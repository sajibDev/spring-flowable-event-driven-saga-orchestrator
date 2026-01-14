import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import { SharedArray } from 'k6/data';

// ============================================================================
// Custom Metrics
// ============================================================================
const orderCreationDuration = new Trend('order_creation_duration', true);
const orderStatusDuration = new Trend('order_status_duration', true);
const orderCreationSuccess = new Rate('order_creation_success');
const orderStatusSuccess = new Rate('order_status_success');
const ordersCreated = new Counter('orders_created');
const statusChecks = new Counter('status_checks');
const sagaCompletions = new Counter('saga_completions');
const sagaFailures = new Counter('saga_failures');

// ============================================================================
// Configuration
// ============================================================================
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const THINK_TIME_MIN = parseFloat(__ENV.THINK_TIME_MIN) || 1;
const THINK_TIME_MAX = parseFloat(__ENV.THINK_TIME_MAX) || 3;
const STATUS_CHECK_RETRIES = parseInt(__ENV.STATUS_CHECK_RETRIES) || 5;
const STATUS_CHECK_DELAY = parseFloat(__ENV.STATUS_CHECK_DELAY) || 2;

// ============================================================================
// Test Data
// ============================================================================
const products = new SharedArray('products', function() {
  return [
    { productId: 'PROD-001', productName: 'Laptop', price: 1200.00, weight: 2.5 },
    { productId: 'PROD-002', productName: 'Mouse', price: 25.00, weight: 0.1 },
    { productId: 'PROD-003', productName: 'Keyboard', price: 75.00, weight: 0.5 },
    { productId: 'PROD-004', productName: 'Monitor', price: 350.00, weight: 5.0 },
    { productId: 'PROD-005', productName: 'Smartphone', price: 899.99, weight: 0.2 },
    { productId: 'PROD-006', productName: 'Tablet', price: 599.99, weight: 0.5 },
    { productId: 'PROD-007', productName: 'Smartwatch', price: 299.99, weight: 0.1 },
    { productId: 'PROD-008', productName: 'Headphones', price: 149.99, weight: 0.3 },
    { productId: 'PROD-009', productName: 'Webcam', price: 89.99, weight: 0.2 },
    { productId: 'PROD-010', productName: 'USB Hub', price: 45.00, weight: 0.1 },
  ];
});

const addresses = new SharedArray('addresses', function() {
  return [
    '123 Main St, New York, NY 10001',
    '456 Oak Ave, Los Angeles, CA 90001',
    '789 Pine Rd, Chicago, IL 60601',
    '321 Elm St, Houston, TX 77001',
    '654 Maple Ave, Phoenix, AZ 85001',
    '987 Cedar Ln, Philadelphia, PA 19101',
    '147 Birch Dr, San Antonio, TX 78201',
    '258 Walnut St, San Diego, CA 92101',
    '369 Cherry Blvd, Dallas, TX 75201',
    '741 Spruce Way, San Jose, CA 95101',
  ];
});

// ============================================================================
// Test Scenarios Configuration
// ============================================================================
export const options = {
  scenarios: {
    // Scenario 1: Smoke Test - Quick validation
    smoke_test: {
      executor: 'shared-iterations',
      vus: 1,
      iterations: 5,
      maxDuration: '1m',
      tags: { scenario: 'smoke' },
      exec: 'smokeTest',
      startTime: '0s',
    },
    
    // Scenario 2: Load Test - Normal load simulation
    load_test: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '1m', target: 10 },   // Ramp up
        { duration: '3m', target: 25 },   // Ramp to normal load
        { duration: '5m', target: 25 },   // Stay at normal load
        { duration: '2m', target: 0 },    // Ramp down
      ],
      gracefulRampDown: '30s',
      tags: { scenario: 'load' },
      exec: 'loadTest',
      startTime: '1m30s',  // Start after smoke test
    },
    
    // Scenario 3: Stress Test - Find breaking points
    stress_test: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '2m', target: 50 },   // Ramp up aggressively
        { duration: '3m', target: 100 },  // Push to stress level
        { duration: '5m', target: 100 },  // Hold at stress level
        { duration: '3m', target: 150 },  // Push beyond normal capacity
        { duration: '2m', target: 0 },    // Ramp down
      ],
      gracefulRampDown: '1m',
      tags: { scenario: 'stress' },
      exec: 'stressTest',
      startTime: '13m',  // Start after load test
    },
    
    // Scenario 4: Spike Test - Sudden traffic spikes
    spike_test: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 10 },  // Baseline
        { duration: '10s', target: 100 }, // Spike!
        { duration: '1m', target: 100 },  // Hold spike
        { duration: '10s', target: 10 },  // Drop back
        { duration: '30s', target: 10 },  // Recovery
        { duration: '10s', target: 150 }, // Another spike!
        { duration: '1m', target: 150 },  // Hold spike
        { duration: '30s', target: 0 },   // Ramp down
      ],
      gracefulRampDown: '30s',
      tags: { scenario: 'spike' },
      exec: 'spikeTest',
      startTime: '28m',  // Start after stress test
    },
    
    // Scenario 5: Soak Test - Long duration stability
    soak_test: {
      executor: 'constant-vus',
      vus: 30,
      duration: '15m',
      tags: { scenario: 'soak' },
      exec: 'soakTest',
      startTime: '33m',  // Start after spike test
    },
  },
  
  thresholds: {
    // Global thresholds
    http_req_duration: ['p(95)<3000', 'p(99)<5000'],
    http_req_failed: ['rate<0.05'],
    
    // Scenario-specific thresholds
    'http_req_duration{scenario:smoke}': ['p(95)<1500'],
    'http_req_duration{scenario:load}': ['p(95)<2000'],
    'http_req_duration{scenario:stress}': ['p(95)<4000'],
    'http_req_duration{scenario:spike}': ['p(95)<5000'],
    'http_req_duration{scenario:soak}': ['p(95)<2500'],
    
    // Custom metric thresholds
    order_creation_duration: ['p(95)<3000', 'avg<1500'],
    order_status_duration: ['p(95)<1000', 'avg<500'],
    order_creation_success: ['rate>0.95'],
    order_status_success: ['rate>0.98'],
  },
  
  // Output configuration
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
};

// ============================================================================
// Helper Functions
// ============================================================================
function randomIntBetween(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

function randomItem(array) {
  return array[Math.floor(Math.random() * array.length)];
}

function generateOrderPayload(vuId, iteration) {
  const numItems = randomIntBetween(1, 4);
  const items = [];
  const selectedProducts = new Set();

  for (let i = 0; i < numItems; i++) {
    let product;
    // Ensure unique products in order
    do {
      product = randomItem(products);
    } while (selectedProducts.has(product.productId) && selectedProducts.size < products.length);
    
    selectedProducts.add(product.productId);
    items.push({
      productId: product.productId,
      productName: product.productName,
      quantity: randomIntBetween(1, 5),
      price: product.price,
    });
  }

  return {
    customerId: `CUST-${vuId}-${iteration}-${Date.now()}`,
    shippingAddress: randomItem(addresses),
    items: items,
  };
}

function createOrder(scenarioTag) {
  const payload = generateOrderPayload(__VU, __ITER);
  const params = {
    headers: {
      'Content-Type': 'application/json',
      'Accept': 'application/json',
      'X-Request-ID': `${__VU}-${__ITER}-${Date.now()}`,
    },
    tags: { 
      name: 'CreateOrder',
      scenario: scenarioTag,
    },
    timeout: '30s',
  };

  const startTime = Date.now();
  const response = http.post(`${BASE_URL}/api/orders`, JSON.stringify(payload), params);
  const duration = Date.now() - startTime;

  orderCreationDuration.add(duration);

  const isSuccess = check(response, {
    'create order: status is 200': (r) => r.status === 200,
    'create order: response time < 5s': (r) => r.timings.duration < 5000,
    'create order: has correlationId': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.correlationId !== undefined && body.correlationId !== null;
      } catch (e) {
        return false;
      }
    },
    'create order: has status': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.status !== undefined;
      } catch (e) {
        return false;
      }
    },
    'create order: valid JSON response': (r) => {
      try {
        JSON.parse(r.body);
        return true;
      } catch (e) {
        return false;
      }
    },
  });

  orderCreationSuccess.add(isSuccess);
  
  if (isSuccess) {
    ordersCreated.add(1);
  }

  let correlationId = null;
  try {
    const body = JSON.parse(response.body);
    correlationId = body.correlationId;
  } catch (e) {
    console.error(`Failed to parse order response: ${e.message}`);
  }

  return { 
    success: isSuccess, 
    correlationId: correlationId,
    duration: duration,
    status: response.status,
  };
}

function checkOrderStatus(correlationId, scenarioTag, retries = STATUS_CHECK_RETRIES) {
  if (!correlationId) {
    return { success: false, finalStatus: null };
  }

  const params = {
    headers: {
      'Accept': 'application/json',
      'X-Request-ID': `status-${__VU}-${__ITER}-${Date.now()}`,
    },
    tags: { 
      name: 'GetOrderStatus',
      scenario: scenarioTag,
    },
    timeout: '10s',
  };

  let finalStatus = null;
  let success = false;

  for (let attempt = 0; attempt < retries; attempt++) {
    const startTime = Date.now();
    const response = http.get(`${BASE_URL}/api/orders/${correlationId}/status`, params);
    const duration = Date.now() - startTime;

    orderStatusDuration.add(duration);
    statusChecks.add(1);

    const checkResult = check(response, {
      'get status: status is 200': (r) => r.status === 200,
      'get status: response time < 2s': (r) => r.timings.duration < 2000,
      'get status: has response body': (r) => r.body && r.body.length > 0,
    });

    orderStatusSuccess.add(checkResult);

    if (response.status === 200) {
      finalStatus = response.body;
      
      // Check if saga completed (success or failure)
      const statusUpper = (finalStatus || '').toUpperCase();
      if (statusUpper.includes('COMPLETED') || statusUpper.includes('SUCCESS')) {
        sagaCompletions.add(1);
        success = true;
        break;
      } else if (statusUpper.includes('FAILED') || statusUpper.includes('CANCELLED') || statusUpper.includes('COMPENSATED')) {
        sagaFailures.add(1);
        success = true;  // Request was successful, saga failed
        break;
      }
    }

    // Wait before next retry
    if (attempt < retries - 1) {
      sleep(STATUS_CHECK_DELAY);
    }
  }

  return { success: success, finalStatus: finalStatus };
}

function thinkTime(min = THINK_TIME_MIN, max = THINK_TIME_MAX) {
  sleep(randomIntBetween(min * 1000, max * 1000) / 1000);
}

// ============================================================================
// Test Scenarios
// ============================================================================

// Smoke Test: Quick validation that the system is working
export function smokeTest() {
  group('Smoke Test - Basic Functionality', function() {
    const orderResult = createOrder('smoke');
    
    if (orderResult.success && orderResult.correlationId) {
      sleep(1);
      checkOrderStatus(orderResult.correlationId, 'smoke', 3);
    }
    
    thinkTime(0.5, 1);
  });
}

// Load Test: Normal expected load
export function loadTest() {
  group('Load Test - Normal Operations', function() {
    // Create order
    const orderResult = createOrder('load');
    
    if (orderResult.success && orderResult.correlationId) {
      // Brief wait before checking status
      sleep(randomIntBetween(1, 3));
      
      // Check order status with retries
      checkOrderStatus(orderResult.correlationId, 'load', STATUS_CHECK_RETRIES);
    }
    
    thinkTime();
  });
}

// Stress Test: Push the system beyond normal load
export function stressTest() {
  group('Stress Test - High Load', function() {
    // Rapidly create orders with minimal think time
    const orderResult = createOrder('stress');
    
    if (orderResult.success && orderResult.correlationId) {
      // Quick status check
      sleep(0.5);
      checkOrderStatus(orderResult.correlationId, 'stress', 2);
    }
    
    // Minimal think time under stress
    thinkTime(0.5, 1.5);
  });
}

// Spike Test: Handle sudden traffic spikes
export function spikeTest() {
  group('Spike Test - Traffic Surge', function() {
    const orderResult = createOrder('spike');
    
    if (orderResult.success && orderResult.correlationId) {
      sleep(1);
      checkOrderStatus(orderResult.correlationId, 'spike', 3);
    }
    
    thinkTime(0.5, 2);
  });
}

// Soak Test: Long-running stability test
export function soakTest() {
  group('Soak Test - Sustained Load', function() {
    const orderResult = createOrder('soak');
    
    if (orderResult.success && orderResult.correlationId) {
      sleep(2);
      checkOrderStatus(orderResult.correlationId, 'soak', STATUS_CHECK_RETRIES);
    }
    
    // Normal think time for realistic sustained load
    thinkTime(2, 4);
  });
}

// Default function (used when no scenario is specified)
export default function() {
  loadTest();
}

// ============================================================================
// Lifecycle Hooks
// ============================================================================
export function setup() {
  console.log('='.repeat(60));
  console.log('Starting Load Test for Saga Orchestrator');
  console.log(`Base URL: ${BASE_URL}`);
  console.log(`Timestamp: ${new Date().toISOString()}`);
  console.log('='.repeat(60));
  
  // Try a simple request to verify connectivity
  const testPayload = {
    customerId: 'SETUP-TEST',
    shippingAddress: '123 Test St, Test City, TS 00000',
    items: [{ productId: 'PROD-001', productName: 'Test', quantity: 1, price: 1.00 }],
  };
  
  const testResponse = http.post(`${BASE_URL}/api/orders`, JSON.stringify(testPayload), {
    headers: { 'Content-Type': 'application/json' },
    timeout: '30s',
    tags: { name: 'SetupTest' },
  });
  
  if (testResponse.status !== 200) {
    console.warn(`Warning: Setup test returned status ${testResponse.status}`);
    console.warn(`Response: ${testResponse.body}`);
  } else {
    console.log('Setup test successful - service is responding');
  }
  
  return {
    startTime: Date.now(),
    baseUrl: BASE_URL,
  };
}

export function teardown(data) {
  const duration = ((Date.now() - data.startTime) / 1000 / 60).toFixed(2);
  console.log('='.repeat(60));
  console.log('Load Test Completed');
  console.log(`Total Duration: ${duration} minutes`);
  console.log(`Timestamp: ${new Date().toISOString()}`);
  console.log('='.repeat(60));
}

// ============================================================================
// Summary Handler
// ============================================================================
export function handleSummary(data) {
  const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
  
  // Build custom summary
  let summary = generateTextSummary(data);
  
  return {
    'stdout': summary,
    [`load-test-results-${timestamp}.json`]: JSON.stringify(data, null, 2),
    'load-test-results-latest.json': JSON.stringify(data, null, 2),
  };
}

function generateTextSummary(data) {
  let output = '\n';
  output += '╔══════════════════════════════════════════════════════════════════╗\n';
  output += '║           SAGA ORCHESTRATOR LOAD TEST RESULTS                    ║\n';
  output += '╠══════════════════════════════════════════════════════════════════╣\n';
  
  // Test execution info
  output += `║ Timestamp: ${new Date().toISOString().padEnd(52)}║\n`;
  output += '╠══════════════════════════════════════════════════════════════════╣\n';
  
  // Request metrics
  output += '║ REQUEST METRICS                                                  ║\n';
  output += '╠──────────────────────────────────────────────────────────────────╣\n';
  
  if (data.metrics.http_reqs) {
    const reqs = data.metrics.http_reqs;
    output += `║ Total Requests:     ${String(reqs.values.count).padEnd(46)}║\n`;
    output += `║ Request Rate:       ${(reqs.values.rate || 0).toFixed(2).padEnd(43)}req/s║\n`;
  }
  
  if (data.metrics.http_req_duration) {
    const dur = data.metrics.http_req_duration.values;
    output += '╠──────────────────────────────────────────────────────────────────╣\n';
    output += '║ RESPONSE TIMES                                                   ║\n';
    output += '╠──────────────────────────────────────────────────────────────────╣\n';
    output += `║ Average:            ${(dur.avg || 0).toFixed(2).padEnd(43)}ms ║\n`;
    output += `║ Minimum:            ${(dur.min || 0).toFixed(2).padEnd(43)}ms ║\n`;
    output += `║ Maximum:            ${(dur.max || 0).toFixed(2).padEnd(43)}ms ║\n`;
    output += `║ Median:             ${(dur.med || 0).toFixed(2).padEnd(43)}ms ║\n`;
    output += `║ P90:                ${(dur['p(90)'] || 0).toFixed(2).padEnd(43)}ms ║\n`;
    output += `║ P95:                ${(dur['p(95)'] || 0).toFixed(2).padEnd(43)}ms ║\n`;
    output += `║ P99:                ${(dur['p(99)'] || 0).toFixed(2).padEnd(43)}ms ║\n`;
  }
  
  if (data.metrics.http_req_failed) {
    const failed = data.metrics.http_req_failed.values;
    output += '╠──────────────────────────────────────────────────────────────────╣\n';
    output += '║ ERROR RATE                                                       ║\n';
    output += '╠──────────────────────────────────────────────────────────────────╣\n';
    output += `║ Failed Requests:    ${((failed.rate || 0) * 100).toFixed(2).padEnd(44)}% ║\n`;
  }
  
  // Custom metrics
  output += '╠══════════════════════════════════════════════════════════════════╣\n';
  output += '║ SAGA METRICS                                                     ║\n';
  output += '╠──────────────────────────────────────────────────────────────────╣\n';
  
  if (data.metrics.orders_created) {
    output += `║ Orders Created:     ${String(data.metrics.orders_created.values.count || 0).padEnd(46)}║\n`;
  }
  if (data.metrics.saga_completions) {
    output += `║ Saga Completions:   ${String(data.metrics.saga_completions.values.count || 0).padEnd(46)}║\n`;
  }
  if (data.metrics.saga_failures) {
    output += `║ Saga Failures:      ${String(data.metrics.saga_failures.values.count || 0).padEnd(46)}║\n`;
  }
  if (data.metrics.status_checks) {
    output += `║ Status Checks:      ${String(data.metrics.status_checks.values.count || 0).padEnd(46)}║\n`;
  }
  
  if (data.metrics.order_creation_success) {
    output += `║ Order Success Rate: ${((data.metrics.order_creation_success.values.rate || 0) * 100).toFixed(2).padEnd(44)}% ║\n`;
  }
  
  // Thresholds
  output += '╠══════════════════════════════════════════════════════════════════╣\n';
  output += '║ THRESHOLD RESULTS                                                ║\n';
  output += '╠──────────────────────────────────────────────────────────────────╣\n';
  
  let passedThresholds = 0;
  let failedThresholds = 0;
  
  if (data.thresholds) {
    for (const [name, threshold] of Object.entries(data.thresholds)) {
      const status = threshold.ok ? '✓ PASS' : '✗ FAIL';
      if (threshold.ok) passedThresholds++;
      else failedThresholds++;
      output += `║ ${name.substring(0, 40).padEnd(40)} ${status.padEnd(24)}║\n`;
    }
  }
  
  output += '╠──────────────────────────────────────────────────────────────────╣\n';
  output += `║ Thresholds: ${passedThresholds} passed, ${failedThresholds} failed`.padEnd(67) + '║\n';
  output += '╚══════════════════════════════════════════════════════════════════╝\n';
  
  return output;
}
