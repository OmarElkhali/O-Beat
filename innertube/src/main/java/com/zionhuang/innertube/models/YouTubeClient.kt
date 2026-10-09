package com.zionhuang.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class YouTubeClient(
    val clientName: String,
    val clientVersion: String,
    val clientId: String,
    val userAgent: String,
    val osVersion: String? = null,
    val osName: String? = null,
    val deviceMake: String? = null,
    val deviceModel: String? = null,
    val androidSdkVersion: Int? = null,
    val loginSupported: Boolean = false,
    val loginRequired: Boolean = false,
    val useSignatureTimestamp: Boolean = false,
    val useWebPoTokens: Boolean = false,
    val isEmbedded: Boolean = false,
    // val origin: String? = null,
    // val referer: String? = null,
) {
    fun toContext(locale: YouTubeLocale, visitorData: String?, dataSyncId: String?) = Context(
        client = Context.Client(
            clientName = clientName,
            clientVersion = clientVersion,
            osVersion = osVersion,
            gl = locale.gl,
            hl = locale.hl,
            visitorData = visitorData,
            osName = osName,
            deviceMake = deviceMake,
            deviceModel = deviceModel,
            androidSdkVersion = androidSdkVersion,
        ),
        user = Context.User(
            onBehalfOfUser = if (loginSupported) dataSyncId else null
        ),
    )

    companion object {
        /**
         * Should be the latest Firefox ESR version.
         */
        const val USER_AGENT_WEB = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0"

        const val ORIGIN_YOUTUBE_MUSIC = "https://music.youtube.com"
        const val REFERER_YOUTUBE_MUSIC = "$ORIGIN_YOUTUBE_MUSIC/"
        const val API_URL_YOUTUBE_MUSIC = "$ORIGIN_YOUTUBE_MUSIC/youtubei/v1/"

        val WEB = YouTubeClient(
            clientName = "WEB",
            clientVersion = "2.20260708.00.00",
            clientId = "1",
            userAgent = USER_AGENT_WEB,
        )

        val WEB_REMIX = YouTubeClient(
            clientName = "WEB_REMIX",
            clientVersion = "1.20260707.12.00",
            clientId = "67",
            userAgent = USER_AGENT_WEB,
            loginSupported = true,
            useSignatureTimestamp = true,
            useWebPoTokens = true,
        )

        val WEB_CREATOR = YouTubeClient(
            clientName = "WEB_CREATOR",
            clientVersion = "1.20260708.06.00",
            clientId = "62",
            userAgent = USER_AGENT_WEB,
            loginSupported = true,
            loginRequired = true,
            useSignatureTimestamp = true,
        )

        val TVHTML5_SIMPLY_EMBEDDED_PLAYER = YouTubeClient(
            clientName = "TVHTML5_SIMPLY_EMBEDDED_PLAYER",
            clientVersion = "2.0",
            clientId = "85",
            userAgent = "Mozilla/5.0 (PlayStation; PlayStation 4/12.02) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.4 Safari/605.1.15",
            loginSupported = true,
            loginRequired = true,
            useSignatureTimestamp = true,
            isEmbedded = true,
        )

        val TVHTML5 = YouTubeClient(
            clientName = "TVHTML5",
            clientVersion = "7.20260707.07.00",
            clientId = "7",
            userAgent = "Mozilla/5.0(SMART-TV; Linux; Tizen 4.0.0.2) AppleWebkit/605.1.15 (KHTML, like Gecko) SamsungBrowser/9.2 TV Safari/605.1.15",
            loginSupported = true,
            loginRequired = true,
            useSignatureTimestamp = true
        )

        val ANDROID = YouTubeClient(
            clientName = "ANDROID",
            clientVersion = "21.26.364",
            clientId = "3",
            userAgent = "com.google.android.youtube/21.26.364 (Linux; U; Android 11) gzip",
            osName = "Android",
            osVersion = "11",
            androidSdkVersion = 30,
            loginSupported = true,
            useSignatureTimestamp = true
        )

        val ANDROID_VR_NO_AUTH = YouTubeClient(
            clientName = "ANDROID_VR",
            clientVersion = "1.61.48",
            clientId = "28",
            userAgent = "com.google.android.apps.youtube.vr.oculus/1.61.48 (Linux; U; Android 12; en_US; Oculus Quest 3; Build/SQ3A.220605.009.A1; Cronet/132.0.6808.3)",
            loginSupported = false,
            useSignatureTimestamp = false
        )

        val IOS = YouTubeClient(
            clientName = "IOS",
            clientVersion = "21.26.4",
            clientId = "5",
            userAgent = "com.google.ios.youtube/21.26.4 (iPhone16,2; U; CPU iOS 18_3_2 like Mac OS X;)",
            osVersion = "18.3.2.22D82",
            osName = "iPhone",
            deviceMake = "Apple",
            deviceModel = "iPhone16,2",
        )

        /**
         * Current lightweight Apple client. It returns direct streams without the
         * deprecated Android VR/SABR-only transport used by older releases.
         */
        val VISIONOS = YouTubeClient(
            clientName = "VISIONOS",
            clientVersion = "1.02",
            clientId = "101",
            userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 15_7_3) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/26.0 Safari/605.1.15",
            osName = "visionOS",
            osVersion = "26.5.23O471",
            deviceMake = "Apple",
            deviceModel = "RealityDevice17,1",
        )

        /** TV client with a direct-stream fallback. */
        val TVHTML5_SIMPLY = YouTubeClient(
            clientName = "TVHTML5_SIMPLY",
            clientVersion = "1.0",
            clientId = "75",
            userAgent = "Mozilla/5.0 (ChromiumStylePlatform) Cobalt/25.lts.30.1034943-gold (unlike Gecko), Unknown_TV_Unknown_0/Unknown (Unknown, Unknown)",
            useSignatureTimestamp = true,
            useWebPoTokens = true,
        )
    }
}
