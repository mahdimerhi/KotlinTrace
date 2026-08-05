// SPDX-License-Identifier: Apache-2.0

package dev.kotlintrace

/**
 * Configuration passed to [KotlinTrace.install].
 *
 * The two switches mirror the `install*()` extension parameters of each
 * backend and let you tune the report at a single place:
 *
 * - [demangle]: convert mangled Kotlin/Native `kfun:` symbols into readable
 *   declaring class + function names.
 * - [includeKotlinSourceLocation]: resolve `(File.kt:line)` via the bundled
 *   source-info facilities, so the reports show where the exception originated.
 */
public data class KotlinTraceOptions(
    /** Backends to deliver crash reports to. */
    public val backends: Set<Backend>,
    /** Whether mangled `kfun:` symbols are demangled. Default: `true`. */
    public val demangle: Boolean = true,
    /** Whether `(File.kt:line)` source locations are resolved. Default: `true`. */
    public val includeKotlinSourceLocation: Boolean = true,
)
