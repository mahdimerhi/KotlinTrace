// SPDX-License-Identifier: Apache-2.0

package dev.kotlintrace

public data class KotlinTraceExceptionReport(
    public val name: String,
    public val reason: String,
    public val frames: List<KotlinTraceReportFrame>,
) {
    // "symbol|file|line" per line — the wire format the vendor sinks consume.
    public val framesText: String
        get() = frames.joinToString("\n") { it.encode() }
}

internal fun KotlinTraceReportFrame.encode(): String =
    listOf(symbol, file.orEmpty(), line?.toString().orEmpty()).joinToString("|")
