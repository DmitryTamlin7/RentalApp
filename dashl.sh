#!/bin/bash


BASE_URL="http://localhost:8080"
EMAIL="landlord@example.com"
PASSWORD="password123"
ROLE="LANDLORD"


GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${YELLOW}ЗАПУСК ТЕСТА ДАШБОРДА — $(date '+%Y-%m-%d %H:%M:%S EET')${NC}"
echo "================================================"


echo -e "${YELLOW}1. Регистрация пользователя...${NC}"
REGISTER_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\",\"fullName\":\"Ivan Landlord\",\"role\":\"$ROLE\"}")

if echo "$REGISTER_RESPONSE" | grep -q "already exists"; then
  echo -e "${YELLOW}Пользователь уже существует — ок${NC}"
else
  echo -e "${GREEN}Регистрация успешна${NC}"
fi


echo -e "${YELLOW}2. Логин...${NC}"
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")

TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.token // empty')
USER_ID=$(echo "$LOGIN_RESPONSE" | jq -r '.id // empty')

if [ -z "$TOKEN" ] || [ "$TOKEN" = "null" ]; then
  echo -e "${RED}ОШИБКА: Не удалось получить токен!${NC}"
  echo "$LOGIN_RESPONSE" | jq
  exit 1
fi

echo -e "${GREEN}Логин успешен. ID: $USER_ID${NC}"
echo "------------------------------------------------"


echo -e "${YELLOW}3. Создание объекта недвижимости...${NC}"
CREATE_RESPONSE=$(curl -s -X POST "$BASE_URL/api/properties" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "address": "ул. Тестовая, 99, Таллин",
    "description": "Тестовая квартира для дашборда",
    "pricePerMonth": 35000
  }')

PROPERTY_ID=$(echo "$CREATE_RESPONSE" | jq -r '.id // empty')
if [ -z "$PROPERTY_ID" ] || [ "$PROPERTY_ID" = "null" ]; then
  echo -e "${RED}ОШИБКА: Не удалось создать объект${NC}"
  echo "$CREATE_RESPONSE" | jq || echo "$CREATE_RESPONSE"
  exit 1
fi

echo -e "${GREEN}Объект создан! ID: $PROPERTY_ID${NC}"
echo "------------------------------------------------"


echo -e "${YELLOW}4. Получение дашборда...${NC}"
DASHBOARD_RESPONSE=$(curl -s -H "Authorization: Bearer $TOKEN" \
  "$BASE_URL/api/dashboard/landlord/properties")

echo "$DASHBOARD_RESPONSE" | jq


if echo "$DASHBOARD_RESPONSE" | grep -q "$PROPERTY_ID"; then
  echo -e "${GREEN}ДАШБОРД: Объект найден в списке${NC}"
else
  echo -e "${RED}ОШИБКА: Объект НЕ найден в дашборде${NC}"
  exit 1
fi


echo -e "${YELLOW}5. Получение статистики...${NC}"
STATS_RESPONSE=$(curl -s -H "Authorization: Bearer $TOKEN" \
  "$BASE_URL/api/dashboard/landlord/stats")

echo "$STATS_RESPONSE" | jq

TOTAL=$(echo "$STATS_RESPONSE" | jq -r '.total')
if [ "$TOTAL" -gt 0 ]; then
  echo -e "${GREEN}СТАТИСТИКА: Объектов: $TOTAL${NC}"
else
  echo -e "${RED}ОШИБКА: Статистика пустая${NC}"
  exit 1
fi


echo -e "${YELLOW}6. Удаление объекта (ID: $PROPERTY_ID)...${NC}"
DELETE_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE \
  -H "Authorization: Bearer $TOKEN" \
  "$BASE_URL/api/dashboard/landlord/properties/$PROPERTY_ID")

if [ "$DELETE_STATUS" = "204" ]; then
  echo -e "${GREEN}Объект успешно удалён${NC}"
else
  echo -e "${RED}ОШИБКА при удалении: HTTP $DELETE_STATUS${NC}"
  exit 1
fi

echo "================================================"
echo -e "${GREEN}ВСЁ УСПЕШНО! ДАШБОРД РАБОТАЕТ!$(date '+%H:%M:%S EET')${NC}"
echo "Тест завершён: $EMAIL | Объектов создано и удалено: 1"
