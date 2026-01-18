import http from 'k6/http';
import { check, sleep } from 'k6';

// Test configuration

export const options = {
    scenarios: {
        send_100_requests: {
            executor: 'shared-iterations',
            vus: 50,
            iterations: 500,
            maxDuration: '60s',
        },
    },

    // stages: [
    //     { duration: '20s', target: 25 },    // Ramp up to 25 users
    //     { duration: '40s', target: 50 }    // Ramp up to 50 users
    //     // { duration: '1m', target: 100 },   // Ramp up to 100 users
    //     // { duration: '3m', target: 150 },   // Ramp up to 150 users
    //     // { duration: '5m', target: 150 },   // Hold at 150 users
    //     // { duration: '2m', target: 75 },    // Ramp down to 75 users
    //     // { duration: '30s', target: 0 },     // Ramp down to 0 users
    // ]
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
    http.post('http://localhost:8085/api/orders', JSON.stringify(payload), {
        headers: {
            'Content-Type': 'application/json',
        },
    });

    // Validate response
    // check(res, {
    //   'status is 202 (Accepted)': (r) => r.status === 202,
    //   'response time < 1000ms': (r) => r.timings.duration < 1000,
    //   'has workflowId in response': (r) => r.body.includes('workflowId'),
    // });

    // Optional: small delay between requests (can be 0 for max load)
    sleep(0.02); // 20ms sleep - adjust based on your needs
}

// Custom console summary output
export function handleSummary(data) {
    // Extract useful metrics
    const metrics = {
        total_iterations: data.metrics.iterations.values.count,
        total_requests: data.metrics.http_reqs.values.count,
        avg_latency: data.metrics.http_req_duration.values.avg.toFixed(2) + ' ms',
        p95_latency: data.metrics.http_req_duration.values['p(95)'].toFixed(2) + ' ms',
        p99_latency: data.metrics.http_req_duration.values['p(99)'].toFixed(2) + ' ms',
        max_latency: data.metrics.http_req_duration.values.max.toFixed(2) + ' ms',
        error_rate: (data.metrics.http_req_failed.values.rate * 100).toFixed(2) + '%',
        test_duration: data.state.testRunDurationMs + ' ms',
    };

    console.log('----- K6 Test Results -----');
    console.log(JSON.stringify(metrics, null, 2));

    // Also return original k6 summary for CLI
    return {
        stdout: data,
    };
}

