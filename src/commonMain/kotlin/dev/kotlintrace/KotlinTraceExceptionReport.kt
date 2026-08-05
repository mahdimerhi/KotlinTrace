// SPDX-License-Identifier: Apache-2.0

package dev.kotlintrace

/**
 * The vendor-neutral crash report produced by [KotlinTraceReportFormatter]
 * and consumed by the backend sinks (Crashlytics, Sentry, Bugsnag).
 *
 * [framesText] is the wire format the sinks upload: one
 * `symbol|file|line` line per frame.
 */
public data class KotlinTraceExceptionReport(
    /** Exception simple name, e.g. `IllegalStateException`. */
    public val name: String,
    /** Exception message, possibly empty. */
    public val reason: String,
    /** Readable stack frames, innermost first. */
    public val frames: List<KotlinTraceReportFrame>,
) {
    /** "symbol|file|line" per line — the wire format the vendor sinks consume. */
    public val framesText: String
        get() = frames.joinToString("\n") { it.encode() }
}

internal fun KotlinTraceReportFrame.encode(): String =
    listOf(symbol, file.orEmpty(), line?.toString().orEmpty()).joinToString("|")
