# Weather-Music MCP Server

**Tu primer servidor MCP (Model Context Protocol)** 🎵🌤️

Este servidor MCP expone la API Weather-Music a Claude, permitiendo que el LLM obtenga datos de clima en tiempo real y recomendaciones musicales basadas en el "mood" que crea el clima.

---

## 📚 Tabla de Contenidos

1. [¿Qué es MCP?](#qué-es-mcp)
2. [Arquitectura de este Servidor](#arquitectura-de-este-servidor)
3. [Herramientas Disponibles](#herramientas-disponibles)
4. [Instalación](#instalación)
5. [Configuración](#configuración)
6. [Ejecución](#ejecución)
7. [Integración con Claude Desktop](#integración-con-claude-desktop)
8. [Ejemplos de Uso](#ejemplos-de-uso)
9. [Troubleshooting](#troubleshooting)
10. [Desarrollo](#desarrollo)

---

## ¿Qué es MCP?

### Model Context Protocol - Explicado desde Cero

**MCP (Model Context Protocol)** es un protocolo que permite que los LLMs (como Claude) interactúen con herramientas y fuentes de datos externas.

#### Analogía 📱

Imagina que Claude es como tu teléfono móvil:
- El teléfono puede hacer muchas cosas por sí solo (calculadora, calendario)
- Pero para funcionalidades avanzadas necesita **apps** (WhatsApp, Spotify, etc.)
- Esas apps le dan acceso a datos externos (mensajes, música)

**MCP es como el App Store para Claude**:
- Claude puede "instalar" servidores MCP (como apps)
- Cada servidor MCP proporciona **tools** (herramientas)
- Claude puede invocar esas tools para obtener datos o realizar acciones

#### Diferencia Clave: Con vs Sin MCP

**Sin MCP:**
```
Usuario: "¿Qué clima hace en Madrid?"
Claude: "Lo siento, no tengo acceso a datos de clima en tiempo real.
         Mi conocimiento fue cortado en enero de 2025."
```

**Con MCP (este servidor):**
```
Usuario: "¿Qué clima hace en Madrid?"
Claude: [invoca tool get_weather]
        → Servidor MCP → API Weather-Music → OpenWeather
        ← Datos reales ←
Claude: "En Madrid hace 15.5°C con lluvia ligera y 85% de humedad."
```

### Componentes del Ecosistema MCP

```
┌──────────────┐
│   Usuario    │ ← Hace preguntas
└──────┬───────┘
       │
       ↓
┌──────────────┐
│    Claude    │ ← LLM que decide qué tools usar
│  (MCP Client)│
└──────┬───────┘
       │
       ↓ Invoca tools via MCP Protocol
┌──────────────┐
│ MCP Server   │ ← Este programa (weather-music-mcp)
│  (Este repo) │
└──────┬───────┘
       │
       ↓ HTTP requests
┌──────────────┐
│Weather-Music │ ← Tu API Spring Boot
│     API      │
└──────┬───────┘
       │
       ↓
┌──────────────┐
│Spotify + OW  │ ← APIs externas
└──────────────┘
```

### Tools, Prompts y Resources

MCP define 3 tipos de primitivas:

1. **Tools** 🔧
   - Funciones que Claude puede invocar
   - Ejemplo: `get_weather(region="Madrid")`
   - Retornan datos en tiempo real

2. **Prompts** 💬
   - Plantillas predefinidas para queries comunes
   - Ejemplo: "Recomiéndame música para el clima de {city}"
   - Facilitan la interacción del usuario

3. **Resources** 📁
   - Datos o contexto que el LLM puede consultar
   - Ejemplo: Lista de moods disponibles, regiones soportadas
   - Información estática o metadatos

**Este servidor implementa:**
- ✅ 4 Tools (PHASE MCP-2)
- ⏳ Prompts (PHASE MCP-6 - pendiente)
- ⏳ Resources (PHASE MCP-6 - pendiente)

---

## Arquitectura de este Servidor

### Diagrama de Flujo Completo

```
Usuario en Claude Desktop
    ↓ "¿Qué música debería escuchar en Madrid con este clima?"
    ↓
Claude LLM analiza la query
    ↓ Decide: "Necesito get_weather_context para Madrid"
    ↓
Claude invoca tool via MCP Protocol (stdio)
    ↓
[Este Servidor MCP]
    ↓ Llama a function get_weather_context("Madrid")
    ↓
[HTTP Client con Retry Logic]
    ↓ GET http://localhost:8080/api/v1/regions/Madrid/weather-context
    ↓
[Weather-Music API - Spring Boot]
    ↓ 1. Obtiene clima de OpenWeather
    ↓ 2. Mapea clima → mood (Rain → Melancholic)
    ↓ 3. Busca música en Spotify que coincida con el mood
    ↓ 4. Retorna datos agregados
    ↑
[HTTP Client recibe respuesta]
    ↑ Retry automático si falla (backoff exponencial)
    ↑
[MCP Server retorna datos a Claude]
    ↑
Claude procesa los datos y genera respuesta
    ↑
Usuario recibe: "En Madrid está lloviendo (15.5°C). Te recomiendo estas
                  playlists melancólicas: 1. Rainy Day, 2. Chill Lofi..."
```

### Características Clave

1. **Async/Await**: Todo es asíncrono (no bloqueante)
2. **Retry Logic**: Reintentos automáticos con backoff exponencial (1s, 2s, 4s)
3. **Error Handling**: Errores convertidos a excepciones claras para Claude
4. **Configuración**: Todo configurable via `.env` file
5. **Logging**: Nivel INFO para monitoreo (configurable a DEBUG)
6. **Type Safety**: Type hints en todo el código
7. **Comentarios Educativos**: Explicaciones inline en cada archivo

---

## Herramientas Disponibles

Este servidor expone 4 MCP tools que Claude puede invocar:

### 1. `get_weather` 🌤️

Obtiene condiciones climáticas actuales para una región.

**Cuándo usarla:**
- Usuario pregunta por el clima
- Solo se necesita información meteorológica

**Parámetros:**
- `region` (string, requerido): Ciudad, "Ciudad,ES" o "lat,lon"

**Ejemplo de query que activa esta tool:**
```
"¿Qué temperatura hace en Madrid?"
"Dime el clima de Barcelona"
"¿Está lloviendo en London?"
```

**Respuesta:**
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

### 2. `get_mood_music` 🎵

Recomienda música basada en el "mood" creado por el clima actual.

**Cuándo usarla:**
- Usuario pide recomendaciones musicales basadas en clima
- Quiere playlists o tracks específicos

**Parámetros:**
- `region` (string, requerido): Ciudad o coordenadas
- `limit` (int, opcional): Número de recomendaciones (1-50, default 10)
- `type` (string, opcional): "playlist", "track", o "both" (default)

**Cómo funciona:**
1. Detecta clima actual
2. Mapea clima → mood:
   - Rain → Melancholic (valence: 0.0-0.4, energy: 0.3-0.6)
   - Sunny → Happy (valence: 0.6-1.0, energy: 0.5-0.8)
   - Cloudy → Calm (valence: 0.3-0.6, energy: 0.2-0.5)
3. Busca música en Spotify que coincida con el mood
4. Retorna top N recomendaciones

**Ejemplo de query:**
```
"Recomiéndame música para el clima de Madrid"
"¿Qué debería escuchar si está lloviendo en Paris?"
"Dame playlists felices para el clima de Barcelona"
```

**Respuesta:**
```json
{
  "region": "Madrid",
  "mood": {
    "name": "Melancholic",
    "description": "Introspective and contemplative music",
    "valence": {"min": 0.0, "max": 0.4},
    "energy": {"min": 0.3, "max": 0.6}
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
      "url": "https://open.spotify.com/playlist/37i9dQZF1DX0SM0LYsmbMT"
    }
  ],
  "total": 10
}
```

### 3. `get_weather_context` 🌤️🎵

**La herramienta más conveniente**: Combina clima + mood + música en una sola respuesta.

**Cuándo usarla:**
- Usuario quiere clima Y música juntos
- Necesitas contexto completo de una vez

**Parámetros:**
- `region` (string, requerido): Ciudad o coordenadas
- `limit` (int, opcional): Número de recomendaciones musicales (1-50, default 10)

**Ejemplo de query:**
```
"¿Qué clima hace en Madrid y qué debería escuchar?"
"Dame el contexto completo para Barcelona"
"Clima y música para Londres"
```

**Ventajas vs llamar get_weather + get_mood_music:**
- ✅ Una sola llamada API (más rápido)
- ✅ Datos garantizados del mismo momento
- ✅ Menos latencia
- ⚠️ Respuesta más grande (más datos para procesar)

### 4. `check_api_health` 🏥

Verifica el estado de las APIs externas (Spotify y OpenWeather).

**Cuándo usarla:**
- Usuario pregunta si los servicios están funcionando
- Debugging (otras tools fallan)
- Monitoreo

**Parámetros:** Ninguno

**Ejemplo de query:**
```
"¿Están funcionando las APIs?"
"Verifica si Spotify está disponible"
"¿Por qué no puedo obtener clima?"
```

**Respuesta:**
```json
{
  "status": "healthy",  // o "degraded" o "unhealthy"
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

**Interpretación:**
- **healthy**: Todos los servicios < 500ms
- **degraded**: Servicios lentos (500-2000ms)
- **unhealthy**: Uno o más servicios caídos

---

## Instalación

### Prerrequisitos

✅ **Python 3.11+** (ya instalado en tu sistema)
✅ **Weather-Music API** corriendo en `localhost:8080`

### Paso 1: Navegar al directorio

```bash
cd mcp-server
```

### Paso 2: Crear entorno virtual

```bash
# Crear entorno virtual con Python 3.11
python3.11 -m venv venv

# Activar el entorno virtual
source venv/bin/activate  # macOS/Linux
# o
venv\Scripts\activate     # Windows
```

Deberías ver `(venv)` en tu prompt.

### Paso 3: Instalar dependencias

```bash
# Instalar dependencias de producción
pip install -r requirements.txt

# (Opcional) Instalar dependencias de desarrollo
pip install -r requirements-dev.txt
```

**Dependencias instaladas:**
- `mcp`: SDK de Anthropic para servidores MCP
- `httpx`: Cliente HTTP asíncrono
- `pydantic`: Validación de datos
- `python-dotenv`: Carga de variables de entorno
- `tenacity`: Retry logic con backoff exponencial
- `cachetools`: Caching en memoria (PHASE MCP-6)

### Paso 4: Instalar el paquete en modo editable

```bash
pip install -e .
```

Esto permite editar el código sin reinstalar.

---

## Configuración

### Paso 1: Crear archivo `.env`

```bash
# Copiar el archivo de ejemplo
cp .env.example .env

# Editar con tu editor favorito
nano .env
# o
code .env
```

### Paso 2: Configurar variables

```bash
# ========================================
# Weather-Music API Configuration
# ========================================

# URL de la API Weather-Music
# Desarrollo: http://localhost:8080
# Producción: https://api.weather-music.example.com
WEATHER_MUSIC_API_URL=http://localhost:8080

# ========================================
# Logging Configuration
# ========================================

# Nivel de logging
# DEBUG: Muy verbose (útil para debugging)
# INFO: Normal (recomendado)
# WARNING: Solo warnings y errores
# ERROR: Solo errores
LOG_LEVEL=INFO

# ========================================
# HTTP Client Configuration
# ========================================

# Timeout para requests HTTP (segundos)
# Cuánto tiempo esperar antes de cancelar
REQUEST_TIMEOUT=30

# Número máximo de reintentos
# Con backoff exponencial: 1s, 2s, 4s
MAX_RETRIES=3

# ========================================
# Cache Configuration (PHASE MCP-6)
# ========================================

# Habilitar caching de respuestas
ENABLE_CACHE=true

# TTL del cache (segundos)
# 1800 = 30 minutos (igual que la API)
CACHE_TTL=1800

# ========================================
# Rate Limiting (PHASE MCP-6)
# ========================================

# Habilitar rate limiting
ENABLE_RATE_LIMITING=true

# Requests máximos por minuto
RATE_LIMIT_PER_MINUTE=100
```

### Variables Explicadas

#### `WEATHER_MUSIC_API_URL`
- **Qué es**: URL base de tu API Spring Boot
- **Valores comunes**:
  - `http://localhost:8080` (desarrollo local)
  - `http://192.168.1.100:8080` (red local)
  - `https://api.example.com` (producción)
- **Importante**: Debe incluir `http://` o `https://`

#### `LOG_LEVEL`
- **Qué es**: Cuánta información se loguea
- **DEBUG**: TODO (requests, responses, retries, etc.)
- **INFO**: Operaciones importantes (recomendado)
- **WARNING**: Solo problemas potenciales
- **ERROR**: Solo errores
- **Recomendación**: INFO en producción, DEBUG para troubleshooting

#### `REQUEST_TIMEOUT`
- **Qué es**: Cuánto esperar una respuesta de la API (segundos)
- **Default**: 30 segundos
- **Demasiado bajo**: Timeouts prematuros
- **Demasiado alto**: Esperas largas si API no responde
- **Recomendación**: 30 segundos es bueno para la mayoría de casos

#### `MAX_RETRIES`
- **Qué es**: Cuántas veces reintentar requests fallidos
- **Default**: 3 reintentos
- **Backoff**: 1s → 2s → 4s entre reintentos
- **0**: Sin reintentos (falla inmediatamente)
- **>5**: Demasiados reintentos (latencia alta)
- **Recomendación**: 3 es un buen balance

---

## Ejecución

### Verificar que la API está corriendo

Antes de ejecutar el servidor MCP, asegúrate que la API Weather-Music esté corriendo:

```bash
# En otra terminal, desde el directorio raíz del proyecto:
cd ..
./gradlew bootRun
```

Espera a ver:
```
Started WeatherApplication in X.XXX seconds
```

### Iniciar el servidor MCP

```bash
# Asegúrate de estar en mcp-server/ con venv activado
python -m weather_music_mcp.server
```

O directamente:
```bash
python src/weather_music_mcp/server.py
```

**Salida esperada:**
```
2026-01-05 18:00:00 - INFO - Configuration validated successfully
2026-01-05 18:00:00 - INFO - Creating MCP server...
2026-01-05 18:00:00 - INFO - Registering MCP tools...
2026-01-05 18:00:00 - INFO - MCP server created successfully
2026-01-05 18:00:00 - INFO - Registered tools: ['get_weather', 'get_mood_music', 'get_weather_context', 'check_api_health']
2026-01-05 18:00:00 - INFO - Starting MCP server...
2026-01-05 18:00:00 - INFO - Server is ready to accept connections
2026-01-05 18:00:00 - INFO - Press Ctrl+C to stop the server
```

El servidor está ahora esperando conexiones de Claude Desktop.

### Detener el servidor

Presiona `Ctrl+C` en la terminal donde corre el servidor:

```
^C
2026-01-05 18:05:00 - INFO - Received shutdown signal (Ctrl+C)
2026-01-05 18:05:00 - INFO - Shutting down server...
2026-01-05 18:05:00 - INFO - HTTP client closed
2026-01-05 18:05:00 - INFO - Server shutdown complete
```

---

## Integración con Claude Desktop

### Paso 1: Configurar Claude Desktop

Edita el archivo de configuración de Claude Desktop:

**macOS:**
```bash
code ~/Library/Application\ Support/Claude/claude_desktop_config.json
```

**Windows:**
```bash
code %APPDATA%\Claude\claude_desktop_config.json
```

### Paso 2: Agregar el servidor MCP

Agrega esta configuración:

```json
{
  "mcpServers": {
    "weather-music": {
      "command": "python",
      "args": [
        "-m",
        "weather_music_mcp.server"
      ],
      "cwd": "/ruta/absoluta/a/weather-music/mcp-server",
      "env": {
        "PYTHONPATH": "/ruta/absoluta/a/weather-music/mcp-server/src",
        "PATH": "/ruta/absoluta/a/weather-music/mcp-server/venv/bin:/usr/bin:/bin"
      }
    }
  }
}
```

**⚠️ Importante**: Reemplaza `/ruta/absoluta/a/weather-music/mcp-server` con la ruta real.

**Para obtener la ruta absoluta:**
```bash
cd mcp-server
pwd
# Copia el output
```

### Paso 3: Reiniciar Claude Desktop

1. Cierra Claude Desktop completamente
2. Abre Claude Desktop nuevamente
3. El servidor MCP debería conectarse automáticamente

### Paso 4: Verificar conexión

En Claude Desktop, deberías ver un ícono de herramientas (🔧) indicando que hay servidores MCP conectados.

Puedes probar con:
```
"Are the weather APIs working?"
```

Claude debería invocar `check_api_health` y responderte con el estado de las APIs.

---

## Ejemplos de Uso

Una vez integrado con Claude Desktop, puedes hacer queries en lenguaje natural:

### Query 1: Solo Clima

```
Usuario: "What's the weather in Madrid?"
```

**Claude invoca:**
```python
get_weather(region="Madrid")
```

**Claude responde:**
```
The current weather in Madrid is light rain with a temperature of 15.5°C.
The humidity is 85% with winds at 12.5 km/h.
```

### Query 2: Solo Música

```
Usuario: "Recommend me music for rainy weather in Paris"
```

**Claude invoca:**
```python
get_mood_music(region="Paris", limit=5, type="playlist")
```

**Claude responde:**
```
Based on the rainy weather in Paris, I recommend these melancholic playlists:

1. Rainy Day - Cozy rainy day vibes (50 tracks)
   https://open.spotify.com/playlist/37i9dQZF1DX0SM0LYsmbMT

2. Chill Lofi Study Beats - Perfect for this weather (175 tracks)
   https://open.spotify.com/playlist/37i9dQZF1DWZd79rJ6a7lp

...
```

### Query 3: Contexto Completo

```
Usuario: "I'm in Barcelona. What's the weather and what should I listen to?"
```

**Claude invoca:**
```python
get_weather_context(region="Barcelona", limit=5)
```

**Claude responde:**
```
In Barcelona, it's currently clear with a temperature of 24°C and 55% humidity.

The sunny weather creates a happy, energetic mood. Here are my music recommendations:

1. Sunny Day Vibes - Upbeat and cheerful (80 tracks)
2. Feel Good Indie - Perfect for this weather
...
```

### Query 4: Health Check

```
Usuario: "Are the external APIs working?"
```

**Claude invoca:**
```python
check_api_health()
```

**Claude responde:**
```
Yes, all external APIs are working normally:
- Spotify: Responding in 145ms
- OpenWeather: Responding in 89ms

Overall status: healthy
```

### Tips para Mejores Queries

✅ **Sé específico con las ciudades:**
- ✅ "Madrid" o "Madrid,ES"
- ❌ "la capital de España"

✅ **Indica qué tipo de recomendación quieres:**
- ✅ "playlists for sunny weather"
- ✅ "individual tracks for rainy day"

✅ **Usa lenguaje natural:**
- ✅ "What should I listen to in London right now?"
- ❌ "Execute get_mood_music tool"

---

## Troubleshooting

### Problema: "Module not found: weather_music_mcp"

**Causa**: El paquete no está instalado en el entorno virtual.

**Solución**:
```bash
cd mcp-server
source venv/bin/activate
pip install -e .
```

### Problema: "Connection refused" o "API request failed"

**Causa**: La API Weather-Music no está corriendo.

**Solución**:
```bash
# En otra terminal:
cd ..
./gradlew bootRun
```

Espera a que la API inicie completamente antes de usar el MCP server.

### Problema: "Configuration validation failed: WEATHER_MUSIC_API_URL cannot be empty"

**Causa**: El archivo `.env` no existe o está mal configurado.

**Solución**:
```bash
cp .env.example .env
nano .env  # Editar y guardar
```

### Problema: El servidor MCP no aparece en Claude Desktop

**Causa**: Configuración incorrecta en `claude_desktop_config.json`.

**Solución**:
1. Verifica que la ruta sea absoluta (no relativa)
2. Verifica que `PATH` incluya el directorio `venv/bin`
3. Reinicia Claude Desktop completamente

**Verificar rutas:**
```bash
cd mcp-server
pwd  # Esta es tu ruta absoluta
which python  # Debería mostrar venv/bin/python
```

### Problema: "Request timeout after 30s"

**Causa**: La API tarda demasiado o no responde.

**Solución**:
1. Verifica que la API esté corriendo
2. Verifica que la URL en `.env` sea correcta
3. Aumenta `REQUEST_TIMEOUT` en `.env` si la API es lenta

### Problema: Ver más detalles de lo que está pasando

**Solución**: Cambiar log level a DEBUG

```bash
# En .env
LOG_LEVEL=DEBUG
```

Reinicia el servidor. Ahora verás:
- Cada request HTTP
- Cada respuesta de la API
- Reintentos y errores detallados

### Ver logs en tiempo real

```bash
python -m weather_music_mcp.server 2>&1 | tee mcp-server.log
```

Esto guarda los logs en `mcp-server.log` mientras los muestra en pantalla.

---

## Desarrollo

### Estructura del Proyecto

```
mcp-server/
├── src/
│   └── weather_music_mcp/
│       ├── __init__.py           # Package metadata
│       ├── server.py             # MCP server principal
│       ├── config.py             # Configuración (.env)
│       ├── client.py             # HTTP client con retry
│       └── tools/
│           ├── __init__.py
│           ├── weather.py        # Tool: get_weather
│           ├── music.py          # Tool: get_mood_music
│           ├── context.py        # Tool: get_weather_context
│           └── health.py         # Tool: check_api_health
├── tests/                        # Tests (pytest)
├── requirements.txt              # Dependencias producción
├── requirements-dev.txt          # Dependencias desarrollo
├── pyproject.toml                # Configuración del paquete
├── .env.example                  # Ejemplo de configuración
└── README.md                     # Este archivo
```

### Ejecutar Tests

```bash
# Todos los tests
pytest

# Tests con coverage
pytest --cov=weather_music_mcp

# Tests verbose
pytest -v
```

### Code Quality

```bash
# Format code
black .

# Lint code
ruff check .

# Type checking
mypy src/
```

### Agregar una Nueva Tool

Ver `DEVELOPMENT.md` para guía detallada de cómo extender el servidor.

---

## Próximas Fases

### PHASE MCP-6: Features Avanzadas (Pendiente)

Funcionalidades planificadas:

1. **Caching** 💾
   - Cache de respuestas en memoria (TTL 30min)
   - Reduce llamadas a la API
   - Mejora latencia

2. **Rate Limiting** ⏱️
   - Límite de 100 requests/min (configurable)
   - Previene abuso
   - Protege la API

3. **Prompts MCP** 💬
   - Prompts predefinidos para queries comunes
   - Ejemplo: "Música para el clima de {ciudad}"
   - Facilita uso para usuarios

4. **Resources MCP** 📁
   - Lista de moods disponibles
   - Regiones soportadas de ejemplo
   - Estado de API en formato resource

---

## Recursos Adicionales

### Documentación MCP

- [MCP Documentation](https://modelcontextprotocol.io/)
- [Anthropic MCP SDK](https://github.com/anthropics/anthropic-sdk-python)
- [Claude Desktop MCP Guide](https://docs.anthropic.com/en/docs/model-context-protocol)

### Documentación del Proyecto

- `DEVELOPMENT.md`: Guía para desarrolladores
- `ARCHITECTURE.md`: Arquitectura de la API Weather-Music
- `openapi.yml`: Contrato de la API
- `CLAUDE.md`: Instrucciones para Claude Code

### Contacto y Soporte

Si encuentras problemas:
1. Revisa la sección Troubleshooting
2. Verifica los logs (`LOG_LEVEL=DEBUG`)
3. Consulta la documentación de MCP

---

## Licencia

MIT License - Ver `LICENSE` file

---

**¡Felicidades! 🎉** Ahora tienes un servidor MCP completamente funcional que permite a Claude acceder a datos de clima y música en tiempo real. Este es tu primer paso en el mundo de MCP - desde aquí puedes crear servidores para cualquier API que quieras exponer a LLMs.
