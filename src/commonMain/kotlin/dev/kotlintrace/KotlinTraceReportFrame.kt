// SPDX-License-Identifier: Apache-2.0

package dev.kotlintrace

/**
 * One frame of a vendor-neutral crash report produced by
 * [KotlinTraceReportFormatter].
 *
 * [symbol] already combines declaring class and function name; [file] and
 * [line] carry the source location the demangler resolved.
 */
public data class KotlinTraceReportFrame(
    /** Human-readable function symbol, e.g. `dev.kotlintrace.sample.CrashBot.crash`. */
    public val symbol: String,
    /** Source file basename, when resolvable. */
    public val file: String?,
    /** 1-based source line, when resolvable. */
    public val line: Int?,
)
