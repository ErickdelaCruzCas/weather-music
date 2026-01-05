# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Weather-music is a Spring Boot reactive application that provides weather-based music recommendations by country. Built with Kotlin and Spring WebFlux, it uses a reactive programming model throughout.

## Tech Stack

- **Language**: Kotlin 2.1.0
- **Framework**: Spring Boot 3.4.1 (Spring Cloud 2024.0.0)
- **JVM**: Java 21
- **Reactive Stack**: Spring WebFlux with Project Reactor
- **Resilience**: Resilience4J circuit breaker
- **Build Tool**: Gradle with Kotlin DSL

## Development Commands

### Build and Run
```bash
./gradlew build                    # Build the project
./gradlew bootRun                  # Run the application
./gradlew clean build              # Clean and build
```

### Testing
```bash
./gradlew test                     # Run all tests
./gradlew test --tests WeatherApplicationTests    # Run specific test class
./gradlew test --tests WeatherApplicationTests.contextLoads    # Run single test
```

### Other Gradle Tasks
```bash
./gradlew tasks                    # List all available tasks
./gradlew dependencies             # View dependency tree
```

## Architecture

**Reactive Programming Model**: This application uses Spring WebFlux with Kotlin coroutines. All endpoints and services should follow reactive patterns using `Mono`, `Flux`, or Kotlin coroutines with `suspend` functions.

**Circuit Breaker Pattern**: Resilience4J is configured for fault tolerance. External API calls (weather services, music services) should be wrapped with circuit breakers to handle failures gracefully.

**Package Structure**: Code is organized under `com.erick.weather` package. Main application class is `WeatherApplication.kt`.

**Configuration**: Application configuration is in `src/main/resources/application.yaml`. The application name is "weather".

## Important Notes

- JVM level is set to Java 21 for better stability and ecosystem compatibility
- Use reactor-kotlin-extensions for better Kotlin integration with Project Reactor
- Tests use JUnit 5 (Jupiter) with Kotlin test support and kotlinx-coroutines-test for testing coroutines

---

## Development Methodology

### Role & Approach

Act as a **Senior Software Engineer** specialized in Kotlin, Spring Boot, and stable public API design.

This project is designed in phases, and you **MUST NOT** advance work from future phases.

The final goal is to expose this API as an MCP (Model Context Protocol), so the OpenAPI contract and documentation are critical.

### General Context

We are building an aggregator API that consumes real external APIs (Spotify + OpenWeather) and exposes its own well-documented endpoints.

**Use Case (POC):** Mood-Based Music Recommendations by Weather and Region

The API recommends music based on the emotional "mood" that the current weather creates, using Spotify's audio features (valence, energy) combined with real-time weather data.

**How it works:**
1. Get current weather for a region (OpenWeather API)
2. Map weather conditions to mood characteristics:
   - Rain/Drizzle → Melancholic music (low valence: 0.0-0.4)
   - Clear/Sunny → Happy music (high valence: 0.6-1.0)
   - Cloudy → Calm music (low energy)
   - Thunderstorm → Intense music (high energy, low valence)
3. Search/filter Spotify playlists and tracks matching those audio features

**Future Extensions (Post-POC):**
- Featured Playlists contextual by weather
- Category-based recommendations (Spotify Browse Categories + Climate)

---

## Project Phases

### 🔹 PHASE 0 — Project Setup (structure only)

**Objective:** Define the project skeleton without business logic.

**Mandatory Stack:**
- Kotlin
- Spring Boot
- Gradle (Kotlin DSL)
- Spring Web
- Configuration via application.yml

**Instructions:**
- Assume the project is generated with Spring Initializr
- Define:
  - Package structure
  - Necessary dependencies
  - Base configuration
- **DO NOT** write HTTP clients or services yet
- **DO NOT** implement endpoints

**Expected Output:**
- Recommended package structure
- build.gradle.kts (key dependencies)
- Base application.yml

---

### 🔹 PHASE 1 — OpenAPI First (CRITICAL)

**Objective:** Define the complete API contract before writing code.

**Mandatory Endpoints (POC):**

1. `GET /api/v1/regions/{region}/weather`
   - Returns current weather data for a region

2. `GET /api/v1/regions/{region}/mood-music`
   - Returns music recommendations based on current weather mood
   - Includes playlists/tracks with matching audio features (valence, energy)

3. `GET /api/v1/regions/{region}/weather-context`
   - Returns combined weather + mood + music recommendations (aggregated endpoint)

4. `GET /api/v1/health/external`
   - Health check for external APIs (Spotify, OpenWeather)

**Future Endpoints (Post-POC):**
- `GET /api/v1/regions/{region}/featured-playlists` (Extension: Option 2)
- `GET /api/v1/regions/{region}/category-music` (Extension: Option 5)

**Rules:**
- Create a complete `openapi.yml` file
- OpenAPI 3.1.0+
- Include:
  - Schemas for all requests/responses
  - Response examples (success and error cases)
  - Clear, semantic descriptions (optimized for LLM consumption)
  - Audio features explained (valence, energy, mood mapping)
- The contract must be stable and consumable by third parties
- Use proper HTTP status codes (200, 400, 404, 500, 503)
- Include rate limiting information

**Additionally:**
For each endpoint, add:
- 1 curl example with real data
- Functional description (what it does and what it DOES NOT do)
- Parameters explanation (region format, optional filters)
- Audio features mapping table (weather → mood → valence/energy ranges)

**Expected Output:**
- Complete openapi.yml
- Curl examples per endpoint
- API documentation ready for Swagger UI

---

### 🔹 PHASE 2 — External Clients (Adapters)

**Objective:** Implement HTTP clients for external APIs.

**External APIs:**
- **Spotify Web API**
  - OAuth2 Client Credentials
- **OpenWeather API**
  - API Key

**Rules:**
- Create dedicated clients:
  - SpotifyClient
  - WeatherClient
- Use WebClient or RestClient
- Handle:
  - Basic errors
  - Timeouts
- **NO** business logic here
- **NO** DTOs shared with domain

**Expected Output:**
- Well-encapsulated HTTP clients
- Separate external models

---

### 🔹 PHASE 3 — Domain and Aggregation

**Objective:** Implement aggregation logic.

**Main Business Rules:**

**1. Weather to Mood Mapping:**
```kotlin
Weather Condition → Mood Characteristics (Valence, Energy)
─────────────────────────────────────────────────────────
Rain, Drizzle      → Melancholic (valence: 0.0-0.4, energy: 0.3-0.6)
Thunderstorm       → Intense      (valence: 0.0-0.3, energy: 0.6-1.0)
Clear, Sunny       → Happy        (valence: 0.6-1.0, energy: 0.5-0.8)
Clouds             → Calm         (valence: 0.3-0.6, energy: 0.2-0.5)
Snow               → Peaceful     (valence: 0.4-0.7, energy: 0.1-0.4)
```

**2. Music Recommendation Logic:**
```
1. Get current weather for region (via WeatherClient)
2. Determine mood characteristics (WeatherMoodMapper)
3. Search Spotify for playlists/tracks matching audio features (via SpotifyClient)
4. Filter and rank results by feature match score
5. Return top N recommendations
```

**Domain Services to Create:**
- `WeatherMoodMapper`: Maps weather conditions to mood characteristics
- `MusicRecommendationService`: Core aggregation logic
- `RegionResolver`: Resolves region names to coordinates
- `AudioFeaturesMatcher`: Matches tracks to mood criteria

**Domain Models:**
- `WeatherCondition`: Domain representation of weather
- `MoodProfile`: Valence/Energy ranges for a mood
- `MusicRecommendation`: Aggregated response model

**Rules:**
- Controllers only orchestrate
- All mapping logic in domain layer
- Code oriented to readability and testability
- No Spotify/OpenWeather DTOs in domain layer

**Expected Output:**
- Clear aggregation service
- Clean and stable domain
- Weather-to-mood mapping testable independently

---

### 🔹 PHASE 4 — Controllers + REST Exposure

**Objective:** Expose endpoints defined in OpenAPI.

**Rules:**
- Thin controllers
- Map domain → response DTO
- No business logic in controllers
- Strictly respect OpenAPI contract

**Expected Output:**
- Functional endpoints
- API usable via curl

---

### 🔹 PHASE 5 — Documentation and Minimum Quality

**Objective:** Leave API ready for third parties and future MCP phase.

**Requirements:**
- Accessible OpenAPI
- Clear documentation
- Brief README explaining:
  - What the API does
  - External dependencies
  - How to test with curl

---

## Global Prohibitions

- ❌ No mocks
- ❌ No logic in controllers
- ❌ Do not expose external models
- ❌ No "magic strings" without enum
- ❌ Do not improvise endpoints outside OpenAPI

---

## Expected Final Result

A realistic aggregator API, well-documented, OpenAPI-first, ready to:
- Be consumed by humans
- Be consumed by an LLM via MCP
- Evolve without breaking contracts

---

## Implementation Status

### ✅ PHASE 0 - Project Setup (COMPLETED)
- Hexagonal architecture structure defined
- All dependencies configured in build.gradle.kts
- application.yaml with external API configuration
- Circuit breaker configuration for Resilience4J

**Files:**
- build.gradle.kts (Spring Boot 3.4.1, Kotlin 2.1.0, Java 21)
- application.yaml (server, external APIs, circuit breaker)
- ARCHITECTURE.md (package structure documentation)

### ✅ PHASE 1 - OpenAPI First (COMPLETED)
- Complete OpenAPI 3.1.0 specification (757 lines)
- 4 endpoints fully documented with schemas and examples
- Comprehensive curl examples in API_EXAMPLES.md

**Files:**
- openapi.yml (complete API contract)
- API_EXAMPLES.md (440 lines of curl examples)

### ✅ PHASE 2 - External Clients (COMPLETED)
- Spotify OAuth2 Client Credentials with token caching
- Spotify API client with search and audio features endpoints
- OpenWeather API client with geocoding support
- Circuit breaker integration on all external calls

**Files (785 lines):**
- infrastructure/config/ExternalApiProperties.kt
- infrastructure/config/WebClientConfig.kt
- infrastructure/client/spotify/SpotifyAuthClient.kt (OAuth2)
- infrastructure/client/spotify/SpotifyClient.kt (search, audio features)
- infrastructure/client/spotify/SpotifyDto.kt (external models)
- infrastructure/client/weather/WeatherClient.kt
- infrastructure/client/weather/OpenWeatherDto.kt

### ✅ PHASE 3 - Domain and Aggregation (COMPLETED)
- Weather-to-mood mapping with 6 mood types
- Proximity-based match scoring algorithm
- Multiple keyword search with audio features enrichment
- Complete hexagonal architecture with ports and adapters

**Files (878 lines):**
- domain/model/WeatherCondition.kt, MoodProfile.kt, MusicRecommendation.kt
- domain/port/WeatherPort.kt, MusicPort.kt
- domain/service/MusicRecommendationService.kt (orchestration)
- domain/service/WeatherMoodMapper.kt (weather→mood logic)
- domain/service/RegionResolver.kt (city/coordinates parsing)
- infrastructure/adapter/WeatherAdapter.kt (implements WeatherPort)
- infrastructure/adapter/MusicAdapter.kt (implements MusicPort)

### ✅ PHASE 4 - Controllers + REST Exposure (COMPLETED)
- 4 REST controllers exposing OpenAPI endpoints
- Complete DTO layer with mappers
- Global exception handler with proper HTTP status codes
- Error mapping from domain exceptions to HTTP responses

**Files (624 lines):**
- application/dto/* (6 files - WeatherDto, MoodDto, MusicDto, ContextDto, HealthDto, ErrorDto)
- application/mapper/* (5 files - domain to DTO mappers)
- application/controller/* (4 files - thin controllers)
- application/exception/GlobalExceptionHandler.kt
- domain/exception/DomainExceptions.kt

### 🔄 PHASE 5 - Documentation and Minimum Quality (IN PROGRESS)
- Unit tests for domain services (WeatherMoodMapper, RegionResolver, MoodProfile)
- Integration tests for controllers (Weather, Music, Context)
- Updated ARCHITECTURE.md with complete implementation details
- API testing guide (pending)

**Test Files:**
- test/.../domain/service/WeatherMoodMapperTest.kt
- test/.../domain/service/RegionResolverTest.kt
- test/.../domain/model/MoodProfileTest.kt
- test/.../application/controller/WeatherControllerTest.kt
- test/.../application/controller/MusicControllerTest.kt
- test/.../application/controller/ContextControllerTest.kt

---

## Implementation Notes

### Key Design Decisions

**1. Hexagonal Architecture**
- Strict separation between domain, application, and infrastructure layers
- Domain layer has no framework dependencies
- Ports define contracts, adapters implement them
- All external models stay in infrastructure layer

**2. Reactive Programming**
- All operations use Mono/Flux from Project Reactor
- Non-blocking I/O throughout the stack
- Reactive error handling with onErrorMap

**3. OAuth2 Token Management**
- SpotifyAuthClient handles OAuth2 Client Credentials flow
- Token caching with expiration checking
- Automatic token refresh

**4. Error Handling**
- Domain exceptions for business errors
- GlobalExceptionHandler maps to HTTP status codes
- Structured error responses matching OpenAPI schema

**5. Weather-to-Mood Mapping**
- 6 mood types: Melancholic, Intense, Happy, Calm, Peaceful, Mysterious
- Each mood has valence and energy ranges (0.0-1.0)
- Proximity-based scoring algorithm for music matching

**6. Music Recommendation Algorithm**
```
1. Get weather for region
2. Map weather condition to mood profile
3. Generate keywords from mood
4. Search Spotify for top 3 keywords
5. Enrich playlists with audio features
6. Filter by mood profile (valence/energy ranges)
7. Calculate match scores
8. Sort by best match and return top N
```

### Environment Variables Required

To run the application, you must set:

```bash
export SPOTIFY_CLIENT_ID="your_spotify_client_id"
export SPOTIFY_CLIENT_SECRET="your_spotify_client_secret"
export OPENWEATHER_API_KEY="your_openweather_api_key"
```

### Running the Application

```bash
# Build
./gradlew build

# Run (requires environment variables)
./gradlew bootRun

# Access Swagger UI
open http://localhost:8080/swagger-ui.html
```

### Testing

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests WeatherMoodMapperTest
```

---

## Next Steps (Post-Phase 5)

### Future Extensions (From Original Plan)
1. **Featured Playlists** (Option 2)
   - Add endpoint: `GET /api/v1/regions/{region}/featured-playlists`
   - Curated playlists based on weather context

2. **Category-based Music** (Option 5)
   - Add endpoint: `GET /api/v1/regions/{region}/category-music`
   - Spotify categories filtered by climate/weather

### MCP Integration
- Expose OpenAPI specification via MCP server
- Allow LLMs to consume weather-music context
- Optimize for natural language queries

---

## Development Guidelines

When working on this codebase:

1. **Respect Layer Boundaries**
   - Never import infrastructure code in domain layer
   - Never put business logic in controllers
   - Keep external DTOs in infrastructure layer

2. **Follow Reactive Patterns**
   - Use Mono/Flux consistently
   - Handle errors with onErrorMap/onErrorResume
   - Never block in reactive chains

3. **Test Coverage**
   - Unit tests for domain logic (no mocks)
   - Integration tests for controllers (mock services)
   - Test error cases and edge cases

4. **OpenAPI Contract**
   - Never change OpenAPI without updating documentation
   - Ensure response DTOs match schemas exactly
   - Maintain backward compatibility

5. **Error Handling**
   - Use domain exceptions for business errors
   - Map all external errors to domain exceptions in adapters
   - Provide meaningful error messages
