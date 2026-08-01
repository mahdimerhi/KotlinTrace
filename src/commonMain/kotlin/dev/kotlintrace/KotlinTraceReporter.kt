// SPDX-License-Identifier: Apache-2.0

package dev.kotlintrace

import kotlin.concurrent.Volatile

internal object KotlinTraceReporter {
    @Volatile
    private var reporters: Map<Backend, (Throwable) -> Unit> = emptyMap()

    fun register(backend: Backend, reporter: (Throwable) -> Unit) {
        reporters = reporters + (backend to reporter)
    }

    fun report(throwable: Throwable, backends: Set<Backend>) {
        for (backend in backends) {
            reporters[backend]?.invoke(throwable)
        }
    }
}
