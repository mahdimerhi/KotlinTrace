// SPDX-License-Identifier: Apache-2.0

package dev.kotlintrace.crashlytics

import dev.kotlintrace.Backend
import dev.kotlintrace.DefaultKotlinTraceDemangler
import dev.kotlintrace.KotlinTrace
import dev.kotlintrace.KotlinTraceOptions
import dev.kotlintrace.KotlinTraceReportFormatter

/**
 * Enables the Crashlytics backend for [KotlinTrace].
 *
 * Registers a reporter that converts the uncaught Kotlin throwable into a
 * readable Crashlytics non-fatal recorded exception (demangled frames +
 * optional file:line) via the Firebase Crashlytics SDK already linked in the
 * app, and returns. The process is terminated by KotlinTrace afterwards, so
 * Crashlytics' own native crash handler — which cannot be disabled and would
 * add a duplicate mangled SIGABRT event — never sees the death.
 *
 * Fails fast at startup if Firebase Crashlytics is not linked.
 *
 * @param demangle whether mangled `kfun:` symbols are demangled.
 * @param includeKotlinSourceLocation whether `(File.kt:line)` source info is
 *   resolved and attached to frames.
 */
public fun KotlinTrace.installCrashlytics(
    demangle: Boolean = true,
    includeKotlinSourceLocation: Boolean = true,
) {
    val formatter = KotlinTraceReportFormatter(
        demangler = if (demangle) DefaultKotlinTraceDemangler() else null,
        includeSourceLocation = includeKotlinSourceLocation,
    )
    KotlinTrace.registerBackend(Backend.CRASHLYTICS) { throwable ->
        val report = formatter.format(throwable)
        CrashlyticsSink.record(report.name, report.reason, report.framesText)
    }
    KotlinTrace.install(KotlinTraceOptions(backends = setOf(Backend.CRASHLYTICS)))
    // Fail fast at startup when Firebase Crashlytics is missing from the app.
    CrashlyticsSink.verifyDependencies()
}
