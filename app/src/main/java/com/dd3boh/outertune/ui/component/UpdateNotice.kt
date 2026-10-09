package com.dd3boh.outertune.ui.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import com.dd3boh.outertune.BuildConfig
import com.dd3boh.outertune.R
import com.dd3boh.outertune.constants.AutomaticUpdatesKey
import com.dd3boh.outertune.constants.LastVersionKey
import com.dd3boh.outertune.constants.UpdateApkUrlKey
import com.dd3boh.outertune.constants.UpdateAvailableKey
import com.dd3boh.outertune.ui.utils.isTrustedUpdateUrl
import com.dd3boh.outertune.ui.utils.GitHubRelease
import com.dd3boh.outertune.ui.utils.ReleaseAsset
import com.dd3boh.outertune.utils.rememberPreference

/** In-app fallback for users who have disabled Android notifications. */
@Composable
fun UpdateNotice() {
    if (BuildConfig.DEBUG) return
    val automatic by rememberPreference(AutomaticUpdatesKey, true)
    val available by rememberPreference(UpdateAvailableKey, false)
    val version by rememberPreference(LastVersionKey, "")
    val apkUrl by rememberPreference(UpdateApkUrlKey, "")
    val uriHandler = LocalUriHandler.current
    var dismissedUrl by rememberSaveable { mutableStateOf("") }
    if (!automatic || !available || !isTrustedUpdateUrl(apkUrl, download = true) || apkUrl == dismissedUrl) return
    if (!GitHubRelease(version, "", emptyList()).isNewerThan(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE.toLong(), ReleaseAsset(android.net.Uri.parse(apkUrl).lastPathSegment.orEmpty(), apkUrl))) return
    AlertDialog(
        onDismissRequest = { dismissedUrl = apkUrl },
        title = { Text(stringResource(R.string.update_available_title, version)) },
        text = { Text(stringResource(R.string.update_notification_description)) },
        confirmButton = {
            TextButton(onClick = {
                dismissedUrl = apkUrl
                uriHandler.openUri(apkUrl)
            }) { Text(stringResource(R.string.update_download)) }
        },
        dismissButton = {
            TextButton(onClick = { dismissedUrl = apkUrl }) { Text(stringResource(R.string.update_later)) }
        }
    )
}
