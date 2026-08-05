// SPDX-License-Identifier: Apache-2.0

package dev.kotlintrace.sentry

import dev.kotlintrace.Backend
import dev.kotlintrace.DefaultKotlinTraceDemangler
import dev.kotlintrace.KotlinTrace
import dev.kotlintrace.KotlinTraceOptions
import dev.kotlintrace.KotlinTraceReportFormatter

/**
 * Enables the Sentry backend for [KotlinTrace].
 *
 * Registers a reporter that converts the uncaught Kotlin throwable into a
 * readable Sentry event (demangled frames + optional file:line) via the
 * sentry-cocoa SDK already linked in the app, waits for the event to
 * flush, and returns. The process is terminated by KotlinTrace afterwards,
 * so Sentry's own crash handler — which the sample disables, and which
 * would add a duplicate mangled SIGABRT event — never sees the death.
 *
 * Fails fast at startup if the Sentry SDK is not linked.
 *
 * @param demangle whether mangled `kfun:` symbols are demangled.
 * @param includeKotlinSourceLocation whether `(File.kt:line)` source info is
 *   resolved and attached to frames.
 */
public fun KotlinTrace.installSentry(
    demangle: Boolean = true,
    includeKotlinSourceLocation: Boolean = true,
) {
    val formatter = KotlinTraceReportFormatter(
        demangler = if (demangle) DefaultKotlinTraceDemangler() else null,
        includeSourceLocation = includeKotlinSourceLocation,
    )
    KotlinTrace.registerBackend(Backend.SENTRY) { throwable ->
        val report = formatter.format(throwable)
        SentrySink.record(report.name, report.reason, report.framesText)
    }
    KotlinTrace.install(KotlinTraceOptions(backends = setOf(Backend.SENTRY)))
    // Fail fast at startup when sentry-cocoa is missing from the app.
    SentrySink.verifyDependencies()
}
