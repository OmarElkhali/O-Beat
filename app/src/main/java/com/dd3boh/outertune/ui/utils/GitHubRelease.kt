package com.dd3boh.outertune.ui.utils

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URI

const val UPDATE_REPOSITORY = "OmarElkhali/O-Beat"
const val UPDATE_REPOSITORY_URL = "https://github.com/$UPDATE_REPOSITORY"

data class ReleaseAsset(val name: String, val downloadUrl: String) {
    val versionCode: Long? get() = Regex("-(\\d+)\\.apk$", RegexOption.IGNORE_CASE)
        .find(name)?.groupValues?.get(1)?.toLongOrNull()
}

data class GitHubRelease(val version: String, val pageUrl: String, val assets: List<ReleaseAsset>) {
    fun compatibleAsset(flavor: String, supportedAbis: List<String>): ReleaseAsset? {
        val knownAbis = listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86")
        fun String.hasToken(token: String) = Regex("(^|[-_.])${Regex.escape(token)}([-_.]|$)").containsMatchIn(this)
        return assets.filter { asset ->
            val name = asset.name.lowercase()
            val apkVersion = Regex("(?i)^O-?Beat-(\\d+(?:\\.\\d+)+)-").find(asset.name)?.groupValues?.get(1)
            name.endsWith(".apk") && !name.hasToken("debug") && !name.hasToken("userdebug") &&
                name.hasToken(flavor) && listOf("full", "core").none { it != flavor && name.hasToken(it) } &&
                (apkVersion == null || compareReleaseVersions(apkVersion, version) == 0)
        }.mapNotNull { asset ->
            val name = asset.name.lowercase()
            val abi = knownAbis.firstOrNull { name.hasToken(it) }
            val abiRank = when {
                abi != null -> supportedAbis.indexOf(abi)
                name.hasToken("universal") -> supportedAbis.size
                else -> -1
            }
            if (abiRank < 0) null else asset to abiRank
        }.sortedWith(compareBy({ it.second }, { it.first.name })).firstOrNull()?.first
    }

    fun isNewerThan(installedVersion: String, installedCode: Long, asset: ReleaseAsset): Boolean =
        asset.versionCode?.let { it > installedCode } ?: (compareReleaseVersions(version, installedVersion)?.let { it > 0 } == true)
}

/** Accept only this repository's HTTPS release links, including cached values. */
fun isTrustedUpdateUrl(url: String, download: Boolean = false): Boolean = runCatching {
    val uri = URI(url)
    uri.scheme == "https" && uri.host == "github.com" && uri.userInfo == null && uri.port == -1 &&
        uri.path.startsWith("/$UPDATE_REPOSITORY/releases/${if (download) "download/" else "tag/"}") &&
        !uri.path.contains("/../")
}.getOrDefault(false)

fun compareReleaseVersions(left: String, right: String): Int? {
    fun parse(value: String): List<Long>? {
        val clean = value.trim().removePrefix("v").removePrefix("V")
        if (!clean.matches(Regex("\\d+(\\.\\d+)*"))) return null
        return clean.split('.').map { it.toLongOrNull() ?: return null }
    }
    val a = parse(left) ?: return null
    val b = parse(right) ?: return null
    for (i in 0 until maxOf(a.size, b.size)) {
        val comparison = (a.getOrElse(i) { 0 }).compareTo(b.getOrElse(i) { 0 })
        if (comparison != 0) return comparison
    }
    return 0
}

fun parseGitHubRelease(response: String): GitHubRelease? {
    val json = Json.parseToJsonElement(response).jsonObject
    fun JsonObject.text(key: String) = this[key]?.jsonPrimitive?.content.orEmpty()
    if (json["draft"]?.jsonPrimitive?.booleanOrNull == true || json["prerelease"]?.jsonPrimitive?.booleanOrNull == true) return null
    val pageUrl = json.text("html_url")
    if (!isTrustedUpdateUrl(pageUrl)) return null
    val assets = json["assets"]?.jsonArray.orEmpty().mapNotNull { element ->
        val asset = element.jsonObject
        val url = asset.text("browser_download_url")
        if (isTrustedUpdateUrl(url, download = true) && asset.text("state") in listOf("", "uploaded"))
            ReleaseAsset(asset.text("name"), url) else null
    }
    val tag = json.text("tag_name")
    // The existing release workflow uses build-date tags; read the APK's actual version in that case.
    val version = tag.takeIf { compareReleaseVersions(it, "0") != null }?.removePrefix("v")?.removePrefix("V")
        ?: assets.firstNotNullOfOrNull { Regex("(?i)^O-?Beat-(\\d+(?:\\.\\d+)+)-").find(it.name)?.groupValues?.get(1) }
        ?: return null
    return GitHubRelease(version, pageUrl, assets)
}
