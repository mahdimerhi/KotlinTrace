// SPDX-License-Identifier: Apache-2.0

package dev.kotlintrace.crashlytics

import dev.kotlintrace.Backend
import dev.kotlintrace.DefaultKotlinTraceDemangler
import dev.kotlintrace.KotlinTrace
import dev.kotlintrace.KotlinTraceOptions
import dev.kotlintrace.KotlinTraceReportFormatter

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
