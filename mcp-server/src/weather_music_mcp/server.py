"""
MCP Server - Main Entry Point

This is the main MCP (Model Context Protocol) server that exposes the
Weather-Music API to Large Language Models like Claude.

What This File Does:
--------------------
1. Creates an MCP server instance
2. Registers the 4 MCP tools (weather, music, context, health)
3. Handles server lifecycle (startup, shutdown)
4. Provides the entry point to run the server

How MCP Servers Work:
----------------------
An MCP server is a program that:
- Listens for requests from an MCP client (like Claude Desktop)
- Exposes "tools" that the LLM can invoke
- Returns results back to the LLM
- Manages resources (HTTP clients, caches, etc.)

Architecture:
-------------
Claude Desktop (MCP Client)
        ↓
MCP Protocol (stdio transport)
        ↓
This Server (MCP Server)
        ↓
Weather-Music API (via HTTP client)
        ↓
External APIs (Spotify, OpenWeather)

The Communication Flow:
-----------------------
1. User asks Claude something like "What's the weather in Madrid?"
2. Claude (MCP client) recognizes this needs the get_weather tool
3. Claude sends a tool invocation request to this server via stdio
4. This server receives the request and calls the get_weather function
5. get_weather fetches data from the Weather-Music API
6. This server returns the data to Claude
7. Claude uses the data to answer the user

Running the Server:
-------------------
From command line:
    python -m weather_music_mcp.server

Or programmatically:
    python src/weather_music_mcp/server.py

The server runs indefinitely until stopped (Ctrl+C).
"""

import asyncio
import logging
from typing import Any

# MCP SDK imports
# The mcp library provides the Server class and decorators
from mcp.server import Server
from mcp.server.stdio import stdio_server

# Import our configuration
from .config import Config

# Import the HTTP client cleanup function
from .client import close_client

# Import all our MCP tools
from .tools import weather, music, context, health

# Get logger for this module
logger = logging.getLogger(__name__)


def create_server() -> Server:
    """
    Create and configure the MCP server instance.

    This function:
    1. Creates an MCP Server instance
    2. Registers all MCP tools
    3. Sets up lifecycle hooks
    4. Returns the configured server

    The server doesn't start yet - it's just configured.
    Call run_server() to actually start it.

    Returns:
        Server: Configured MCP server instance

    Example:
        >>> server = create_server()
        >>> # Server is created but not running yet
        >>> await run_server(server)  # Now it runs
    """
    logger.info("Creating MCP server...")

    # Create the MCP server instance
    # The name "weather-music-mcp" identifies this server to MCP clients
    server = Server("weather-music-mcp")

    logger.info("Registering MCP tools...")

    # Register Tool 1: get_weather
    # ------------------------------
    # This decorator registers the function as an MCP tool
    # Claude can invoke this tool by name
    @server.list_tools()
    async def list_tools() -> list[dict[str, Any]]:
        """
        List all available tools.

        MCP clients (like Claude) call this to discover what tools are available.
        This is called once when the client connects.

        Returns:
            list: List of tool descriptions
        """
        return [
            {
                "name": weather.TOOL_NAME,
                "description": weather.TOOL_DESCRIPTION,
                "inputSchema": weather.INPUT_SCHEMA,
            },
            {
                "name": music.TOOL_NAME,
                "description": music.TOOL_DESCRIPTION,
                "inputSchema": music.INPUT_SCHEMA,
            },
            {
                "name": context.TOOL_NAME,
                "description": context.TOOL_DESCRIPTION,
                "inputSchema": context.INPUT_SCHEMA,
            },
            {
                "name": health.TOOL_NAME,
                "description": health.TOOL_DESCRIPTION,
                "inputSchema": health.INPUT_SCHEMA,
            },
        ]

    @server.call_tool()
    async def call_tool(name: str, arguments: dict[str, Any]) -> list[dict[str, Any]]:
        """
        Handle tool invocation requests from the MCP client.

        When Claude wants to use a tool, it calls this function with:
        - name: The tool name (e.g., "get_weather")
        - arguments: The tool parameters (e.g., {"region": "Madrid"})

        This function routes the request to the appropriate tool function.

        Args:
            name: Tool name to invoke
            arguments: Tool arguments (parameters)

        Returns:
            list: Tool result wrapped in MCP format

        Raises:
            ValueError: If tool name is unknown

        Example:
            Claude wants weather for Madrid:
            call_tool("get_weather", {"region": "Madrid"})
            → Calls weather.get_weather("Madrid")
            → Returns weather data to Claude
        """
        logger.info(f"Tool invocation: {name} with arguments: {arguments}")

        try:
            # Route to the appropriate tool function
            if name == weather.TOOL_NAME:
                result = await weather.get_weather(**arguments)

            elif name == music.TOOL_NAME:
                result = await music.get_mood_music(**arguments)

            elif name == context.TOOL_NAME:
                result = await context.get_weather_context(**arguments)

            elif name == health.TOOL_NAME:
                result = await health.check_api_health(**arguments)

            else:
                # Unknown tool name
                error_msg = f"Unknown tool: {name}"
                logger.error(error_msg)
                raise ValueError(error_msg)

            # Log successful tool invocation
            logger.info(f"Tool {name} executed successfully")

            # Return result in MCP format
            # MCP expects a list of content blocks
            return [
                {
                    "type": "text",
                    "text": str(result),  # Convert dict to string for transport
                }
            ]

        except Exception as e:
            # Tool execution failed
            logger.error(f"Tool {name} failed: {e}", exc_info=True)

            # Return error in MCP format
            # Claude will see this error and can inform the user
            return [
                {
                    "type": "text",
                    "text": f"Error: {str(e)}",
                    "isError": True,
                }
            ]

    logger.info("MCP server created successfully")
    logger.info(f"Registered tools: {[weather.TOOL_NAME, music.TOOL_NAME, context.TOOL_NAME, health.TOOL_NAME]}")

    return server


async def run_server(server: Server) -> None:
    """
    Run the MCP server using stdio transport.

    This function starts the server and keeps it running until interrupted.
    The server communicates with MCP clients via standard input/output (stdio).

    How stdio Transport Works:
    --------------------------
    - MCP client (Claude Desktop) launches this server as a subprocess
    - Client sends requests via the server's stdin
    - Server sends responses via stdout
    - All communication uses JSON-RPC over stdio

    Args:
        server: Configured MCP server instance

    Example:
        >>> server = create_server()
        >>> await run_server(server)
        # Server now running, waiting for requests...
        # Press Ctrl+C to stop
    """
    logger.info("Starting MCP server...")
    logger.info("Server is ready to accept connections")
    logger.info("Press Ctrl+C to stop the server")

    # Print configuration for verification
    Config.print_config()

    try:
        # Run the server with stdio transport
        # This is async and runs indefinitely
        async with stdio_server() as (read_stream, write_stream):
            # Initialize the server with the stdio streams
            await server.run(
                read_stream,
                write_stream,
                server.create_initialization_options(),
            )

    except KeyboardInterrupt:
        # User pressed Ctrl+C
        logger.info("Received shutdown signal (Ctrl+C)")

    except Exception as e:
        # Unexpected error
        logger.error(f"Server error: {e}", exc_info=True)
        raise

    finally:
        # Cleanup on shutdown
        logger.info("Shutting down server...")

        # Close the HTTP client and free resources
        await close_client()

        logger.info("Server shutdown complete")


async def main() -> None:
    """
    Main entry point for the MCP server.

    This function:
    1. Creates the server
    2. Runs it until stopped
    3. Handles cleanup

    This is the function that gets called when you run:
        python -m weather_music_mcp.server
    """
    logger.info("=" * 60)
    logger.info("Weather-Music MCP Server")
    logger.info("=" * 60)

    # Create the server
    server = create_server()

    # Run the server (blocks until stopped)
    await run_server(server)


# Entry point when running as a script
# --------------------------------------
if __name__ == "__main__":
    """
    Script entry point.

    When you run this file directly:
        python src/weather_music_mcp/server.py

    This block executes and starts the server.
    """
    try:
        # Run the async main function
        # asyncio.run() creates an event loop and runs the coroutine
        asyncio.run(main())

    except KeyboardInterrupt:
        # User pressed Ctrl+C (already handled in run_server)
        pass

    except Exception as e:
        # Fatal error
        logger.error(f"Fatal error: {e}", exc_info=True)
        raise
