"""
Configuration Module for Weather-Music MCP Server

This module handles all configuration loading from environment variables (.env file).

Why Configuration is Important:
--------------------------------
MCP servers need to be configurable to work in different environments (development,
staging, production). Using environment variables allows you to change settings without
modifying code.

How it works:
-------------
1. python-dotenv reads the .env file
2. os.getenv() fetches each variable
3. Default values are provided as fallback
4. Configuration is validated on startup

Environment Variables:
----------------------
- WEATHER_MUSIC_API_URL: URL of the Weather-Music REST API
- LOG_LEVEL: Logging verbosity (DEBUG, INFO, WARNING, ERROR)
- REQUEST_TIMEOUT: HTTP request timeout in seconds
- MAX_RETRIES: Number of retry attempts for failed requests
- ENABLE_CACHE: Whether to cache API responses
- CACHE_TTL: Cache time-to-live in seconds
- ENABLE_RATE_LIMITING: Whether to enable rate limiting
- RATE_LIMIT_PER_MINUTE: Max requests per minute

Example .env file:
------------------
    WEATHER_MUSIC_API_URL=http://localhost:8080
    LOG_LEVEL=INFO
    REQUEST_TIMEOUT=30
    MAX_RETRIES=3
"""

import os
import logging
from typing import Optional
from dotenv import load_dotenv

# Load environment variables from .env file
# This reads the .env file and makes the variables available via os.getenv()
# If .env doesn't exist, that's okay - we'll use default values
load_dotenv()


class Config:
    """
    Configuration class for the MCP server.

    This class centralizes all configuration values and provides validation.
    Instead of scattering os.getenv() calls throughout the codebase, we
    consolidate them here.

    Benefits:
    ---------
    - Single source of truth for configuration
    - Easy to test (can override values in tests)
    - Type hints for better IDE support
    - Validation on startup
    """

    # API Configuration
    # -----------------
    # Base URL of the Weather-Music API
    # The MCP server will make HTTP requests to this URL
    API_URL: str = os.getenv("WEATHER_MUSIC_API_URL", "http://localhost:8080")

    # Logging Configuration
    # ---------------------
    # Controls how much information is logged
    # INFO = normal operation, DEBUG = verbose for troubleshooting
    LOG_LEVEL: str = os.getenv("LOG_LEVEL", "INFO")

    # HTTP Client Configuration
    # -------------------------
    # How long to wait for the API to respond (in seconds)
    # If the API takes longer than this, the request fails
    REQUEST_TIMEOUT: int = int(os.getenv("REQUEST_TIMEOUT", "30"))

    # Retry Configuration
    # -------------------
    # How many times to retry a failed request
    # Uses exponential backoff: 1s, 2s, 4s delays between retries
    MAX_RETRIES: int = int(os.getenv("MAX_RETRIES", "3"))

    # Cache Configuration (used in PHASE MCP-6)
    # ------------------------------------------
    # Whether to cache API responses in memory
    # Caching reduces API calls and improves response time
    ENABLE_CACHE: bool = os.getenv("ENABLE_CACHE", "true").lower() == "true"

    # Cache time-to-live in seconds (30 minutes = 1800 seconds)
    # After this time, cached data expires and fresh data is fetched
    CACHE_TTL: int = int(os.getenv("CACHE_TTL", "1800"))

    # Rate Limiting Configuration (used in PHASE MCP-6)
    # --------------------------------------------------
    # Whether to limit the number of requests the MCP server accepts
    # Protects against abuse and prevents overloading the API
    ENABLE_RATE_LIMITING: bool = os.getenv("ENABLE_RATE_LIMITING", "true").lower() == "true"

    # Maximum number of requests allowed per minute
    # Requests beyond this limit are rejected with an error
    RATE_LIMIT_PER_MINUTE: int = int(os.getenv("RATE_LIMIT_PER_MINUTE", "100"))

    @classmethod
    def validate(cls) -> None:
        """
        Validate configuration values.

        This method checks that all configuration values are valid.
        It's called on server startup to fail fast if configuration is wrong.

        Raises:
            ValueError: If any configuration value is invalid

        Example:
            >>> Config.validate()  # Raises ValueError if invalid
        """
        # Validate API URL
        if not cls.API_URL:
            raise ValueError("WEATHER_MUSIC_API_URL cannot be empty")

        if not (cls.API_URL.startswith("http://") or cls.API_URL.startswith("https://")):
            raise ValueError(f"WEATHER_MUSIC_API_URL must start with http:// or https://, got: {cls.API_URL}")

        # Validate log level
        valid_log_levels = ["DEBUG", "INFO", "WARNING", "ERROR", "CRITICAL"]
        if cls.LOG_LEVEL.upper() not in valid_log_levels:
            raise ValueError(f"LOG_LEVEL must be one of {valid_log_levels}, got: {cls.LOG_LEVEL}")

        # Validate numeric values
        if cls.REQUEST_TIMEOUT <= 0:
            raise ValueError(f"REQUEST_TIMEOUT must be positive, got: {cls.REQUEST_TIMEOUT}")

        if cls.MAX_RETRIES < 0:
            raise ValueError(f"MAX_RETRIES must be non-negative, got: {cls.MAX_RETRIES}")

        if cls.CACHE_TTL <= 0:
            raise ValueError(f"CACHE_TTL must be positive, got: {cls.CACHE_TTL}")

        if cls.RATE_LIMIT_PER_MINUTE <= 0:
            raise ValueError(f"RATE_LIMIT_PER_MINUTE must be positive, got: {cls.RATE_LIMIT_PER_MINUTE}")

    @classmethod
    def setup_logging(cls) -> None:
        """
        Configure logging for the MCP server.

        Sets up Python's logging module with the configured log level.
        All log messages will include timestamp, level, and message.

        Example:
            >>> Config.setup_logging()
            >>> logging.info("Server started")  # Will be logged if LOG_LEVEL=INFO
        """
        logging.basicConfig(
            level=getattr(logging, cls.LOG_LEVEL.upper()),
            format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
            datefmt="%Y-%m-%d %H:%M:%S",
        )

    @classmethod
    def print_config(cls) -> None:
        """
        Print current configuration (useful for debugging).

        Displays all configuration values in a readable format.
        Useful for verifying configuration on server startup.

        Example:
            >>> Config.print_config()
            Weather-Music MCP Server Configuration:
            ========================================
            API URL: http://localhost:8080
            Log Level: INFO
            ...
        """
        logging.info("Weather-Music MCP Server Configuration:")
        logging.info("=" * 50)
        logging.info(f"API URL: {cls.API_URL}")
        logging.info(f"Log Level: {cls.LOG_LEVEL}")
        logging.info(f"Request Timeout: {cls.REQUEST_TIMEOUT}s")
        logging.info(f"Max Retries: {cls.MAX_RETRIES}")
        logging.info(f"Cache Enabled: {cls.ENABLE_CACHE}")
        logging.info(f"Cache TTL: {cls.CACHE_TTL}s")
        logging.info(f"Rate Limiting Enabled: {cls.ENABLE_RATE_LIMITING}")
        logging.info(f"Rate Limit: {cls.RATE_LIMIT_PER_MINUTE} req/min")
        logging.info("=" * 50)


# Initialize configuration on module import
# This ensures logging is configured as soon as this module is imported
Config.setup_logging()

# Validate configuration on startup
# This fails fast if configuration is invalid
try:
    Config.validate()
    logging.info("Configuration validated successfully")
except ValueError as e:
    logging.error(f"Configuration validation failed: {e}")
    raise
