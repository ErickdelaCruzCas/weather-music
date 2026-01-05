# Use Case: Mood-Based Music by Weather

## Overview

This API provides music recommendations based on the emotional "mood" created by current weather conditions in different regions. It combines real-time weather data with Spotify's audio analysis features to create contextual music experiences.

---

## POC: Mood-Based Music Recommendations

### Core Concept

Weather influences human emotions and mood. Our API leverages this psychological connection by:

1. **Detecting** current weather conditions for a region
2. **Mapping** weather to emotional characteristics
3. **Matching** Spotify music with similar emotional profiles using audio features
4. **Recommending** playlists and tracks that fit the weather-induced mood

### Scientific Foundation: Audio Features

Spotify analyzes every track using machine learning to extract audio features:

#### Valence (0.0 - 1.0)
**Musical positiveness/negativity**
- **High valence (0.6-1.0)**: Happy, cheerful, euphoric
- **Medium valence (0.3-0.6)**: Neutral, contemplative
- **Low valence (0.0-0.3)**: Sad, depressed, angry

#### Energy (0.0 - 1.0)
**Intensity and activity level**
- **High energy (0.6-1.0)**: Fast, loud, noisy
- **Medium energy (0.3-0.6)**: Moderate intensity
- **Low energy (0.0-0.3)**: Calm, quiet, peaceful

---

## Weather-to-Mood Mapping

### Mapping Table

| Weather Condition | Mood | Valence Range | Energy Range | Characteristics |
|------------------|------|---------------|--------------|-----------------|
| Rain, Drizzle | Melancholic | 0.0 - 0.4 | 0.3 - 0.6 | Introspective, contemplative, gentle |
| Thunderstorm | Intense | 0.0 - 0.3 | 0.6 - 1.0 | Dramatic, powerful, dark |
| Clear, Sunny | Happy | 0.6 - 1.0 | 0.5 - 0.8 | Uplifting, energetic, positive |
| Clouds | Calm | 0.3 - 0.6 | 0.2 - 0.5 | Relaxed, neutral, mellow |
| Snow | Peaceful | 0.4 - 0.7 | 0.1 - 0.4 | Serene, quiet, ambient |
| Fog, Mist | Mysterious | 0.2 - 0.5 | 0.2 - 0.5 | Ethereal, atmospheric |

### Musical Examples by Mood

**Melancholic (Rain/Drizzle)**
- Genres: Indie, Lo-fi, Acoustic, Jazz
- Example keywords: "rainy day", "chill", "melancholy", "introspective"

**Intense (Thunderstorm)**
- Genres: Rock, Metal, Electronic, Orchestral
- Example keywords: "epic", "dramatic", "dark", "intense"

**Happy (Clear/Sunny)**
- Genres: Pop, Dance, Reggae, Feel-good
- Example keywords: "sunny", "happy", "upbeat", "summer vibes"

**Calm (Cloudy)**
- Genres: Ambient, Classical, Soft Rock
- Example keywords: "calm", "peaceful", "relaxing", "chill"

**Peaceful (Snow)**
- Genres: Ambient, Classical, Meditation
- Example keywords: "winter", "peaceful", "serene", "quiet"

---

## API Workflow

### Example: Rainy Day in Madrid

```
1. User Request:
   GET /api/v1/regions/Madrid/weather-context

2. System fetches weather:
   OpenWeather API → { temp: 15°C, condition: "Rain", humidity: 85% }

3. System maps weather to mood:
   "Rain" → Melancholic { valence: 0.0-0.4, energy: 0.3-0.6 }

4. System searches Spotify:
   Search playlists with keywords: "rainy day", "chill rain"
   Filter tracks by: valence < 0.4 AND energy BETWEEN 0.3-0.6

5. System ranks and returns:
   Top 10 playlists/tracks matching criteria

6. Response:
   {
     "region": "Madrid",
     "weather": { "condition": "Rain", "temp": 15, "description": "light rain" },
     "mood": {
       "name": "Melancholic",
       "valence": { "min": 0.0, "max": 0.4 },
       "energy": { "min": 0.3, "max": 0.6 }
     },
     "recommendations": [
       {
         "name": "Rainy Day Jazz",
         "type": "playlist",
         "avgValence": 0.35,
         "avgEnergy": 0.42,
         "url": "spotify:playlist:..."
       }
     ]
   }
```

---

## API Endpoints (POC)

### 1. GET /api/v1/regions/{region}/weather
**Purpose:** Get current weather for a region

**Parameters:**
- `region` (path): City name, coordinates, or region identifier

**Response:**
```json
{
  "region": "Madrid",
  "weather": {
    "condition": "Rain",
    "temperature": 15,
    "description": "light rain",
    "humidity": 85,
    "timestamp": "2026-01-05T12:00:00Z"
  }
}
```

---

### 2. GET /api/v1/regions/{region}/mood-music
**Purpose:** Get music recommendations based on current weather mood

**Parameters:**
- `region` (path): City name or region
- `limit` (query, optional): Number of recommendations (default: 10)
- `type` (query, optional): "playlist" or "track" (default: both)

**Response:**
```json
{
  "region": "Madrid",
  "mood": {
    "name": "Melancholic",
    "description": "Introspective and contemplative",
    "valence": { "min": 0.0, "max": 0.4 },
    "energy": { "min": 0.3, "max": 0.6 }
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
      "url": "https://open.spotify.com/playlist/...",
      "imageUrl": "https://..."
    }
  ],
  "total": 10
}
```

---

### 3. GET /api/v1/regions/{region}/weather-context
**Purpose:** Aggregated endpoint combining weather + mood + music

**Parameters:**
- `region` (path): City name or region
- `limit` (query, optional): Number of recommendations

**Response:**
```json
{
  "region": "Madrid",
  "timestamp": "2026-01-05T12:00:00Z",
  "weather": {
    "condition": "Rain",
    "temperature": 15,
    "description": "light rain"
  },
  "mood": {
    "name": "Melancholic",
    "valence": { "min": 0.0, "max": 0.4 },
    "energy": { "min": 0.3, "max": 0.6 }
  },
  "recommendations": [...]
}
```

---

### 4. GET /api/v1/health/external
**Purpose:** Health check for external dependencies

**Response:**
```json
{
  "status": "healthy",
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

---

## Future Extensions

### Extension 1: Featured Playlists (Post-POC)
**Endpoint:** `GET /api/v1/regions/{region}/featured-playlists`

Combine Spotify's curated Featured Playlists with weather context:
- Get region-specific featured playlists
- Annotate which ones fit the current weather
- Provide contextual recommendations

### Extension 2: Category-Based Music (Post-POC)
**Endpoint:** `GET /api/v1/regions/{region}/category-music`

Use Spotify Browse Categories (mood, activity) with weather:
- Map weather to relevant categories (e.g., Rain → "chill", "sleep")
- Fetch playlists from those categories
- Filter by region/locale

---

## Technical Considerations

### Region Resolution
- City names → Geocoding API → Coordinates
- Support for ISO 3166-1 alpha-2 country codes
- Major cities have predefined coordinates (cache)

### Caching Strategy
- Weather data: Cache 30 minutes (changes slowly)
- Spotify playlists: Cache 4 hours (static content)
- Audio features: Cache 24 hours (rarely changes)

### Rate Limiting
- OpenWeather: 60 calls/minute (free tier)
- Spotify: 100 calls/30 seconds
- Our API: 100 requests/minute per IP

### Error Handling
- Weather API down → Return cached data or 503
- Spotify API down → Return cached recommendations or 503
- Invalid region → 404 with suggestions
- Rate limit exceeded → 429 with retry-after header

---

## MCP Integration Readiness

This API is designed to be consumed by LLMs via MCP:

**LLM-Friendly Features:**
- Clear, semantic endpoint names
- Self-explanatory response structures
- Embedded context (weather + mood + music in single response)
- Human-readable mood descriptions
- Direct Spotify URLs for playback

**Example LLM Query:**
```
User: "What music should I listen to in London right now?"
LLM → GET /api/v1/regions/London/weather-context
LLM Response: "It's currently raining in London (15°C).
Perfect weather for some melancholic music! I recommend
the 'Rainy Day Jazz' playlist with chill, introspective vibes."
```
