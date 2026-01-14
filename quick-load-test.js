import http from 'k6/http';
import { check, sleep } from 'k6';

// Quick load test - Ramp up to 10 users over 5 minutes
export const options = {
  stages: [
    { duration: '1m', target: 5 },    // Ramp up to 5 users
    { duration: '2m', target: 10 },   // Ramp to 10 users
    { duration: '1m', target: 10 },   // Hold at 10 users
    { duration: '1m', target: 0 },    // Ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<2500'],
    http_req_failed: ['rate<0.1'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

const products = [
  { productId: 'PROD-001', productName: 'Laptop', price: 1200.00 },
  { productId: 'PROD-002', productName: 'Mouse', price: 25.00 },
  { productId: 'PROD-003', productName: 'Keyboard', price: 75.00 },
  { productId: 'PROD-004', productName: 'Monitor', price: 350.00 },
  { productId: 'PROD-005', productName: 'Smartphone', price: 899.99 },
];

const addresses = [
  '123 Main St, New York, NY 10001',
  '456 Oak Ave, Los Angeles, CA 90001',
  '789 Pine Rd, Chicago, IL 60601',
  '321 Elm St, Houston, TX 77001',
];

function randomInt(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

function randomItem(array) {
  return array[Math.floor(Math.random() * array.length)];
}

export default function() {
  // Generate order with 1-3 items
  const numItems = randomInt(1, 3);
  const items = [];
  
  for (let i = 0; i < numItems; i++) {
    const product = randomItem(products);
    items.push({
      productId: product.productId,
      productName: product.productName,
      quantity: randomInt(1, 3),
      price: product.price,
    });
  }

  const order = {
    customerId: `CUST-LOAD-${__VU}-${__ITER}`,
    shippingAddress: randomItem(addresses),
    items: items,
  };

  // Create order
  const response = http.post(
    `${BASE_URL}/api/orders`,
    JSON.stringify(order),
    { 
      headers: { 'Content-Type': 'application/json' },
      tags: { name: 'CreateOrder' },
    }
  );

  check(response, {
    'status is 200': (r) => r.status === 200,
    'response time < 3s': (r) => r.timings.duration < 3000,
  });

  // Random think time between 1-4 seconds
  sleep(randomInt(1, 4));
}

export function handleSummary(data) {
  const reqs = data.metrics.http_reqs && data.metrics.http_reqs.values ? data.metrics.http_reqs.values.count : 0;
  const avgDuration = data.metrics.http_req_duration && data.metrics.http_req_duration.values ? data.metrics.http_req_duration.values.avg : 0;
  const p95Duration = data.metrics.http_req_duration && data.metrics.http_req_duration.values ? data.metrics.http_req_duration.values['p(95)'] : 0;
  const failRate = data.metrics.http_req_failed && data.metrics.http_req_failed.values ? data.metrics.http_req_failed.values.rate : 0;

  console.log('\n========================================')
  console.log('      QUICK LOAD TEST RESULTS');
  console.log('========================================')
  console.log(`Total Requests:    ${reqs}`);
  console.log(`Avg Duration:      ${avgDuration.toFixed(2)}ms`);
  console.log(`P95 Duration:      ${p95Duration.toFixed(2)}ms`);
  console.log(`Failure Rate:      ${(failRate * 100).toFixed(2)}%`);
  console.log('========================================\n');

  return {
    'stdout': '',
    'quick-load-results.json': JSON.stringify(data, null, 2),
  };
}
