package com.dd3boh.outertune.ui.screens.settings

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import com.dd3boh.outertune.BuildConfig
import com.dd3boh.outertune.R
import com.dd3boh.outertune.constants.AutomaticUpdatesKey
import com.dd3boh.outertune.constants.LastVersionKey
import com.dd3boh.outertune.constants.UpdateApkUrlKey
import com.dd3boh.outertune.constants.UpdateAvailableKey
import com.dd3boh.outertune.constants.UpdateReleaseUrlKey
import com.dd3boh.outertune.ui.component.PreferenceEntry
import com.dd3boh.outertune.ui.component.SwitchPreference
import com.dd3boh.outertune.ui.utils.UpdateCheckResult
import com.dd3boh.outertune.ui.utils.Updater
import com.dd3boh.outertune.ui.utils.isTrustedUpdateUrl
import com.dd3boh.outertune.ui.utils.GitHubRelease
import com.dd3boh.outertune.ui.utils.ReleaseAsset
import com.dd3boh.outertune.utils.rememberPreference
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

@Composable
fun UpdateSettings() {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val (automatic, setAutomatic) = rememberPreference(AutomaticUpdatesKey, true)
    val available by rememberPreference(UpdateAvailableKey, false)
    val version by rememberPreference(LastVersionKey, BuildConfig.VERSION_NAME)
    val apkUrl by rememberPreference(UpdateApkUrlKey, "")
    val releaseUrl by rememberPreference(UpdateReleaseUrlKey, "")
    var checking by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<UpdateCheckResult?>(null) }
    var permissionDenied by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionDenied = !granted
        if (granted) scope.launch(Dispatchers.IO) { Updater.notifyAvailableUpdate(context) }
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.update_section_title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.update_installed_version, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.bodyMedium)
            if (BuildConfig.DEBUG) Text(stringResource(R.string.update_debug_notice), style = MaterialTheme.typography.bodySmall)
            if (available && isTrustedUpdateUrl(apkUrl, download = true) &&
                GitHubRelease(version, "", emptyList()).isNewerThan(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE.toLong(), ReleaseAsset(android.net.Uri.parse(apkUrl).lastPathSegment.orEmpty(), apkUrl))) {
                Text(stringResource(R.string.update_available_title, version), color = MaterialTheme.colorScheme.primary)
                Button(onClick = { uriHandler.openUri(apkUrl) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.update_download))
                }
            }
            Button(
                onClick = {
                    checking = true
                    scope.launch {
                        try { result = Updater.tryCheckUpdate(context, force = true) }
                        finally { checking = false }
                    }
                },
                enabled = !checking,
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(if (checking) R.string.update_checking else R.string.update_check_now)) }
            val status = when (result) {
                UpdateCheckResult.UP_TO_DATE -> R.string.update_current
                UpdateCheckResult.NO_RELEASE -> R.string.update_none_published
                UpdateCheckResult.NO_COMPATIBLE_APK -> R.string.update_no_compatible_apk
                UpdateCheckResult.FAILED -> R.string.update_check_failed
                else -> null
            }
            if (status != null) Text(stringResource(status), style = MaterialTheme.typography.bodyMedium)
            if (isTrustedUpdateUrl(releaseUrl)) {
                TextButton(onClick = { uriHandler.openUri(releaseUrl) }) { Text(stringResource(R.string.update_release_notes)) }
            }
        }
        SwitchPreference(
            title = { Text(stringResource(R.string.update_automatic)) },
            description = stringResource(R.string.update_automatic_description),
            checked = automatic,
            onCheckedChange = setAutomatic,
            isEnabled = !BuildConfig.DEBUG
        )
        PreferenceEntry(
            title = { Text(stringResource(R.string.update_enable_notifications)) },
            description = stringResource(R.string.update_notifications_description),
            isEnabled = !BuildConfig.DEBUG,
            onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    !NotificationManagerCompat.from(context).areNotificationsEnabled() && !permissionDenied) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    } else {
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:${context.packageName}"))
                    }
                    context.startActivity(intent)
                }
            }
        )
    }
    androidx.compose.runtime.LaunchedEffect(automatic) { Updater.schedule(context) }
}
