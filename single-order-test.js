import http from 'k6/http';
import { check, sleep } from 'k6';

// Single order test - Just one request for debugging
export const options = {
  vus: 1,
  iterations: 1,
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export default function() {
  console.log('\n=== Creating Test Order ===');
  
  const order = {
    customerId: 'CUST-TEST-001',
    shippingAddress: '123 Main St, New York, NY 10001',
    items: [
      {
        productId: 'PROD-001',
        productName: 'Laptop',
        quantity: 1,
        price: 1200.00
      },
      {
        productId: 'PROD-002',
        productName: 'Mouse',
        quantity: 2,
        price: 25.00
      }
    ]
  };

  console.log('Request Payload:', JSON.stringify(order, null, 2));

  const createResponse = http.post(
    `${BASE_URL}/api/orders`,
    JSON.stringify(order),
    { 
      headers: { 
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      }
    }
  );

  console.log(`\n=== Response ===`);
  console.log(`Status: ${createResponse.status}`);
  console.log(`Duration: ${createResponse.timings.duration.toFixed(2)}ms`);
  console.log(`Body: ${createResponse.body}`);

  const checks = check(createResponse, {
    'status is 200': (r) => r.status === 200,
    'has correlationId': (r) => {
      try {
        return JSON.parse(r.body).correlationId !== undefined;
      } catch (e) {
        return false;
      }
    },
    'has status field': (r) => {
      try {
        return JSON.parse(r.body).status !== undefined;
      } catch (e) {
        return false;
      }
    },
  });

  if (!checks) {
    console.log('\n✗ Order creation failed!');
    return;
  }

  console.log('\n✓ Order created successfully!');

  // Extract and check status
  let correlationId;
  try {
    const responseData = JSON.parse(createResponse.body);
    correlationId = responseData.correlationId;
    console.log(`Correlation ID: ${correlationId}`);
    console.log(`Initial Status: ${responseData.status}`);
  } catch (e) {
    console.log('Failed to parse response');
    return;
  }

  // Check order status multiple times
  for (let i = 1; i <= 5; i++) {
    console.log(`\n=== Status Check ${i} ===`);
    sleep(2);

    const statusResponse = http.get(`${BASE_URL}/api/orders/${correlationId}/status`);
    
    console.log(`Status: ${statusResponse.status}`);
    console.log(`Response: ${statusResponse.body}`);

    check(statusResponse, {
      'status check is 200': (r) => r.status === 200,
    });

    const status = statusResponse.body.toUpperCase();
    if (status.includes('COMPLETED') || status.includes('SUCCESS')) {
      console.log('\n✓ Order completed successfully!');
      break;
    } else if (status.includes('FAILED') || status.includes('CANCELLED')) {
      console.log('\n✗ Order failed!');
      break;
    }
  }
}
