import http from 'k6/http';
import { check, sleep, group } from 'k6';

export let options = {
  stages: [
    { duration: '1m', target: 25 },    // Ramp up to 25 users
    { duration: '2m', target: 50 },    // Ramp up to 50 users
    { duration: '3m', target: 100 },   // Ramp up to 100 users
    { duration: '2m', target: 150 },   // Ramp up to 150 users
    { duration: '5m', target: 150 },   // Hold at 150 users
    { duration: '2m', target: 75 },    // Ramp down to 75 users
    { duration: '1m', target: 0 },     // Ramp down to 0 users
  ],
  thresholds: {
    http_req_duration: ['p(95)<2500', 'p(99)<3500'],  // 95% of requests must complete below 2.5s, 99% below 3.5s
    // Adjusted for intentional 30% failure rate (20% shipping + 10% payment)
    http_req_failed: ['rate<0.35'],  // Error rate must be below 35% (allowing some margin)
  },
};

const BASE_URL = 'http://localhost:8080';
const products = [
  { productId: 'PROD-001', productName: 'Laptop', price: 1200.00 },
  { productId: 'PROD-002', productName: 'Mouse', price: 25.00 },
  { productId: 'PROD-003', productName: 'Keyboard', price: 75.00 },
  { productId: 'PROD-004', productName: 'Monitor', price: 350.00 },
  { productId: 'PROD-005', productName: 'Smartphone', price: 899.99 },
  { productId: 'PROD-006', productName: 'Tablet', price: 599.99 },
  { productId: 'PROD-007', productName: 'Smartwatch', price: 299.99 },
];

const cities = [
  '123 Main St, New York, NY 10001',
  '456 Oak Ave, Los Angeles, CA 90001',
  '789 Pine Rd, Chicago, IL 60601',
  '321 Elm St, Houston, TX 77001',
  '654 Maple Ave, Phoenix, AZ 85001',
];

function generateOrder() {
  const customerId = `CUST-${__VU}-${__ITER}`;
  const numItems = Math.floor(Math.random() * 3) + 1;
  const items = [];

  for (let i = 0; i < numItems; i++) {
    const product = products[Math.floor(Math.random() * products.length)];
    items.push({
      productId: product.productId,
      productName: product.productName,
      quantity: Math.floor(Math.random() * 3) + 1,
      price: product.price,
    });
  }

  return {
    customerId: customerId,
    shippingAddress: cities[Math.floor(Math.random() * cities.length)],
    items: items,
  };
}

export default function() {
  // Test 1: Create Order
  group('Create Order', function() {
    const order = generateOrder();
    const payload = JSON.stringify(order);

    const response = http.post(`${BASE_URL}/api/orders`, payload, {
      headers: { 'Content-Type': 'application/json' },
      tags: { name: 'CreateOrder' },
    });

    check(response, {
      'status is 200 or 201': (r) => r.status === 200 || r.status === 201,
      'response time < 2000ms': (r) => r.timings.duration < 2000,
      'has orderId in response': (r) => r.body.includes('orderId') || r.body.includes('id'),
    });

    // Extract orderId from response if available
    let orderId = null;
    try {
      const responseBody = JSON.parse(response.body);
      orderId = responseBody.orderId || responseBody.id;
    } catch (e) {
      // Response might not be valid JSON
    }

    // Small delay before checking status
    sleep(1);

    // Test 2: Check Order Status (if orderId was obtained)
    if (orderId) {
      group(`Check Order Status - ${orderId}`, function() {
        const statusResponse = http.get(
          `${BASE_URL}/api/orders/${orderId}/status`,
          { tags: { name: 'GetOrderStatus' } }
        );

        check(statusResponse, {
          'status is 200': (r) => r.status === 200,
          'response time < 1000ms': (r) => r.timings.duration < 1000,
          'has status field': (r) => r.body.includes('status'),
        });
      });
    }
  });

  sleep(Math.random() * 3 + 1);  // Random sleep between 1-4 seconds
}

export function handleSummary(data) {
  return {
    'stdout': textSummary(data, { indent: ' ', enableColors: true }),
    'load-test-results.json': JSON.stringify(data),
  };
}

// Helper function for text summary
function textSummary(data, options) {
  let summary = `\n\n=== Test Summary ===\n`;
  if (data.metrics.http_reqs) {
    summary += `Total Requests: ${data.metrics.http_reqs.value}\n`;
  }
  if (data.metrics.http_req_duration) {
    const stats = data.metrics.http_req_duration.value;
    summary += `Response Time - Avg: ${stats.avg.toFixed(2)}ms, Min: ${stats.min.toFixed(2)}ms, Max: ${stats.max.toFixed(2)}ms\n`;
  }
  if (data.metrics.http_req_failed) {
    summary += `Failed Requests: ${data.metrics.http_req_failed.value}\n`;
  }
  return summary;
}
