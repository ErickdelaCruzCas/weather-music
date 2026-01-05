# HTTP Client Guide

Este proyecto incluye un archivo `api-requests.http` para probar la API de forma interactiva.

## Cómo Usar

### IntelliJ IDEA (Built-in)

1. **Abrir el archivo:**
   - Abre `api-requests.http` en IntelliJ IDEA
   - IntelliJ detectará automáticamente el formato

2. **Ejecutar requests:**
   - Haz clic en el botón verde "▶" junto a cada request
   - O usa el atajo: `Ctrl+Enter` (Windows/Linux) o `Cmd+Enter` (Mac)

3. **Ver respuestas:**
   - Las respuestas aparecen en el panel "Run"
   - Puedes ver headers, body, y tiempo de respuesta

4. **Cambiar entorno:**
   - En la parte superior del editor, selecciona el entorno (development/production)
   - El archivo `http-client.env.json` define las variables por entorno

### VS Code (REST Client Extension)

1. **Instalar extensión:**
   ```
   ext install humao.rest-client
   ```

2. **Abrir el archivo:**
   - Abre `api-requests.http` en VS Code

3. **Ejecutar requests:**
   - Haz clic en "Send Request" sobre cada request
   - O usa el atajo: `Ctrl+Alt+R` (Windows/Linux) o `Cmd+Alt+R` (Mac)

4. **Ver respuestas:**
   - Se abre una nueva pestaña con la respuesta completa

## Estructura del Archivo

El archivo `api-requests.http` está organizado por secciones:

### 1. Health Endpoints
```http
GET {{baseUrl}}/api/{{apiVersion}}/health/external
```

Verifica el estado de los servicios externos (Spotify, OpenWeather).

### 2. Weather Endpoints
```http
GET {{baseUrl}}/api/{{apiVersion}}/regions/Madrid/weather
GET {{baseUrl}}/api/{{apiVersion}}/regions/40.4168,-3.7038/weather
```

Obtiene el clima actual para una región (nombre de ciudad o coordenadas).

### 3. Mood Music Endpoints
```http
GET {{baseUrl}}/api/{{apiVersion}}/regions/Madrid/mood-music?limit=10&type=both
```

Obtiene recomendaciones de música basadas en el mood del clima.

**Parámetros:**
- `limit` (opcional, default=10): Número de recomendaciones (1-50)
- `type` (opcional, default=both): Tipo de contenido (playlist, track, both)

### 4. Weather Context Endpoints (Agregado)
```http
GET {{baseUrl}}/api/{{apiVersion}}/regions/Madrid/weather-context?limit=10
```

Obtiene clima + mood + música en una sola llamada (optimizado).

## Antes de Empezar

### 1. Configurar Variables de Entorno

Asegúrate de tener las API keys configuradas:

```bash
export SPOTIFY_CLIENT_ID="your_spotify_client_id"
export SPOTIFY_CLIENT_SECRET="your_spotify_client_secret"
export OPENWEATHER_API_KEY="your_openweather_api_key"
```

### 2. Iniciar la Aplicación

```bash
./gradlew bootRun
```

La aplicación se inicia en `http://localhost:8080`

### 3. Verificar que está corriendo

```http
GET http://localhost:8080/api/v1/health/external
```

Debería responder con el estado de los servicios externos.

## Ejemplos de Uso

### Workflow Completo

1. **Check health:**
   ```http
   GET {{baseUrl}}/api/v1/health/external
   ```

2. **Get weather:**
   ```http
   GET {{baseUrl}}/api/v1/regions/Madrid/weather
   ```

3. **Get mood music:**
   ```http
   GET {{baseUrl}}/api/v1/regions/Madrid/mood-music?limit=10
   ```

4. **Get everything:**
   ```http
   GET {{baseUrl}}/api/v1/regions/Madrid/weather-context?limit=10
   ```

### Probar Diferentes Moods

El archivo incluye secciones con ciudades agrupadas por condición climática típica:

**Rainy Cities → Melancholic Mood:**
- Seattle, London, Dublin
- Expected: valence 0.0-0.4, energy 0.3-0.6
- Keywords: rainy day, chill, melancholy, lo-fi

**Sunny Cities → Happy Mood:**
- Barcelona, Miami, Los Angeles
- Expected: valence 0.6-1.0, energy 0.5-0.8
- Keywords: happy, sunny, upbeat, summer vibes

**Cloudy Cities → Calm Mood:**
- Berlin, Paris, Amsterdam
- Expected: valence 0.3-0.6, energy 0.2-0.5
- Keywords: calm, peaceful, relaxing, ambient

**Snowy Cities → Peaceful Mood:**
- Oslo, Reykjavik, Helsinki
- Expected: valence 0.4-0.7, energy 0.1-0.4
- Keywords: peaceful, winter, serene, meditation

### Usar Coordenadas

En lugar de nombres de ciudades, puedes usar coordenadas (lat,lon):

```http
GET {{baseUrl}}/api/v1/regions/40.4168,-3.7038/weather-context
```

**Coordenadas de referencia:**
- Madrid: `40.4168,-3.7038`
- London: `51.5074,-0.1278`
- New York: `40.7128,-74.0060`
- Tokyo: `35.6762,139.6503`
- Sydney: `-33.8688,151.2093`

## Testing de Errores

El archivo incluye requests para probar manejo de errores:

### 404 - Region Not Found
```http
GET {{baseUrl}}/api/v1/regions/InvalidCity123/weather
```

### 400 - Invalid Type Parameter
```http
GET {{baseUrl}}/api/v1/regions/Madrid/mood-music?type=invalid
```

## Variables de Entorno

El archivo `http-client.env.json` define variables por entorno:

```json
{
  "development": {
    "baseUrl": "http://localhost:8080",
    "apiVersion": "v1"
  },
  "production": {
    "baseUrl": "https://api.weather-music.example.com",
    "apiVersion": "v1"
  }
}
```

Para cambiar de entorno en IntelliJ IDEA:
- Haz clic en el selector de entorno en la parte superior del editor
- Selecciona "development" o "production"

## Tips y Trucos

### 1. Ejecutar Multiple Requests
- Puedes seleccionar múltiples requests y ejecutarlos en secuencia
- Útil para workflows completos

### 2. Ver Historia de Requests
- IntelliJ IDEA guarda un historial de todas las requests
- Accede desde: Tools > HTTP Client > Show HTTP Requests History

### 3. Export Results
- Puedes exportar las respuestas a archivos
- Útil para comparar resultados entre ejecuciones

### 4. Variables Dinámicas
- Puedes usar variables dinámicas: `{{$timestamp}}`, `{{$randomInt}}`
- Ejemplo: `?cache_bust={{$timestamp}}`

### 5. Response Handler Scripts
- IntelliJ IDEA soporta scripts para procesar respuestas
- Ejemplo: extraer un campo y usarlo en el siguiente request

## Troubleshooting

### Connection Refused
- Verifica que la aplicación esté corriendo: `./gradlew bootRun`
- Check el puerto: por defecto es 8080

### 401 Unauthorized (si implementas auth en el futuro)
- Verifica que las credenciales estén configuradas
- Check que el token no haya expirado

### 503 Service Unavailable
- Los servicios externos (Spotify/OpenWeather) pueden estar caídos
- Verifica con: `GET /api/v1/health/external`
- Check las API keys en las variables de entorno

### Circuit Breaker Open
- Si ves este error, el circuit breaker está abierto
- Espera 60 segundos para que se resetee
- Revisa los logs para ver qué servicio está fallando

## Recursos Adicionales

- **OpenAPI Spec:** Ver `openapi.yml` para detalles de schemas
- **API Examples:** Ver `API_EXAMPLES.md` para más ejemplos con curl
- **Testing Guide:** Ver `TESTING.md` para testing completo
- **Swagger UI:** `http://localhost:8080/swagger-ui.html` (cuando la app está corriendo)

## Feedback

Para reportar problemas o sugerir mejoras:
- Abre un issue en el repositorio
- O contacta al equipo de desarrollo
