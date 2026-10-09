package com.zionhuang.innertube

import com.zionhuang.innertube.utils.runCatchingCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Test

class ResultTest {
    @Test
    fun `cancelled API request does not run its fallback`() = runBlocking {
        var fallbackCalled = false
        val request = launch {
            runCatchingCancellable<Unit> { awaitCancellation() }
                .getOrElse { fallbackCalled = true }
        }
        yield()
        request.cancelAndJoin()
        assertFalse(fallbackCalled)
    }

    @Test
    fun `ordinary failures and successful values remain Results`() {
        val failure = IllegalStateException("API unavailable")
        assertSame(failure, runCatchingCancellable<Unit> { throw failure }.exceptionOrNull())
        assertEquals(42, runCatchingCancellable { 42 }.getOrThrow())
    }
}
