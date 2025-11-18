#!/bin/bash

# Цвета
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

BASE_URL="http://localhost:8080"

echo -e "${YELLOW}ЧИНАЗЕС — ВХОД ЗА ID 33 (landlord@test.com)${NC}"
echo "----------------------------------------"

# === 1. Логин за ID 33 ===
echo -e "${YELLOW}1. Логин: landlord@test.com (ID 33)...${NC}"
TOKEN=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"landlord@test.com","password":"Pass123"}' \
  | grep -o '"token":"[^"]*' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
  echo -e "${RED}Ошибка: не удалось войти${NC}"
  exit 1
fi
echo -e "${GREEN}Вошёл! Токен получен${NC}"


PROPERTY_ID=15
echo -e "${YELLOW}2. Используем объект ID: $PROPERTY_ID${NC}"


echo -e "${YELLOW}3. Создание брони для tenant@test.com...${NC}"
BOOKING_RES=$(curl -s -X POST "$BASE_URL/api/dashboard/landlord/bookings/direct" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"tenantEmail\": \"tenant@test.com\",
    \"propertyId\": $PROPERTY_ID,
    \"startDate\": \"2025-12-20\",
    \"endDate\": \"2025-12-25\"
  }")

BOOKING_ID=$(echo "$BOOKING_RES" | grep -o '"bookingId":[0-9]*' | cut -d':' -f2 | tr -d ' \n\r')
if [ -z "$BOOKING_ID" ]; then
  echo -e "${RED}Ошибка бронирования:${NC}"
  echo "$BOOKING_RES"
  exit 1
fi
echo -e "${GREEN}Бронь создана! ID: $BOOKING_ID${NC}"

# === 4. Подтверждение брони ===
echo -e "${YELLOW}4. Подтверждение брони ID: $BOOKING_ID...${NC}"
curl -s -X POST "$BASE_URL/api/dashboard/landlord/bookings/$BOOKING_ID/confirm" \
  -H "Authorization: Bearer $TOKEN" > /dev/null

echo -e "${GREEN}Бронь подтверждена!${NC}"


echo "----------------------------------------"
echo -e "${GREEN}ГОТОВО!${NC}"
echo "LANDLORD: ID 33 (landlord@test.com)"
echo "TENANT:   tenant@test.com (ID 32)"
echo "Объект:   ID $PROPERTY_ID"
echo "Бронь:    ID $BOOKING_ID — подтверждена"
echo "----------------------------------------"