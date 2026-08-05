// SPDX-License-Identifier: Apache-2.0

package dev.kotlintrace

import kotlin.concurrent.Volatile

/**
 * Entry point of the KotlinTrace library.
 *
 * [install] arms the Kotlin/Native uncaught-exception hook once: when an
 * unhandled Kotlin exception kills the process, every registered backend's
 * reporter is invoked with the [Throwable], then the process exits cleanly
 * (KotlinTrace already reported, so the duplicate native SIGABRT event with
 * mangled `kfun:` symbols is avoided).
 *
 * Consumers normally never call [registerBackend] directly — use the
 * per-backend extension functions instead, for example
 * `dev.kotlintrace.crashlytics`'s `KotlinTrace.installCrashlytics()`.
 */
object KotlinTrace {
    @Volatile
    private var hookInstalled = false

    @Volatile
    internal var installedBackends: Set<Backend> = emptySet()

    /**
     * Installs KotlinTrace for the requested [backends][KotlinTraceOptions.backends]
     * and arms the uncaught-exception hook.
     *
     * Safe to call more than once; only the first call configures the hook.
     */
    fun install(options: KotlinTraceOptions) {
        installedBackends = installedBackends + options.backends
        if (hookInstalled) return
        hookInstalled = true
        PlatformHook.install(options)
    }

    // Public so adapter modules (separate Gradle modules) can register their reporters.
    /**
     * Registers a reporter for [backend]. Adapter modules call this inside
     * their `install*()` extension; app code should use those extensions.
     */
    fun registerBackend(backend: Backend, reporter: (Throwable) -> Unit) {
        KotlinTraceReporter.register(backend, reporter)
    }
}
