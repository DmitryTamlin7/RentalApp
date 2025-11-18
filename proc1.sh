#!/bin/bash

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

BASE_URL="http://localhost:8080"
EMAIL="landlord@example.com"
PASSWORD="password123"
ROLE="LANDLORD"

echo -e "${YELLOW}Запуск: $(TZ='Europe/Tallinn' date '+%Y-%m-%d %H:%M:%S EET')${NC}"
echo "================================================"

echo -e "${YELLOW}1. Регистрация (если нужно)${NC}"
REGISTER_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\",\"fullName\":\"Ivan Landlord\",\"role\":\"$ROLE\"}")

if echo "$REGISTER_RESPONSE" | grep -q "already exists"; then
  echo -e "${YELLOW}Пользователь существует — пропускаем${NC}"
else
  echo -e "${GREEN}Зарегистрирован новый пользователь${NC}"
fi
echo "------------------------------------------------"

echo -e "${YELLOW}2. Логин${NC}"
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")

TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.token')
USER_ID=$(echo "$LOGIN_RESPONSE" | jq -r '.id')

if [ "$TOKEN" = "null" ] || [ -z "$TOKEN" ] || [ "$USER_ID" = "null" ]; then
  echo -e "${RED}ОШИБКА: Не удалось залогиниться${NC}"
  echo "$LOGIN_RESPONSE" | jq
  exit 1
fi

echo -e "${GREEN}Успешный логин: ID=$USER_ID${NC}"
echo "------------------------------------------------"


echo -e "${YELLOW}3. Создание недвижимости (ownerId: $USER_ID)${NC}"

PROPERTY_RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X POST "$BASE_URL/api/properties" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{
    \"ownerId\": $USER_ID,
    \"address\": \"ул. Пушкина, 12, Таллин\",
    \"description\": \"Тестовая квартира\"
  }")

HTTP_BODY=$(echo "$PROPERTY_RESPONSE" | sed -n '1,/HTTP_STATUS/p' | sed '$d')
HTTP_STATUS=$(echo "$PROPERTY_RESPONSE" | grep -o 'HTTP_STATUS:[0-9]*' | cut -d: -f2)

echo "HTTP: $HTTP_STATUS"
echo "$HTTP_BODY" | jq 2>/dev/null || echo "$HTTP_BODY"

if [ "$HTTP_STATUS" -eq 200 ] && echo "$HTTP_BODY" | jq -e '.id' > /dev/null; then
  echo -e "${GREEN}ГОТОВО! Property создан с ID: $(echo "$HTTP_BODY" | jq '.id')${NC}"
else
  echo -e "${RED}ОШИБКА: Не удалось создать Property${NC}"
  exit 1
fi

echo "================================================"
echo -e "${GREEN}ВСЁ РАБОТАЕТ!$(TZ='Europe/Tallinn' date '+%H:%M:%S EET')${NC}"