# eCommerce Portfolio Project
![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-6DB33F?logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?logo=postgresql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?logo=redis&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?logo=docker&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-C71A36?logo=apachemaven&logoColor=white)
![Build Status](https://github.com/FilipChyla/eCommerce-portfolio-project/actions/workflows/ci-cd.yml/badge.svg)
![Last Commit](https://img.shields.io/github/last-commit/FilipChyla/eCommerce-portfolio-project)
![License](https://img.shields.io/github/license/FilipChyla/eCommerce-portfolio-project)

A production-inspired e-commerce REST API built with **Java 21 and Spring Boot**.

The project is being developed as a backend portfolio project with a focus on practical backend development, security,
database design, testing, containerization, and maintainable application architecture.

> This project is actively under development.

---

## Why this project?

This project is my main backend portfolio project and is intended to demonstrate practical skills relevant to a
**Java / Spring Boot Backend Developer** role.

The main focus areas are:

- Spring Boot application development
- REST API design
- authentication and authorization
- secure session and token management
- relational database design
- Redis-based data storage
- caching
- validation and error handling
- automated testing
- Docker-based development
- CI/CD
- maintainable application architecture

The project focuses on building a solid backend foundation and making deliberate
engineering decisions to improve scalability, security, and maintainability.

---

# Tech Stack

| Category                        | Technology                                |
|---------------------------------|-------------------------------------------|
| Language                        | Java 21                                   |
| Framework                       | Spring Boot                               |
| Build Tool                      | Maven                                     |
| Database                        | PostgreSQL                                |
| In-memory / distributed storage | Redis                                     |
| ORM                             | Spring Data JPA / Hibernate               |
| Security                        | Spring Security + JWT                     |
| Validation                      | Jakarta Bean Validation                   |
| Database Migrations             | Flyway                                    |
| Object Mapping                  | MapStruct                                 |
| Caching                         | Spring Cache / Redis                      |
| Testing                         | JUnit 5, Mockito, MockMvc, Testcontainers |
| Code Coverage                   | JaCoCo (branch coverage)                  |
| Test Reporting                  | Allure Report                             |
| Containerization                | Docker / Docker Compose                   |
| API Documentation               | OpenAPI / Swagger                         |
| CI/CD                           | GitHub Actions                            |

---

## Reports

- [Test Report (Allure)](https://filipchyla.github.io/eCommerce-portfolio-project/)
- [Code Coverage (JaCoCo)](https://filipchyla.github.io/eCommerce-portfolio-project/coverage/)
---

# Architecture

The application follows a **feature-based package structure**.

Business features are grouped together, while cross-cutting concerns such as security, exception handling, and configuration are kept in dedicated packages.

Example:

```text
src/main/java/io/github/filipchyla/shopapi
├── auth/
│   ├── controller/
│   ├── dto/
│   └── service/
├── user/
│   ├── controller/
│   ├── dto/
│   ├── repository/
│   └── service/
├── security/
└── config/
```

This structure helps keep related functionality together and allows the application to grow without creating a large collection of unrelated global layers.

---

## Module overview

### security
Contains the application's security infrastructure, including custom filters, and authentication-related components.

### role
Provides role definitions and persistence used for role-based authorization.

### user
Provides endpoints for managing the authenticated user's profile and account.

| Method   | Endpoint                   | Description                                   | Authentication |
|----------|----------------------------|-----------------------------------------------|----------------|
| `GET`    | `/api/v1/user/me`          | Get authenticated user's profile information  | Required       |
| `PATCH`  | `/api/v1/user/me`          | Update the authenticated user's profile       | Required       |
| `PATCH`  | `/api/v1/user/me/password` | Change authenticated user's password          | Required       |
| `DELETE` | `/api/v1/user/me`          | Disable authenticated user's account          | Required       |

### auth
Handles user registration and authentication using short-lived JWT access tokens
and Redis-backed refresh token sessions. Refresh token sessions are limited per user,
and token reuse is prevented.

| Method | Endpoint                    | Description                                 | Authentication         |
|--------|-----------------------------|---------------------------------------------|------------------------|
| `POST` | `/api/v1/auth/register`     | Register a new user                         | Not required           |
| `POST` | `/api/v1/auth/authenticate` | Authenticate a user                         | Not required           |
| `POST` | `/api/v1/auth/refresh`      | Rotate refresh token                        | Refresh token required |
| `POST` | `/api/v1/auth/logout`       | Invalidate given refresh token              | Refresh token required |
| `POST` | `/api/v1/auth/logout-all`   | Invalidate all of the user's refresh tokens | Required               |

### product
Provides product and category management, including public product browsing,
administrative operations, filtering, pagination, and caching.

| Method   | Endpoint                        | Description              | Authentication      |
|----------|---------------------------------|--------------------------|---------------------|
| `GET`    | `/api/v1/categories`            | Get categories tree      | Not required        |
| `POST`   | `/api/v1/admin/categories`      | Add new category         | Admin role required |
| `PATCH`  | `/api/v1/admin/categories/{id}` | Update existing category | Admin role required |
| `DELETE` | `/api/v1/admin/categories/{id}` | Delete category          | Admin role required |

| Method   | Endpoint                            | Description                                    | Authentication      |
|----------|-------------------------------------|------------------------------------------------|---------------------|
| `GET`    | `/api/v1/products`                  | Get products list, can filter, paged           | Not required        |
| `GET`    | `/api/v1/products/{id}`             | Get single product info                        | Not required        |
| `POST`   | `/api/v1/admin/products`            | Add new product                                | Admin role required |
| `PATCH`  | `/api/v1/admin/products/{id}`       | Update existing product                        | Admin role required |
| `PATCH`  | `/api/v1/admin/products/{id}/stock` | Update quantity of product by given difference | Admin role required |
| `DELETE` | `/api/v1/admin/products/{id}`       | Delete product                                 | Admin role required |

### cart
Provides shopping cart functionality for both authenticated and guest users.
Guest carts are stored in Redis and can be merged into the authenticated user's cart.

| Method   | Endpoint                         | Description                                | Authentication |
|----------|----------------------------------|--------------------------------------------|----------------|
| `GET`    | `/api/v1/cart`                   | Get the cart with all its items            | Not required   |
| `DELETE` | `/api/v1/cart`                   | Clear all items in cart                    | Not required   |
| `POST`   | `/api/v1/cart/items`             | Add product to cart                        | Not required   |
| `PATCH`  | `/api/v1/cart/items/{productId}` | Change product quantity in cart            | Not required   |
| `DELETE` | `/api/v1/cart/items/{productId}` | Delete product from cart                   | Not required   |
| `POST`   | `/api/v1/cart/merge`             | Merge guest shopping cart with user's cart | Required       |

### config 
Module for handling configuration settings and environment variables.

### exception
Module for handling exceptions and errors in the application.

---

# Running Locally

## Requirements

* Java 21
* Docker Desktop
* Git

---

## Clone the Repository

```bash
git clone https://github.com/FilipChyla/eCommerce-portfolio-project.git
cd eCommerce-portfolio-project
```

---

## Environment Variables

Create a `.env` file in the project root using `.env.example` as a template.

---

## Run in Production-like Mode

The full Docker Compose setup runs the entire application stack in containers,
including the Spring Boot application, PostgreSQL, and Redis.

```bash
docker compose -f compose.full.yaml up --build
```
To stop the environment:
```bash
docker compose -f compose.full.yaml down
```


---

## Run in Development Mode

The application can be run locally using the `dev` Spring profile.

Spring Boot's Docker Compose integration automatically starts the services
defined in `compose.yaml`, such as PostgreSQL and Redis, while the application
runs directly on the host machine.

Start the application with Maven:
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

On windows:
```bash
./mvnw spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Docker must be running before starting the application.

---

# Database

The database schema is managed using **Flyway migrations**.

The application uses Hibernate's schema validation to verify that the database structure matches the migration scripts.

No manual database schema setup is required.

---

# Roadmap

## Completed

* [x] User registration and login
* [x] JWT authentication
* [x] User profile management
* [x] Input validation
* [x] Global exception handling
* [x] Flyway database migrations
* [x] Docker development environment
* [x] Testing
* [x] CI pipeline
* [x] Refresh tokens
* [x] Role-based authorization
* [x] Rate limiting
* [x] Basic product and category module
* [x] API documentation
* [x] Shopping cart

## In Progress

* [ ] Orders
* [ ] Kafka integration

## Planned

* [ ] Payment integration
* [ ] Product reviews
* [ ] Product search
* [ ] Product images

---

# License

This project is licensed under the MIT License.
