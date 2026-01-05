"""
Weather Context Tool - Aggregated Weather + Mood + Music Data

This module implements the 'get_weather_context' MCP tool.

Purpose:
--------
This is the MOST CONVENIENT tool for getting complete weather-music context.
Instead of calling get_weather + get_mood_music separately, this tool returns
everything in a single API call.

Why This Tool Exists:
---------------------
1. Performance: One API call instead of two
2. Convenience: Claude gets all data at once
3. Consistency: Weather and music data are guaranteed to be from the same moment

Use Cases:
----------
- User wants weather AND music recommendations
- Building a dashboard that shows complete context
- LLM needs comprehensive information in one query

Trade-offs:
-----------
+ Faster: Single API call
+ Simpler: No need to coordinate multiple tool calls
- Larger response: More data to process
- Less flexible: Can't get weather without music

Example Queries that Trigger this Tool:
----------------------------------------
- "What's the weather in Madrid and what should I listen to?"
- "Tell me about the weather and music for Barcelona"
- "Give me the full context for London today"
- "Weather and music recommendations for Paris"
"""

import logging
from typing import Dict, Any

from ..client import get_client, WeatherMusicAPIError

logger = logging.getLogger(__name__)


async def get_weather_context(
    region: str,
    limit: int = 10,
) -> Dict[str, Any]:
    """
    Get aggregated weather context with music recommendations.

    This is the "all-in-one" tool that returns weather data, mood profile,
    and music recommendations in a single response.

    Data Returned:
    --------------
    1. Weather data (current conditions, temperature, humidity, etc.)
    2. Mood profile (derived from weather: name, valence, energy ranges)
    3. Music recommendations (Spotify playlists/tracks matching the mood)

    The data is aggregated on the server side, ensuring consistency
    (weather and music are based on the same moment in time).

    Args:
        region: Region identifier (city name, city with country, or coordinates)
        limit: Maximum number of music recommendations to return (1-50, default 10)

    Returns:
        dict: Complete weather-music context:
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
                        "url": "https://open.spotify.com/playlist/...",
                        "imageUrl": "https://i.scdn.co/image/..."
                    },
                    ...
                ],
                "total": 10
            }

    Raises:
        WeatherMusicAPIError: If the API request fails
        ValueError: If parameters are invalid

    Example Usage from Claude:
    ---------------------------
    User: "What's the weather in Madrid and what should I listen to?"

    Claude thinks: "User wants complete weather-music context"
    Claude invokes: get_weather_context(region="Madrid", limit=5)
    Tool returns: {
        "weather": {"condition": "Rain", "temperature": 15.5},
        "mood": {"name": "Melancholic"},
        "recommendations": [...]
    }
    Claude responds: "In Madrid, it's currently light rain at 15.5°C with 85%
                      humidity. The rainy weather creates a melancholic mood,
                      so I recommend these playlists:
                      1. Rainy Day - Cozy rainy day vibes
                      2. Chill Lofi Study Beats - Perfect for this weather
                      ..."

    Comparison with Other Tools:
    -----------------------------
    vs. get_weather:
        - get_weather: Only weather data
        - get_weather_context: Weather + mood + music (more complete)

    vs. get_mood_music:
        - get_mood_music: Music recommendations (includes mood but not weather details)
        - get_weather_context: Everything in one call

    Use get_weather_context when:
        - User asks for both weather and music
        - You need complete context in a single call
        - Performance is important (one call vs. two)

    Use get_weather when:
        - User only asks about weather
        - You don't need music recommendations

    Use get_mood_music when:
        - User only asks about music
        - You want to specify type (playlist/track) filter

    Example Usage in Code:
    -----------------------
    >>> data = await get_weather_context("Barcelona", limit=3)
    >>> print(f"Weather: {data['weather']['condition']}")
    Clear
    >>> print(f"Mood: {data['mood']['name']}")
    Happy
    >>> print(f"Recommendations: {data['total']}")
    3
    """
    # Input validation
    # ----------------
    if not region or not region.strip():
        logger.warning("get_weather_context called with empty region")
        raise ValueError("Region parameter cannot be empty")

    region = region.strip()

    # Validate limit parameter
    if limit < 1 or limit > 50:
        logger.warning(f"get_weather_context called with invalid limit={limit}")
        raise ValueError("Limit must be between 1 and 50")

    # Log the tool invocation
    logger.info(f"get_weather_context tool invoked: region='{region}', limit={limit}")

    try:
        # Get the HTTP client
        client = await get_client()

        # Build the API endpoint
        # Endpoint: GET /api/v1/regions/{region}/weather-context?limit={limit}
        endpoint = f"/api/v1/regions/{region}/weather-context"

        # Build query parameters
        params = {"limit": limit}

        # Call the API (with automatic retries)
        # This single call fetches weather, determines mood, and gets music recommendations
        data = await client.get(endpoint, params=params)

        # Log success with comprehensive information
        weather_condition = data.get("weather", {}).get("condition", "Unknown")
        mood_name = data.get("mood", {}).get("name", "Unknown")
        rec_count = data.get("total", 0)
        logger.info(
            f"get_weather_context successful: region='{region}', "
            f"weather={weather_condition}, mood={mood_name}, "
            f"recommendations={rec_count}"
        )

        # Return the complete context to Claude
        return data

    except WeatherMusicAPIError as e:
        # API error
        logger.error(f"get_weather_context failed for region='{region}': {e}")
        raise

    except Exception as e:
        # Unexpected error
        logger.error(
            f"Unexpected error in get_weather_context for region='{region}': {e}",
            exc_info=True
        )
        raise


# Tool Metadata for MCP
# ----------------------

TOOL_NAME = "get_weather_context"
TOOL_DESCRIPTION = """
Get complete weather context with music recommendations in a single call.

This is the MOST CONVENIENT tool for getting weather-music information.
It combines:
1. Current weather data (temperature, conditions, humidity, etc.)
2. Mood profile (derived from weather with valence/energy ranges)
3. Music recommendations (Spotify playlists/tracks matching the mood)

Use this tool when the user asks for:
- Weather AND music recommendations together
- Complete context about a region's weather and music
- Dashboard-style information (all data in one response)

Benefits:
- Single API call (faster than get_weather + get_mood_music)
- Guaranteed consistency (weather and music from same moment)
- Most complete information

When to use other tools instead:
- Use get_weather if user ONLY asks about weather
- Use get_mood_music if user ONLY asks about music recommendations
- Use check_api_health to verify API availability

Performance:
- Slightly slower than individual tools (more data to fetch)
- But faster than calling get_weather + get_mood_music separately
- Response is cached for 30 minutes on the API side
"""

INPUT_SCHEMA = {
    "type": "object",
    "properties": {
        "region": {
            "type": "string",
            "description": (
                "Region identifier. Can be a city name (e.g., 'Madrid'), "
                "city with country code (e.g., 'Madrid,ES'), "
                "or coordinates (e.g., '40.4168,-3.7038')"
            ),
        },
        "limit": {
            "type": "integer",
            "description": "Maximum number of music recommendations to return (1-50, default 10)",
            "default": 10,
            "minimum": 1,
            "maximum": 50,
        },
    },
    "required": ["region"],
}
