// SPDX-License-Identifier: Apache-2.0

package dev.kotlintrace

internal actual object PlatformHook {
    private var options: KotlinTraceOptions? = null

    internal actual fun install(options: KotlinTraceOptions) {
        this.options = options
    }
}
