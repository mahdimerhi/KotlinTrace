// SPDX-License-Identifier: Apache-2.0

package dev.kotlintrace.bugsnag

import dev.kotlintrace.Backend
import dev.kotlintrace.DefaultKotlinTraceDemangler
import dev.kotlintrace.KotlinTrace
import dev.kotlintrace.KotlinTraceOptions
import dev.kotlintrace.KotlinTraceReportFormatter

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
