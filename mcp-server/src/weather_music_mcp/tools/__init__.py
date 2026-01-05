"""
MCP Tools Package

This package contains all the MCP tools (functions that Claude can invoke).

What are MCP Tools?
-------------------
Tools are functions that an LLM can call when it needs to perform an action or
retrieve data from an external source. When Claude detects that a user's query
requires information from the Weather-Music API, it will invoke one of these tools.

Example Flow:
    1. User asks: "What's the weather in Madrid?"
    2. Claude recognizes this requires weather data
    3. Claude invokes the `get_weather` tool with region="Madrid"
    4. The tool calls the Weather-Music API
    5. The tool returns the data to Claude
    6. Claude uses the data to answer the user

Available Tools:
    - weather.get_weather: Get current weather for a region
    - music.get_mood_music: Get music recommendations based on weather
    - context.get_weather_context: Get aggregated weather + music context
    - health.check_api_health: Check external API health status
"""
