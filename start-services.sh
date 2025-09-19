#!/bin/bash

# Personal Finance Management System - Service Startup Script
# Demonstrates containerization and microservices orchestration

echo "🚀 Starting Personal Finance Management System"
echo "=============================================="

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker first."
    exit 1
fi

# Function to check if a service is healthy
check_service_health() {
    local service_name=$1
    local port=$2
    local max_attempts=30
    local attempt=1
    
    echo "⏳ Waiting for $service_name to be healthy..."
    
    while [ $attempt -le $max_attempts ]; do
        if curl -f -s "http://localhost:$port/actuator/health" > /dev/null 2>&1; then
            echo "✅ $service_name is healthy!"
            return 0
        fi
        
        echo "   Attempt $attempt/$max_attempts - $service_name not ready yet..."
        sleep 5
        attempt=$((attempt + 1))
    done
    
    echo "❌ $service_name failed to start within expected time"
    return 1
}

# Start infrastructure services
echo "📦 Starting infrastructure services..."
docker-compose up -d postgres redis elasticsearch kibana zookeeper kafka

echo "⏳ Waiting for infrastructure services to be ready..."
sleep 30

# Check infrastructure health
echo "🔍 Checking infrastructure health..."

# Wait for PostgreSQL
echo "   Checking PostgreSQL..."
until docker exec finance-postgres pg_isready -U finance_user -d finance_db > /dev/null 2>&1; do
    echo "   PostgreSQL not ready, waiting..."
    sleep 2
done
echo "✅ PostgreSQL is ready!"

# Wait for Redis
echo "   Checking Redis..."
until docker exec finance-redis redis-cli ping > /dev/null 2>&1; do
    echo "   Redis not ready, waiting..."
    sleep 2
done
echo "✅ Redis is ready!"

# Wait for Elasticsearch
echo "   Checking Elasticsearch..."
until curl -f -s "http://localhost:9200/_cluster/health" > /dev/null 2>&1; do
    echo "   Elasticsearch not ready, waiting..."
    sleep 5
done
echo "✅ Elasticsearch is ready!"

# Wait for Kafka
echo "   Checking Kafka..."
sleep 15  # Kafka takes a bit longer to start
echo "✅ Kafka should be ready!"

# Build all microservices
echo "🔨 Building microservices..."

services=("user-service" "transaction-service" "analytics-service" "notification-service" "api-gateway")

for service in "${services[@]}"; do
    if [ -d "$service" ]; then
        echo "   Building $service..."
        cd "$service"
        mvn clean package -DskipTests
        if [ $? -ne 0 ]; then
            echo "❌ Failed to build $service"
            exit 1
        fi
        cd ..
        echo "✅ $service built successfully!"
    else
        echo "⚠️  $service directory not found, skipping..."
    fi
done

# Start microservices
echo "🚀 Starting microservices..."

# Start services in dependency order
echo "   Starting User Service..."
docker-compose up -d user-service
sleep 10

echo "   Starting Transaction Service..."
docker-compose up -d transaction-service
sleep 10

echo "   Starting Analytics Service..."
docker-compose up -d analytics-service
sleep 10

echo "   Starting Notification Service..."
docker-compose up -d notification-service
sleep 10

echo "   Starting API Gateway..."
docker-compose up -d api-gateway
sleep 10

# Health checks
echo "🏥 Performing health checks..."

services_health=(
    "User Service:8081"
    "Transaction Service:8082"
    "Analytics Service:8083"
    "Notification Service:8084"
    "API Gateway:8080"
)

all_healthy=true
for service_info in "${services_health[@]}"; do
    IFS=':' read -r service_name port <<< "$service_info"
    if ! check_service_health "$service_name" "$port"; then
        all_healthy=false
    fi
done

echo ""
echo "=============================================="
if [ "$all_healthy" = true ]; then
    echo "🎉 All services are running successfully!"
    echo ""
    echo "📋 Service URLs:"
    echo "   • API Gateway:        http://localhost:8080"
    echo "   • User Service:       http://localhost:8081"
    echo "   • Transaction Service: http://localhost:8082"
    echo "   • Analytics Service:  http://localhost:8083"
    echo "   • Notification Service: http://localhost:8084"
    echo ""
    echo "📊 Infrastructure URLs:"
    echo "   • Kibana Dashboard:   http://localhost:5601"
    echo "   • Elasticsearch:      http://localhost:9200"
    echo ""
    echo "🔧 Management Endpoints:"
    echo "   • Health Checks:      http://localhost:8080/actuator/health"
    echo "   • Metrics:           http://localhost:8080/actuator/metrics"
    echo ""
    echo "📚 API Documentation:"
    echo "   • Swagger UI:        http://localhost:8080/swagger-ui.html"
    echo ""
    echo "🎯 Sample API Calls:"
    echo "   • Register User:     POST http://localhost:8080/api/users/register"
    echo "   • Create Transaction: POST http://localhost:8080/api/transactions"
    echo "   • Get Analytics:     GET http://localhost:8080/api/analytics/dashboard"
else
    echo "❌ Some services failed to start properly"
    echo "📋 Check logs with: docker-compose logs [service-name]"
    echo "🔄 Restart with: docker-compose restart [service-name]"
fi

echo "=============================================="