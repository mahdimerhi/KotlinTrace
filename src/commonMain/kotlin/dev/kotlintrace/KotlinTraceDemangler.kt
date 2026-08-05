// SPDX-License-Identifier: Apache-2.0

package dev.kotlintrace

/**
 * Converts one raw Kotlin/Native stack-trace line into a structured
 * [KotlinTraceFrame].
 *
 * [KotlinTraceReportFormatter] uses the demangler to classify lines: when a
 * demangler is active, lines it rejects are treated as native noise and
 * dropped from reports.
 */
public fun interface KotlinTraceDemangler {
    /**
     * @param rawFrame a single trimmed stack-trace line, typically in the
     *   Kotlin/Native runtime format:
     *   `"at 4   main.kexe   0x10204b533   kfun:#boomInner(kotlin.Int){} + 203 (/abs/main.kt:17:11)"`
     * @return the demangled frame, or `null` if the line is not a Kotlin frame.
     */
    public fun demangle(rawFrame: String): KotlinTraceFrame?
}
