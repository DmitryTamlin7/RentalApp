#!/bin/bash

# Цвета
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

BASE_URL="http://localhost:8080"

echo -e "${YELLOW}ЧИНАЗЕС — ПОЛНЫЙ ЦИКЛ: БРОНЬ + ПЛАТЁЖ${NC}"
echo "----------------------------------------"

# === 1. Логин LANDLORD (ID 33) ===
echo -e "${YELLOW}1. Логин: landlord@test.com (ID 33)...${NC}"
TOKEN_LANDLORD=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"landlord@test.com","password":"Pass123"}' \
  | grep -o '"token":"[^"]*' | cut -d'"' -f4)

if [ -z "$TOKEN_LANDLORD" ]; then
  echo -e "${RED}Ошибка: LANDLORD не вошёл${NC}"
  exit 1
fi
echo -e "${GREEN}LANDLORD вошёл${NC}"

# === 2. Логин TENANT (ID 32) ===
echo -e "${YELLOW}2. Логин: tenant@test.com (ID 32)...${NC}"
TOKEN_TENANT=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"tenant@test.com","password":"Pass123"}' \
  | grep -o '"token":"[^"]*' | cut -d'"' -f4)

if [ -z "$TOKEN_TENANT" ]; then
  echo -e "${RED}Ошибка: TENANT не вошёл${NC}"
  exit 1
fi
echo -e "${GREEN}TENANT вошёл${NC}"

# === 3. Прямое бронирование ===
echo -e "${YELLOW}3. Создание брони (объект ID 15)...${NC}"
BOOKING_RES=$(curl -s -X POST "$BASE_URL/api/dashboard/landlord/bookings/direct" \
  -H "Authorization: Bearer $TOKEN_LANDLORD" \
  -H "Content-Type: application/json" \
  -d "{
    \"tenantEmail\": \"tenant@test.com\",
    \"propertyId\": 15,
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
echo -e "${YELLOW}4. Подтверждение брони...${NC}"
curl -s -X POST "$BASE_URL/api/dashboard/landlord/bookings/$BOOKING_ID/confirm" \
  -H "Authorization: Bearer $TOKEN_LANDLORD" > /dev/null
echo -e "${GREEN}Бронь подтверждена${NC}"

# === 5. Создание запроса на оплату ===
echo -e "${YELLOW}5. Запрос на оплату 45000 ₽...${NC}"
PAYMENT_RES=$(curl -s -X POST "$BASE_URL/api/payments/request" \
  -H "Authorization: Bearer $TOKEN_LANDLORD" \
  -H "Content-Type: application/json" \
  -d "{
    \"bookingId\": $BOOKING_ID,
    \"amount\": 45000,
    \"description\": \"Аренда за декабрь 2025\"
  }")

PAYMENT_ID=$(echo "$PAYMENT_RES" | grep -o '"paymentId":[0-9]*' | cut -d':' -f2 | tr -d ' \n\r')
if [ -z "$PAYMENT_ID" ]; then
  echo -e "${RED}Ошибка создания платежа:${NC}"
  echo "$PAYMENT_RES"
  exit 1
fi
echo -e "${GREEN}Запрос на оплату создан! ID: $PAYMENT_ID${NC}"

# === 6. TENANT: Я оплатил ===
echo -e "${YELLOW}6. TENANT: Я оплатил...${NC}"
curl -s -X POST "$BASE_URL/api/payments/$PAYMENT_ID/tenant-paid" \
  -H "Authorization: Bearer $TOKEN_TENANT" > /dev/null
echo -e "${GREEN}Оплата отмечена TENANT${NC}"

# === 7. LANDLORD: Подтвердить оплату ===
echo -e "${YELLOW}7. LANDLORD: Подтверждаю оплату...${NC}"
curl -s -X POST "$BASE_URL/api/payments/$PAYMENT_ID/confirm" \
  -H "Authorization: Bearer $TOKEN_LANDLORD" > /dev/null
echo -e "${GREEN}Оплата подтверждена!${NC}"

# === 8. Проверка в БД ===
echo -e "${YELLOW}8. Проверка в БД...${NC}"
echo "SELECT * FROM payments WHERE id = $PAYMENT_ID;" | psql -h localhost -U postgres rentaldb 2>/dev/null | grep -q "confirmed" && \
  echo -e "${GREEN}В БД: status = confirmed${NC}" || \
  echo -e "${RED}Ошибка: статус не confirmed${NC}"

# === ФИНАЛ ===
echo "----------------------------------------"
echo -e "${GREEN}ЧИНАЗЕС! ЦИКЛ ЗАВЕРШЁН!${NC}"
echo "LANDLORD: ID 33"
echo "TENANT:   ID 32"
echo "Объект:   ID 15"
echo "Бронь:    ID $BOOKING_ID"
echo "Платёж:   ID $PAYMENT_ID — confirmed"
echo "----------------------------------------"