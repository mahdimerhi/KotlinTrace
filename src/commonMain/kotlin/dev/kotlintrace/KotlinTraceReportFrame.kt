// SPDX-License-Identifier: Apache-2.0

package dev.kotlintrace

public data class KotlinTraceReportFrame(
    public val symbol: String,
    public val file: String?,
    public val line: Int?,
)
