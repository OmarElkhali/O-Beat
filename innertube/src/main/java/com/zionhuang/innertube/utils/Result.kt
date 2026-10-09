package com.zionhuang.innertube.utils

import kotlin.coroutines.cancellation.CancellationException

/** Keep API failures as Results while allowing cancelled requests to stop their callers. */
inline fun <T> runCatchingCancellable(block: () -> T): Result<T> =
    runCatching(block).onFailure { if (it is CancellationException) throw it }
