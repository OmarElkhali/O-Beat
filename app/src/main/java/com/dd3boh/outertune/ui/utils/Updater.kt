package com.dd3boh.outertune.ui.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.edit
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.dd3boh.outertune.BuildConfig
import com.dd3boh.outertune.R
import com.dd3boh.outertune.constants.AutomaticUpdatesKey
import com.dd3boh.outertune.constants.LastNotifiedUpdateKey
import com.dd3boh.outertune.constants.LastUpdateCheckKey
import com.dd3boh.outertune.constants.LastVersionKey
import com.dd3boh.outertune.constants.UpdateApkUrlKey
import com.dd3boh.outertune.constants.UpdateAvailableKey
import com.dd3boh.outertune.constants.UpdateReleaseUrlKey
import com.dd3boh.outertune.utils.dataStore
import com.dd3boh.outertune.utils.get
import com.dd3boh.outertune.utils.reportException
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

enum class UpdateCheckResult { AVAILABLE, UP_TO_DATE, NO_RELEASE, NO_COMPATIBLE_APK, FAILED, SKIPPED }

object Updater {
    private const val WORK_NAME = "obeat-release-check"
    private const val CHANNEL_ID = "obeat-updates"
    private const val NOTIFICATION_ID = 1004
    private val checkMutex = Mutex()
    private val client = HttpClient {
        install(HttpTimeout) { requestTimeoutMillis = 20_000 }
    }

    /** Native persistent work survives restarts; Android chooses the battery-friendly execution time. */
    suspend fun schedule(context: Context): Unit = withContext(Dispatchers.IO) {
        val manager = WorkManager.getInstance(context)
        if (BuildConfig.DEBUG || context.dataStore[AutomaticUpdatesKey] == false) {
            manager.cancelUniqueWork(WORK_NAME)
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
            return@withContext
        }
        val request = PeriodicWorkRequest.Builder(UpdateCheckWorker::class.java, 24, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        manager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    suspend fun tryCheckUpdate(context: Context, force: Boolean = false): UpdateCheckResult = withContext(Dispatchers.IO) {
        checkMutex.withLock {
            // An APK upgrade retains DataStore: clear the old offer even before the next network check.
            if (context.dataStore.get(UpdateAvailableKey, false)) {
                val cachedUrl = context.dataStore.get(UpdateApkUrlKey, "")
                val cachedRelease = GitHubRelease(context.dataStore.get(LastVersionKey, ""), "", emptyList())
                val cachedAsset = ReleaseAsset(Uri.parse(cachedUrl).lastPathSegment.orEmpty(), cachedUrl)
                if (!isTrustedUpdateUrl(cachedUrl, download = true) ||
                    !cachedRelease.isNewerThan(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE.toLong(), cachedAsset)) {
                    context.dataStore.edit { settings ->
                        settings[UpdateAvailableKey] = false
                        settings.remove(UpdateApkUrlKey)
                    }
                    NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
                }
            }
            if (!force && (BuildConfig.DEBUG || !context.dataStore.get(AutomaticUpdatesKey, true))) return@withLock UpdateCheckResult.SKIPPED
            val now = System.currentTimeMillis()
            val lastCheck = context.dataStore.get(LastUpdateCheckKey, 0L)
            if (!force && now >= lastCheck && now - lastCheck < TimeUnit.HOURS.toMillis(24)) {
                notifyAvailableUpdate(context)
                return@withLock UpdateCheckResult.SKIPPED
            }
            try {
                val response = client.get("https://api.github.com/repos/$UPDATE_REPOSITORY/releases/latest") {
                    header("Accept", "application/vnd.github+json")
                    header("X-GitHub-Api-Version", "2022-11-28")
                }
                if (response.status == HttpStatusCode.NotFound) {
                    context.dataStore.edit { settings ->
                        settings[LastUpdateCheckKey] = now
                        settings[UpdateAvailableKey] = false
                        settings.remove(UpdateApkUrlKey)
                        settings.remove(UpdateReleaseUrlKey)
                    }
                    NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
                    return@withLock UpdateCheckResult.NO_RELEASE
                }
                check(response.status == HttpStatusCode.OK) { "GitHub release check returned ${response.status.value}" }
                val release = parseGitHubRelease(response.bodyAsText())
                    ?: error("GitHub returned an invalid stable release")
                val asset = release.compatibleAsset(BuildConfig.FLAVOR, Build.SUPPORTED_ABIS.toList())
                val available = asset != null && release.isNewerThan(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE.toLong(), asset)
                context.dataStore.edit { settings ->
                    settings[LastUpdateCheckKey] = now
                    settings[LastVersionKey] = release.version
                    settings[UpdateAvailableKey] = available
                    settings[UpdateReleaseUrlKey] = release.pageUrl
                    if (available) settings[UpdateApkUrlKey] = asset!!.downloadUrl else settings.remove(UpdateApkUrlKey)
                }
                if (available) notifyAvailableUpdate(context) else NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
                when {
                    available -> UpdateCheckResult.AVAILABLE
                    asset == null -> UpdateCheckResult.NO_COMPATIBLE_APK
                    else -> UpdateCheckResult.UP_TO_DATE
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                reportException(error)
                UpdateCheckResult.FAILED
            }
        }
    }

    suspend fun notifyAvailableUpdate(context: Context) {
        if (BuildConfig.DEBUG) return
        if (!context.dataStore.get(AutomaticUpdatesKey, true) || !context.dataStore.get(UpdateAvailableKey, false)) return
        val apkUrl = context.dataStore.get(UpdateApkUrlKey, "")
        if (!isTrustedUpdateUrl(apkUrl, download = true) || context.dataStore.get(LastNotifiedUpdateKey, "") == apkUrl) return
        val version = context.dataStore.get(LastVersionKey, "")
        if (!GitHubRelease(version, "", emptyList()).isNewerThan(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE.toLong(), ReleaseAsset(Uri.parse(apkUrl).lastPathSegment.orEmpty(), apkUrl))) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val notificationManager = NotificationManagerCompat.from(context)
        if (!notificationManager.areNotificationsEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, context.getString(R.string.update_channel), NotificationManager.IMPORTANCE_DEFAULT))
            if (manager.getNotificationChannel(CHANNEL_ID)?.importance == NotificationManager.IMPORTANCE_NONE) return
        }
        val download = PendingIntent.getActivity(context, NOTIFICATION_ID, Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl)), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.download)
            .setContentTitle(context.getString(R.string.update_available_title, version))
            .setContentText(context.getString(R.string.update_notification_description))
            .setContentIntent(download)
            .addAction(R.drawable.download, context.getString(R.string.update_download), download)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(NOTIFICATION_ID, notification)
        context.dataStore.edit { it[LastNotifiedUpdateKey] = apkUrl }
    }
}

class UpdateCheckWorker(context: Context, parameters: WorkerParameters) : Worker(context, parameters) {
    override fun doWork(): Result = runBlocking {
        if (!applicationContext.dataStore.get(AutomaticUpdatesKey, true)) return@runBlocking Result.success()
        when (Updater.tryCheckUpdate(applicationContext, force = true)) {
            UpdateCheckResult.FAILED -> if (runAttemptCount < 3) Result.retry() else Result.failure()
            else -> Result.success()
        }
    }
}
