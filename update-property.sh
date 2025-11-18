#!/bin/bash

# === СКРИПТ: СОЗДАЁТ ПОЛЬЗОВАТЕЛЯ → ОБЪЕКТ → ОБНОВЛЯЕТ ОБЪЕКТ ===
# Запускай: ./combo-test.sh
# Требования: curl, jq

BACKEND="http://localhost:8080"
TIMESTAMP=$(date +%s)
EMAIL="testuser_$TIMESTAMP@localhost"
PASSWORD="test123"
FULLNAME="Тестов Тест Тестович"

echo "=== СОЗДАНИЕ ПОЛЬЗОВАТЕЛЯ ==="
echo "Email: $EMAIL"
echo "Пароль: $PASSWORD"

# 1. РЕГИСТРАЦИЯ
REGISTER_RESPONSE=$(curl -s -X POST "$BACKEND/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "'"$EMAIL"'",
    "password": "'"$PASSWORD"'",
    "fullName": "'"$FULLNAME"'",
    "role": "LANDLORD"
  }')

echo "$REGISTER_RESPONSE" | jq .

# 2. ЛОГИН
echo -e "\n=== ВХОД ==="
TOKEN_RESPONSE=$(curl -s -X POST "$BACKEND/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "'"$EMAIL"'",
    "password": "'"$PASSWORD"'"
  }')

TOKEN=$(echo "$TOKEN_RESPONSE" | jq -r '.token')
USER_ID=$(echo "$TOKEN_RESPONSE" | jq -r '.user.id')

if [ "$TOKEN" = "null" ] || [ -z "$TOKEN" ]; then
  echo "ОШИБКА: Не удалось войти"
  exit 1
fi

echo "Токен получен: ${TOKEN:0:30}..."
echo "User ID: $USER_ID"

# 3. СОЗДАНИЕ ОБЪЕКТА
echo -e "\n=== СОЗДАНИЕ ОБЪЕКТА ==="
PROPERTY_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BACKEND/api/properties" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "address": "Квартира на Ленина 10",
    "description": "1-комнатная, 5 этаж, ремонт",
    "pricePerMonth": 45000
  }')

HTTP_CODE=$(echo "$PROPERTY_RESPONSE" | tail -n1)
PROPERTY_BODY=$(echo "$PROPERTY_RESPONSE" | sed '$d')

if [ "$HTTP_CODE" -ne 201 ] && [ "$HTTP_CODE" -ne 200 ]; then
  echo "ОШИБКА: Не удалось создать объект (HTTP $HTTP_CODE)"
  echo "$PROPERTY_BODY"
  exit 1
fi

PROPERTY_ID=$(echo "$PROPERTY_BODY" | jq -r '.id')
echo "Объект создан! ID: $PROPERTY_ID"
echo "$PROPERTY_BODY" | jq .

# 4. ОБНОВЛЕНИЕ ОБЪЕКТА
echo -e "\n=== ОБНОВЛЕНИЕ ОБЪЕКТА ==="
sleep 1

UPDATE_RESPONSE=$(curl -s -w "\n%{http_code}" -X PUT "$BACKEND/api/properties/$PROPERTY_ID" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "address": "Квартира на Ленина 10 (обновлено!)",
    "description": "1-комнатная, 5 этаж, новый ремонт, Wi-Fi",
    "pricePerMonth": 52000
  }')

HTTP_CODE=$(echo "$UPDATE_RESPONSE" | tail -n1)
UPDATE_BODY=$(echo "$UPDATE_RESPONSE" | sed '$d')

echo "HTTP: $HTTP_CODE"
if [ "$HTTP_CODE" -eq 200 ]; then
  echo "УСПЕШНО ОБНОВЛЕНО!"
  echo "$UPDATE_BODY" | jq .
else
  echo "ОШИБКА обновления:"
  echo "$UPDATE_BODY"
fi

# 5. ИТОГ
echo -e "\n=== ГОТОВО! ==="
echo "Пользователь: $EMAIL"
echo "Объект ID: $PROPERTY_ID"
echo "Цена: 45 000 → 52 000 ₽"
echo "Адрес обновлён"
echo -e "\nМожешь войти в дашборд как:"
echo "   Email: $EMAIL"
echo "   Пароль: $PASSWORD"