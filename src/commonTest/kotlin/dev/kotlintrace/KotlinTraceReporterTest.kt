// SPDX-License-Identifier: Apache-2.0

package dev.kotlintrace

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KotlinTraceReporterTest {

    @Test
    fun reportsOnlyToRegisteredBackendInConfiguredSet() {
        val reported = mutableListOf<String>()
        KotlinTrace.registerBackend(Backend.CRASHLYTICS) { reported += "crashlytics" }
        KotlinTrace.registerBackend(Backend.SENTRY) { reported += "sentry" }

        KotlinTraceReporter.report(IllegalStateException("boom"), setOf(Backend.CRASHLYTICS))

        assertEquals(listOf("crashlytics"), reported)
    }

    @Test
    fun installMergesBackendsAcrossCalls() {
        KotlinTrace.install(KotlinTraceOptions(backends = setOf(Backend.CRASHLYTICS)))
        KotlinTrace.install(KotlinTraceOptions(backends = setOf(Backend.SENTRY)))

        assertTrue(Backend.CRASHLYTICS in KotlinTrace.installedBackends)
        assertTrue(Backend.SENTRY in KotlinTrace.installedBackends)
        assertFalse(Backend.BUGSNAG in KotlinTrace.installedBackends)
    }

    @Test
    fun unregisteredBackendIsIgnored() {
        val reported = mutableListOf<String>()
        KotlinTrace.registerBackend(Backend.BUGSNAG) { reported += "bugsnag" }

        KotlinTraceReporter.report(IllegalStateException("boom"), setOf(Backend.CRASHLYTICS))

        assertTrue(reported.isEmpty())
    }
}
