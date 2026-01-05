# Architecture

## Overview

This project follows a **Hexagonal Architecture** (Ports and Adapters) pattern, organized in a clean, layered structure that promotes separation of concerns and testability.

## Package Structure

```
com.erick.weather
├── application/              # Application Layer (Entry points)
│   ├── controller/          # REST Controllers (expose HTTP endpoints)
│   └── dto/                 # Data Transfer Objects (API request/response)
│
├── domain/                   # Domain Layer (Business Logic)
│   ├── model/               # Domain models and entities
│   ├── service/             # Business logic services
│   └── port/                # Interfaces for external dependencies
│
├── infrastructure/           # Infrastructure Layer (External integrations)
│   ├── client/              # External API clients
│   │   ├── spotify/        # Spotify API client and DTOs
│   │   └── weather/        # OpenWeather API client and DTOs
│   └── config/              # Spring configuration classes
│
└── WeatherApplication.kt     # Main application entry point
```

## Layer Responsibilities

### 📍 Application Layer
**Purpose:** Handle HTTP requests and responses

- **Controllers**: Thin controllers that orchestrate domain services
- **DTOs**: Data structures for API contracts (input/output)
- **Rules**:
  - No business logic
  - Only orchestration
  - Map between DTOs and domain models

### 🎯 Domain Layer
**Purpose:** Core business logic and rules

- **Model**: Domain entities and value objects
- **Service**: Business logic implementation
- **Port**: Interfaces defining contracts for external dependencies
- **Rules**:
  - No framework dependencies
  - Pure business logic
  - Framework-agnostic

### 🔌 Infrastructure Layer
**Purpose:** External integrations and technical concerns

- **Client**: HTTP clients for external APIs
  - Each client has its own package with DTOs
  - Implements ports defined in domain layer
- **Config**: Spring Boot configuration classes
- **Rules**:
  - Implement domain ports
  - Handle technical details (HTTP, serialization, etc.)
  - External DTOs stay in infrastructure

## Design Principles

### 1. Dependency Rule
Dependencies point inward: Infrastructure → Domain ← Application

```
Infrastructure ──→ Domain ←── Application
```

### 2. Separation of Concerns
- **Controllers**: HTTP handling only
- **Services**: Business logic only
- **Clients**: External API communication only

### 3. DTO Separation
- **Application DTOs**: API contracts (public interface)
- **Infrastructure DTOs**: External API models (not exposed)
- **Domain Models**: Business entities (internal)

### 4. No Shared Models
External API models (from Spotify, OpenWeather) must NOT be exposed in our API.
Always map to domain models or application DTOs.

## Technology Stack

- **Language**: Kotlin 2.1.0
- **Framework**: Spring Boot 3.4.1 (WebFlux)
- **Reactive**: Project Reactor + Kotlin Coroutines
- **Resilience**: Resilience4J Circuit Breaker
- **Documentation**: SpringDoc OpenAPI
- **Build**: Gradle with Kotlin DSL

## API Documentation

Once the application is running, OpenAPI documentation will be available at:
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/api-docs

## Configuration

Application configuration is externalized in `application.yaml`:
- Server settings
- External API endpoints
- Circuit breaker configuration
- Logging levels

Environment-specific secrets (API keys, credentials) are injected via environment variables.
