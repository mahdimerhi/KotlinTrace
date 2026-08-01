// SPDX-License-Identifier: Apache-2.0

package dev.kotlintrace

public data class KotlinTraceOptions(
    public val backends: Set<Backend>,
    public val demangle: Boolean = true,
    public val includeKotlinSourceLocation: Boolean = true,
)
