// SPDX-License-Identifier: Apache-2.0

package dev.kotlintrace.bugsnag

import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
internal actual object BugsnagSink {
    internal actual fun verifyDependencies() {
        KotlinTraceCheckBugsnagDependencies()
    }

    internal actual fun record(name: String, reason: String, framesText: String) {
        KotlinTraceRecordBugsnagEvent(name, reason, framesText)
    }
}
