"""
Weather Tool - Get Current Weather Conditions

This module implements the 'get_weather' MCP tool.

What is an MCP Tool?
--------------------
An MCP Tool is a function that an LLM (like Claude) can invoke when it needs to
perform an action or retrieve data. Tools are the core of the Model Context Protocol.

When a user asks Claude something like "What's the weather in Madrid?", Claude:
1. Recognizes this requires weather data
2. Invokes the get_weather tool with region="Madrid"
3. Waits for the tool to return data
4. Uses the data to answer the user

This is different from Claude just generating text - the tool actually fetches
real, current data from the Weather-Music API.

Data Flow:
----------
User → Claude → MCP Tool → HTTP Client → Weather-Music API → OpenWeather API
                    ↑                                              ↓
                    ←────────────────────────────────────────────←

Example Queries that Trigger this Tool:
----------------------------------------
- "What's the weather in Madrid?"
- "Tell me the temperature in Barcelona"
- "Is it raining in London?"
- "What are the current conditions in 40.4168,-3.7038?"
"""

import logging
from typing import Dict, Any

from ..client import get_client, WeatherMusicAPIError

# Get logger for this module
logger = logging.getLogger(__name__)


async def get_weather(region: str) -> Dict[str, Any]:
    """
    Get current weather conditions for a region.

    This is the main MCP tool function that Claude will invoke when users ask
    about weather. It's a simple wrapper around the Weather-Music API endpoint.

    How it Works:
    -------------
    1. Validate the region parameter
    2. Get the HTTP client (with retry logic)
    3. Call GET /api/v1/regions/{region}/weather
    4. Return the JSON response to Claude
    5. Claude uses the data to answer the user

    Args:
        region: Region identifier. Can be:
            - City name: "Madrid", "London", "New York"
            - City with country: "Madrid,ES", "London,GB"
            - Coordinates: "40.4168,-3.7038" (latitude,longitude)

    Returns:
        dict: Weather data from the API containing:
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

    Raises:
        WeatherMusicAPIError: If the API request fails
        ValueError: If region parameter is invalid

    Example Usage from Claude:
    ---------------------------
    User: "What's the weather in Madrid?"

    Claude thinks: "I need weather data for Madrid"
    Claude invokes: get_weather(region="Madrid")
    Tool returns: {"region": "Madrid", "weather": {...}}
    Claude responds: "The weather in Madrid is currently light rain with
                      a temperature of 15.5°C and 85% humidity."

    Example Usage in Code:
    -----------------------
    >>> data = await get_weather("Madrid")
    >>> print(data["weather"]["temperature"])
    15.5
    >>> print(data["weather"]["condition"])
    Rain
    """
    # Input validation
    # ----------------
    # Check that region parameter is not empty
    # This prevents wasting API calls with invalid input
    if not region or not region.strip():
        logger.warning("get_weather called with empty region")
        raise ValueError("Region parameter cannot be empty")

    region = region.strip()

    # Log the tool invocation
    # This helps with debugging and monitoring
    logger.info(f"get_weather tool invoked with region='{region}'")

    try:
        # Get the global HTTP client
        # The client has retry logic built-in (see client.py)
        client = await get_client()

        # Make the API request
        # Endpoint: GET /api/v1/regions/{region}/weather
        # This maps 1:1 to the Weather-Music API endpoint
        endpoint = f"/api/v1/regions/{region}/weather"

        # Call the API (with automatic retries on transient failures)
        data = await client.get(endpoint)

        # Log success
        logger.info(f"get_weather successful for region='{region}', condition={data.get('weather', {}).get('condition')}")

        # Return the data to Claude
        # MCP handles JSON serialization automatically
        return data

    except WeatherMusicAPIError as e:
        # API error (network, HTTP error, timeout, etc.)
        # Log the error and re-raise so Claude knows the tool failed
        logger.error(f"get_weather failed for region='{region}': {e}")

        # Re-raise the error
        # Claude will see this error and can inform the user
        raise

    except Exception as e:
        # Unexpected error
        logger.error(f"Unexpected error in get_weather for region='{region}': {e}", exc_info=True)
        raise


# Tool Metadata for MCP
# ----------------------
# This metadata helps Claude understand when and how to use this tool
# It will be used when registering the tool with the MCP server

TOOL_NAME = "get_weather"
TOOL_DESCRIPTION = """
Get current weather conditions for a region.

Use this tool when the user asks about:
- Current weather conditions
- Temperature
- Humidity or wind speed
- Weather descriptions (sunny, rainy, cloudy, etc.)

This tool provides real-time weather data from the OpenWeather API.

Note: This only returns CURRENT weather. It does NOT provide forecasts.
For music recommendations based on weather, use get_mood_music or get_weather_context instead.
"""

# Input Schema (JSON Schema format)
# This tells Claude what parameters the tool expects
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
        }
    },
    "required": ["region"],
}
