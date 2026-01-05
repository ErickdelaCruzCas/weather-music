"""
Music Tool - Get Weather-Based Music Recommendations

This module implements the 'get_mood_music' MCP tool.

Purpose:
--------
This tool recommends music based on the emotional "mood" created by current weather.
It's the core feature of the Weather-Music API.

How Weather Affects Music Recommendations:
-------------------------------------------
Weather → Mood Profile → Audio Features (Valence, Energy) → Spotify Search

Examples:
- Rainy weather → Melancholic mood → Low valence (0.0-0.4), Medium energy → Chill playlists
- Sunny weather → Happy mood → High valence (0.6-1.0), High energy → Upbeat playlists
- Cloudy weather → Calm mood → Medium valence, Low energy → Relaxing music

Audio Features Explained:
--------------------------
- Valence (0.0 - 1.0): Musical positiveness
  * High valence (0.6-1.0) = Happy, cheerful, euphoric
  * Low valence (0.0-0.4) = Sad, melancholic, angry

- Energy (0.0 - 1.0): Intensity and activity level
  * High energy (0.6-1.0) = Fast, loud, intense
  * Low energy (0.0-0.4) = Calm, quiet, peaceful

Example Queries that Trigger this Tool:
----------------------------------------
- "Recommend me music based on the weather in Madrid"
- "What should I listen to if it's raining in London?"
- "Give me happy playlists for sunny Barcelona"
- "I'm in Paris, what music matches the current weather?"
"""

import logging
from typing import Dict, Any, Optional

from ..client import get_client, WeatherMusicAPIError

logger = logging.getLogger(__name__)


async def get_mood_music(
    region: str,
    limit: int = 10,
    type: str = "both",
) -> Dict[str, Any]:
    """
    Get music recommendations based on current weather mood.

    This tool combines weather detection with Spotify's audio analysis to
    recommend music that matches the "mood" created by the current weather.

    How it Works (Behind the Scenes):
    ----------------------------------
    1. User asks for music recommendations
    2. Claude invokes this tool with region (and optional limit/type)
    3. API fetches current weather for the region
    4. API maps weather condition to mood profile:
       - Rain → Melancholic (low valence, medium energy)
       - Sunny → Happy (high valence, high energy)
       - Cloudy → Calm (medium valence, low energy)
    5. API searches Spotify for music matching the mood's audio features
    6. API returns top recommendations sorted by feature match score
    7. Tool returns data to Claude
    8. Claude presents recommendations to the user

    Args:
        region: Region identifier (city name, city with country, or coordinates)
        limit: Maximum number of recommendations to return (1-50, default 10)
        type: Type of recommendations to return:
            - "playlist": Only playlists
            - "track": Only individual tracks
            - "both": Both playlists and tracks (default)

    Returns:
        dict: Music recommendations with mood profile:
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
    User: "I'm in Madrid and it's raining. What should I listen to?"

    Claude thinks: "User wants music recommendations for Madrid weather"
    Claude invokes: get_mood_music(region="Madrid", limit=5, type="playlist")
    Tool returns: {"mood": {"name": "Melancholic"}, "recommendations": [...]}
    Claude responds: "Based on the rainy weather in Madrid, I recommend these
                      melancholic playlists that match the mood:
                      1. Rainy Day (50 tracks)
                      2. Chill Lofi Study Beats (175 tracks)
                      ..."

    Example Usage in Code:
    -----------------------
    >>> data = await get_mood_music("Barcelona", limit=5, type="playlist")
    >>> print(data["mood"]["name"])
    Happy
    >>> for rec in data["recommendations"]:
    ...     print(f"{rec['name']} - {rec['url']}")
    """
    # Input validation
    # ----------------
    if not region or not region.strip():
        logger.warning("get_mood_music called with empty region")
        raise ValueError("Region parameter cannot be empty")

    region = region.strip()

    # Validate limit parameter
    if limit < 1 or limit > 50:
        logger.warning(f"get_mood_music called with invalid limit={limit}")
        raise ValueError("Limit must be between 1 and 50")

    # Validate type parameter
    valid_types = ["playlist", "track", "both"]
    if type not in valid_types:
        logger.warning(f"get_mood_music called with invalid type='{type}'")
        raise ValueError(f"Type must be one of {valid_types}, got '{type}'")

    # Log the tool invocation with all parameters
    logger.info(f"get_mood_music tool invoked: region='{region}', limit={limit}, type='{type}'")

    try:
        # Get the HTTP client
        client = await get_client()

        # Build the API endpoint
        # Endpoint: GET /api/v1/regions/{region}/mood-music?limit={limit}&type={type}
        endpoint = f"/api/v1/regions/{region}/mood-music"

        # Build query parameters
        params = {
            "limit": limit,
            "type": type,
        }

        # Call the API (with automatic retries)
        data = await client.get(endpoint, params=params)

        # Log success with mood information
        mood_name = data.get("mood", {}).get("name", "Unknown")
        rec_count = data.get("total", 0)
        logger.info(
            f"get_mood_music successful: region='{region}', "
            f"mood={mood_name}, recommendations={rec_count}"
        )

        # Return the data to Claude
        return data

    except WeatherMusicAPIError as e:
        # API error
        logger.error(f"get_mood_music failed for region='{region}': {e}")
        raise

    except Exception as e:
        # Unexpected error
        logger.error(
            f"Unexpected error in get_mood_music for region='{region}': {e}",
            exc_info=True
        )
        raise


# Tool Metadata for MCP
# ----------------------

TOOL_NAME = "get_mood_music"
TOOL_DESCRIPTION = """
Get music recommendations based on current weather mood.

Use this tool when the user asks for:
- Music recommendations based on weather
- Playlists that match the current weather
- Songs for a specific mood created by weather
- What to listen to given current conditions

This tool:
1. Detects current weather for the region
2. Maps weather to a mood profile (e.g., Rain → Melancholic)
3. Recommends Spotify playlists/tracks matching the mood's audio features

The recommendations are based on Spotify's audio analysis (valence, energy)
and are NOT personalized to user preferences.

Note: For complete weather + music context, use get_weather_context instead.
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
            "description": "Maximum number of recommendations to return (1-50, default 10)",
            "default": 10,
            "minimum": 1,
            "maximum": 50,
        },
        "type": {
            "type": "string",
            "description": "Type of recommendations: 'playlist', 'track', or 'both' (default)",
            "enum": ["playlist", "track", "both"],
            "default": "both",
        },
    },
    "required": ["region"],
}
