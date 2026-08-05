// SPDX-License-Identifier: Apache-2.0

package dev.kotlintrace

/**
 * One demangled stack-trace frame — the output of [KotlinTraceDemangler].
 *
 * Unlike [KotlinTraceReportFrame] (the vendor-neutral report shape), this
 * keeps the declaring class and function name separate.
 */
public data class KotlinTraceFrame(
    /** Declaring class, or `null` for top-level functions. */
    public val declaringClass: String?,
    /** Function or constructor name. */
    public val functionName: String,
    /** Source file basename, when resolvable. */
    public val fileName: String?,
    /** 1-based source line, when resolvable. */
    public val lineNumber: Int?,
)
