// SPDX-License-Identifier: Apache-2.0

package dev.kotlintrace.bugsnag

import dev.kotlintrace.Backend
import dev.kotlintrace.DefaultKotlinTraceDemangler
import dev.kotlintrace.KotlinTrace
import dev.kotlintrace.KotlinTraceOptions
import dev.kotlintrace.KotlinTraceReportFormatter

/**
 * Enables the Bugsnag backend for [KotlinTrace].
 *
 * Registers a reporter that converts the uncaught Kotlin throwable into a
 * readable Bugsnag event (demangled frames + optional file:line) via the
 * bugsnag-cocoa SDK already linked in the app, and synchronously persists
 * and uploads the payload (StoreAndSend + unhandled) before it returns.
 * The process is terminated by KotlinTrace afterwards, so Bugsnag's own
 * error detection — which the sample disables — never sees the death.
 *
 * Fails fast at startup if the Bugsnag SDK is not linked.
 *
 * @param demangle whether mangled `kfun:` symbols are demangled.
 * @param includeKotlinSourceLocation whether `(File.kt:line)` source info is
 *   resolved and attached to frames.
 */
public fun KotlinTrace.installBugsnag(
    demangle: Boolean = true,
    includeKotlinSourceLocation: Boolean = true,
) {
    val formatter = KotlinTraceReportFormatter(
        demangler = if (demangle) DefaultKotlinTraceDemangler() else null,
        includeSourceLocation = includeKotlinSourceLocation,
    )
    KotlinTrace.registerBackend(Backend.BUGSNAG) { throwable ->
        val report = formatter.format(throwable)
        BugsnagSink.record(report.name, report.reason, report.framesText)
    }
    KotlinTrace.install(KotlinTraceOptions(backends = setOf(Backend.BUGSNAG)))
    // Fail fast at startup when bugsnag-cocoa is missing or not started.
    BugsnagSink.verifyDependencies()
}
