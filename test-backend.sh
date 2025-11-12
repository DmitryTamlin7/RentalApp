#!/bin/bash

# Цвета
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

BASE_URL="http://localhost:8080"

echo -e "${YELLOW}Запуск: добавление брони от TENANT к LANDLORD${NC}"
echo "----------------------------------------"

# === 1. Логин LANDLORD (landlord@test.com) ===
echo -e "${YELLOW}1. Логин LANDLORD...${NC}"
LANDLORD_LOGIN=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "landlord@test.com",
    "password": "Pass123"
  }')

TOKEN_LANDLORD=$(echo "$LANDLORD_LOGIN" | grep -o '"token":"[^"]*' | cut -d'"' -f4)
if [ -z "$TOKEN_LANDLORD" ]; then
  echo -e "${RED}Ошибка логина LANDLORD${NC}"
  echo "$LANDLORD_LOGIN"
  exit 1
fi
echo -e "${GREEN}LANDLORD вошёл${NC}"

# === 2. Логин TENANT (tenant@test.com) ===
echo -e "${YELLOW}2. Логин TENANT...${NC}"
TENANT_LOGIN=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "tenant@test.com",
    "password": "Pass123"
  }')

TOKEN_TENANT=$(echo "$TENANT_LOGIN" | grep -o '"token":"[^"]*' | cut -d'"' -f4)
if [ -z "$TOKEN_TENANT" ]; then
  echo -e "${RED}Ошибка логина TENANT${NC}"
  echo "$TENANT_LOGIN"
  exit 1
fi
echo -e "${GREEN}TENANT вошёл${NC}"

# === 3. Получение ID TENANT ===
echo -e "${YELLOW}3. Получение ID TENANT...${NC}"
PROFILE=$(curl -s -H "Authorization: Bearer $TOKEN_TENANT" "$BASE_URL/api/users/profile")

if echo "$PROFILE" | grep -q "tenant@test.com"; then
  echo -e "${GREEN}Профиль TENANT OK${NC}"
else
  echo -e "${RED}Ошибка профиля TENANT:${NC}"
  echo "$PROFILE"
  exit 1
fi

TENANT_ID=$(echo "$PROFILE" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
if [ -z "$TENANT_ID" ]; then
  echo -e "${RED}Не удалось получить ID TENANT${NC}"
  exit 1
fi
echo -e "${GREEN}ID TENANT: $TENANT_ID${NC}"

# === 4. Создание объекта недвижимости ===
echo -e "${YELLOW}4. Создание объекта недвижимости...${NC}"
PROPERTY_RES=$(curl -s -X POST "$BASE_URL/api/properties" \
  -H "Authorization: Bearer $TOKEN_LANDLORD" \
  -H "Content-Type: application/json" \
  -d '{
    "address": "Москва, ул. Новая, 10",
    "description": "Квартира для теста",
    "pricePerMonth": 45000
  }')

PROPERTY_ID=$(echo "$PROPERTY_RES" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2 | tr -d ' \n\r')
if [ -z "$PROPERTY_ID" ]; then
  echo -e "${RED}Ошибка создания объекта:${NC}"
  echo "$PROPERTY_RES"
  exit 1
fi
echo -e "${GREEN}Объект создан, ID: $PROPERTY_ID${NC}"

# === 5. Создание бронирования ===
echo -e "${YELLOW}5. Создание бронирования...${NC}"
BOOKING_RES=$(curl -s -X POST "$BASE_URL/api/bookings" \
  -H "Authorization: Bearer $TOKEN_TENANT" \
  -H "Content-Type: application/json" \
  -d "{\"tenantId\": $TENANT_ID, \"propertyId\": $PROPERTY_ID, \"startDate\": \"2025-12-10\", \"endDate\": \"2025-12-15\"}")

echo "DEBUG JSON: {\"tenantId\": $TENANT_ID, \"propertyId\": $PROPERTY_ID, ...}"  # ← отладка

BOOKING_ID=$(echo "$BOOKING_RES" | grep -o '"bookingId":[0-9]*' | cut -d':' -f2 | tr -d ' \n\r')
if [ -n "$BOOKING_ID" ]; then
  echo -e "${GREEN}БРОНЬ СОЗДАНА! ID: $BOOKING_ID${NC}"
else
  echo -e "${RED}Ошибка бронирования:${NC}"
  echo "$BOOKING_RES"
  exit 1
fi



# === Финал ===
echo "----------------------------------------"
echo -e "${GREEN}ГОТОВО!${NC}"
echo "LANDLORD: landlord@test.com (ID: 33)"
echo "TENANT:   tenant@test.com (ID: $TENANT_ID)"
echo "Объект:   ID $PROPERTY_ID"
echo "Бронь:    ID $BOOKING_ID"
echo "----------------------------------------"