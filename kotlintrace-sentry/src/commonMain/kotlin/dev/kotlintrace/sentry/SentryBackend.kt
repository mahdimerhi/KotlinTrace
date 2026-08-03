// SPDX-License-Identifier: Apache-2.0

package dev.kotlintrace.sentry

import dev.kotlintrace.Backend
import dev.kotlintrace.DefaultKotlinTraceDemangler
import dev.kotlintrace.KotlinTrace
import dev.kotlintrace.KotlinTraceOptions
import dev.kotlintrace.KotlinTraceReportFormatter

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
