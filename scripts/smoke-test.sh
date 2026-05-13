#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
EMAIL="${SMOKE_EMAIL:-smoke-$(date +%s)@example.com}"
PASSWORD="${SMOKE_PASSWORD:-password123}"

if ! command -v jq >/dev/null 2>&1; then
  echo "jq is required for this smoke test." >&2
  exit 1
fi

docker compose up --build -d

cleanup() {
  docker compose down
}
trap cleanup EXIT

echo "Waiting for API health..."
for _ in {1..60}; do
  if curl -fsS "$BASE_URL/actuator/health" >/dev/null 2>&1; then
    break
  fi
  sleep 2
done

curl -fsS "$BASE_URL/actuator/health" | jq .

REGISTER_RESPONSE=$(curl -sS -X POST "$BASE_URL/api/v1/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")
TOKEN=$(echo "$REGISTER_RESPONSE" | jq -r '.accessToken')

if [[ "$TOKEN" == "null" || -z "$TOKEN" ]]; then
  TOKEN=$(curl -sS -X POST "$BASE_URL/api/v1/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}" \
    | jq -r '.accessToken')
fi

CATEGORY_ID=$(curl -sS -X POST "$BASE_URL/api/v1/categories" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Smoke Food","type":"EXPENSE"}' \
  | jq -r '.id')

curl -fsS -X POST "$BASE_URL/api/v1/transactions" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"categoryId\": $CATEGORY_ID,
    \"type\": \"EXPENSE\",
    \"amount\": 12.50,
    \"description\": \"Smoke lunch\",
    \"transactionDate\": \"2024-04-10\"
  }" | jq .

curl -fsS "$BASE_URL/api/v1/transactions?type=EXPENSE&from=2024-04-01&to=2024-04-30" \
  -H "Authorization: Bearer $TOKEN" | jq .

curl -fsS "$BASE_URL/api/v1/summaries/monthly?year=2024&month=4" \
  -H "Authorization: Bearer $TOKEN" | jq .

echo "Smoke test passed."
