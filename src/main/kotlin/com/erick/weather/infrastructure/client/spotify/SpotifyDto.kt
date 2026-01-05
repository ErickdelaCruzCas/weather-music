package com.erick.weather.infrastructure.client.spotify

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * External DTOs for Spotify API responses.
 * These models represent the structure of data returned by Spotify API.
 * DO NOT expose these models outside the infrastructure layer.
 */

// OAuth2 Token Response
data class SpotifyTokenResponse(
    @JsonProperty("access_token")
    val accessToken: String,
    @JsonProperty("token_type")
    val tokenType: String,
    @JsonProperty("expires_in")
    val expiresIn: Int
)

// Search Response
data class SpotifySearchResponse(
    val playlists: PlaylistsPage?,
    val tracks: TracksPage?
)

data class PlaylistsPage(
    val items: List<SpotifyPlaylist>,
    val total: Int,
    val limit: Int,
    val offset: Int,
    val next: String?
)

data class TracksPage(
    val items: List<SpotifyTrack>,
    val total: Int,
    val limit: Int,
    val offset: Int,
    val next: String?
)

// Playlist
data class SpotifyPlaylist(
    val id: String,
    val name: String,
    val description: String?,
    val images: List<SpotifyImage>?,
    val tracks: PlaylistTracksInfo?,
    @JsonProperty("external_urls")
    val externalUrls: ExternalUrls,
    val owner: Owner?
)

data class PlaylistTracksInfo(
    val total: Int
)

data class Owner(
    val id: String,
    @JsonProperty("display_name")
    val displayName: String?
)

// Track
data class SpotifyTrack(
    val id: String,
    val name: String,
    val artists: List<Artist>,
    val album: Album?,
    @JsonProperty("duration_ms")
    val durationMs: Int,
    @JsonProperty("external_urls")
    val externalUrls: ExternalUrls,
    val popularity: Int?
)

data class Artist(
    val id: String,
    val name: String,
    @JsonProperty("external_urls")
    val externalUrls: ExternalUrls?
)

data class Album(
    val id: String,
    val name: String,
    val images: List<SpotifyImage>?,
    @JsonProperty("release_date")
    val releaseDate: String?
)

// Audio Features
data class SpotifyAudioFeatures(
    val id: String,
    val valence: Double,        // 0.0 - 1.0 (musical positiveness)
    val energy: Double,         // 0.0 - 1.0 (intensity and activity)
    val danceability: Double,   // 0.0 - 1.0 (how suitable for dancing)
    val acousticness: Double,   // 0.0 - 1.0 (confidence the track is acoustic)
    val instrumentalness: Double, // 0.0 - 1.0 (predicts whether a track contains no vocals)
    val speechiness: Double,    // 0.0 - 1.0 (presence of spoken words)
    val liveness: Double,       // 0.0 - 1.0 (presence of audience)
    val tempo: Double,          // BPM
    val loudness: Double,       // dB
    @JsonProperty("duration_ms")
    val durationMs: Int,
    @JsonProperty("time_signature")
    val timeSignature: Int
)

// Multiple Audio Features Response
data class SpotifyAudioFeaturesResponse(
    @JsonProperty("audio_features")
    val audioFeatures: List<SpotifyAudioFeatures?>
)

// Playlist Tracks Response
data class PlaylistTracksResponse(
    val items: List<PlaylistTrackItem>,
    val total: Int,
    val limit: Int,
    val offset: Int,
    val next: String?
)

data class PlaylistTrackItem(
    val track: SpotifyTrack?,
    @JsonProperty("added_at")
    val addedAt: String?
)

// Common
data class SpotifyImage(
    val url: String,
    val height: Int?,
    val width: Int?
)

data class ExternalUrls(
    val spotify: String
)

// Category (for future extensions)
data class SpotifyCategory(
    val id: String,
    val name: String,
    val icons: List<SpotifyImage>?
)

data class CategoriesPage(
    val categories: CategoriesItems
)

data class CategoriesItems(
    val items: List<SpotifyCategory>,
    val total: Int
)

// Featured Playlists (for future extensions)
data class FeaturedPlaylistsResponse(
    val message: String?,
    val playlists: PlaylistsPage
)
