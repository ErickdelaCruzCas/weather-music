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
