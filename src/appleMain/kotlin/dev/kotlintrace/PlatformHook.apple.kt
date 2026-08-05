// SPDX-License-Identifier: Apache-2.0

package dev.kotlintrace

import kotlin.concurrent.Volatile
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.setUnhandledExceptionHook
import platform.posix.usleep

@OptIn(ExperimentalNativeApi::class)
internal actual object PlatformHook {
    @Volatile
    private var previousHook: ((Throwable) -> Unit)? = null

    internal actual fun install(options: KotlinTraceOptions) {
        if (previousHook != null) return
        // Must run before any Kotlin stack trace is built: point the runtime's
        // source-info dispatcher at libbacktrace (simulator and device alike).
        SourceInfoHook.install()
        previousHook = setUnhandledExceptionHook { throwable ->
            // Swallow reporter failures so only the original crash can terminate the app.
            runCatching {
                // Keep the raw crash visible in the console for diagnostics
                // (the default hook used to print it before aborting).
                println("Uncaught Kotlin exception: $throwable")
                throwable.stackTraceToString().let(::println)
                KotlinTraceReporter.report(throwable, KotlinTrace.installedBackends)
                // Backends persist asynchronously (e.g. Crashlytics'
                // recordExceptionModel: dispatches to the main queue); without
                // this delay the process exit below cuts the write off mid-air
                // and the report is lost.
                usleep(1_000_000u)
            }
            // Do NOT chain the previous (default) hook: it calls
            // terminateWithUnhandledException -> abort(), raising SIGABRT that
            // Crashlytics' mach handler captures as a second, mangled 'Crash'
            // event. Exit cleanly so the only record of this crash is the
            // demangled report KotlinTrace already sent to each backend.
            // NOTE: the previous hook is intentionally never invoked, so the
            // process must be terminated here to keep the app from running on
            // after an unhandled exception.
            kotlin.system.exitProcess(1)
        }
    }
}
