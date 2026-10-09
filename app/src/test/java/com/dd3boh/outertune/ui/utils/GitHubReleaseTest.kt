package com.dd3boh.outertune.ui.utils

import org.junit.Assert.*
import org.junit.Test

class GitHubReleaseTest {
    private val page = "$UPDATE_REPOSITORY_URL/releases/tag/v0.9.4"
    private fun asset(name: String) = ReleaseAsset(name, "$UPDATE_REPOSITORY_URL/releases/download/v0.9.4/$name")

    @Test
    fun versionsAndAndroidVersionCodesPreventDowngrades() {
        assertEquals(1, compareReleaseVersions("v0.10.0", "0.9.9"))
        assertEquals(0, compareReleaseVersions("0.9.4.0", "0.9.4"))
        assertEquals(-1, compareReleaseVersions("0.9.3.2", "0.9.4"))
        assertNull(compareReleaseVersions("build-2026-10-08", "0.9.4"))
        assertNull(compareReleaseVersions("v0.9.4-beta", "0.9.4"))
        val release = GitHubRelease("0.9.4", page, emptyList())
        assertTrue(release.isNewerThan("0.9.3.2", 68, asset("O-Beat-0.9.4-full-arm64-v8a-release-69.apk")))
        assertFalse(release.isNewerThan("0.9.3.2", 69, asset("O-Beat-0.9.4-full-arm64-v8a-release-69.apk")))
        assertFalse(release.isNewerThan("0.9.3.2", 70, asset("O-Beat-0.9.4-full-arm64-v8a-release-69.apk")))
        assertTrue(release.isNewerThan("0.9.4", 68, asset("O-Beat-0.9.4-full-arm64-v8a-release-69.apk")))
    }

    @Test
    fun selectEditionAndPreferredAbiBeforeUniversal() {
        val names = listOf(
            "O-Beat-0.9.4-core-arm64-v8a-release-69.apk",
            "O-Beat-0.9.4-full-arm64-v8a-debug-69.apk",
            "O-Beat-0.9.4-full-x86-release-69.apk",
            "O-Beat-0.9.4-full-universal-release-69.apk",
            "O-Beat-0.9.4-full-armeabi-v7a-release-69.apk",
            "O-Beat-0.9.4-full-arm64-v8a-release-69.apk"
        )
        val release = GitHubRelease("0.9.4", page, names.map(::asset))
        assertEquals(names.last(), release.compatibleAsset("full", listOf("arm64-v8a", "armeabi-v7a"))?.name)
        assertEquals(names[3], release.compatibleAsset("full", listOf("x86_64"))?.name)
        assertEquals(names.first(), release.compatibleAsset("core", listOf("arm64-v8a"))?.name)
        assertNull(release.copy(assets = listOf(asset(names.last()))).compatibleAsset("core", listOf("arm64-v8a")))
        assertNull(release.copy(assets = listOf(asset(names[2]))).compatibleAsset("full", listOf("x86_64")))
        assertNull(release.copy(assets = listOf(asset("O-Beat-0.9.3.2-full-arm64-v8a-release-69.apk"))).compatibleAsset("full", listOf("arm64-v8a")))
        assertNull(release.copy(assets = listOf(asset("O-Beat-0.9.4-full-riscv64-release-69.apk"))).compatibleAsset("full", listOf("arm64-v8a")))
    }

    @Test
    fun parseTagInsteadOfDisplayNameAndRejectUntrustedAssets() {
        val response = """{
          "tag_name":"v0.9.4","name":"A beautiful release","html_url":"$page",
          "draft":false,"prerelease":false,"assets":[
            {"name":"evil.apk","browser_download_url":"https://example.com/evil.apk"},
            {"name":"O-Beat-0.9.4-full-universal-release-69.apk","state":"uploaded",
             "browser_download_url":"$UPDATE_REPOSITORY_URL/releases/download/v0.9.4/O-Beat-0.9.4-full-universal-release-69.apk"}
          ]} """
        val release = parseGitHubRelease(response)!!
        assertEquals("0.9.4", release.version)
        assertEquals(1, release.assets.size)
        assertEquals(69L, release.assets.first().versionCode)
        assertNull(parseGitHubRelease(response.replace("\"draft\":false", "\"draft\":true")))
        assertNull(parseGitHubRelease(response.replace("\"prerelease\":false", "\"prerelease\":true")))
        assertEquals("0.9.4", parseGitHubRelease(response.replace("\"tag_name\":\"v0.9.4\"", "\"tag_name\":\"build-2026-10-08\""))?.version)
        assertFalse(isTrustedUpdateUrl("https://github.com.attacker.example/$UPDATE_REPOSITORY/releases/tag/v1"))
        assertFalse(isTrustedUpdateUrl("http://github.com/$UPDATE_REPOSITORY/releases/tag/v1"))
        assertFalse(isTrustedUpdateUrl("https://github.com/OuterTune/OuterTune/releases/tag/v1"))
        assertFalse(isTrustedUpdateUrl("https://github.com/$UPDATE_REPOSITORY/releases/tag/v1", download = true))
    }
}
