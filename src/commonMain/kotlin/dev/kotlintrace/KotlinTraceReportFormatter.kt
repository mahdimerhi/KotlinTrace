// SPDX-License-Identifier: Apache-2.0

package dev.kotlintrace

/**
 * Builds vendor-neutral [KotlinTraceExceptionReport]s from a [Throwable].
 *
 * With the default [demangler], mangled Kotlin/Native `kfun:` frame lines
 * are turned into readable symbols with optional `(File.kt:line)` source
 * info, while native bridge frames and Kotlin runtime bootstrap frames
 * (exception `<init>` chain, `Function0.invoke` trampoline) are dropped so
 * each report leads with the first application frame. With `demangler =
 * null` the raw lines are passed through untouched.
 */
public class KotlinTraceReportFormatter(
    /** The demangler used to classify/rewrite frames, or `null` for raw passthrough. */
    public val demangler: KotlinTraceDemangler? = DefaultKotlinTraceDemangler(),
    /** Whether demangled frames carry `(file:line)` source info. */
    public val includeSourceLocation: Boolean = true,
) {
    /**
     * Formats [throwable] into a crash report: exception name, message and a
     * readable frame list.
     */
    public fun format(throwable: Throwable): KotlinTraceExceptionReport =
        KotlinTraceExceptionReport(
            name = throwable::class.simpleName ?: "Throwable",
            reason = throwable.message ?: "",
            frames = formatStackTrace(throwable.stackTraceToString()),
        )

    /**
     * Parses a raw stack trace (any form `Throwable.toString()` produces,
     * including `stackTraceToString()`) line by line into report frames.
     */
    public fun formatStackTrace(rawTrace: String): List<KotlinTraceReportFrame> =
        rawTrace.lineSequence()
            .drop(1)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { line ->
                val demangled = demangler?.demangle(line)
                when {
                    demangled != null -> KotlinTraceReportFrame(
                        symbol = listOfNotNull(demangled.declaringClass, demangled.functionName)
                            .joinToString("."),
                        file = if (includeSourceLocation) demangled.fileName else null,
                        line = if (includeSourceLocation) demangled.lineNumber else null,
                    )
                    // With a demangler active, frames that do not match a
                    // kfun: shape are native noise (pthread/libsystem bridge
                    // frames, hex-encoded path symbols); drop them so reports
                    // contain only demangled Kotlin frames.
                    demangler != null -> null
                    else -> KotlinTraceReportFrame(symbol = line, file = null, line = null)
                }
            }
            .filterNotNull()
            .filter { frame -> demangler == null || !isBootstrapNoise(frame) }
            .toList()

    // The Kotlin/Native runtime frames a report with its own machinery:
    // the exception constructor chain (kotlin.Throwable.<init> and friends)
    // and the lambda trampoline (kotlin.Function0.invoke). They carry no
    // diagnostic value, so drop them to let the first application frame
    // lead the report.
    private fun isBootstrapNoise(frame: KotlinTraceReportFrame): Boolean {
        val symbol = frame.symbol
        if (!symbol.startsWith("kotlin.")) return false
        val function = symbol.substringAfterLast('.')
        return function == "<init>" ||
            (function == "invoke" && symbol.substringBeforeLast('.').startsWith("kotlin.Function"))
    }
}
