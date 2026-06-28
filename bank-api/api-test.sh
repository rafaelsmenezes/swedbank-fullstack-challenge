#!/bin/bash
# ============================================
# SwedenBank API Test Script
# ============================================
BASE_URL="http://localhost:8080/api/v1"

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[0;33m'
NC='\033[0m' # No Color

# Check if the server is running
echo -e "${YELLOW}Checking if the server is running...${NC}"
if ! curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/accounts?userId=1" | grep -q "200"; then
    echo -e "${RED}Error: Server did not respond at $BASE_URL${NC}"
    echo "Make sure the backend is running (./gradlew bootRun)"
    exit 1
fi
echo -e "${GREEN}Server OK!${NC}\n"

# Function to call endpoints and display results
call_endpoint() {
    local method=$1
    local url=$2
    local data=$3

    echo -e "${GREEN}➜ $method $url${NC}"
    if [ -n "$data" ]; then
        echo " Body: $data"
        response=$(curl -s -w "\n%{http_code}" -X "$method" "$url" \
            -H "Content-Type: application/json" \
            -d "$data")
    else
        response=$(curl -s -w "\n%{http_code}" -X "$method" "$url")
    fi

    status=$(echo "$response" | tail -n1)
    body=$(echo "$response" | head -n-1)

    if [ "$status" -ge 200 ] && [ "$status" -lt 300 ]; then
        echo -e " ${GREEN}Status: $status${NC}"
    else
        echo -e " ${RED}Status: $status${NC}"
    fi
    echo " Response: $body"
    echo ""

    # Return the body for later parsing
    echo "$body"
}

# Function to extract ID from JSON (uses jq if available, otherwise basic grep)
extract_id() {
    local json=$1
    local key=$2
    if command -v jq &> /dev/null; then
        echo "$json" | jq -r ".$key // empty" 2>/dev/null
    else
        echo "$json" | grep -o "\"$key\":[0-9]*" | head -1 | cut -d: -f2
    fi
}

echo "================================"
echo " SWEDENBANK API TESTS"
echo "================================"
echo ""

# 1. List user accounts (userId=1)
echo "1️⃣ List accounts for user 1"
accounts_response=$(call_endpoint "GET" "$BASE_URL/accounts?userId=1")
account_id=$(extract_id "$accounts_response" "id")

if [ -z "$account_id" ]; then
    echo -e "${YELLOW}Could not extract account_id, using fallback: 1${NC}"
    account_id=1
fi
echo -e " ${YELLOW}Using Account ID: $account_id${NC}\n"

# 2. Account details
echo "2️⃣ Account details for ID $account_id"
call_endpoint "GET" "$BASE_URL/accounts/$account_id"
echo ""

# 3. Balance
echo "3️⃣ Balance for account $account_id"
call_endpoint "GET" "$BASE_URL/accounts/$account_id/balance"
echo ""

# 4. Paginated transaction history
echo "4️⃣ Transaction history (page 0, size 20)"
call_endpoint "GET" "$BASE_URL/accounts/$account_id/transactions?page=0&size=20"
echo ""

# 5. Credit (USD → EUR conversion)
echo "5️⃣ Credit 100 USD (converted to EUR)"
credit_response=$(call_endpoint "POST" "$BASE_URL/accounts/$account_id/credit" \
    '{"amount":100,"fromCurrency":"USD","description":"Test credit"}')
credit_tx_id=$(extract_id "$credit_response" "id")
if [ -n "$credit_tx_id" ]; then
    echo -e " ${YELLOW}Credit transaction created with ID: $credit_tx_id${NC}"
fi
echo ""

# 6. Debit (in EUR)
echo "6️⃣ Debit 50 EUR"
debit_response=$(call_endpoint "POST" "$BASE_URL/accounts/$account_id/debit" \
    '{"amount":50,"currency":"EUR","description":"Test debit"}')
debit_tx_id=$(extract_id "$debit_response" "id")
if [ -n "$debit_tx_id" ]; then
    echo -e " ${YELLOW}Debit transaction created with ID: $debit_tx_id${NC}"
fi
echo ""

# 7. Exchange rate (pure conversion)
echo "7️⃣ Exchange: 100 USD → EUR"
call_endpoint "GET" "$BASE_URL/exchange?from=USD&to=EUR&amount=100"
echo ""

# 8. Transaction details (uses the last created transaction)
tx_to_check=${credit_tx_id:-$debit_tx_id}
if [ -z "$tx_to_check" ]; then
    tx_to_check=1
fi
echo "8️⃣ Transaction details for ID $tx_to_check"
call_endpoint "GET" "$BASE_URL/transactions/$tx_to_check"
echo ""

echo -e "${GREEN}✅ Tests completed!${NC}"