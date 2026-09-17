# Personal Finance System

### Event-Driven Microservices for Personal Finance

A self-directed backend project exploring how a personal finance platform can be designed with **microservices, asynchronous events, caching, search, and containerized infrastructure**.

> Built as an engineering playground for architecture, integration, and backend development.

---

## 🏗️ Architecture

```text
                         ┌─────────────────┐
                         │   API Gateway   │
                         └────────┬────────┘
                                  │
          ┌───────────────────────┼───────────────────────┐
          │                       │                       │
  ┌───────▼───────┐      ┌───────▼────────┐      ┌──────▼────────┐
  │  User Service │      │ Transaction Svc │      │ Analytics Svc │
  └───────────────┘      └───────┬────────┘      └──────┬────────┘
                                  │                       │
                                  ▼                       │
                           ┌────────────┐                 │
                           │    Kafka   │◄────────────────┘
                           └─────┬──────┘
                                 │
                         ┌───────▼────────┐
                         │ Notification   │
                         │    Service     │
                         └────────────────┘

        Redis ────────── caching
        PostgreSQL ───── transactional data
        Elasticsearch ── search & analytics
```

The architecture intentionally separates responsibilities while using events for asynchronous communication between services.

## 🧰 Tech Stack

- **Java 17**
- **Spring Boot / Spring Cloud**
- **Apache Kafka** — event streaming
- **Redis** — caching
- **Elasticsearch** — search and analytics
- **PostgreSQL** — transactional storage
- **Docker / Docker Compose** — local infrastructure
- **Maven** — build and dependency management
- **JWT / SpringDoc OpenAPI** — authentication and API documentation
- **Testcontainers** — integration testing

## ✨ Engineering Highlights

- Multi-module Maven project with separated services
- Event-driven transaction processing
- Redis caching strategy
- Elasticsearch-powered search and analytics
- REST APIs documented with OpenAPI
- Containerized infrastructure for local development
- Integration-testing support with Testcontainers

## 📦 Services

| Service | Responsibility |
|---|---|
| API Gateway | Entry point and request routing |
| User Service | User and authentication workflows |
| Transaction Service | Transaction management |
| Analytics Service | Reporting and financial insights |
| Notification Service | Event-driven notifications |

## 🚀 Getting Started

### Prerequisites

- Java 17+
- Maven 3.6+ or Maven Wrapper
- Docker & Docker Compose

### Run infrastructure

```bash
docker-compose -f docker-compose-infra.yml up -d
```

### Build

```bash
./mvnw clean package
```

### Run services

```bash
./start-services.sh
```

On Windows:

```powershell
./start-services.ps1
```

Alternatively, run the complete environment with Docker:

```bash
docker-compose -f docker-compose-prod.yml up -d --build
```

## 🔎 API Documentation

Each service exposes OpenAPI / Swagger UI during local development.

Typical endpoints:

```text
API Gateway       http://localhost:8080
User Service      http://localhost:8081
Transaction       http://localhost:8082
Analytics         http://localhost:8083
Notification      http://localhost:8084
```

## 🔐 Configuration & Security

Credentials and environment-specific values should be supplied through environment variables or local configuration and **must not be committed to the repository**.

For local development, configure database and infrastructure settings according to your environment.

## 📁 Project Structure

```text
personal-finance-system/
├── api-gateway/
├── user-service/
├── transaction-service/
├── analytics-service/
├── notification-service/
├── pom.xml
├── docker-compose-infra.yml
└── docker-compose-prod.yml
```

## 🎯 Why This Project Exists

This project is primarily about practicing real backend engineering concerns rather than building a production financial product. It provides a sandbox for exploring:

- service boundaries
- event-driven architecture
- data consistency
- caching
- search
- API design
- integration testing
- containerized development

---

**Arianto Eka Putra · Software Engineer**
