// SPDX-License-Identifier: Apache-2.0

package dev.kotlintrace.sample

import dev.kotlintrace.KotlinTrace
import dev.kotlintrace.bugsnag.installBugsnag
import dev.kotlintrace.crashlytics.installCrashlytics
import dev.kotlintrace.sentry.installSentry

object CrashBot {
    const val CRASH_MESSAGE = "KotlinTrace sample crash from shared Kotlin (CrashBot)"

    fun setupCrashlytics() {
        KotlinTrace.installCrashlytics()
    }

    fun setupSentry() {
        KotlinTrace.installSentry()
    }

    fun setupBugsnag() {
        KotlinTrace.installBugsnag()
    }

    // Crashes on a Kotlin/Native thread so the uncaught-exception hook fires
    // (an exception crossing the Swift boundary becomes an NSException instead).
    fun triggerCrash() {
        crashOnBackgroundThread {
            throw crash()
        }
    }

    internal fun crash(): Throwable = IllegalStateException(CRASH_MESSAGE)

    // Test hook: prints the exception's stack trace in normal flow (pre-termination),
    // so source-info resolution is observable on stderr.
    fun logCrash() {
        crash().printStackTrace()
    }
}

internal expect fun crashOnBackgroundThread(block: () -> Unit)
