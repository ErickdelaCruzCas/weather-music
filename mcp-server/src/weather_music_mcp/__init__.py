"""
Weather-Music MCP Server

This is a Model Context Protocol (MCP) server that exposes the Weather-Music API
to Large Language Models like Claude.

MCP (Model Context Protocol) is a protocol that allows LLMs to interact with
external tools and data sources. This server acts as a bridge between Claude
and the Weather-Music REST API.

Architecture:
    Claude (LLM) ↔ MCP Server (this package) ↔ Weather-Music API

The server provides 4 main tools:
    - get_weather: Get current weather for a region
    - get_mood_music: Get music recommendations based on weather mood
    - get_weather_context: Get complete context (weather + mood + music)
    - check_api_health: Check health of external APIs

Author: Weather-Music Project
Version: 1.0.0
License: MIT
"""

__version__ = "1.0.0"
__author__ = "Weather-Music Project"
