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
        // Must run before any Kotlin stack trace is built: on the simulator,
        // point the runtime's source-info dispatcher at libbacktrace.
        SourceInfoHook.install()
        previousHook = setUnhandledExceptionHook { throwable ->
            // Swallow reporter failures so only the original crash can terminate the app.
            runCatching {
                KotlinTraceReporter.report(throwable, KotlinTrace.installedBackends)
                // Backends persist asynchronously (e.g. Crashlytics'
                // recordExceptionModel: dispatches to the main queue); without
                // this delay the runtime's abort() cuts the write off mid-air
                // and the report is lost.
                usleep(1_000_000u)
            }
            previousHook?.invoke(throwable)
        }
    }
}
