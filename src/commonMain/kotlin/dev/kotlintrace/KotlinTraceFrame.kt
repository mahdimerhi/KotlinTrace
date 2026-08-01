// SPDX-License-Identifier: Apache-2.0

package dev.kotlintrace

public data class KotlinTraceFrame(
    public val declaringClass: String?,
    public val functionName: String,
    public val fileName: String?,
    public val lineNumber: Int?,
)
