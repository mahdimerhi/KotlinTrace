// SPDX-License-Identifier: Apache-2.0

package dev.kotlintrace.sample

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.staticCFunction
import platform.posix.pthread_create
import platform.posix.pthread_tVar

// kotlin.concurrent.thread does not exist in Kotlin/Native. A @convention(c)
// trampoline is required so the exception escapes into foreign code and the
// runtime invokes the uncaught-exception hook (verified empirically).
private var crashBlock: (() -> Unit)? = null

@OptIn(ExperimentalForeignApi::class)
private fun crashTrampoline(arg: CPointer<*>?): CPointer<*>? {
    crashBlock?.invoke()
    return null
}

@OptIn(ExperimentalForeignApi::class)
internal actual fun crashOnBackgroundThread(block: () -> Unit) {
    crashBlock = block
    memScoped {
        val thread = alloc<pthread_tVar>()
        pthread_create(thread.ptr, null, staticCFunction(::crashTrampoline), null)
    }
}
