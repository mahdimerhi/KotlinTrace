// SPDX-License-Identifier: Apache-2.0

package dev.kotlintrace

public class KotlinTraceReportFormatter(
    public val demangler: KotlinTraceDemangler? = DefaultKotlinTraceDemangler(),
    public val includeSourceLocation: Boolean = true,
) {
    public fun format(throwable: Throwable): KotlinTraceExceptionReport =
        KotlinTraceExceptionReport(
            name = throwable::class.simpleName ?: "Throwable",
            reason = throwable.message ?: "",
            frames = formatStackTrace(throwable.stackTraceToString()),
        )

    public fun formatStackTrace(rawTrace: String): List<KotlinTraceReportFrame> =
        rawTrace.lineSequence()
            .drop(1)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { line ->
                val demangled = demangler?.demangle(line)
                if (demangled != null) {
                    KotlinTraceReportFrame(
                        symbol = listOfNotNull(demangled.declaringClass, demangled.functionName)
                            .joinToString("."),
                        file = if (includeSourceLocation) demangled.fileName else null,
                        line = if (includeSourceLocation) demangled.lineNumber else null,
                    )
                } else {
                    KotlinTraceReportFrame(symbol = line, file = null, line = null)
                }
            }
            .toList()
}
