"""
Health Check Tool - Monitor External API Status

This module implements the 'check_api_health' MCP tool.

Purpose:
--------
This tool checks the health and availability of external API dependencies
(Spotify and OpenWeather) used by the Weather-Music API.

Why Health Checks Matter:
--------------------------
External APIs can go down or become slow. Before making requests that might
fail, it's useful to check if the APIs are working. This tool helps with:

1. Debugging: "Are the APIs down or is it my code?"
2. Monitoring: Track API response times
3. User Experience: Inform users if services are unavailable

Health Statuses:
----------------
- healthy: All services responding normally (< 500ms response time)
- degraded: Services responding slowly (500ms - 2000ms)
- unhealthy: One or more services unavailable

What This Tool Does NOT Do:
----------------------------
- Does NOT check authentication credentials
- Does NOT verify API rate limits or quotas
- Does NOT test actual functionality (only connectivity)
- Does NOT guarantee subsequent requests will work

Example Queries that Trigger this Tool:
----------------------------------------
- "Are the external APIs working?"
- "Check if Spotify and OpenWeather are available"
- "Is the weather service down?"
- "Why isn't the music recommendation working?"
"""

import logging
from typing import Dict, Any

from ..client import get_client, WeatherMusicAPIError

logger = logging.getLogger(__name__)


async def check_api_health() -> Dict[str, Any]:
    """
    Check health status of external API dependencies.

    This tool pings the Weather-Music API's health endpoint, which in turn
    checks the availability of Spotify and OpenWeather APIs.

    How it Works:
    -------------
    1. Claude invokes this tool (no parameters needed)
    2. Tool calls GET /api/v1/health/external
    3. API pings Spotify and OpenWeather
    4. API measures response times
    5. API determines overall health status
    6. Tool returns status to Claude
    7. Claude informs the user

    Returns:
        dict: Health status information:
            {
                "status": "healthy",  # or "degraded" or "unhealthy"
                "timestamp": "2026-01-05T12:00:00Z",
                "services": {
                    "spotify": {
                        "status": "up",  # or "down"
                        "responseTime": 145  # milliseconds, null if down
                    },
                    "openweather": {
                        "status": "up",
                        "responseTime": 89
                    }
                }
            }

    Raises:
        WeatherMusicAPIError: If the health check request itself fails
                              (different from services being down)

    Status Interpretation:
    ----------------------
    Overall Status:
        - healthy: All services up and responding quickly (< 500ms)
        - degraded: All services up but some are slow (500ms - 2000ms)
        - unhealthy: One or more services are down

    Service Status:
        - up: Service is responding
        - down: Service is not responding or timed out

    Response Time:
        - < 200ms: Excellent
        - 200-500ms: Good
        - 500-2000ms: Slow (degraded)
        - > 2000ms or null: Down

    Example Usage from Claude:
    ---------------------------
    User: "Are the APIs working?"

    Claude thinks: "User wants to check external API health"
    Claude invokes: check_api_health()
    Tool returns: {
        "status": "healthy",
        "services": {
            "spotify": {"status": "up", "responseTime": 145},
            "openweather": {"status": "up", "responseTime": 89}
        }
    }
    Claude responds: "Yes, all external APIs are working normally.
                      - Spotify: Responding in 145ms
                      - OpenWeather: Responding in 89ms"

    User: "Why can't I get weather data?"

    Claude thinks: "Let me check if the APIs are working"
    Claude invokes: check_api_health()
    Tool returns: {
        "status": "unhealthy",
        "services": {
            "spotify": {"status": "up", "responseTime": 120},
            "openweather": {"status": "down", "responseTime": null, "error": "Connection timeout"}
        }
    }
    Claude responds: "There's currently an issue with the OpenWeather API -
                      it's not responding. Spotify is working fine. This is
                      likely a temporary issue with OpenWeather's servers.
                      Please try again in a few minutes."

    When to Use This Tool:
    ----------------------
    1. User explicitly asks about API health
    2. Other tools are failing and you want to diagnose
    3. Before making critical requests (pre-flight check)
    4. For monitoring and alerting purposes

    Example Usage in Code:
    -----------------------
    >>> status = await check_api_health()
    >>> print(status["status"])
    healthy
    >>> print(status["services"]["spotify"]["responseTime"])
    145
    >>> if status["status"] != "healthy":
    ...     print("Warning: Some APIs are having issues")
    """
    # Log the tool invocation
    # Note: This tool takes no parameters
    logger.info("check_api_health tool invoked")

    try:
        # Get the HTTP client
        client = await get_client()

        # Build the API endpoint
        # Endpoint: GET /api/v1/health/external
        endpoint = "/api/v1/health/external"

        # Call the API
        # This endpoint is usually fast (just pinging services)
        # No query parameters needed
        data = await client.get(endpoint)

        # Log the health status
        overall_status = data.get("status", "unknown")
        spotify_status = data.get("services", {}).get("spotify", {}).get("status", "unknown")
        weather_status = data.get("services", {}).get("openweather", {}).get("status", "unknown")

        logger.info(
            f"check_api_health successful: overall={overall_status}, "
            f"spotify={spotify_status}, openweather={weather_status}"
        )

        # Log warning if services are not healthy
        if overall_status != "healthy":
            logger.warning(
                f"External APIs are {overall_status}: "
                f"Spotify={spotify_status}, OpenWeather={weather_status}"
            )

        # Return the health data to Claude
        return data

    except WeatherMusicAPIError as e:
        # The health check request itself failed
        # This is different from the services being down
        logger.error(f"check_api_health request failed: {e}")

        # Even if the health endpoint fails, we can provide partial information
        # Return an error status that Claude can interpret
        return {
            "status": "unhealthy",
            "timestamp": None,
            "services": {
                "spotify": {"status": "unknown", "responseTime": None},
                "openweather": {"status": "unknown", "responseTime": None},
            },
            "error": f"Health check failed: {e.message}",
        }

    except Exception as e:
        # Unexpected error
        logger.error(f"Unexpected error in check_api_health: {e}", exc_info=True)

        # Return error status
        return {
            "status": "unhealthy",
            "timestamp": None,
            "services": {
                "spotify": {"status": "unknown", "responseTime": None},
                "openweather": {"status": "unknown", "responseTime": None},
            },
            "error": f"Unexpected error: {str(e)}",
        }


# Tool Metadata for MCP
# ----------------------

TOOL_NAME = "check_api_health"
TOOL_DESCRIPTION = """
Check health status of external API dependencies (Spotify and OpenWeather).

Use this tool when:
- User asks if the APIs are working
- Troubleshooting why other tools are failing
- Monitoring API availability and performance
- Before making critical requests (pre-flight check)

This tool checks:
- Spotify API availability and response time
- OpenWeather API availability and response time
- Overall system health (healthy/degraded/unhealthy)

Response times:
- < 500ms: Healthy
- 500ms - 2000ms: Degraded (slow but working)
- > 2000ms or no response: Unhealthy (down)

Note: This only checks connectivity and response time.
It does NOT verify:
- Authentication credentials
- API rate limits or quotas
- Actual functionality (only that services respond)

If this tool reports issues, other tools (get_weather, get_mood_music, etc.)
will likely fail as well.
"""

INPUT_SCHEMA = {
    "type": "object",
    "properties": {},
    "required": [],
}
# Note: This tool takes no parameters - it just checks health of all services
