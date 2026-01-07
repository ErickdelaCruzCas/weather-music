# Weather-Music API

A Spring Boot reactive API that provides **weather-based music recommendations by region**. Get current weather conditions and discover music that matches the mood created by the weather using Spotify's audio features (valence, energy) combined with real-time weather data.

## 🎯 What It Does

This API aggregates data from **Spotify** and **OpenWeather** APIs to:
- Fetch current weather conditions for any region (city or coordinates)
- Map weather conditions to emotional moods (happy, melancholic, calm, intense, peaceful, mysterious)
- Recommend music (playlists/tracks) that matches the current weather mood
- Provide aggregated weather + mood + music context in a single endpoint

**Example Use Case:** It's raining in London → API returns melancholic music (low valence: 0.0-0.4) with playlists like "Rainy Day Jazz" or "Melancholic Indie".

## 🚀 Quick Start

### Prerequisites

- **Java 21** (JDK 21)
- **Gradle** (included via wrapper)
- **Spotify Developer Account** → [Get credentials](https://developer.spotify.com/dashboard)
- **OpenWeather API Key** → [Get API key](https://openweathermap.org/api)

### 1. Set Environment Variables

```bash
export SPOTIFY_CLIENT_ID="your_spotify_client_id"
export SPOTIFY_CLIENT_SECRET="your_spotify_client_secret"
export OPENWEATHER_API_KEY="your_openweather_api_key"
```

### 2. Build and Run

```bash
# Build the project
./gradlew build

# Run the application
./gradlew bootRun
```

The API will start at `http://localhost:8080`

### 3. Access Swagger UI

Open your browser at:
```
http://localhost:8080/swagger-ui.html
```

## 📡 Quick API Examples

### Get Weather for a Region

```bash
curl "http://localhost:8080/api/v1/regions/London/weather"
```

**Response:**
```json
{
  "region": "London",
  "temperature": 12.5,
  "feelsLike": 10.8,
  "condition": "Rain",
  "description": "light rain",
  "humidity": 82,
  "windSpeed": 4.5,
  "timestamp": "2026-01-07T14:30:00Z"
}
```

### Get Mood-Based Music Recommendations

```bash
curl "http://localhost:8080/api/v1/regions/London/mood-music?limit=5"
```

**Response:**
```json
{
  "region": "London",
  "mood": {
    "type": "Melancholic",
    "description": "Reflective and introspective mood...",
    "valenceRange": { "min": 0.0, "max": 0.4 },
    "energyRange": { "min": 0.3, "max": 0.6 }
  },
  "recommendations": [
    {
      "name": "Rainy Day Jazz",
      "type": "playlist",
      "spotifyUrl": "https://open.spotify.com/playlist/...",
      "imageUrl": "https://i.scdn.co/image/...",
      "matchScore": 95.5,
      "audioFeatures": {
        "valence": 0.35,
        "energy": 0.45
      }
    }
  ],
  "totalRecommendations": 5
}
```

### Get Full Weather + Music Context

```bash
curl "http://localhost:8080/api/v1/regions/Paris/weather-context"
```

This returns weather + mood profile + music recommendations in a single aggregated response.

### Check External APIs Health

```bash
curl "http://localhost:8080/api/v1/health/external"
```

## 📚 Complete Documentation

- **[Architecture Guide](ARCHITECTURE.md)** - Hexagonal architecture, package structure, design principles
- **[API Examples](API_EXAMPLES.md)** - 440+ lines of curl examples with all endpoints and error cases
- **[Testing Guide](TESTING.md)** - Unit tests, integration tests, manual testing procedures
- **[Use Case Documentation](USE_CASE.md)** - Weather-to-mood mapping, audio features explained
- **[HTTP Client Guide](HTTP_CLIENT_GUIDE.md)** - IntelliJ IDEA and VS Code REST client integration
- **[OpenAPI Specification](openapi.yml)** - Complete API contract (OpenAPI 3.1.0)

## 🧪 Testing

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests WeatherMoodMapperTest

# Run with coverage
./gradlew test jacocoTestReport
```

See [TESTING.md](TESTING.md) for comprehensive testing guide.

## 🏗️ Tech Stack

- **Language:** Kotlin 2.1.0
- **Framework:** Spring Boot 3.4.1 (Spring Cloud 2024.0.0)
- **Reactive Stack:** Spring WebFlux + Project Reactor
- **JVM:** Java 21
- **Resilience:** Resilience4J circuit breaker
- **Build Tool:** Gradle with Kotlin DSL
- **Documentation:** SpringDoc OpenAPI (Swagger UI)

## 🌍 External Dependencies

### Spotify Web API
- **Authentication:** OAuth2 Client Credentials flow
- **Endpoints Used:**
  - `/v1/search` - Search playlists/tracks
  - `/v1/audio-features` - Get audio features (valence, energy)
- **Rate Limits:** Handled via circuit breaker
- **Documentation:** https://developer.spotify.com/documentation/web-api

### OpenWeather API
- **Authentication:** API Key
- **Endpoints Used:**
  - `/geo/1.0/direct` - Geocoding (city → coordinates)
  - `/data/2.5/weather` - Current weather data
- **Rate Limits:** 60 calls/minute (free tier)
- **Documentation:** https://openweathermap.org/api

## 🎵 Weather-to-Mood Mapping

| Weather Condition | Mood Type | Valence Range | Energy Range | Example Genres |
|------------------|-----------|---------------|--------------|----------------|
| Rain, Drizzle | Melancholic | 0.0 - 0.4 | 0.3 - 0.6 | Indie, Jazz, Acoustic |
| Thunderstorm | Intense | 0.0 - 0.3 | 0.6 - 1.0 | Rock, Metal, Electronic |
| Clear, Sunny | Happy | 0.6 - 1.0 | 0.5 - 0.8 | Pop, Reggae, Dance |
| Clouds | Calm | 0.3 - 0.6 | 0.2 - 0.5 | Ambient, Lo-fi, Chill |
| Snow | Peaceful | 0.4 - 0.7 | 0.1 - 0.4 | Classical, Folk, Instrumental |
| Fog, Mist | Mysterious | 0.2 - 0.5 | 0.3 - 0.6 | Atmospheric, Ambient, Downtempo |

**Audio Features:**
- **Valence:** Musical positiveness (0.0 = sad, 1.0 = happy)
- **Energy:** Intensity and activity (0.0 = calm, 1.0 = energetic)

## 🔧 Architecture

This project follows **Hexagonal Architecture** (Ports and Adapters):

```
application/     → Controllers, DTOs, Mappers (HTTP layer)
domain/          → Business logic, models, ports (core)
infrastructure/  → External API clients, adapters (external layer)
```

**Key Principles:**
- Domain layer has NO framework dependencies
- All external models stay in infrastructure layer
- Reactive programming throughout (Mono/Flux)
- Circuit breaker pattern for external API calls

See [ARCHITECTURE.md](ARCHITECTURE.md) for complete details.

## 📋 API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/regions/{region}/weather` | GET | Current weather for a region |
| `/api/v1/regions/{region}/mood-music` | GET | Music recommendations based on weather mood |
| `/api/v1/regions/{region}/weather-context` | GET | Aggregated weather + mood + music |
| `/api/v1/health/external` | GET | Health check for Spotify and OpenWeather APIs |
| `/swagger-ui.html` | GET | Interactive API documentation |
| `/api-docs` | GET | OpenAPI specification (JSON) |
| `/api-docs.yaml` | GET | OpenAPI specification (YAML) |

## 🔒 Security Notes

- API keys are configured via environment variables (never committed to git)
- Spotify OAuth2 tokens are cached with automatic refresh
- Circuit breaker prevents cascading failures from external APIs
- Input validation for region names and coordinates
- Rate limiting information exposed in headers

## 🚧 Project Status

**Phase 5 - Documentation and Minimum Quality (COMPLETE)**

- ✅ Complete OpenAPI 3.1.0 specification
- ✅ Hexagonal architecture implementation
- ✅ Reactive programming with Spring WebFlux
- ✅ Circuit breaker integration (Resilience4J)
- ✅ Unit and integration tests
- ✅ Comprehensive documentation

**Next Steps (Future Extensions):**
- Featured playlists endpoint (curated by weather context)
- Category-based music endpoint (Spotify categories + climate)
- MCP (Model Context Protocol) integration for LLM consumption

---

## 💡 Próximos Casos de Uso Propuestos

Aquí presentamos tres casos de uso innovadores que expanden la integración entre Spotify y OpenWeather:

### 1️⃣ **Weather Forecast Playlist Generator** 🌤️ → 📅
**Descripción:** Crea una playlist dinámica basada en el pronóstico del clima para los próximos 7 días.

**Cómo funciona:**
- Obtén el forecast de 7 días para una región (OpenWeather Forecast API)
- Analiza las tendencias del clima (días soleados consecutivos, frentes de lluvia, cambios bruscos)
- Genera una playlist "narrativa" que refleje la evolución emocional de la semana
  - Lunes lluvioso → Tracks melancólicos para empezar
  - Miércoles soleado → Transición a música energética
  - Viernes tormentoso → Música intensa para cerrar la semana

**Endpoints propuestos:**
```
GET /api/v1/regions/{region}/forecast-playlist
  ?days=7          # Días a incluir (1-7)
  &duration=120    # Duración en minutos
  &transition=smooth  # smooth | abrupt (estilo de transición entre moods)
```

**Ejemplo de respuesta:**
```json
{
  "region": "Barcelona",
  "forecast": [
    {"day": "Mon", "weather": "Rain", "mood": "Melancholic", "tracks": 8},
    {"day": "Tue", "weather": "Clouds", "mood": "Calm", "tracks": 7},
    {"day": "Wed", "weather": "Clear", "mood": "Happy", "tracks": 10}
  ],
  "playlist": {
    "name": "Barcelona Weather Journey (Jan 7-14)",
    "totalTracks": 50,
    "totalDuration": 120,
    "spotifyUrl": "https://open.spotify.com/playlist/..."
  }
}
```

**Valor agregado:**
- Planificación emocional basada en clima futuro
- Playlists "storytelling" que narran la semana climática
- Ideal para planear viajes, eventos al aire libre, o simplemente prepararse mentalmente

---

### 2️⃣ **Multi-City Weather Sync** 🌍 → 🎧
**Descripción:** Genera recomendaciones musicales que combinan el clima de múltiples ciudades simultáneamente.

**Casos de uso:**
- **Equipos remotos:** Playlist que refleja el clima de todos los miembros del equipo distribuido
- **Viajeros:** Música que mezcla el clima de origen y destino (transición emocional del viaje)
- **Amigos en diferentes zonas horarias:** Playlist compartida que une climas y estados de ánimo

**Cómo funciona:**
- Acepta una lista de regiones con pesos opcionales
- Calcula un "mood profile agregado" basado en:
  - Promedio ponderado de valence/energy de cada región
  - Detección de "clima dominante" (si 3/4 ciudades están lluviosas → bias melancólico)
  - Modo "contrast" para incluir tracks que representen todos los climas

**Endpoints propuestos:**
```
POST /api/v1/multi-city/mood-music
{
  "cities": [
    {"region": "London", "weight": 0.4},
    {"region": "Tokyo", "weight": 0.3},
    {"region": "São Paulo", "weight": 0.3}
  ],
  "mode": "blended",  # blended | contrast | dominant
  "limit": 20
}
```

**Ejemplo de respuesta:**
```json
{
  "aggregatedMood": {
    "type": "Blended (Calm-Melancholic)",
    "valenceRange": {"min": 0.25, "max": 0.55},
    "energyRange": {"min": 0.25, "max": 0.55}
  },
  "cityWeathers": [
    {"region": "London", "condition": "Rain", "contribution": 40},
    {"region": "Tokyo", "condition": "Clear", "contribution": 30},
    {"region": "São Paulo", "condition": "Clouds", "contribution": 30}
  ],
  "recommendations": [
    {
      "name": "Global Chill Mix",
      "matchScore": 92,
      "represents": ["London", "São Paulo"]
    }
  ]
}
```

**Valor agregado:**
- Conecta emocionalmente a personas en diferentes geografías
- Playlist colaborativas basadas en contexto climático real
- Útil para empresas con equipos distribuidos, familias separadas, o comunidades globales

---

### 3️⃣ **Weather Alert Soundtrack** ⚠️ → 🎵
**Descripción:** Sistema de notificaciones musicales que responde a cambios dramáticos o eventos climáticos extremos.

**Cómo funciona:**
- Monitoreo continuo del clima en regiones suscritas
- Detección de eventos climáticos significativos:
  - **Cambios bruscos:** Temperatura baja >10°C en 2 horas → "Chill" playlist
  - **Alertas meteorológicas:** Tormenta eléctrica → "Epic Storm" playlist
  - **Eventos especiales:** Primera nevada del año → "Winter Wonderland" playlist
  - **Golden hour:** Atardecer despejado → "Sunset Vibes" playlist
- Envío de notificaciones push con playlist recomendada

**Endpoints propuestos:**
```
POST /api/v1/alerts/subscribe
{
  "region": "Miami",
  "events": ["storm", "temperature_drop", "first_snow", "golden_hour"],
  "webhook": "https://myapp.com/weather-music-notification"
}

GET /api/v1/alerts/current/{region}
  → Devuelve si hay una alerta activa y la playlist asociada
```

**Ejemplo de webhook payload:**
```json
{
  "event": "storm_warning",
  "region": "Miami",
  "timestamp": "2026-01-07T18:30:00Z",
  "weather": {
    "condition": "Thunderstorm",
    "windSpeed": 45.0,
    "severity": "high"
  },
  "recommendedPlaylist": {
    "name": "Epic Storm Anthems",
    "description": "Intense music for intense weather",
    "spotifyUrl": "https://open.spotify.com/playlist/...",
    "mood": "Intense",
    "tracks": 25
  }
}
```

**Valor agregado:**
- Soundtrack dinámico en tiempo real para el clima actual
- Gamificación del clima (coleccionar playlists de eventos raros)
- Útil para:
  - **Fotógrafos:** Alertas de golden hour con música inspiradora
  - **Atletas:** Notificación de clima ideal para correr/ciclismo
  - **Content creators:** Música para videos según el clima del momento

---

### 🔧 Consideraciones Técnicas para Implementación

**APIs adicionales necesarias:**
- **OpenWeather Forecast API** (5 day / 3 hour forecast) - Caso 1
- **OpenWeather One Call API** (minutely, hourly, daily, alerts) - Caso 3
- **Spotify Create Playlist API** (para generar playlists dinámicas) - Casos 1 y 3
- **Webhook/WebSocket support** (para notificaciones en tiempo real) - Caso 3

**Arquitectura sugerida:**
- **Caso 1 & 2:** Extensión natural de los endpoints actuales (similares a `/mood-music`)
- **Caso 3:** Requiere:
  - Background job/scheduler (Spring @Scheduled o Quartz)
  - Persistencia de suscripciones (base de datos)
  - Event-driven architecture (WebFlux + Server-Sent Events o WebSockets)
  - Rate limiting más sofisticado para monitoreo continuo

**Fases de implementación:**
1. **Fase 6 - Caso 1:** Forecast Playlist (menos complejo, usa APIs similares)
2. **Fase 7 - Caso 2:** Multi-City Sync (nueva lógica de agregación, sin infraestructura adicional)
3. **Fase 8 - Caso 3:** Weather Alerts (más complejo, requiere background processing)

## 📝 Development

See [CLAUDE.md](CLAUDE.md) for:
- Development phases 0-5
- Development methodology
- Implementation guidelines
- Code conventions

## 📄 License

This is a proof-of-concept project demonstrating weather-based music recommendations.

## 🙋 Support

For issues or questions:
- Check the [Testing Guide](TESTING.md) for troubleshooting
- Review [API Examples](API_EXAMPLES.md) for usage patterns
- Consult [Architecture Guide](ARCHITECTURE.md) for design decisions

---

**Built with Spring Boot 3.4.1, Kotlin 2.1.0, and Java 21**