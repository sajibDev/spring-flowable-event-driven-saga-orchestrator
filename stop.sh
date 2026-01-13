#!/bin/bash

echo "🛑 Stopping Flowable Saga Orchestrator Application"
echo "================================================="
echo ""

echo "Stopping Docker containers..."
docker-compose down

echo ""
echo "✅ All services stopped!"
echo ""
echo "💡 To remove all data volumes, run: docker-compose down -v"
