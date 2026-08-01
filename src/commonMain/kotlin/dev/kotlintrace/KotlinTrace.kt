// SPDX-License-Identifier: Apache-2.0

package dev.kotlintrace

import kotlin.concurrent.Volatile

object KotlinTrace {
    @Volatile
    private var hookInstalled = false

    @Volatile
    internal var installedBackends: Set<Backend> = emptySet()

    fun install(options: KotlinTraceOptions) {
        installedBackends = installedBackends + options.backends
        if (hookInstalled) return
        hookInstalled = true
        PlatformHook.install(options)
    }

    // Public so adapter modules (separate Gradle modules) can register their reporters.
    fun registerBackend(backend: Backend, reporter: (Throwable) -> Unit) {
        KotlinTraceReporter.register(backend, reporter)
    }
}
