import http from 'k6/http';
import { check, sleep } from 'k6';

// Test configuration
export const options = {
  // Ramping up to 50 virtual users
  stages: [
    { duration: '10s', target: 50 },  // Ramp up to 50 users in 10 seconds
    { duration: '2m', target: 50 },   // Stay at 50 users for 2 minutes
    { duration: '10s', target: 0 },   // Ramp down to 0 users in 10 seconds
  ],
  thresholds: {
    http_req_duration: ['p(95)<500', 'p(99)<1000'],  // 95% of requests < 500ms, 99% < 1000ms
    http_req_failed: ['rate<0.1'],                     // Error rate < 10%
  },
};

// Test data - multiple order payloads to vary requests
const orderPayloads = [
  {
    customerId: '12345',
    items: [],
    shippingAddress: '123 Main St, Anytown, USA',
  },
  {
    customerId: '123',
    items: [
      {
        productId: '98765',
        quantity: 2,
      },
    ],
    shippingAddress: '456 Elm St, Othertown, USA',
  },
  {
    customerId: 'CUST-001',
    items: [
      {
        productId: 'PROD-001',
        productName: 'Laptop',
        quantity: 2,
        price: 1200.00,
      },
      {
        productId: 'PROD-002',
        productName: 'Mouse',
        quantity: 1,
        price: 50.00,
      },
    ],
    shippingAddress: '789 Oak Ave, Downtown, USA',
  },
  {
    customerId: 'CUST-002',
    items: [
      {
        productId: 'PROD-003',
        productName: 'Monitor',
        quantity: 1,
        price: 500.00,
      },
    ],
    shippingAddress: '321 Pine Rd, Uptown, USA',
  },
];

export default function () {
  // Pick a random order payload
  const payload = orderPayloads[Math.floor(Math.random() * orderPayloads.length)];

  // Make the request
  const res = http.post('http://localhost:8085/api/orders', JSON.stringify(payload), {
    headers: {
      'Content-Type': 'application/json',
    },
  });

  // Validate response
  check(res, {
    'status is 202 (Accepted)': (r) => r.status === 202,
    'response time < 1000ms': (r) => r.timings.duration < 1000,
    'has workflowId in response': (r) => r.body.includes('workflowId'),
  });

  // Optional: small delay between requests (can be 0 for max load)
  sleep(0.02); // 20ms sleep - adjust based on your needs
}

