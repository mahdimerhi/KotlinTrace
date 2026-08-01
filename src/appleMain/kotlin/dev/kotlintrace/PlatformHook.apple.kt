// SPDX-License-Identifier: Apache-2.0

package dev.kotlintrace

import kotlin.concurrent.Volatile
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.setUnhandledExceptionHook

@OptIn(ExperimentalNativeApi::class)
internal actual object PlatformHook {
    @Volatile
    private var previousHook: ((Throwable) -> Unit)? = null

    internal actual fun install(options: KotlinTraceOptions) {
        if (previousHook != null) return
        previousHook = setUnhandledExceptionHook { throwable ->
            // Swallow reporter failures so only the original crash can terminate the app.
            runCatching { KotlinTraceReporter.report(throwable, KotlinTrace.installedBackends) }
            previousHook?.invoke(throwable)
        }
    }
}
