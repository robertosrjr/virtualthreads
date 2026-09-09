#!/bin/bash

# Load Test Script for Pedidos API
# Demonstrates the difference between Virtual Threads and Platform Threads
# Usage: ./scripts/load-test.sh [concurrent_threads] [requests_per_thread]

CONCURRENT_THREADS=${1:-10}
REQUESTS_PER_THREAD=${2:-10}
BASE_URL="http://localhost:8080"
TOTAL_REQUESTS=$((CONCURRENT_THREADS * REQUESTS_PER_THREAD))

CUSTOMER_ID="123e4567-e89b-12d3-a456-426614174000"
PRODUCT_ID="abc12345-e89b-12d3-a456-426614174000"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if API is running
echo -e "${BLUE}Checking API connectivity...${NC}"
if ! curl -s "$BASE_URL/api/v1/orders" > /dev/null 2>&1; then
    echo -e "${RED}Error: API is not responding at $BASE_URL${NC}"
    echo "Please start the application with: ./mvnw spring-boot:run -f pedidos-infrastructure"
    exit 1
fi

echo -e "${GREEN}✓ API is running${NC}"
echo ""

# Function to create a single order
create_order() {
    curl -s -X POST "$BASE_URL/api/v1/orders" \
        -H "Content-Type: application/json" \
        -d "{
            \"customerId\": \"$CUSTOMER_ID\",
            \"items\": [
                {
                    \"productId\": \"$PRODUCT_ID\",
                    \"productName\": \"Load Test Product\",
                    \"quantity\": 1,
                    \"unitPrice\": 100.00,
                    \"currency\": \"BRL\"
                }
            ]
        }" \
        -w "%{http_code}" \
        -o /tmp/response.json
}

# Run load test
echo -e "${BLUE}Starting Load Test${NC}"
echo "Configuration:"
echo "  Concurrent threads: $CONCURRENT_THREADS"
echo "  Requests per thread: $REQUESTS_PER_THREAD"
echo "  Total requests: $TOTAL_REQUESTS"
echo "  API URL: $BASE_URL"
echo ""

START_TIME=$(date +%s%N)

# Use xargs to parallelize requests
seq 1 $TOTAL_REQUESTS | xargs -P $CONCURRENT_THREADS -I {} sh -c '
    response=$(curl -s -X POST "http://localhost:8080/api/v1/orders" \
        -H "Content-Type: application/json" \
        -d "{
            \"customerId\": \"123e4567-e89b-12d3-a456-426614174000\",
            \"items\": [
                {
                    \"productId\": \"abc12345-e89b-12d3-a456-426614174000\",
                    \"productName\": \"Load Test Product\",
                    \"quantity\": 1,
                    \"unitPrice\": 100.00,
                    \"currency\": \"BRL\"
                }
            ]
        }" \
        -w "\n%{http_code}" \
        -o /tmp/response_{}.json)
    http_code=$(echo "$response" | tail -n 1)
    if [ "$http_code" != "201" ]; then
        echo "Failed: HTTP $http_code"
    fi
'

END_TIME=$(date +%s%N)
DURATION_NS=$((END_TIME - START_TIME))
DURATION_MS=$((DURATION_NS / 1000000))
DURATION_SEC=$(awk "BEGIN {printf \"%.2f\", $DURATION_MS / 1000}")

# Calculate statistics
THROUGHPUT=$(awk "BEGIN {printf \"%.2f\", $TOTAL_REQUESTS / ($DURATION_MS / 1000)}")
AVG_TIME=$(awk "BEGIN {printf \"%.2f\", $DURATION_MS / $TOTAL_REQUESTS}")

echo -e "${GREEN}Load Test Completed${NC}"
echo ""
echo "Results:"
echo "  Total Requests: $TOTAL_REQUESTS"
echo "  Duration: ${DURATION_SEC}s (${DURATION_MS}ms)"
echo -e "  ${YELLOW}Throughput: ${THROUGHPUT} req/s${NC}"
echo "  Avg Time/Request: ${AVG_TIME}ms"
echo ""

# Check thread type from response headers
THREAD_TYPE=$(curl -s -I "$BASE_URL/api/v1/orders" | grep -i "X-Thread-Type" | awk '{print $2}' | tr -d '\r')
if [ -z "$THREAD_TYPE" ]; then
    THREAD_TYPE="unknown"
fi

echo "Thread Configuration:"
echo "  X-Thread-Type: ${THREAD_TYPE}"
echo ""

# Recommendations
echo -e "${BLUE}Recommendations:${NC}"
echo "1. Note the throughput above"
echo "2. Disable Virtual Threads in application.yml:"
echo "   spring.threads.virtual.enabled: false"
echo "3. Restart the application"
echo "4. Run this test again with the same parameters"
echo "5. Compare the throughput values"
echo ""

echo -e "${YELLOW}Expected Results:${NC}"
echo "With Virtual Threads:  Higher throughput (more req/s)"
echo "With Platform Threads: Lower throughput (fewer req/s)"
echo ""
echo "This is because Virtual Threads are more efficient for I/O-bound operations"
echo "like the simulated customer validation and shipping calculation in this POC."
