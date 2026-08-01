// SPDX-License-Identifier: Apache-2.0

package dev.kotlintrace

internal expect object PlatformHook {
    internal fun install(options: KotlinTraceOptions)
}
