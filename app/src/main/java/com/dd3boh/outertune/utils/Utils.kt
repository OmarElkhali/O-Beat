/*
 * Copyright (C) 2025 O​u​t​er​Tu​ne Project
 *
 * SPDX-License-Identifier: GPL-3.0
 *
 * For any other attributions, refer to the git commit history
 */

package com.dd3boh.outertune.utils

import androidx.compose.ui.util.fastAny
import androidx.media3.exoplayer.offline.Download
import com.dd3boh.outertune.constants.MAX_COIL_JOBS
import com.dd3boh.outertune.constants.MAX_DL_JOBS
import com.dd3boh.outertune.constants.MAX_LM_SCANNER_JOBS
import com.dd3boh.outertune.constants.MAX_YTM_CONTENT_JOBS
import com.dd3boh.outertune.constants.MAX_YTM_SYNC_JOBS
import com.dd3boh.outertune.playback.DownloadUtil
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.newFixedThreadPoolContext
import java.time.LocalDateTime
import java.time.ZoneOffset


/**
 *
 * coilCoroutine: Coil image resolution
 * dlCoroutine: Download jobs
 * lmScannerCoroutine: Local media scanner
 * syncCoroutine: Sync operations
 * ytmContentCoroutine: YouTube Music content fetching
 */
@OptIn(DelicateCoroutinesApi::class)
val coilCoroutine = newFixedThreadPoolContext(MAX_COIL_JOBS, "CoilImageResolver")

@OptIn(DelicateCoroutinesApi::class)
val dlCoroutine = newFixedThreadPoolContext(MAX_DL_JOBS, "DownloadJobs")

@OptIn(DelicateCoroutinesApi::class)
val lmScannerCoroutine = newFixedThreadPoolContext(MAX_LM_SCANNER_JOBS, "LocalMediaScanner")

@OptIn(DelicateCoroutinesApi::class)
val syncCoroutine = Dispatchers.IO

@OptIn(DelicateCoroutinesApi::class)
val ytmContentCoroutine = newFixedThreadPoolContext(MAX_YTM_CONTENT_JOBS, "YtmContentFetcher")

fun reportException(throwable: Throwable) {
    throwable.printStackTrace()
}








