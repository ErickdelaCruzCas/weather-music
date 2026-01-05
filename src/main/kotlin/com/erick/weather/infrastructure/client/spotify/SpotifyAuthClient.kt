package com.erick.weather.infrastructure.client.spotify

import com.erick.weather.infrastructure.config.SpotifyProperties
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.*
import java.util.concurrent.atomic.AtomicReference

/**
 * Handles OAuth2 Client Credentials flow for Spotify API authentication.
 * Manages token lifecycle (request, cache, refresh).
 */
@Component
class SpotifyAuthClient(
    private val spotifyProperties: SpotifyProperties
) {

    private val logger = LoggerFactory.getLogger(SpotifyAuthClient::class.java)

    private val authWebClient: WebClient = WebClient.builder()
        .baseUrl(spotifyProperties.authUrl)
        .build()

    // Cached token with expiration time
    private val cachedToken: AtomicReference<CachedToken?> = AtomicReference(null)

    /**
     * Get valid access token (from cache or request new one)
     * @return Access token for Spotify API
     */
    fun getAccessToken(): Mono<String> {
        val cached = cachedToken.get()

        // Return cached token if still valid
        if (cached != null && !cached.isExpired()) {
            logger.debug("Using cached Spotify access token")
            return Mono.just(cached.token)
        }

        // Request new token
        logger.info("Requesting new Spotify access token")
        return requestAccessToken()
            .doOnNext { response ->
                val expiresAt = Instant.now().plusSeconds(response.expiresIn.toLong() - 60) // 60s buffer
                cachedToken.set(CachedToken(response.accessToken, expiresAt))
                logger.info("Spotify access token obtained, expires in {} seconds", response.expiresIn)
            }
            .map { it.accessToken }
    }

    /**
     * Request new access token using Client Credentials flow
     * @return Token response from Spotify
     */
    private fun requestAccessToken(): Mono<SpotifyTokenResponse> {
        val credentials = Base64.getEncoder().encodeToString(
            "${spotifyProperties.clientId}:${spotifyProperties.clientSecret}".toByteArray()
        )

        val formData = LinkedMultiValueMap<String, String>().apply {
            add("grant_type", "client_credentials")
        }

        return authWebClient.post()
            .header(HttpHeaders.AUTHORIZATION, "Basic $credentials")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters.fromFormData(formData))
            .retrieve()
            .bodyToMono(SpotifyTokenResponse::class.java)
            .doOnError { error ->
                logger.error("Failed to obtain Spotify access token", error)
            }
    }

    /**
     * Force token refresh (clears cache)
     */
    fun refreshToken(): Mono<String> {
        logger.info("Force refreshing Spotify access token")
        cachedToken.set(null)
        return getAccessToken()
    }

    /**
     * Cached token with expiration
     */
    private data class CachedToken(
        val token: String,
        val expiresAt: Instant
    ) {
        fun isExpired(): Boolean = Instant.now().isAfter(expiresAt)
    }
}
