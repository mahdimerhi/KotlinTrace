// SPDX-License-Identifier: Apache-2.0

package dev.kotlintrace

import kotlin.concurrent.Volatile

public object KotlinTrace {
    @Volatile
    private var installed = false

    public fun install(options: KotlinTraceOptions) {
        if (installed) return
        installed = true
        PlatformHook.install(options)
    }
}
