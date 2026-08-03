// SPDX-License-Identifier: Apache-2.0

package dev.kotlintrace.sentry

import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
internal actual object SentrySink {
    internal actual fun verifyDependencies() {
        KotlinTraceCheckSentryDependencies()
    }

    internal actual fun record(name: String, reason: String, framesText: String) {
        KotlinTraceRecordSentryEvent(name, reason, framesText)
    }
}
