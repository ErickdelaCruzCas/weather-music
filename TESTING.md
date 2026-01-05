# Testing Guide

This document provides instructions for testing the Weather-Music API at various levels.

## Prerequisites

Before running the application, ensure you have:

1. **API Credentials**:
   ```bash
   export SPOTIFY_CLIENT_ID="your_spotify_client_id"
   export SPOTIFY_CLIENT_SECRET="your_spotify_client_secret"
   export OPENWEATHER_API_KEY="your_openweather_api_key"
   ```

2. **Java 21** installed
3. **Gradle** (or use the wrapper `./gradlew`)

## Running Tests

### Unit Tests

Run all unit tests:
```bash
./gradlew test
```

Run specific test class:
```bash
./gradlew test --tests WeatherMoodMapperTest
./gradlew test --tests RegionResolverTest
./gradlew test --tests MoodProfileTest
```

Run tests with detailed output:
```bash
./gradlew test --info
```

### Integration Tests

Run controller integration tests:
```bash
./gradlew test --tests "*ControllerTest"
```

Run specific controller test:
```bash
./gradlew test --tests WeatherControllerTest
./gradlew test --tests MusicControllerTest
./gradlew test --tests ContextControllerTest
```

### Test Coverage

Generate test coverage report:
```bash
./gradlew test jacocoTestReport
```

View coverage report:
```bash
open build/reports/jacoco/test/html/index.html
```

## Running the Application

### Start the Application

```bash
./gradlew bootRun
```

The application will start on port 8080 by default.

### Verify Application is Running

```bash
curl http://localhost:8080/actuator/health
```

Expected response:
```json
{
  "status": "UP"
}
```

## Manual API Testing

### 1. Check External Services Health

```bash
curl http://localhost:8080/api/v1/health/external
```

Expected response:
```json
{
  "status": "healthy",
  "timestamp": "2026-01-05T12:00:00Z",
  "services": {
    "spotify": {
      "status": "up",
      "responseTime": 145
    },
    "openweather": {
      "status": "up",
      "responseTime": 89
    }
  }
}
```

### 2. Get Weather for a City

```bash
curl "http://localhost:8080/api/v1/regions/Madrid/weather"
```

Expected response:
```json
{
  "region": "Madrid",
  "weather": {
    "condition": "Clear",
    "temperature": 25.0,
    "temperatureUnit": "celsius",
    "description": "clear sky",
    "humidity": 45,
    "windSpeed": 10.0,
    "timestamp": "2026-01-05T12:00:00Z"
  }
}
```

### 3. Get Mood-Based Music Recommendations

```bash
curl "http://localhost:8080/api/v1/regions/Madrid/mood-music?limit=5"
```

Expected response:
```json
{
  "region": "Madrid",
  "mood": {
    "name": "HAPPY",
    "description": "Uplifting and energetic music",
    "valence": {
      "min": 0.6,
      "max": 1.0
    },
    "energy": {
      "min": 0.5,
      "max": 0.8
    }
  },
  "recommendations": [
    {
      "id": "37i9dQZF1DXdPec7aLTmlC",
      "name": "Happy Hits!",
      "type": "playlist",
      "description": "The happiness you've been looking for.",
      "trackCount": 100,
      "avgValence": 0.85,
      "avgEnergy": 0.72,
      "url": "https://open.spotify.com/playlist/37i9dQZF1DXdPec7aLTmlC",
      "imageUrl": "https://i.scdn.co/image/..."
    }
  ],
  "total": 5
}
```

### 4. Get Complete Weather Context

```bash
curl "http://localhost:8080/api/v1/regions/London/weather-context?limit=10"
```

This endpoint combines weather, mood, and music in a single response.

### 5. Test with Coordinates

```bash
curl "http://localhost:8080/api/v1/regions/40.4168,-3.7038/weather"
```

Madrid coordinates: `40.4168,-3.7038`
London coordinates: `51.5074,-0.1278`
New York coordinates: `40.7128,-74.0060`

### 6. Test Different Music Types

Get only playlists:
```bash
curl "http://localhost:8080/api/v1/regions/Paris/mood-music?type=playlist&limit=5"
```

Get only tracks:
```bash
curl "http://localhost:8080/api/v1/regions/Paris/mood-music?type=track&limit=5"
```

Get both (default):
```bash
curl "http://localhost:8080/api/v1/regions/Paris/mood-music?type=both&limit=5"
```

## Testing Different Weather Conditions

To test different mood mappings, use cities with different typical weather:

### Rainy Weather → Melancholic Mood
```bash
curl "http://localhost:8080/api/v1/regions/Seattle/mood-music"
curl "http://localhost:8080/api/v1/regions/London/mood-music"
```

Expected mood: `MELANCHOLIC` (valence: 0.0-0.4, energy: 0.3-0.6)

### Sunny Weather → Happy Mood
```bash
curl "http://localhost:8080/api/v1/regions/Barcelona/mood-music"
curl "http://localhost:8080/api/v1/regions/Miami/mood-music"
```

Expected mood: `HAPPY` (valence: 0.6-1.0, energy: 0.5-0.8)

### Cloudy Weather → Calm Mood
```bash
curl "http://localhost:8080/api/v1/regions/Berlin/mood-music"
curl "http://localhost:8080/api/v1/regions/Paris/mood-music"
```

Expected mood: `CALM` (valence: 0.3-0.6, energy: 0.2-0.5)

## Error Testing

### 404 - Region Not Found
```bash
curl "http://localhost:8080/api/v1/regions/InvalidCity123/weather"
```

Expected response (404):
```json
{
  "error": "NOT_FOUND",
  "message": "Region 'InvalidCity123' not found",
  "timestamp": "2026-01-05T12:00:00Z"
}
```

### 400 - Invalid Parameters
```bash
curl "http://localhost:8080/api/v1/regions/Madrid/mood-music?limit=100"
```

Expected response (400):
```json
{
  "error": "BAD_REQUEST",
  "message": "Validation failed",
  "timestamp": "2026-01-05T12:00:00Z"
}
```

### 400 - Invalid Type
```bash
curl "http://localhost:8080/api/v1/regions/Madrid/mood-music?type=invalid"
```

Expected response (400):
```json
{
  "error": "BAD_REQUEST",
  "message": "Invalid type: invalid. Must be one of: playlist, track, both",
  "timestamp": "2026-01-05T12:00:00Z"
}
```

## Interactive Testing with Swagger UI

Once the application is running, access Swagger UI:

```bash
open http://localhost:8080/swagger-ui.html
```

Swagger UI provides:
- Interactive API documentation
- Try-it-out functionality for all endpoints
- Request/response examples
- Schema documentation

## Testing Best Practices

### 1. Test Layer Isolation

**Unit Tests** - Test domain logic in isolation:
- WeatherMoodMapperTest: Test weather-to-mood mapping
- RegionResolverTest: Test region parsing
- MoodProfileTest: Test match scoring algorithms

**Integration Tests** - Test controllers with mocked services:
- WeatherControllerTest: Test weather endpoint
- MusicControllerTest: Test music endpoint with validation
- ContextControllerTest: Test aggregated endpoint

### 2. Test Coverage Goals

- Domain layer: 90%+ coverage (core business logic)
- Controllers: 80%+ coverage (all happy and error paths)
- Adapters: 70%+ coverage (error mapping)

### 3. What to Test

**Domain Logic:**
- ✅ Weather-to-mood mapping for all weather types
- ✅ Match scoring algorithm with various inputs
- ✅ Region parsing (city names, coordinates, edge cases)
- ✅ Mood profile matching logic

**API Contract:**
- ✅ Response structure matches OpenAPI schemas
- ✅ Validation rules (min/max limits)
- ✅ Error responses with proper status codes
- ✅ Query parameter handling

**Error Handling:**
- ✅ Invalid regions return 404
- ✅ Invalid parameters return 400
- ✅ External service failures return 503
- ✅ Unexpected errors return 500

## Continuous Integration

For CI/CD pipelines:

```bash
# Build and run all tests
./gradlew clean build

# Generate test report
./gradlew test jacocoTestReport

# Check for test failures
./gradlew test --fail-fast
```

## Performance Testing

### Load Testing with Apache Bench

```bash
# Test weather endpoint
ab -n 100 -c 10 http://localhost:8080/api/v1/regions/Madrid/weather

# Test mood-music endpoint
ab -n 100 -c 10 "http://localhost:8080/api/v1/regions/Madrid/mood-music?limit=5"
```

### Expected Performance

- Weather endpoint: < 500ms (depends on OpenWeather API)
- Mood-music endpoint: < 2000ms (multiple API calls to Spotify)
- Weather-context endpoint: < 2500ms (aggregated, multiple operations)

## Troubleshooting

### Tests Failing with "Connection Refused"

Ensure external API credentials are set:
```bash
echo $SPOTIFY_CLIENT_ID
echo $OPENWEATHER_API_KEY
```

### Circuit Breaker Open

If you see "Circuit breaker is OPEN" errors:
- Wait for the circuit to reset (default: 60 seconds)
- Check external service availability
- Review circuit breaker logs

### Rate Limiting

If you hit Spotify or OpenWeather rate limits:
- Reduce test frequency
- Use different API keys for testing
- Implement request caching (future enhancement)

## Next Steps

After manual testing is successful:
1. Verify all automated tests pass
2. Check test coverage reports
3. Review error handling for edge cases
4. Test with real API credentials
5. Prepare for production deployment
