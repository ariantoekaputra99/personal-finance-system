# Personal Finance Management System

Sistem manajemen keuangan personal dengan arsitektur microservices yang mengintegrasikan berbagai teknologi modern.

## Teknologi yang Digunakan

- **Spring IoC**: Dependency injection dan inversion of control
- **Java Stream**: Processing data transaksi dan laporan
- **Advanced Native SQL**: Query kompleks untuk analisis keuangan
- **Docker & Microservices**: Containerization dan arsitektur microservices
- **Apache Kafka**: Real-time transaction processing dan event streaming
- **Redis**: Caching dan session management
- **Elasticsearch**: Search engine dan analytics
- **PostgreSQL**: Database utama untuk data transaksional

## Arsitektur Microservices

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   API Gateway   │    │   User Service  │    │Transaction Svc  │
│   (Port 8080)   │    │   (Port 8081)   │    │   (Port 8082)   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
         ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
         │Analytics Service│    │Notification Svc │    │   Kafka Cluster │
         │   (Port 8083)   │    │   (Port 8084)   │    │   (Port 9092)   │
         └─────────────────┘    └─────────────────┘    └─────────────────┘
                 │                       │                       │
         ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
         │  Elasticsearch  │    │     Redis       │    │   PostgreSQL    │
         │   (Port 9200)   │    │   (Port 6379)   │    │   (Port 5432)   │
         └─────────────────┘    └─────────────────┘    └─────────────────┘
```

## Fitur Utama

1. **User Management**: Registrasi, login, profil pengguna
2. **Transaction Management**: CRUD transaksi dengan kategorisasi
3. **Real-time Analytics**: Dashboard dengan metrics real-time
4. **Smart Notifications**: Notifikasi berbasis event dan threshold
5. **Advanced Search**: Pencarian transaksi dengan Elasticsearch
6. **Caching Strategy**: Multi-level caching untuk performa optimal

## Quick Start

### Option 1: Local Development (Recommended)
```bash
# 1. Start infrastructure services only
docker-compose -f docker-compose-infra.yml up -d

# 2. Build all services
./mvnw clean package

# 3. Run services locally
./start-services.ps1  # Windows
# atau
./start-services.sh   # Linux/Mac
```

### Option 2: Full Docker Environment
```bash
# Start all services with Docker
docker-compose -f docker-compose-prod.yml up -d --build
```

## API Endpoints

### User Service (8081)
- `POST /api/users/register` - Registrasi user
- `POST /api/users/login` - Login user
- `GET /api/users/profile` - Get user profile

### Transaction Service (8082)
- `POST /api/transactions` - Create transaction
- `GET /api/transactions` - Get transactions with filters
- `GET /api/transactions/analytics` - Get transaction analytics

### Analytics Service (8083)
- `GET /api/analytics/dashboard` - Dashboard data
- `GET /api/analytics/reports` - Financial reports
- `GET /api/analytics/trends` - Spending trends

## Database Configuration

Sistem menggunakan PostgreSQL dengan konfigurasi:
- **Host**: localhost:5432
- **Database**: finance_db
- **Username**: postgres
- **Password**: 1234567890

## Configuration Files

Semua service menggunakan `application.properties` format:
- `user-service/src/main/resources/application.properties`
- `transaction-service/src/main/resources/application.properties`
- `analytics-service/src/main/resources/application.properties`
- `notification-service/src/main/resources/application.properties`
- `api-gateway/src/main/resources/application.properties`

## Development

Setiap microservice menggunakan:
- **Spring Boot 3.1.5** dengan Maven build system
- **Spring Cloud 2022.0.4** untuk microservices patterns
- **Java 17** dengan Stream API
- **Lombok 1.18.30** untuk code generation
- **MapStruct 1.5.5** untuk object mapping
- **JWT (JJWT 0.11.5)** untuk authentication
- **SpringDoc OpenAPI 2.2.0** untuk API documentation
- **Testcontainers 1.19.1** untuk integration testing

## Build & Run

### Prerequisites
- Java 17+
- Maven 3.6+ (atau gunakan Maven Wrapper)
- Docker & Docker Compose

### Quick Start
```bash
# 1. Clone repository
git clone <repository-url>
cd personal-finance-system

# 2. Start infrastructure services only
docker-compose -f docker-compose-infra.yml up -d

# 3. Build all services
./mvnw clean package

# 4. Run services locally
./start-services.sh  # Linux/Mac
# atau
.\start-services.ps1 # Windows
```

### Manual Service Startup
```bash
# Run individual services
./mvnw spring-boot:run -pl user-service
./mvnw spring-boot:run -pl transaction-service
./mvnw spring-boot:run -pl analytics-service
./mvnw spring-boot:run -pl notification-service
./mvnw spring-boot:run -pl api-gateway
```

## API Documentation

### 🚀 Swagger/OpenAPI Documentation
Setiap microservice dilengkapi dengan **interactive Swagger UI**:

- **API Gateway**: http://localhost:8080/swagger-ui.html
- **User Service**: http://localhost:8081/swagger-ui.html  
- **Transaction Service**: http://localhost:8082/swagger-ui.html
- **Analytics Service**: http://localhost:8083/swagger-ui.html
- **Notification Service**: http://localhost:8084/swagger-ui.html

### 📋 Features
- **Interactive Testing**: Test APIs langsung dari browser
- **JWT Authentication**: Bearer token support
- **Request/Response Examples**: Sample data lengkap
- **Error Handling**: Dokumentasi error responses
- **Rate Limiting**: Informasi API limits



## Project Structure

```
personal-finance-system/
├── pom.xml                      # Parent POM (Maven multi-module)
├── mvnw / mvnw.cmd              # Maven wrapper
├── README.md                    # This file
├── api-gateway/                 # API Gateway Service
│   ├── pom.xml
│   └── src/main/
├── user-service/                # User Management Service
│   ├── pom.xml
│   └── src/main/
├── transaction-service/         # Transaction Management Service
│   ├── pom.xml
│   └── src/main/
├── analytics-service/           # Analytics & Reporting Service
│   ├── pom.xml
│   └── src/main/
└── notification-service/        # Notification Service
    ├── pom.xml
    └── src/main/
```

## Maven Configuration

### Parent POM Features
- **Multi-module structure** dengan 5 microservices
- **Dependency management** untuk konsistensi versi
- **Plugin management** untuk build consistency
- **Common dependencies** (Lombok, Testing)

### Key Dependencies Managed
- Spring Boot 3.1.5
- Spring Cloud 2022.0.4
- Lombok 1.18.30
- MapStruct 1.5.5
- JJWT 0.11.5
- SpringDoc OpenAPI 2.2.0
- Testcontainers 1.19.1