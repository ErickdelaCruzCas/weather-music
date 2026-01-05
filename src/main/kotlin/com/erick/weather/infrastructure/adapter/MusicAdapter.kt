package com.erick.weather.infrastructure.adapter

import com.erick.weather.domain.exception.ExternalServiceErrorException
import com.erick.weather.domain.exception.ExternalServiceException
import com.erick.weather.domain.model.MoodProfile
import com.erick.weather.domain.model.MusicRecommendation
import com.erick.weather.domain.model.RecommendationType
import com.erick.weather.domain.port.MusicPort
import com.erick.weather.infrastructure.client.spotify.SpotifyClient
import com.erick.weather.infrastructure.client.spotify.SpotifyPlaylist
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Adapter that implements MusicPort using SpotifyClient.
 * Maps external Spotify DTOs to domain models.
 */
@Component
class MusicAdapter(
    private val spotifyClient: SpotifyClient
) : MusicPort {

    private val logger = LoggerFactory.getLogger(MusicAdapter::class.java)

    override fun searchMusic(
        query: String,
        type: RecommendationType,
        limit: Int
    ): Mono<List<MusicRecommendation>> {
        return when (type) {
            RecommendationType.PLAYLIST -> searchPlaylists(query, limit)
            RecommendationType.TRACK -> searchTracks(query, limit)
        }.onErrorMap { error ->
            when (error) {
                is WebClientResponseException ->
                    ExternalServiceErrorException("spotify", "Spotify search error: ${error.message}", error)
                else ->
                    ExternalServiceException("spotify", "Spotify service is unavailable", error)
            }
        }
    }

    override fun searchMusicByMood(
        mood: MoodProfile,
        limit: Int,
        type: RecommendationType?
    ): Mono<List<MusicRecommendation>> {
        // Simple mood-based search using mood name as keyword
        val query = mood.name.name.lowercase()
        return searchMusic(query, type ?: RecommendationType.PLAYLIST, limit)
    }

    override fun getRecommendationsForMood(
        mood: MoodProfile,
        keywords: List<String>,
        limit: Int
    ): Mono<List<MusicRecommendation>> {
        logger.debug("Getting recommendations for mood: {} with keywords: {}", mood.name, keywords)

        // Search playlists for each keyword and combine results
        return Flux.fromIterable(keywords.take(3)) // Limit to top 3 keywords to avoid rate limits
            .flatMap { keyword ->
                searchPlaylistsWithFeatures(keyword, 10)
                    .onErrorResume { error ->
                        logger.warn("Error searching keyword '{}': {}", keyword, error.message)
                        Mono.just(emptyList())
                    }
            }
            .collectList()
            .map { allResults ->
                allResults.flatten()
                    .distinctBy { it.id } // Remove duplicates
                    .filter { it.fitsProfile(mood) } // Filter by mood profile
                    .map { it.withMatchScore(mood) } // Calculate match scores
                    .sortedByDescending { it.matchScore } // Sort by best match
                    .take(limit)
            }
    }

    override fun isHealthy(): Mono<Boolean> {
        return spotifyClient.healthCheck()
    }

    // Private helper methods

    private fun searchPlaylists(query: String, limit: Int): Mono<List<MusicRecommendation>> {
        return spotifyClient.searchPlaylists(query, limit)
            .map { playlists ->
                playlists.map { playlist ->
                    MusicRecommendation(
                        id = playlist.id,
                        name = playlist.name,
                        type = RecommendationType.PLAYLIST,
                        description = playlist.description,
                        trackCount = playlist.tracks?.total,
                        avgValence = 0.5, // Default, will be calculated if needed
                        avgEnergy = 0.5,
                        url = playlist.externalUrls.spotify,
                        imageUrl = playlist.images?.firstOrNull()?.url
                    )
                }
            }
    }

    private fun searchTracks(query: String, limit: Int): Mono<List<MusicRecommendation>> {
        return spotifyClient.searchTracks(query, limit)
            .flatMap { tracks ->
                if (tracks.isEmpty()) {
                    return@flatMap Mono.just(emptyList())
                }

                val trackIds = tracks.map { it.id }
                spotifyClient.getMultipleAudioFeatures(trackIds)
                    .map { audioFeatures ->
                        tracks.zip(audioFeatures) { track, features ->
                            MusicRecommendation(
                                id = track.id,
                                name = track.name,
                                type = RecommendationType.TRACK,
                                description = track.artists.joinToString(", ") { it.name },
                                trackCount = null,
                                avgValence = features.valence,
                                avgEnergy = features.energy,
                                url = track.externalUrls.spotify,
                                imageUrl = track.album?.images?.firstOrNull()?.url
                            )
                        }
                    }
            }
    }

    private fun searchPlaylistsWithFeatures(query: String, limit: Int): Mono<List<MusicRecommendation>> {
        return spotifyClient.searchPlaylists(query, limit)
            .flatMap { playlists ->
                if (playlists.isEmpty()) {
                    return@flatMap Mono.just(emptyList())
                }

                // Get audio features for each playlist (limit to top 3 to avoid rate limits)
                Flux.fromIterable(playlists.take(3))
                    .flatMap { playlist ->
                        enrichPlaylistWithFeatures(playlist)
                            .onErrorResume { error ->
                                logger.warn("Error enriching playlist '{}': {}", playlist.name, error.message)
                                // Return playlist with default features on error
                                Mono.just(
                                    MusicRecommendation(
                                        id = playlist.id,
                                        name = playlist.name,
                                        type = RecommendationType.PLAYLIST,
                                        description = playlist.description,
                                        trackCount = playlist.tracks?.total,
                                        avgValence = 0.5,
                                        avgEnergy = 0.5,
                                        url = playlist.externalUrls.spotify,
                                        imageUrl = playlist.images?.firstOrNull()?.url
                                    )
                                )
                            }
                    }
                    .collectList()
            }
    }

    private fun enrichPlaylistWithFeatures(playlist: SpotifyPlaylist): Mono<MusicRecommendation> {
        return spotifyClient.getPlaylistWithAudioFeatures(playlist.id)
            .map { playlistWithFeatures ->
                MusicRecommendation(
                    id = playlist.id,
                    name = playlist.name,
                    type = RecommendationType.PLAYLIST,
                    description = playlist.description,
                    trackCount = playlistWithFeatures.tracks.size,
                    avgValence = playlistWithFeatures.avgValence ?: 0.5,
                    avgEnergy = playlistWithFeatures.avgEnergy ?: 0.5,
                    url = playlist.externalUrls.spotify,
                    imageUrl = playlist.images?.firstOrNull()?.url
                )
            }
    }
}
