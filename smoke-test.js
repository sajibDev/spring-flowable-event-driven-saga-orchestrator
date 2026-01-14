import http from 'k6/http';
import { check, sleep } from 'k6';

// Quick smoke test - 1 user, 5 orders, ~1 minute
export const options = {
  vus: 1,
  iterations: 5,
  duration: '1m',
  thresholds: {
    http_req_duration: ['p(95)<2000'],
    http_req_failed: ['rate<0.1'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

const products = [
  { productId: 'PROD-001', productName: 'Laptop', price: 1200.00 },
  { productId: 'PROD-002', productName: 'Mouse', price: 25.00 },
  { productId: 'PROD-003', productName: 'Keyboard', price: 75.00 },
  { productId: 'PROD-005', productName: 'Smartphone', price: 899.99 },
];

const addresses = [
  '123 Main St, New York, NY 10001',
  '456 Oak Ave, Los Angeles, CA 90001',
  '789 Pine Rd, Chicago, IL 60601',
];

function randomItem(array) {
  return array[Math.floor(Math.random() * array.length)];
}

export default function() {
  // Generate random order
  const order = {
    customerId: `CUST-SMOKE-${__VU}-${__ITER}-${Date.now()}`,
    shippingAddress: randomItem(addresses),
    items: [
      {
        productId: randomItem(products).productId,
        productName: randomItem(products).productName,
        quantity: Math.floor(Math.random() * 3) + 1,
        price: randomItem(products).price,
      }
    ]
  };

  // Create order
  const createResponse = http.post(
    `${BASE_URL}/api/orders`,
    JSON.stringify(order),
    { headers: { 'Content-Type': 'application/json' } }
  );

  const createChecks = check(createResponse, {
    'create order: status is 200': (r) => r.status === 200,
    'create order: has correlationId': (r) => {
      try {
        return JSON.parse(r.body).correlationId !== undefined;
      } catch (e) {
        return false;
      }
    },
  });

  if (createChecks) {
    console.log(`✓ Order created successfully - Iteration ${__ITER}`);
  } else {
    console.log(`✗ Order creation failed - Status: ${createResponse.status}`);
  }

  // Get correlationId and check status
  let correlationId = null;
  try {
    correlationId = JSON.parse(createResponse.body).correlationId;
  } catch (e) {
    console.error('Failed to parse response');
  }

  if (correlationId) {
    sleep(2); // Wait for processing

    const statusResponse = http.get(`${BASE_URL}/api/orders/${correlationId}/status`);
    
    check(statusResponse, {
      'get status: status is 200': (r) => r.status === 200,
      'get status: has response': (r) => r.body && r.body.length > 0,
    });

    console.log(`Order ${correlationId} status: ${statusResponse.body}`);
  }

  sleep(1);
}

export function handleSummary(data) {
  console.log('\n========================================');
  console.log('       SMOKE TEST RESULTS');
  console.log('========================================');
  const totalReqs = data.metrics.http_reqs && data.metrics.http_reqs.values ? data.metrics.http_reqs.values.count : 0;
  const avgDuration = data.metrics.http_req_duration && data.metrics.http_req_duration.values ? data.metrics.http_req_duration.values.avg : 0;
  const failRate = data.metrics.http_req_failed && data.metrics.http_req_failed.values ? data.metrics.http_req_failed.values.rate : 0;
  
  console.log(`Total Requests: ${totalReqs}`);
  console.log(`Avg Duration: ${avgDuration.toFixed(2)}ms`);
  console.log(`Failed: ${(failRate * 100).toFixed(2)}%`);
  console.log('========================================\n');

  return {
    'stdout': '',
  };
}
