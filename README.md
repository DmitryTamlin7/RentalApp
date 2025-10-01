# Rental Management - backend⚙️

Требования:
- JDK 17
- Gradle 8.x (рекомендуется)
- Docker & docker-compose

Запуск локально (через Docker):
1. ./gradlew clean bootJar
2. docker-compose up --build

Проверка:
- Health: GET http://localhost:8080/api/health -> {"status":"ok", "timestamp":...}

Разработка:
- Запуск в IDE: запусти класс RentalApplication.
- Миграции Flyway выполняются автоматически при старте.
