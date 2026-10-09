/*
 * Copyright (C) 2025 OuterTune Project
 *
 * SPDX-License-Identifier: GPL-3.0
 *
 * For any other attributions, refer to the git commit history
 */

package com.dd3boh.outertune.utils

import android.net.ConnectivityManager
import android.util.Log
import androidx.media3.common.PlaybackException
import com.dd3boh.outertune.constants.AudioQuality
import com.dd3boh.outertune.utils.YTPlayerUtils.MAIN_CLIENT
import com.dd3boh.outertune.utils.YTPlayerUtils.STREAM_FALLBACK_CLIENTS
import com.dd3boh.outertune.utils.YTPlayerUtils.validateStatus
import com.dd3boh.outertune.utils.potoken.PoTokenGenerator
import com.dd3boh.outertune.utils.potoken.PoTokenResult
import com.zionhuang.innertube.NewPipeUtils
import com.zionhuang.innertube.YouTube
import com.zionhuang.innertube.models.YouTubeClient
import com.zionhuang.innertube.models.YouTubeClient.Companion.TVHTML5_SIMPLY
import com.zionhuang.innertube.models.YouTubeClient.Companion.VISIONOS
import com.zionhuang.innertube.models.YouTubeClient.Companion.WEB_CREATOR
import com.zionhuang.innertube.models.YouTubeClient.Companion.WEB_REMIX
import com.zionhuang.innertube.models.response.PlayerResponse
import com.zionhuang.innertube.utils.runCatchingCancellable
import okhttp3.OkHttpClient
import kotlin.coroutines.cancellation.CancellationException

object YTPlayerUtils {

    private const val TAG = "YTPlayerUtils"

    private val httpClient = OkHttpClient.Builder()
        .proxy(YouTube.proxy)
        .protocols(listOf(okhttp3.Protocol.HTTP_1_1))
        .build()

    private val poTokenGenerator = PoTokenGenerator()

    /**
     * The main client is used for metadata and initial streams.
     * Do not use other clients for this because it can result in inconsistent metadata.
     * For example other clients can have different normalization targets (loudnessDb).
     *
     * VisionOS is the default because it currently returns direct audio streams without a
     * WebView BotGuard round-trip. Web clients remain available as fallbacks when needed.
     */
    private val MAIN_CLIENT: YouTubeClient = VISIONOS

    /**
     * Clients used for fallback streams in case the streams of the main client do not work.
     */
    private val STREAM_FALLBACK_CLIENTS: Array<YouTubeClient> = arrayOf(
        TVHTML5_SIMPLY,
        WEB_CREATOR,
        WEB_REMIX,
    )


    data class PlaybackData(
        val audioConfig: PlayerResponse.PlayerConfig.AudioConfig?,
        val videoDetails: PlayerResponse.VideoDetails?,
        val playbackTracking: PlayerResponse.PlaybackTracking?,
        val format: PlayerResponse.StreamingData.Format,
        val streamUrl: String,
        val streamExpiresInSeconds: Int,
        val streamClient: YouTubeClient,
    )

    fun streamRequestHeaders(client: YouTubeClient): Map<String, String> = mapOf(
        "User-Agent" to client.userAgent,
        "Origin" to YouTubeClient.ORIGIN_YOUTUBE_MUSIC,
        "Referer" to YouTubeClient.REFERER_YOUTUBE_MUSIC,
    )

    /**
     * Custom player response intended to use for playback.
     * Metadata like audioConfig and videoDetails are from [MAIN_CLIENT], or the working
     * fallback response when the main request fails before returning metadata.
     * Format & stream can be from [MAIN_CLIENT] or [STREAM_FALLBACK_CLIENTS].
     */
    suspend fun playerResponseForPlayback(
        videoId: String,
        playlistId: String? = null,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager,
    ): Result<PlaybackData> = runCatchingCancellable {
        Log.d(TAG, "Playback info requested: $videoId")

        /**
         * This is required for some clients to get working streams however
         * it should not be forced for the [MAIN_CLIENT] because the response of the [MAIN_CLIENT]
         * is required even if the streams won't work from this client.
         * This is why it is allowed to be null.
         */
        val signatureTimestamp by lazy { getSignatureTimestampOrNull(videoId) }

        val isLoggedIn = YouTube.cookie != null
        val sessionId =
            if (isLoggedIn) {
                // signed in sessions use dataSyncId as identifier
                YouTube.dataSyncId
            } else {
                // signed out sessions use visitorData as identifier
                YouTube.visitorData
            }

        Log.d(TAG, "[$videoId] isLoggedIn: $isLoggedIn")

        var webPlayerPot: String? = null
        var webStreamingPot: String? = null
        var didRequestWebPoTokens = false

        /**
         * BotGuard runs in a WebView and may take several seconds to initialise. Direct clients
         * (VisionOS) do not need it, so never make normal playback wait for it.
         */
        fun ensureWebPoTokens() {
            if (didRequestWebPoTokens) return
            didRequestWebPoTokens = true
            getWebClientPoTokenOrNull(videoId, sessionId)?.let {
                webPlayerPot = it.playerRequestPoToken
                webStreamingPot = it.streamingDataPoToken
            } ?: Log.w(TAG, "[$videoId] No web po token")
        }

        if (MAIN_CLIENT.useWebPoTokens) ensureWebPoTokens()
        val mainPlayerResult =
            YouTube.player(
                videoId, playlistId, MAIN_CLIENT,
                if (MAIN_CLIENT.useSignatureTimestamp) signatureTimestamp else null,
                webPlayerPot,
            )
        val mainPlayerResponse = mainPlayerResult.getOrNull()
        var lastRequestFailure = mainPlayerResult.exceptionOrNull()

        var format: PlayerResponse.StreamingData.Format? = null
        var streamUrl: String? = null
        var streamExpiresInSeconds: Int? = null
        var streamClient: YouTubeClient? = null

        var streamPlayerResponse: PlayerResponse? = null
        for (clientIndex in (-1 until STREAM_FALLBACK_CLIENTS.size)) {
            // reset for each client
            format = null
            streamUrl = null
            streamExpiresInSeconds = null

            // decide which client to use for streams and load its player response
            val client: YouTubeClient
            if (clientIndex == -1) {
                Log.d(TAG, "Trying client: ${MAIN_CLIENT.clientName}")
                // try with streams from main client first
                client = MAIN_CLIENT
                streamPlayerResponse = mainPlayerResponse
            } else {
                Log.d(TAG, "Trying fallback client: ${STREAM_FALLBACK_CLIENTS[clientIndex].clientName}")
                // after main client use fallback clients
                client = STREAM_FALLBACK_CLIENTS[clientIndex]

                if (client.loginRequired && !isLoggedIn) {
                    // skip client if it requires login but user is not logged in
                    continue
                }

                if (client.useWebPoTokens) ensureWebPoTokens()
                streamPlayerResponse =
                    YouTube.player(
                        videoId, playlistId, client,
                        if (client.useSignatureTimestamp) signatureTimestamp else null,
                        webPlayerPot,
                    )
                        .onFailure { lastRequestFailure = it }
                        .getOrNull()
            }

            Log.d(TAG, "[$videoId] stream client: ${client.clientName}, " +
                    "playabilityStatus: ${streamPlayerResponse?.playabilityStatus?.let {
                        it.status + (it.reason?.let { " - $it" } ?: "")
                    }}")

            // process current client response
            if (streamPlayerResponse?.playabilityStatus?.status == "OK") {
                format =
                    findFormat(
                        streamPlayerResponse,
                        audioQuality,
                        connectivityManager,
                    ) ?: continue
                streamUrl = findUrlOrNull(format, videoId) ?: continue
                streamExpiresInSeconds =
                    streamPlayerResponse.streamingData?.expiresInSeconds ?: continue

                if (client.useWebPoTokens && webStreamingPot != null) {
                    streamUrl += "&pot=$webStreamingPot";
                }

                if (validateStatus(streamUrl, client)) {
                    // working stream found
                    Log.i(TAG, "[$videoId] [${client.clientName}] found working stream")
                    streamClient = client
                    break
                } else {
                    Log.w(TAG, "[$videoId] [${client.clientName}] got bad http status code")
                }
            }
        }

        if (streamPlayerResponse == null) {
            throw lastRequestFailure ?: Exception("Bad stream player response")
        }
        if (streamPlayerResponse.playabilityStatus.status != "OK") {
            throw PlaybackException(
                streamPlayerResponse.playabilityStatus.reason,
                null,
                PlaybackException.ERROR_CODE_REMOTE_ERROR
            )
        }
        if (streamExpiresInSeconds == null) {
            throw Exception("Missing stream expire time")
        }
        if (format == null) {
            throw Exception("Could not find format")
        }
        if (streamUrl == null) {
            throw Exception("Could not find stream url")
        }
        if (streamClient == null) {
            throw Exception("No verified stream client")
        }

        val metadataResponse = mainPlayerResponse ?: streamPlayerResponse
        PlaybackData(
            metadataResponse.playerConfig?.audioConfig,
            metadataResponse.videoDetails,
            metadataResponse.playbackTracking,
            format,
            streamUrl,
            streamExpiresInSeconds,
            streamClient,
        )
    }

    /**
     * Simple player response intended to use for metadata only.
     * Stream URLs of this response might not work so don't use them.
     */
    suspend fun playerResponseForMetadata(
        videoId: String,
        playlistId: String? = null,
    ): Result<PlayerResponse> =
        YouTube.player(videoId, playlistId, client = WEB_REMIX) // ANDROID_VR does not work with history

    private fun findFormat(
        playerResponse: PlayerResponse,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager,
    ): PlayerResponse.StreamingData.Format? =
        playerResponse.streamingData?.adaptiveFormats
            ?.filter { it.isAudio }
            ?.maxByOrNull {
                it.bitrate * when (audioQuality) {
                    AudioQuality.AUTO -> if (connectivityManager.isActiveNetworkMetered) -1 else 1
                    AudioQuality.HIGH -> 1
                    AudioQuality.LOW -> -1
                } + (if (it.mimeType.startsWith("audio/webm")) 10240 else 0) // prefer opus stream
            }

    /**
     * Checks if the stream url returns a successful status.
     * If this returns true the url is likely to work.
     * If this returns false the url might cause an error during playback.
     */
    private fun validateStatus(url: String, client: YouTubeClient): Boolean {
        try {
            val request = okhttp3.Request.Builder()
                .get()
                .url(url)
                .apply {
                    streamRequestHeaders(client).forEach { (name, value) -> header(name, value) }
                    header("Range", "bytes=0-0")
                }
                .build()
            return httpClient.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            reportException(e)
        }
        return false
    }

    /**
     * Wrapper around the [NewPipeUtils.getSignatureTimestamp] function which reports exceptions
     */
    private fun getSignatureTimestampOrNull(
        videoId: String
    ): Int? {
        return NewPipeUtils.getSignatureTimestamp(videoId)
            .onFailure {
                reportException(it)
            }
            .getOrNull()
    }

    /**
     * Wrapper around the [NewPipeUtils.getStreamUrl] function which reports exceptions
     */
    private fun findUrlOrNull(
        format: PlayerResponse.StreamingData.Format,
        videoId: String
    ): String? {
        return NewPipeUtils.getStreamUrl(format, videoId)
            .onFailure {
                reportException(it)
            }
            .getOrNull()
    }

    /**
     * Wrapper around the [PoTokenGenerator.getWebClientPoToken] function which reports exceptions
     */
    private fun getWebClientPoTokenOrNull(videoId: String, sessionId: String?): PoTokenResult? {
        if (sessionId == null) {
            Log.d(TAG, "[$videoId] Session identifier is null")
            return null
        }
        try {
            return poTokenGenerator.getWebClientPoToken(videoId, sessionId)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            reportException(e)
        }
        return null
    }
}
