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
