#!/bin/bash

# Script para testar Prometheus Metrics do Pedidos API

API="http://localhost:8080/api/v1/orders"
METRICS="http://localhost:8080/actuator/prometheus"
CUSTOMER_ID="123e4567-e89b-12d3-a456-426614174000"
PRODUCT_ID="abc12345-e89b-12d3-a456-426614174000"

echo "========================================="
echo "PEDIDOS API - Metrics Testing"
echo "========================================="
echo ""

# Create 5 orders to generate metrics
echo "📊 Creating 5 orders to generate metrics..."
echo ""

for i in {1..5}; do
  echo "Order $i..."
  curl -s -X POST "$API" \
    -H "Content-Type: application/json" \
    -d "{
      \"customerId\": \"$CUSTOMER_ID\",
      \"items\": [
        {
          \"productId\": \"$PRODUCT_ID\",
          \"productName\": \"Product $i\",
          \"quantity\": $i,
          \"unitPrice\": $((2500 + i * 100)).00,
          \"currency\": \"BRL\"
        }
      ]
    }" > /dev/null 2>&1
  echo "  ✅ Order $i created"
done

echo ""
echo "========================================="
echo "📈 Prometheus Metrics Endpoint"
echo "========================================="
echo ""

# Fetch metrics
echo "Fetching metrics from: $METRICS"
echo ""

METRICS_DATA=$(curl -s "$METRICS")

# Filter business metrics
echo "💰 Business Metrics (Orders):"
echo ""
echo "$METRICS_DATA" | grep -E "orders_(created|failed|total_value)" | grep -v "#" | head -10

echo ""
echo "⏱️  Performance Metrics (Latencies):"
echo ""
echo "$METRICS_DATA" | grep -E "orders_(create\.duration|validation\.customer|calculation\.shipping)" | grep -v "#" | head -15

echo ""
echo "========================================="
echo "Virtual/Platform Thread Metrics:"
echo ""
echo "$METRICS_DATA" | grep -E "threads_(virtual|platform)_active" | grep -v "#"

echo ""
echo "========================================="
echo "HTTP Server Metrics:"
echo ""
echo "$METRICS_DATA" | grep "http_server_requests_seconds_sum" | grep "orders" | grep -v "#" | head -5

echo ""
echo "========================================="
echo "✅ Metrics Test Complete!"
echo ""
echo "Access Prometheus metrics at: $METRICS"
echo "Access Swagger UI at: http://localhost:8080/swagger-ui.html"
echo ""
