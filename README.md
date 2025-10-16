# 🏠 Rental Management App

A simple **rental management system** for properties, users, and bookings.  
Built with **Spring Boot** and **PostgreSQL**. All data is stored in a relational database and exposed via a **REST API**.  

---

## ✨ Features
- Manage **Users**: create, update, delete  
- Manage **Properties**: create, update, delete  
- Manage **Bookings**: create, update, cancel  
- **JSON REST API** for frontend or external integrations  
- PostgreSQL database integration ✅  

---

## 🏗 Architecture

### Packages
- `controller` – REST API controllers  
- `service` – Business logic for users, properties, bookings  
- `repository` – Spring Data JPA repositories  
- `model` – JPA entities: `User`, `Property`, `Booking`  
- `dto` – Data Transfer Objects for requests/responses  
- `config` – Configuration classes (optional)  

### Database Schema
- **users**: `id`, `email`, `full_name`, `password`, `role`  
- **properties**: `id`, `owner_id`, `address`, `description`, `status`, `created_at`  
- **booking**: `id`, `tenant_id`, `property_id`, `start_date`, `end_date`, `status`  

### REST Endpoints

#### Users
- `POST /api/users` – Create user  
- `GET /api/users` – Get all users  
- `GET /api/users/{id}` – Get user by id  
- `PUT /api/users/{id}` – Update user  
- `DELETE /api/users/{id}` – Delete user  

#### Properties
- `POST /api/properties` – Create property  
- `GET /api/properties` – Get all properties  
- `GET /api/properties/{id}` – Get property by id  
- `DELETE /api/properties/{id}` – Delete property  

#### Bookings
- `POST /api/bookings` – Create booking  
- `GET /api/bookings` – Get all bookings  
- `GET /api/bookings/{id}` – Get booking by id  
- `PUT /api/bookings/{id}` – Update booking  
- `PUT /api/bookings/{id}/cancel` – Cancel booking  

---

## 🛠 Tech Stack
- **Java 17** 🟢  
- **Spring Boot 3** ☕  
- **Spring Data JPA** 📦  
- **Lombok** ✏️  
- **PostgreSQL 15** 🐘  
- **Gradle** ⚙️  
- **JUnit 5** 🧪  
- **Docker (optional)** 🐳  
- **GitHub Actions (CI/CD)** ⚡  

---

## 🚀 How to Run
1. Clone repository  
2. Configure `application.yml` with PostgreSQL credentials  
3. Run `./gradlew bootRun`  
4. Use REST API via Postman, curl, or frontend  

---

## 📌 Notes
- All strings (emails, names, addresses) should be **in English** to avoid encoding issues.  
- Bookings are linked to **Users** and **Properties** via foreign keys.  
