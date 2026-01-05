"""
Tests Package for Weather-Music MCP Server

This package contains all tests for the MCP server.

Testing Strategy:
    - Unit tests: Test individual components in isolation
    - Integration tests: Test tool interactions with mock API responses
    - No real API calls in tests (use mocking)

Test Coverage:
    - Server initialization and configuration
    - HTTP client with retry logic
    - Each MCP tool (weather, music, context, health)
    - Cache manager functionality
    - Rate limiter functionality
"""
