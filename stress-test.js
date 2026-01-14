import http from 'k6/http';
import { check, sleep } from 'k6';

// Stress test - Push to 50 users to find breaking points
export const options = {
  stages: [
    { duration: '2m', target: 20 },   // Warm up
    { duration: '3m', target: 50 },   // Push to stress level
    { duration: '3m', target: 50 },   // Hold at stress
    { duration: '2m', target: 0 },    // Cool down
  ],
  thresholds: {
    http_req_duration: ['p(95)<5000'],
    http_req_failed: ['rate<0.15'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

const products = [
  { productId: 'PROD-001', productName: 'Laptop', price: 1200.00 },
  { productId: 'PROD-002', productName: 'Mouse', price: 25.00 },
  { productId: 'PROD-003', productName: 'Keyboard', price: 75.00 },
  { productId: 'PROD-004', productName: 'Monitor', price: 350.00 },
  { productId: 'PROD-005', productName: 'Smartphone', price: 899.99 },
  { productId: 'PROD-006', productName: 'Tablet', price: 599.99 },
  { productId: 'PROD-007', productName: 'Smartwatch', price: 299.99 },
];

const addresses = [
  '123 Main St, New York, NY 10001',
  '456 Oak Ave, Los Angeles, CA 90001',
  '789 Pine Rd, Chicago, IL 60601',
  '321 Elm St, Houston, TX 77001',
  '654 Maple Ave, Phoenix, AZ 85001',
];

function randomInt(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

function randomItem(array) {
  return array[Math.floor(Math.random() * array.length)];
}

export default function() {
  const numItems = randomInt(1, 4);
  const items = [];
  
  for (let i = 0; i < numItems; i++) {
    const product = randomItem(products);
    items.push({
      productId: product.productId,
      productName: product.productName,
      quantity: randomInt(1, 5),
      price: product.price,
    });
  }

  const order = {
    customerId: `CUST-STRESS-${__VU}-${__ITER}`,
    shippingAddress: randomItem(addresses),
    items: items,
  };

  const response = http.post(
    `${BASE_URL}/api/orders`,
    JSON.stringify(order),
    { 
      headers: { 'Content-Type': 'application/json' },
      timeout: '30s',
    }
  );

  const success = check(response, {
    'status is 200': (r) => r.status === 200,
    'response time < 5s': (r) => r.timings.duration < 5000,
  });

  if (!success) {
    console.log(`Request failed - VU:${__VU}, Iter:${__ITER}, Status:${response.status}`);
  }

  // Minimal think time during stress
  sleep(randomInt(1, 2));
}

export function handleSummary(data) {
  const reqs = data.metrics.http_reqs && data.metrics.http_reqs.values ? data.metrics.http_reqs.values.count : 0;
  const rate = data.metrics.http_reqs && data.metrics.http_reqs.values ? data.metrics.http_reqs.values.rate : 0;
  const avgDuration = data.metrics.http_req_duration && data.metrics.http_req_duration.values ? data.metrics.http_req_duration.values.avg : 0;
  const maxDuration = data.metrics.http_req_duration && data.metrics.http_req_duration.values ? data.metrics.http_req_duration.values.max : 0;
  const p95Duration = data.metrics.http_req_duration && data.metrics.http_req_duration.values ? data.metrics.http_req_duration.values['p(95)'] : 0;
  const p99Duration = data.metrics.http_req_duration && data.metrics.http_req_duration.values ? data.metrics.http_req_duration.values['p(99)'] : 0;
  const failRate = data.metrics.http_req_failed && data.metrics.http_req_failed.values ? data.metrics.http_req_failed.values.rate * 100 : 0;

  console.log('\n========================================');
  console.log('       STRESS TEST RESULTS');
  console.log('========================================');
  console.log(`Total Requests:    ${reqs}`);
  console.log(`Request Rate:      ${rate.toFixed(2)} req/s`);
  console.log(`Avg Duration:      ${avgDuration.toFixed(2)}ms`);
  console.log(`Max Duration:      ${maxDuration.toFixed(2)}ms`);
  console.log(`P95 Duration:      ${p95Duration.toFixed(2)}ms`);
  console.log(`P99 Duration:      ${p99Duration.toFixed(2)}ms`);
  console.log(`Failure Rate:      ${failRate.toFixed(2)}%`);
  console.log('========================================\n');

  return {
    'stdout': '',
    'stress-test-results.json': JSON.stringify(data, null, 2),
  };
}
