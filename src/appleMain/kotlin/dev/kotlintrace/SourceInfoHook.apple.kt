// SPDX-License-Identifier: Apache-2.0

package dev.kotlintrace

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.staticCFunction
import sourceinfohook.Kotlin_getSourceInfo_Function
import sourceinfohook.Kotlin_getSourceInfo_libbacktrace

// Kotlin/Native's runtime resolves Throwable stack-trace source info
// ((File.kt:line) suffixes) through the writable function pointer
// Kotlin_getSourceInfo_Function. The default CoreSymbolication backend is
// broken on simulators (KT-75992) and cannot resolve the app's own symbols on
// device (Xcode strips __debug_* from installed binaries), so we point the
// runtime at the bundled libbacktrace backend, which reads DWARF directly
// from the app binary or a dSYM next to the executable.
//
// Prerequisite: the app must ship its dSYM inside the bundle as
// <App>/<App>.dSYM/Contents/Resources/DWARF/<App> (libbacktrace's path-based
// dSYM lookup), since Xcode strips __debug_* sections from the app binary.
internal object SourceInfoHook {
    @OptIn(ExperimentalForeignApi::class)
    fun install() {
        if (Kotlin_getSourceInfo_Function != null) return
        Kotlin_getSourceInfo_Function = staticCFunction { addr, result, size ->
            Kotlin_getSourceInfo_libbacktrace(addr, result, size)
        }
    }
}
