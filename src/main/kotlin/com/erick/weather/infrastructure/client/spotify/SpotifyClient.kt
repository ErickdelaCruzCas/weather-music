package com.erick.weather.infrastructure.client.spotify

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import java.time.Duration

@Component
class SpotifyClient(
    @Qualifier("spotifyWebClient") private val webClient: WebClient,
    private val authClient: SpotifyAuthClient
) {

    private val logger = LoggerFactory.getLogger(SpotifyClient::class.java)

    /**
     * Search playlists by query
     * @param query Search query (e.g., "rainy day", "chill")
     * @param limit Number of results (max 50)
     * @return Search results with playlists
     */
    @CircuitBreaker(name = "spotify", fallbackMethod = "searchPlaylistsFallback")
    fun searchPlaylists(query: String, limit: Int = 20): Mono<List<SpotifyPlaylist>> {
        logger.debug("Searching playlists with query: '{}', limit: {}", query, limit)

        return authClient.getAccessToken()
            .flatMap { token ->
                webClient.get()
                    .uri { uriBuilder ->
                        uriBuilder
                            .path("/search")
                            .queryParam("q", query)
                            .queryParam("type", "playlist")
                            .queryParam("limit", limit.coerceIn(1, 50))
                            .build()
                    }
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .retrieve()
                    .bodyToMono(SpotifySearchResponse::class.java)
                    .map { it.playlists?.items ?: emptyList() }
            }
            .retryWhen(
                Retry.backoff(2, Duration.ofMillis(500))
                    .filter { it is WebClientResponseException && it.statusCode.is5xxServerError }
            )
            .doOnSuccess { logger.info("Found {} playlists for query: '{}'", it.size, query) }
            .doOnError { error -> logger.error("Error searching playlists: {}", query, error) }
    }

    /**
     * Search tracks by query
     * @param query Search query
     * @param limit Number of results (max 50)
     * @return Search results with tracks
     */
    @CircuitBreaker(name = "spotify", fallbackMethod = "searchTracksFallback")
    fun searchTracks(query: String, limit: Int = 20): Mono<List<SpotifyTrack>> {
        logger.debug("Searching tracks with query: '{}', limit: {}", query, limit)

        return authClient.getAccessToken()
            .flatMap { token ->
                webClient.get()
                    .uri { uriBuilder ->
                        uriBuilder
                            .path("/search")
                            .queryParam("q", query)
                            .queryParam("type", "track")
                            .queryParam("limit", limit.coerceIn(1, 50))
                            .build()
                    }
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .retrieve()
                    .bodyToMono(SpotifySearchResponse::class.java)
                    .map { it.tracks?.items ?: emptyList() }
            }
            .retryWhen(
                Retry.backoff(2, Duration.ofMillis(500))
                    .filter { it is WebClientResponseException && it.statusCode.is5xxServerError }
            )
            .doOnSuccess { logger.info("Found {} tracks for query: '{}'", it.size, query) }
            .doOnError { error -> logger.error("Error searching tracks: {}", query, error) }
    }

    /**
     * Get audio features for a track
     * @param trackId Spotify track ID
     * @return Audio features (valence, energy, etc.)
     */
    @CircuitBreaker(name = "spotify", fallbackMethod = "getAudioFeaturesFallback")
    fun getAudioFeatures(trackId: String): Mono<SpotifyAudioFeatures> {
        logger.debug("Getting audio features for track: {}", trackId)

        return authClient.getAccessToken()
            .flatMap { token ->
                webClient.get()
                    .uri("/audio-features/{id}", trackId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .retrieve()
                    .bodyToMono(SpotifyAudioFeatures::class.java)
            }
            .doOnSuccess { logger.debug("Retrieved audio features for track: {}", trackId) }
            .doOnError { error -> logger.error("Error getting audio features for track: {}", trackId, error) }
    }

    /**
     * Get audio features for multiple tracks
     * @param trackIds List of Spotify track IDs (max 100)
     * @return List of audio features
     */
    @CircuitBreaker(name = "spotify", fallbackMethod = "getMultipleAudioFeaturesFallback")
    fun getMultipleAudioFeatures(trackIds: List<String>): Mono<List<SpotifyAudioFeatures>> {
        if (trackIds.isEmpty()) {
            return Mono.just(emptyList())
        }

        logger.debug("Getting audio features for {} tracks", trackIds.size)

        val idsParam = trackIds.take(100).joinToString(",")

        return authClient.getAccessToken()
            .flatMap { token ->
                webClient.get()
                    .uri { uriBuilder ->
                        uriBuilder
                            .path("/audio-features")
                            .queryParam("ids", idsParam)
                            .build()
                    }
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .retrieve()
                    .bodyToMono(SpotifyAudioFeaturesResponse::class.java)
                    .map { response ->
                        response.audioFeatures.filterNotNull()
                    }
            }
            .doOnSuccess { logger.info("Retrieved audio features for {} tracks", it.size) }
            .doOnError { error -> logger.error("Error getting multiple audio features", error) }
    }

    /**
     * Get playlist tracks
     * @param playlistId Spotify playlist ID
     * @param limit Number of tracks to retrieve (max 100)
     * @return List of tracks in the playlist
     */
    @CircuitBreaker(name = "spotify", fallbackMethod = "getPlaylistTracksFallback")
    fun getPlaylistTracks(playlistId: String, limit: Int = 50): Mono<List<SpotifyTrack>> {
        logger.debug("Getting tracks for playlist: {}, limit: {}", playlistId, limit)

        return authClient.getAccessToken()
            .flatMap { token ->
                webClient.get()
                    .uri { uriBuilder ->
                        uriBuilder
                            .path("/playlists/{playlist_id}/tracks")
                            .queryParam("limit", limit.coerceIn(1, 100))
                            .build(playlistId)
                    }
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .retrieve()
                    .bodyToMono(PlaylistTracksResponse::class.java)
                    .map { response ->
                        response.items.mapNotNull { it.track }
                    }
            }
            .doOnSuccess { logger.info("Retrieved {} tracks from playlist: {}", it.size, playlistId) }
            .doOnError { error -> logger.error("Error getting playlist tracks: {}", playlistId, error) }
    }

    /**
     * Get playlist with tracks and calculate average audio features
     * @param playlistId Spotify playlist ID
     * @return Playlist with enriched audio features data
     */
    fun getPlaylistWithAudioFeatures(playlistId: String): Mono<PlaylistWithFeatures> {
        return getPlaylistTracks(playlistId, 50)
            .flatMap { tracks ->
                if (tracks.isEmpty()) {
                    return@flatMap Mono.just(PlaylistWithFeatures(playlistId, emptyList(), null, null))
                }

                val trackIds = tracks.map { it.id }
                getMultipleAudioFeatures(trackIds)
                    .map { features ->
                        val avgValence = features.map { it.valence }.average()
                        val avgEnergy = features.map { it.energy }.average()
                        PlaylistWithFeatures(playlistId, tracks, avgValence, avgEnergy)
                    }
            }
    }

    /**
     * Health check for Spotify API
     * @return True if service is available
     */
    fun healthCheck(): Mono<Boolean> {
        return authClient.getAccessToken()
            .flatMap { token ->
                webClient.get()
                    .uri("/search?q=test&type=track&limit=1")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .retrieve()
                    .toBodilessEntity()
                    .map { true }
            }
            .onErrorReturn(false)
            .timeout(Duration.ofSeconds(3))
    }

    // Fallback methods for Circuit Breaker

    private fun searchPlaylistsFallback(query: String, limit: Int, throwable: Throwable): Mono<List<SpotifyPlaylist>> {
        logger.warn("Spotify search playlists fallback triggered for query: '{}'. Reason: {}", query, throwable.message)
        return Mono.error(SpotifyServiceException("Spotify service temporarily unavailable", throwable))
    }

    private fun searchTracksFallback(query: String, limit: Int, throwable: Throwable): Mono<List<SpotifyTrack>> {
        logger.warn("Spotify search tracks fallback triggered for query: '{}'. Reason: {}", query, throwable.message)
        return Mono.error(SpotifyServiceException("Spotify service temporarily unavailable", throwable))
    }

    private fun getAudioFeaturesFallback(trackId: String, throwable: Throwable): Mono<SpotifyAudioFeatures> {
        logger.warn("Spotify audio features fallback triggered for track: {}. Reason: {}", trackId, throwable.message)
        return Mono.error(SpotifyServiceException("Spotify service temporarily unavailable", throwable))
    }

    private fun getMultipleAudioFeaturesFallback(trackIds: List<String>, throwable: Throwable): Mono<List<SpotifyAudioFeatures>> {
        logger.warn("Spotify multiple audio features fallback triggered. Reason: {}", throwable.message)
        return Mono.error(SpotifyServiceException("Spotify service temporarily unavailable", throwable))
    }

    private fun getPlaylistTracksFallback(playlistId: String, limit: Int, throwable: Throwable): Mono<List<SpotifyTrack>> {
        logger.warn("Spotify playlist tracks fallback triggered for playlist: {}. Reason: {}", playlistId, throwable.message)
        return Mono.error(SpotifyServiceException("Spotify service temporarily unavailable", throwable))
    }

    /**
     * Helper data class for playlist with audio features
     */
    data class PlaylistWithFeatures(
        val playlistId: String,
        val tracks: List<SpotifyTrack>,
        val avgValence: Double?,
        val avgEnergy: Double?
    )
}

class SpotifyServiceException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
