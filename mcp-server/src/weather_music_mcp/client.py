"""
HTTP Client Module for Weather-Music API

This module provides an async HTTP client with retry logic for calling the
Weather-Music REST API.

Why Async?
----------
The MCP server uses async/await to handle multiple requests concurrently.
This means the server can handle multiple LLM requests at the same time
without blocking.

Why Retry Logic?
----------------
Network requests can fail temporarily (network glitches, API overload).
Instead of failing immediately, we retry with exponential backoff:
- 1st retry: wait 1 second
- 2nd retry: wait 2 seconds
- 3rd retry: wait 4 seconds

This gives the API time to recover without hammering it with requests.

Components:
-----------
- httpx.AsyncClient: Async HTTP client (like requests but async)
- tenacity: Retry library with backoff strategies
- Config: Configuration from config.py
"""

import logging
import httpx
from typing import Dict, Any, Optional
from tenacity import (
    retry,
    stop_after_attempt,
    wait_exponential,
    retry_if_exception_type,
    before_sleep_log,
)

from .config import Config

# Get logger for this module
# This allows us to log what the HTTP client is doing
logger = logging.getLogger(__name__)


class WeatherMusicAPIError(Exception):
    """
    Custom exception for Weather-Music API errors.

    This exception is raised when the API returns an error or when
    communication fails. It wraps the original error with context.

    Attributes:
        message: Human-readable error message
        status_code: HTTP status code (if applicable)
        details: Additional error details from the API
    """

    def __init__(self, message: str, status_code: Optional[int] = None, details: Optional[Dict] = None):
        self.message = message
        self.status_code = status_code
        self.details = details or {}
        super().__init__(self.message)

    def __str__(self) -> str:
        """String representation of the error."""
        if self.status_code:
            return f"[{self.status_code}] {self.message}"
        return self.message


class WeatherMusicClient:
    """
    Async HTTP client for the Weather-Music API.

    This class encapsulates all HTTP communication with the Weather-Music REST API.
    It handles:
    - Making async HTTP requests
    - Automatic retries with exponential backoff
    - Error handling and conversion to meaningful exceptions
    - Logging of requests and responses

    Usage:
        >>> client = WeatherMusicClient()
        >>> await client.initialize()  # Start the client
        >>> data = await client.get("/api/v1/regions/Madrid/weather")
        >>> await client.close()  # Cleanup when done

    Or use as async context manager:
        >>> async with WeatherMusicClient() as client:
        ...     data = await client.get("/api/v1/regions/Madrid/weather")
    """

    def __init__(self):
        """
        Initialize the Weather-Music API client.

        Note: This doesn't create the httpx client yet. Call initialize()
        or use as async context manager.
        """
        self.base_url = Config.API_URL
        self.timeout = Config.REQUEST_TIMEOUT
        self.client: Optional[httpx.AsyncClient] = None

        logger.info(f"WeatherMusicClient initialized with base_url={self.base_url}")

    async def initialize(self) -> None:
        """
        Initialize the async HTTP client.

        Creates the httpx.AsyncClient instance with configured timeout.
        Must be called before making requests (or use as context manager).

        Example:
            >>> client = WeatherMusicClient()
            >>> await client.initialize()
            >>> # Now ready to make requests
        """
        if self.client is None:
            # Create async HTTP client with timeout configuration
            # timeout applies to the entire request (connection + read)
            self.client = httpx.AsyncClient(
                base_url=self.base_url,
                timeout=self.timeout,
                follow_redirects=True,  # Follow HTTP redirects automatically
            )
            logger.info(f"HTTP client initialized (timeout={self.timeout}s)")

    async def close(self) -> None:
        """
        Close the HTTP client and cleanup resources.

        Always call this when done with the client to free resources.
        Automatically called when using as context manager.

        Example:
            >>> client = WeatherMusicClient()
            >>> await client.initialize()
            >>> # ... use client ...
            >>> await client.close()  # Cleanup
        """
        if self.client:
            await self.client.aclose()
            logger.info("HTTP client closed")
            self.client = None

    async def __aenter__(self):
        """
        Async context manager entry.

        Allows using the client with 'async with' statement:
            async with WeatherMusicClient() as client:
                data = await client.get("/endpoint")
        """
        await self.initialize()
        return self

    async def __aexit__(self, exc_type, exc_val, exc_tb):
        """Async context manager exit - cleanup resources."""
        await self.close()

    @retry(
        # Stop after MAX_RETRIES attempts
        stop=stop_after_attempt(Config.MAX_RETRIES + 1),  # +1 because first attempt doesn't count as retry
        # Wait with exponential backoff: 1s, 2s, 4s, ...
        # min=1 means minimum 1 second wait, max=10 means maximum 10 seconds wait
        wait=wait_exponential(multiplier=1, min=1, max=10),
        # Only retry on network/timeout errors, not on 4xx client errors
        # We retry on httpx.RequestError (network issues) and httpx.TimeoutException
        retry=retry_if_exception_type((httpx.RequestError, httpx.TimeoutException)),
        # Log before each retry attempt
        before_sleep=before_sleep_log(logger, logging.WARNING),
    )
    async def _make_request(
        self,
        method: str,
        endpoint: str,
        params: Optional[Dict[str, Any]] = None,
    ) -> httpx.Response:
        """
        Make an HTTP request with retry logic.

        This is an internal method that handles the actual HTTP request.
        The @retry decorator automatically retries failed requests with
        exponential backoff.

        Flow:
        -----
        1. Make HTTP request
        2. If network error → wait (exponential backoff) → retry
        3. If timeout → wait (exponential backoff) → retry
        4. If 5xx server error → raise exception (don't retry, API has issues)
        5. If 4xx client error → raise exception (don't retry, our fault)
        6. If 2xx success → return response

        Args:
            method: HTTP method (GET, POST, etc.)
            endpoint: API endpoint path (e.g., "/api/v1/health/external")
            params: Optional query parameters

        Returns:
            httpx.Response: The HTTP response

        Raises:
            httpx.RequestError: Network-related errors
            httpx.TimeoutException: Timeout errors

        Note:
            This method is decorated with @retry, so it will automatically
            retry on transient failures.
        """
        if not self.client:
            raise RuntimeError("Client not initialized. Call initialize() first or use as context manager.")

        # Log the request (helpful for debugging)
        logger.info(f"{method} {endpoint} params={params}")

        # Make the actual HTTP request
        # This is async - it doesn't block while waiting for the response
        response = await self.client.request(
            method=method,
            url=endpoint,
            params=params or {},
        )

        # Log the response status
        logger.info(f"{method} {endpoint} -> {response.status_code}")

        return response

    async def get(
        self,
        endpoint: str,
        params: Optional[Dict[str, Any]] = None,
    ) -> Dict[str, Any]:
        """
        Make a GET request to the Weather-Music API.

        This is the main method used by MCP tools to fetch data from the API.
        It handles retries, errors, and JSON parsing automatically.

        Args:
            endpoint: API endpoint path (e.g., "/api/v1/regions/Madrid/weather")
            params: Optional query parameters (e.g., {"limit": 10})

        Returns:
            dict: Parsed JSON response from the API

        Raises:
            WeatherMusicAPIError: If the request fails or API returns an error

        Example:
            >>> client = WeatherMusicClient()
            >>> await client.initialize()
            >>> data = await client.get("/api/v1/regions/Madrid/weather")
            >>> print(data["weather"]["temperature"])
            15.5
        """
        try:
            # Make the request (with automatic retries)
            response = await self._make_request("GET", endpoint, params)

            # Check for HTTP errors (4xx, 5xx)
            # raise_for_status() raises HTTPStatusError if status code indicates error
            response.raise_for_status()

            # Parse JSON response
            # The Weather-Music API always returns JSON
            data = response.json()

            logger.debug(f"GET {endpoint} successful, received {len(str(data))} bytes")

            return data

        except httpx.HTTPStatusError as e:
            # HTTP error (4xx, 5xx)
            # Convert to our custom exception with context
            error_detail = None
            try:
                # Try to parse error details from response body
                error_detail = e.response.json()
            except Exception:
                # If JSON parsing fails, use plain text
                error_detail = {"message": e.response.text}

            logger.error(f"HTTP error {e.response.status_code}: {error_detail}")

            raise WeatherMusicAPIError(
                message=f"API request failed: {error_detail.get('message', 'Unknown error')}",
                status_code=e.response.status_code,
                details=error_detail,
            )

        except httpx.TimeoutException as e:
            # Request timeout
            logger.error(f"Request timeout after {self.timeout}s: {e}")
            raise WeatherMusicAPIError(
                message=f"Request timed out after {self.timeout}s",
                status_code=None,
                details={"error": "timeout"},
            )

        except httpx.RequestError as e:
            # Network error (connection failed, DNS failure, etc.)
            logger.error(f"Request error: {e}")
            raise WeatherMusicAPIError(
                message=f"Network error: {str(e)}",
                status_code=None,
                details={"error": "network_error"},
            )

        except Exception as e:
            # Unexpected error
            logger.error(f"Unexpected error: {e}", exc_info=True)
            raise WeatherMusicAPIError(
                message=f"Unexpected error: {str(e)}",
                status_code=None,
                details={"error": "unknown"},
            )


# Global client instance (created lazily)
# This is used by MCP tools to make API requests
_client_instance: Optional[WeatherMusicClient] = None


async def get_client() -> WeatherMusicClient:
    """
    Get or create the global HTTP client instance.

    This function provides a singleton client that's shared across all MCP tools.
    Using a single client is more efficient than creating a new client for each request.

    Returns:
        WeatherMusicClient: Initialized HTTP client

    Example:
        >>> client = await get_client()
        >>> data = await client.get("/api/v1/health/external")
    """
    global _client_instance

    if _client_instance is None:
        _client_instance = WeatherMusicClient()
        await _client_instance.initialize()
        logger.info("Global HTTP client created")

    return _client_instance


async def close_client() -> None:
    """
    Close the global HTTP client and cleanup resources.

    Should be called when the MCP server shuts down to properly cleanup.

    Example:
        >>> await close_client()  # Cleanup on server shutdown
    """
    global _client_instance

    if _client_instance:
        await _client_instance.close()
        _client_instance = None
        logger.info("Global HTTP client closed")
