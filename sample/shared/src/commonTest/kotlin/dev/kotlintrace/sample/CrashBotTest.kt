// SPDX-License-Identifier: Apache-2.0

package dev.kotlintrace.sample

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CrashBotTest {

    @Test
    fun crashThrowsIllegalStateExceptionWithSampleMessage() {
        val throwable = CrashBot.crash()

        assertIs<IllegalStateException>(throwable)
        assertEquals(CrashBot.CRASH_MESSAGE, throwable.message)
    }

    @Test
    fun setupCrashlyticsDoesNotThrowOnJvm() {
        CrashBot.setupCrashlytics()
    }
}
