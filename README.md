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
* Stateless authentication
* Secure password hashing
* Protected API endpoints

### User Management

* Update user profile information
* Input validation
* Global exception handling

### Database & Persistence

* PostgreSQL database
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
| ------------------ | --------------------------- |
| Language           | Java 21                     |
| Framework          | Spring Boot                 |
| Build Tool         | Maven                       |
| Database           | PostgreSQL                  |
| ORM                | Spring Data JPA / Hibernate |
| Security           | Spring Security + JWT       |
| Validation         | Jakarta Validation          |
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

JWT_SECRET_KEY=a_very_long_random_string_for_signing_tokens
JWT_EXPIRATION_TIME=3600000
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

| Method | Endpoint             | Description         | Authentication |
| ------ | -------------------- | ------------------- | -------------- |
| `POST` | `/api/auth/register` | Register a new user | Not required   |
| `POST` | `/api/auth/login`    | Authenticate a user | Not required   |

## User Management

| Method  | Endpoint        | Description                             | Authentication |
| ------- | --------------- | --------------------------------------- | -------------- |
| `PATCH` | `/api/users/me` | Update the authenticated user's profile | Required       |

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


## In Progress

* [ ] Testing
* [ ] Role-based authorization
* [ ] CI pipeline
* [ ] Refresh tokens
* [ ] Product module
* [ ] API documentation

## Planned

* [ ] Shopping cart
* [ ] Orders
* [ ] Payment integration
* [ ] Product search
* [ ] Rate limiting
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
* Stateless JWT authentication
* Global exception handling
* Jakarta Bean Validation
* Docker-based development environment
* Automated testing

The goal is not to build the smallest possible application, but to gradually develop a realistic backend system while documenting the architectural and technical decisions made during development.

---

# Project Status

This project is actively under development.

The current focus is on building a solid foundation around authentication, user management, persistence, testing, and infrastructure before expanding into the core e-commerce functionality.

---

# License

This project is licensed under the MIT License.
