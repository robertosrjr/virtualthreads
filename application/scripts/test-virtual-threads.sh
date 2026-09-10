#!/bin/bash

# Script para testar Virtual Threads no Pedidos API

API="http://localhost:8080/api/v1/orders"
CUSTOMER_ID="123e4567-e89b-12d3-a456-426614174000"
PRODUCT_ID="abc12345-e89b-12d3-a456-426614174000"

echo "========================================="
echo "PEDIDOS API - Virtual Threads Test"
echo "========================================="
echo ""

# Test 1: Create an order
echo "1️⃣  Creating an order..."
echo ""

RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$API" \
  -H "Content-Type: application/json" \
  -D - \
  -d "{
    \"customerId\": \"$CUSTOMER_ID\",
    \"items\": [
      {
        \"productId\": \"$PRODUCT_ID\",
        \"productName\": \"Notebook\",
        \"quantity\": 1,
        \"unitPrice\": 2500.00,
        \"currency\": \"BRL\"
      }
    ]
  }")

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | head -n -1)

echo "HTTP Status: $HTTP_CODE"
echo ""

# Extract thread type from headers
THREAD_TYPE=$(echo "$BODY" | grep -i "x-thread-type" | awk '{print $NF}' | tr -d '\r')
echo "🧵 Thread Type: $THREAD_TYPE"
echo ""

# Extract Order ID from response
ORDER_ID=$(echo "$BODY" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
echo "✅ Order Created: $ORDER_ID"
echo ""

if [ -z "$ORDER_ID" ]; then
  echo "❌ Failed to create order"
  exit 1
fi

# Test 2: Get the order
echo "2️⃣  Fetching the order..."
echo ""

GET_RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$API/$ORDER_ID" -D -)
GET_HTTP=$(echo "$GET_RESPONSE" | tail -n1)
GET_BODY=$(echo "$GET_RESPONSE" | head -n -1)

echo "HTTP Status: $GET_HTTP"
THREAD_TYPE_GET=$(echo "$GET_BODY" | grep -i "x-thread-type" | awk '{print $NF}' | tr -d '\r')
echo "🧵 Thread Type: $THREAD_TYPE_GET"
echo ""

# Test 3: List orders
echo "3️⃣  Listing all orders..."
echo ""

LIST_RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$API" -D -)
LIST_HTTP=$(echo "$LIST_RESPONSE" | tail -n1)
LIST_BODY=$(echo "$LIST_RESPONSE" | head -n -1)

echo "HTTP Status: $LIST_HTTP"
THREAD_TYPE_LIST=$(echo "$LIST_BODY" | grep -i "x-thread-type" | awk '{print $NF}' | tr -d '\r')
echo "🧵 Thread Type: $THREAD_TYPE_LIST"
echo ""

# Test 4: Update status
echo "4️⃣  Updating order status to CONFIRMED..."
echo ""

UPDATE_RESPONSE=$(curl -s -w "\n%{http_code}" -X PATCH "$API/$ORDER_ID/status" \
  -H "Content-Type: application/json" \
  -D - \
  -d '{"newStatus":"CONFIRMED"}')

UPDATE_HTTP=$(echo "$UPDATE_RESPONSE" | tail -n1)
UPDATE_BODY=$(echo "$UPDATE_RESPONSE" | head -n -1)

echo "HTTP Status: $UPDATE_HTTP"
THREAD_TYPE_UPDATE=$(echo "$UPDATE_BODY" | grep -i "x-thread-type" | awk '{print $NF}' | tr -d '\r')
echo "🧵 Thread Type: $THREAD_TYPE_UPDATE"
echo ""

echo "========================================="
echo "✅ Tests Complete!"
echo "========================================="
echo ""
echo "Summary:"
echo "  POST   (Create): $THREAD_TYPE"
echo "  GET    (Fetch):  $THREAD_TYPE_GET"
echo "  GET    (List):   $THREAD_TYPE_LIST"
echo "  PATCH  (Update): $THREAD_TYPE_UPDATE"
echo ""
echo "If all show 'virtual', Virtual Threads are working! 🚀"
