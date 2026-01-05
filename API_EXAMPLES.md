# API Examples - Curl Commands

This document provides practical curl examples for all Weather-Music API endpoints.

## Base URL

```bash
# Local development
BASE_URL="http://localhost:8080"

# Production (example)
BASE_URL="https://api.weather-music.example.com"
```

---

## 1. Get Weather Data

### Endpoint: `GET /api/v1/regions/{region}/weather`

**What it does:**
- Fetches current weather conditions for a specified region
- Returns temperature, humidity, wind speed, and weather description

**What it does NOT do:**
- Does NOT provide weather forecasts
- Does NOT include music recommendations

### Example 1: Get weather by city name

```bash
curl -X GET "${BASE_URL}/api/v1/regions/Madrid/weather" \
  -H "Accept: application/json"
```

**Response:**
```json
{
  "region": "Madrid",
  "weather": {
    "condition": "Rain",
    "temperature": 15.5,
    "temperatureUnit": "celsius",
    "description": "light rain",
    "humidity": 85,
    "windSpeed": 12.5,
    "timestamp": "2026-01-05T12:00:00Z"
  }
}
```

### Example 2: Get weather by city with country code

```bash
curl -X GET "${BASE_URL}/api/v1/regions/London,GB/weather" \
  -H "Accept: application/json"
```

### Example 3: Get weather by coordinates

```bash
# Barcelona coordinates: 41.3874,2.1686
curl -X GET "${BASE_URL}/api/v1/regions/41.3874,2.1686/weather" \
  -H "Accept: application/json"
```

---

## 2. Get Mood-Based Music Recommendations

### Endpoint: `GET /api/v1/regions/{region}/mood-music`

**What it does:**
- Fetches current weather for the region
- Maps weather to mood characteristics (valence, energy)
- Searches Spotify for matching playlists and tracks
- Returns music recommendations with audio features

**What it does NOT do:**
- Does NOT create personalized recommendations based on user history
- Does NOT require Spotify authentication
- Does NOT modify user playlists

### Example 1: Get default music recommendations

```bash
curl -X GET "${BASE_URL}/api/v1/regions/Madrid/mood-music" \
  -H "Accept: application/json"
```

**Response:**
```json
{
  "region": "Madrid",
  "mood": {
    "name": "Melancholic",
    "description": "Introspective and contemplative music",
    "valence": {
      "min": 0.0,
      "max": 0.4
    },
    "energy": {
      "min": 0.3,
      "max": 0.6
    }
  },
  "recommendations": [
    {
      "id": "37i9dQZF1DX0SM0LYsmbMT",
      "name": "Rainy Day",
      "type": "playlist",
      "description": "Cozy rainy day vibes",
      "trackCount": 50,
      "avgValence": 0.32,
      "avgEnergy": 0.45,
      "url": "https://open.spotify.com/playlist/37i9dQZF1DX0SM0LYsmbMT",
      "imageUrl": "https://i.scdn.co/image/ab67706f00000002..."
    }
  ],
  "total": 10
}
```

### Example 2: Get only playlists with custom limit

```bash
curl -X GET "${BASE_URL}/api/v1/regions/Barcelona/mood-music?type=playlist&limit=5" \
  -H "Accept: application/json"
```

### Example 3: Get only tracks

```bash
curl -X GET "${BASE_URL}/api/v1/regions/London/mood-music?type=track&limit=20" \
  -H "Accept: application/json"
```

### Example 4: URL-encoded city names with spaces

```bash
# For cities with spaces (e.g., "New York")
curl -X GET "${BASE_URL}/api/v1/regions/New%20York/mood-music" \
  -H "Accept: application/json"

# Or use quotes
curl -X GET "${BASE_URL}/api/v1/regions/New York/mood-music" \
  -H "Accept: application/json"
```

---

## 3. Get Complete Weather Context (Aggregated)

### Endpoint: `GET /api/v1/regions/{region}/weather-context`

**What it does:**
- Combines weather data + mood profile + music recommendations in one call
- Most convenient endpoint for complete context
- Optimized for LLM/MCP integration

**What it does NOT do:**
- Does NOT provide weather forecasts
- Does NOT include historical data

### Example 1: Get complete context

```bash
curl -X GET "${BASE_URL}/api/v1/regions/Madrid/weather-context" \
  -H "Accept: application/json"
```

**Response:**
```json
{
  "region": "Madrid",
  "timestamp": "2026-01-05T12:00:00Z",
  "weather": {
    "condition": "Rain",
    "temperature": 15.5,
    "temperatureUnit": "celsius",
    "description": "light rain",
    "humidity": 85,
    "windSpeed": 12.5
  },
  "mood": {
    "name": "Melancholic",
    "description": "Introspective and contemplative music",
    "valence": { "min": 0.0, "max": 0.4 },
    "energy": { "min": 0.3, "max": 0.6 }
  },
  "recommendations": [
    {
      "id": "37i9dQZF1DX0SM0LYsmbMT",
      "name": "Rainy Day",
      "type": "playlist",
      "avgValence": 0.32,
      "avgEnergy": 0.45,
      "url": "https://open.spotify.com/playlist/37i9dQZF1DX0SM0LYsmbMT"
    }
  ],
  "total": 10
}
```

### Example 2: Get context with custom limit

```bash
curl -X GET "${BASE_URL}/api/v1/regions/Barcelona/weather-context?limit=15" \
  -H "Accept: application/json"
```

### Example 3: Pretty-print JSON output (for debugging)

```bash
curl -X GET "${BASE_URL}/api/v1/regions/Madrid/weather-context" \
  -H "Accept: application/json" | jq '.'
```

---

## 4. Health Check for External APIs

### Endpoint: `GET /api/v1/health/external`

**What it does:**
- Checks connectivity to Spotify and OpenWeather APIs
- Measures response times
- Returns overall health status

**What it does NOT do:**
- Does NOT verify API credentials
- Does NOT check rate limits

### Example 1: Get health status

```bash
curl -X GET "${BASE_URL}/api/v1/health/external" \
  -H "Accept: application/json"
```

**Response (All Healthy):**
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

**Response (Service Down):**
```json
{
  "status": "unhealthy",
  "timestamp": "2026-01-05T12:00:00Z",
  "services": {
    "spotify": {
      "status": "down",
      "responseTime": null,
      "error": "Connection timeout"
    },
    "openweather": {
      "status": "up",
      "responseTime": 95
    }
  }
}
```

### Example 2: Check health status only (no body, just status code)

```bash
curl -X GET "${BASE_URL}/api/v1/health/external" \
  -o /dev/null -w "%{http_code}\n" -s
```

---

## Error Examples

### Example 1: Invalid region (404)

```bash
curl -X GET "${BASE_URL}/api/v1/regions/XYZ123/weather" \
  -H "Accept: application/json"
```

**Response:**
```json
{
  "error": "NOT_FOUND",
  "message": "Region 'XYZ123' not found. Please check the region name.",
  "timestamp": "2026-01-05T12:00:00Z",
  "details": {
    "suggestions": ["Madrid", "Barcelona", "Valencia"]
  }
}
```

### Example 2: Invalid query parameter (400)

```bash
curl -X GET "${BASE_URL}/api/v1/regions/Madrid/mood-music?limit=999" \
  -H "Accept: application/json"
```

**Response:**
```json
{
  "error": "BAD_REQUEST",
  "message": "Limit must be between 1 and 50",
  "timestamp": "2026-01-05T12:00:00Z"
}
```

### Example 3: Service unavailable (503)

```bash
curl -X GET "${BASE_URL}/api/v1/regions/Madrid/weather" \
  -H "Accept: application/json"
```

**Response:**
```json
{
  "error": "SERVICE_UNAVAILABLE",
  "message": "External weather service is temporarily unavailable. Please try again later.",
  "timestamp": "2026-01-05T12:00:00Z",
  "details": {
    "service": "openweather",
    "retryAfter": 60
  }
}
```

---

## Advanced Usage

### Batch requests for multiple regions

```bash
#!/bin/bash

regions=("Madrid" "Barcelona" "London" "Paris" "Berlin")

for region in "${regions[@]}"; do
  echo "=== $region ==="
  curl -X GET "${BASE_URL}/api/v1/regions/${region}/weather-context?limit=5" \
    -H "Accept: application/json" -s | jq '.mood.name, .weather.condition'
  echo ""
done
```

### Save response to file

```bash
curl -X GET "${BASE_URL}/api/v1/regions/Madrid/weather-context" \
  -H "Accept: application/json" \
  -o madrid-context.json
```

### Include headers in response (debugging)

```bash
curl -X GET "${BASE_URL}/api/v1/regions/Madrid/weather" \
  -H "Accept: application/json" \
  -i
```

### Set custom timeout

```bash
curl -X GET "${BASE_URL}/api/v1/regions/Madrid/weather-context" \
  -H "Accept: application/json" \
  --max-time 10
```

---

## Testing Workflow

### Full workflow example:

```bash
#!/bin/bash
set -e

BASE_URL="http://localhost:8080"
REGION="Madrid"

echo "1. Checking API health..."
curl -s "${BASE_URL}/api/v1/health/external" | jq '.status'

echo -e "\n2. Getting weather for ${REGION}..."
WEATHER=$(curl -s "${BASE_URL}/api/v1/regions/${REGION}/weather")
echo "$WEATHER" | jq '.weather.condition'

echo -e "\n3. Getting mood-based music recommendations..."
MUSIC=$(curl -s "${BASE_URL}/api/v1/regions/${REGION}/mood-music?limit=3")
echo "$MUSIC" | jq '.mood.name, .recommendations[0].name'

echo -e "\n4. Getting complete context..."
CONTEXT=$(curl -s "${BASE_URL}/api/v1/regions/${REGION}/weather-context?limit=5")
echo "$CONTEXT" | jq '{region, mood: .mood.name, tracks: .total}'

echo -e "\n✓ All tests passed!"
```

---

## Notes

### URL Encoding
- City names with spaces must be URL-encoded: `New York` → `New%20York`
- Special characters should be encoded
- Coordinates format: `latitude,longitude` (e.g., `40.4168,-3.7038`)

### Response Format
- All responses are in JSON format
- Use `Accept: application/json` header (recommended)
- Timestamps follow ISO 8601 format

### Rate Limiting
- Default: 100 requests/minute per IP
- Exceeding limit returns `429 Too Many Requests`
- Includes `Retry-After` header with seconds to wait

### Caching
- Weather data: Cached for 30 minutes
- Music recommendations: Cached for 4 hours
- Use `Cache-Control` headers to control caching
