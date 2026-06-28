#!/bin/bash
# ============================================
# SwedenBank API Test Script (Carga de Transações)
# ============================================
BASE_URL="http://localhost:8080/api/v1"

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[0;33m'
NC='\033[0m' # No Color

TOTAL_TRANSACTIONS=105 
SLEEP_TIME=0.05        

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
    local silent=$4 

    if [ "$silent" != "true" ]; then
        echo -e "${GREEN}➜ $method $url${NC}"
        if [ -n "$data" ]; then
            echo " Body: $data"
        fi
    fi

    if [ -n "$data" ]; then
        response=$(curl -s -w "\n%{http_code}" -X "$method" "$url" \
            -H "Content-Type: application/json" \
            -d "$data")
    else
        response=$(curl -s -w "\n%{http_code}" -X "$method" "$url")
    fi

    status=$(echo "$response" | tail -n1)
    body=$(echo "$response" | head -n-1)

    if [ "$silent" != "true" ]; then
        if [ "$status" -ge 200 ] && [ "$status" -lt 300 ]; then
            echo -e " ${GREEN}Status: $status${NC}"
        else
            echo -e " ${RED}Status: $status${NC}"
        fi
        echo " Response: $body"
        echo ""
    fi

    echo "$body"
}

# Function to extract ID from JSON
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
echo " SWEDENBANK API TESTS & MASS LOAD"
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

# 2. Loop para inserir mais de 100 transações
echo "2️⃣ Gerando $TOTAL_TRANSACTIONS transações dinamicamente..."
echo -e "${YELLOW}Aguarde, processando inserções em lote...${NC}"

credit_count=0
debit_count=0

for ((i=1; i<=TOTAL_TRANSACTIONS; i++))
do
    # Alterna entre Crédito e Débito para balancear os testes
    if [ $((i % 2)) -eq 0 ]; then
        # Inserção de Crédito
        payload="{\"amount\":$((10 + i)),\"fromCurrency\":\"USD\",\"description\":\"Carga automatica Credito #$i\"}"
        call_endpoint "POST" "$BASE_URL/accounts/$account_id/credit" "$payload" "true"
        ((credit_count++))
    else
        # Inserção de Débito
        payload="{\"amount\":$((5 + i % 10)),\"currency\":\"EUR\",\"description\":\"Carga automatica Debito #$i\"}"
        call_endpoint "POST" "$BASE_URL/accounts/$account_id/debit" "$payload" "true"
        ((debit_count++))
    fi
    
    # Exibe progresso a cada 10 transações para o terminal não parecer travado
    if [ $((i % 10)) -eq 0 ]; then
        echo " ⚙️ Progresso: $i / $TOTAL_TRANSACTIONS transações enviadas..."
    fi
    
    sleep $SLEEP_TIME
done

echo -e "${GREEN}▶ Sucesso: $credit_count Créditos e $debit_count Débitos gerados!${NC}\n"

# 3. Balance final
echo "3️⃣ Balance atualizado para a conta $account_id"
call_endpoint "GET" "$BASE_URL/accounts/$account_id/balance"
echo ""

# 4. Paginated transaction history (Validando a paginação com a nova massa)
echo "4️⃣ Histórico de transações paginado (Visualizando página 0, tamanho 20)"
call_endpoint "GET" "$BASE_URL/accounts/$account_id/transactions?page=0&size=20"
echo ""

echo "5️⃣ Histórico de transações paginado (Visualizando página 1, tamanho 50)"
call_endpoint "GET" "$BASE_URL/accounts/$account_id/transactions?page=1&size=50"
echo ""

echo -e "${GREEN}✅ Carga de dados e testes concluídos!${NC}"