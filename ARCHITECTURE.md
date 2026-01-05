# Architecture

## Overview

This project follows a **Hexagonal Architecture** (Ports and Adapters) pattern, organized in a clean, layered structure that promotes separation of concerns and testability.

## Package Structure

```
com.erick.weather
├── application/              # Application Layer (Entry points)
│   ├── controller/          # REST Controllers (expose HTTP endpoints)
│   │   ├── WeatherController.kt
│   │   ├── MusicController.kt
│   │   ├── ContextController.kt
│   │   └── HealthController.kt
│   ├── dto/                 # Data Transfer Objects (API request/response)
│   │   ├── WeatherDto.kt    # Weather response DTOs
│   │   ├── MoodDto.kt       # Mood profile DTOs
│   │   ├── MusicDto.kt      # Music recommendation DTOs
│   │   ├── ContextDto.kt    # Aggregated context DTOs
│   │   ├── HealthDto.kt     # Health check DTOs
│   │   └── ErrorDto.kt      # Error response DTOs
│   ├── mapper/              # Domain to DTO mappers
│   │   ├── WeatherMapper.kt
│   │   ├── MoodMapper.kt
│   │   ├── MusicMapper.kt
│   │   ├── ContextMapper.kt
│   │   └── HealthMapper.kt
│   └── exception/           # Exception handling
│       └── GlobalExceptionHandler.kt
│
├── domain/                   # Domain Layer (Business Logic)
│   ├── model/               # Domain models and entities
│   │   ├── WeatherCondition.kt
│   │   ├── MoodProfile.kt
│   │   └── MusicRecommendation.kt
│   ├── service/             # Business logic services
│   │   ├── MusicRecommendationService.kt
│   │   ├── WeatherMoodMapper.kt
│   │   └── RegionResolver.kt
│   ├── port/                # Interfaces for external dependencies
│   │   ├── WeatherPort.kt
│   │   └── MusicPort.kt
│   └── exception/           # Domain exceptions
│       └── DomainExceptions.kt
│
├── infrastructure/           # Infrastructure Layer (External integrations)
│   ├── adapter/             # Port implementations
│   │   ├── WeatherAdapter.kt
│   │   └── MusicAdapter.kt
│   ├── client/              # External API clients
│   │   ├── spotify/        # Spotify API integration
│   │   │   ├── SpotifyClient.kt
│   │   │   ├── SpotifyAuthClient.kt
│   │   │   └── SpotifyDto.kt
│   │   └── weather/        # OpenWeather API integration
│   │       ├── WeatherClient.kt
│   │       └── OpenWeatherDto.kt
│   └── config/              # Spring configuration classes
│       ├── ExternalApiProperties.kt
│       └── WebClientConfig.kt
│
└── WeatherApplication.kt     # Main application entry point
```

## Layer Responsibilities

### 📍 Application Layer
**Purpose:** Handle HTTP requests and responses

- **Controllers**: Thin controllers that orchestrate domain services
  - WeatherController: Weather queries
  - MusicController: Mood-based music recommendations
  - ContextController: Aggregated weather + music context
  - HealthController: External services health checks
- **DTOs**: Data structures for API contracts (OpenAPI schemas)
  - Separate DTOs for each response type
  - Error response DTOs with structured error information
- **Mappers**: Convert between domain models and application DTOs
  - One mapper per domain concept
  - Pure transformation logic (no business rules)
- **Exception Handling**: Global exception handler
  - Maps domain exceptions to HTTP status codes
  - Provides structured error responses
  - Logs errors appropriately
- **Rules**:
  - No business logic
  - Only orchestration and mapping
  - Controllers must remain thin (< 50 lines)

### 🎯 Domain Layer
**Purpose:** Core business logic and rules

- **Models**: Domain entities with business behavior
  - WeatherCondition: Weather data with enum types
  - MoodProfile: Mood characteristics with matching algorithms
  - MusicRecommendation: Music entities with scoring logic
  - Domain models contain business rules (e.g., match scoring)
- **Services**: Business logic orchestration
  - MusicRecommendationService: Main orchestration service
  - WeatherMoodMapper: Weather-to-mood mapping logic
  - RegionResolver: Region parsing and validation
- **Ports**: Interfaces for external dependencies
  - WeatherPort: Contract for weather data retrieval
  - MusicPort: Contract for music search and recommendations
  - Ports define what we need, not how to get it
- **Exceptions**: Domain-specific exceptions
  - RegionNotFoundException: Invalid or unknown regions
  - ExternalServiceException: External service failures
  - Clear semantic meaning
- **Rules**:
  - No framework dependencies
  - Pure business logic
  - Framework-agnostic
  - All domain logic testable without infrastructure

### 🔌 Infrastructure Layer
**Purpose:** External integrations and technical concerns

- **Adapters**: Implementations of domain ports
  - WeatherAdapter: Implements WeatherPort using WeatherClient
  - MusicAdapter: Implements MusicPort using SpotifyClient
  - Map infrastructure DTOs to domain models
  - Handle error mapping to domain exceptions
- **Clients**: HTTP clients for external APIs
  - **SpotifyClient**: Search playlists/tracks, get audio features
  - **SpotifyAuthClient**: OAuth2 Client Credentials flow with token caching
  - **WeatherClient**: Get weather by city/coordinates, geocoding
  - Each client has dedicated DTOs for external API responses
  - Circuit breaker integration for resilience
- **Config**: Spring Boot configuration classes
  - ExternalApiProperties: Configuration properties for external APIs
  - WebClientConfig: WebClient beans with timeouts and settings
- **Rules**:
  - Implement domain ports
  - Handle technical details (HTTP, serialization, OAuth2, etc.)
  - External DTOs stay in infrastructure (never exposed)
  - Map all external exceptions to domain exceptions

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

## Data Flow

### Example: Weather Context Request

```
User Request
    ↓
[WeatherController]
    ↓
[ContextMapper] ← [MusicRecommendationService] → [WeatherMoodMapper]
                          ↓                            ↓
                    [WeatherPort]                [MusicPort]
                          ↓                            ↓
                  [WeatherAdapter]              [MusicAdapter]
                          ↓                            ↓
                  [WeatherClient]              [SpotifyClient]
                          ↓                            ↓
                   OpenWeather API              Spotify API
```

**Flow breakdown:**
1. Controller receives HTTP request
2. Service orchestrates domain logic
3. WeatherMoodMapper maps weather to mood
4. Ports define what we need (abstraction)
5. Adapters implement how to get it (concrete)
6. Clients make external API calls
7. Response flows back through layers, mapping at each boundary

## Implemented Endpoints

### Weather Endpoints
- `GET /api/v1/regions/{region}/weather`
  - Returns current weather for a region
  - Supports city names and coordinates

### Music Endpoints
- `GET /api/v1/regions/{region}/mood-music`
  - Returns music recommendations based on weather mood
  - Query params: `limit` (1-50), `type` (playlist/track/both)

### Context Endpoints
- `GET /api/v1/regions/{region}/weather-context`
  - Aggregated endpoint with weather + mood + recommendations
  - Query param: `limit` (1-50)
  - Optimized for single-call access

### Health Endpoints
- `GET /api/v1/health/external`
  - Health status of external services (Spotify, OpenWeather)
  - Returns service availability and response times

## API Documentation

Once the application is running, OpenAPI documentation will be available at:
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/api-docs
- OpenAPI YAML: See `openapi.yml` in project root

## Configuration

Application configuration is externalized in `application.yaml`:
- Server settings
- External API endpoints
- Circuit breaker configuration
- Logging levels

Environment-specific secrets (API keys, credentials) are injected via environment variables.
