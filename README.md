# eCommerce Portfolio Project

Production-inspired e-commerce REST API built with Java, Spring Boot, PostgreSQL, JWT, Flyway, and Docker.

The project is being developed as a backend portfolio project with a focus on practical backend development, security, database design, testing, containerization, and maintainable application architecture.

> This project is actively under development.

---

## Current Features

### Authentication & Authorization

* User registration
* User login
* JWT-based authentication
* Refresh tokens
* Role-based authorization (USER, ADMIN)
* Secure password hashing
* Protected API endpoints

### User Management

* Update user profile information
* Password change
* Account deletion
* Input validation
* Global exception handling

### Database & Persistence

* PostgreSQL database
* Redis
* Spring Data JPA
* Hibernate
* Flyway database migrations
* UUID-based entity identifiers

### Development & Infrastructure

* Docker-based PostgreSQL development environment
* Docker Compose
* Environment-based configuration
* Spring Boot Docker Compose integration

---

# Tech Stack

| Category           | Technology                  |
|--------------------|-----------------------------|
| Language           | Java 21                     |
| Framework          | Spring Boot                 |
| Build Tool         | Maven                       |
| Database           | PostgreSQL and Redis        |
| ORM                | Spring Data JPA / Hibernate |
| Security           | Spring Security + JWT       |
| Validation         | Jakarta Validation          |
| Testing            | JUnit and testcontainers    |
| Database Migration | Flyway                      |
| Object Mapping     | MapStruct                   |
| Containerization   | Docker & Docker Compose     |

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
├── exception/
└── config/
```

This structure helps keep related functionality together and allows the application to grow without creating a large collection of unrelated global layers.

---

# Security

The application uses Spring Security with JWT-based authentication.

### Access Tokens

* Short-lived JWT access tokens
* Access tokens are not stored server-side
* Access tokens are sent using the `Authorization: Bearer <token>` header

### Refresh Tokens

* Refresh tokens are stored server-side in Redis
* Refresh tokens are delivered using secure HttpOnly cookies
* Refresh token rotation is used
* Revoked refresh tokens cannot be reused
* Refresh token sessions are limited per user

### Password Security

* Passwords are hashed using a strong password hashing algorithm
* Raw passwords are never stored in the database

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

Example:

```env
POSTGRES_DB=shop
POSTGRES_USER=your_user
POSTGRES_PASSWORD=your_password

DB_PORT=5432

JWT_SECRET=a_very_long_random_string_for_signing_tokens
JWT_EXPIRATION_TIME=3600000

REDIS_HOST=localhost
REDIS_PORT=6379
```

---

## Run with Docker

To start the complete application environment:

```bash
docker compose -f compose.full.yaml up --build
```

To stop the containers:

```bash
docker compose -f compose.full.yaml down
```

---

## Run from IntelliJ IDEA

The application can also be started directly from IntelliJ IDEA.

Spring Boot's Docker Compose integration can automatically start the required Docker Compose services, such as PostgreSQL, while the Spring Boot application itself is run by IntelliJ IDEA.

Make sure Docker Desktop is running before starting the application.

---

# Database

The database schema is managed using **Flyway migrations**.

The application uses Hibernate's schema validation to verify that the database structure matches the migration scripts.

No manual database schema setup is required.

---

# API

The API is organized around RESTful endpoints.

## Authentication

| Method | Endpoint                    | Description                         | Authentication          |
| ------ |---------------------------- | ----------------------------------- | ----------------------- |
| `POST` | `/api/v1/auth/register`     | Register a new user                 | Not required            |
| `POST` | `/api/v1/auth/authenticate` | Authenticate a user                 | Not required            |
| `POST` | `/api/v1/auth/refresh`      | Rotate refresh token                | Refresh token required  |
| `POST` | `/api/v1/auth/logout`       | Invalidate given refresh token      | Refresh token required  |
| `POST` | `/api/v1/auth/logout-all`   | Invalidate all user's refresh token | Required                |

## User Management

| Method   | Endpoint                   | Description                                   | Authentication |
|----------|----------------------------|-----------------------------------------------|----------------|
| `GET`    | `/api/v1/user/me`          | Get authenticated user's profile informations | Required       |
| `PATCH`  | `/api/v1/user/me`          | Update the authenticated user's profile       | Required       |
| `PATCH`  | `/api/v1/user/me/password` | Change authenticated user's password          | Required       |
| `DELETE` | `/api/v1/user/me`          | Disable authenticated user's account          | Required       |

> The API documentation will be expanded as new features are implemented.

---

# Roadmap

## Completed

* [x] User registration
* [x] User login
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

## In Progress

* [ ] Product module
* [ ] API documentation

## Planned

* [ ] Shopping cart
* [ ] Orders
* [ ] Payment integration
* [ ] Product search
* [ ] Shipping integration

---

# Design Decisions

This project intentionally uses several patterns and technologies commonly found in production Spring Boot applications:

* Feature-based package structure
* DTO pattern
* MapStruct for object mapping
* Service layer
* Spring Data JPA
* Flyway database migrations
* Stateless JWT-based access token authentication
* Global exception handling
* Jakarta Bean Validation
* Docker-based development environment
* Automated testing

The goal is to gradually develop a realistic backend system while documenting the architectural and technical decisions made during development.

---

# Project Status

This project is actively under development.

The current focus is on building a solid foundation around authentication, user management, persistence, testing, and infrastructure before expanding into the core e-commerce functionality.

---

# License

This project is licensed under the MIT License.
