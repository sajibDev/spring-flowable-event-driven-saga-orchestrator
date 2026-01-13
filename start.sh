#!/bin/bash

echo "🚀 Starting Flowable Saga Orchestrator Application"
echo "=================================================="
echo ""

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker first."
    exit 1
fi

echo "📦 Starting infrastructure services (PostgreSQL & RabbitMQ)..."
docker-compose up -d

echo "⏳ Waiting for services to be ready..."
sleep 10

echo ""
echo "✅ Infrastructure services started!"
echo ""
echo "📊 Access Points:"
echo "  - RabbitMQ Management UI: http://localhost:15672 (guest/guest)"
echo "  - PostgreSQL Saga DB:     localhost:5432"
echo "  - PostgreSQL Order DB:    localhost:5433"
echo "  - PostgreSQL Inventory DB: localhost:5434"
echo "  - PostgreSQL Payment DB:  localhost:5435"
echo "  - PostgreSQL Shipping DB: localhost:5436"
echo ""
echo "🏗️ Building the application..."
./gradlew clean build -x test

if [ $? -ne 0 ]; then
    echo "❌ Build failed. Please fix the errors and try again."
    exit 1
fi

echo ""
echo "✅ Build successful!"
echo ""
echo "🎯 To start the services, open 5 separate terminal windows and run:"
echo ""
echo "  Terminal 1: ./gradlew :saga-orchestrator:bootRun"
echo "  Terminal 2: ./gradlew :order-service:bootRun"
echo "  Terminal 3: ./gradlew :inventory-service:bootRun"
echo "  Terminal 4: ./gradlew :payment-service:bootRun"
echo "  Terminal 5: ./gradlew :shipping-service:bootRun"
echo ""
echo "📡 Service Ports:"
echo "  - Saga Orchestrator: http://localhost:8080"
echo "  - Order Service:     http://localhost:8081"
echo "  - Inventory Service: http://localhost:8082"
echo "  - Payment Service:   http://localhost:8083"
echo "  - Shipping Service:  http://localhost:8084"
echo ""
echo "🎯 Monitoring UIs:"
echo "  - Flowable REST API:  http://localhost:8080/flowable-rest"
echo "  - RabbitMQ Management: http://localhost:15672 (guest/guest)"
echo ""
echo "📝 After all services are running, test with:"
echo "  curl -X POST http://localhost:8080/api/orders \\"
echo "    -H 'Content-Type: application/json' \\"
echo "    -d @sample-order.json"
echo ""
echo "Happy orchestrating! 🎭"
