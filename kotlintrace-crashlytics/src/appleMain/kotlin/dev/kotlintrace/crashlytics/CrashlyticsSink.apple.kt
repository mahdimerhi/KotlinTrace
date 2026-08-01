// SPDX-License-Identifier: Apache-2.0

package dev.kotlintrace.crashlytics

import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
internal actual object CrashlyticsSink {
    internal actual fun verifyDependencies() {
        FIRCheckCrashlyticsDependencies()
    }

    internal actual fun record(name: String, reason: String, framesText: String) {
        FIRRecordException(name, reason, framesText)
    }
}
