// SPDX-License-Identifier: Apache-2.0

package dev.kotlintrace.crashlytics

import dev.kotlintrace.DefaultKotlinTraceDemangler
import dev.kotlintrace.KotlinTraceDemangler

public class CrashlyticsReportFormatter(
    public val demangler: KotlinTraceDemangler? = DefaultKotlinTraceDemangler(),
    public val includeSourceLocation: Boolean = true,
) {
    public fun format(throwable: Throwable): CrashlyticsExceptionReport =
        CrashlyticsExceptionReport(
            name = throwable::class.simpleName ?: "Throwable",
            reason = throwable.message ?: "",
            framesText = formatStackTrace(throwable.stackTraceToString())
                .joinToString("\n") { it.encode() },
        )

    public fun formatStackTrace(rawTrace: String): List<CrashlyticsFrame> =
        rawTrace.lineSequence()
            .drop(1)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { line ->
                val demangled = demangler?.demangle(line)
                if (demangled != null) {
                    CrashlyticsFrame(
                        symbol = listOfNotNull(demangled.declaringClass, demangled.functionName)
                            .joinToString("."),
                        file = if (includeSourceLocation) demangled.fileName else null,
                        line = if (includeSourceLocation) demangled.lineNumber else null,
                    )
                } else {
                    CrashlyticsFrame(symbol = line, file = null, line = null)
                }
            }
            .toList()
}
