// SPDX-License-Identifier: Apache-2.0

package dev.kotlintrace.crashlytics

internal expect object CrashlyticsSink {
    internal fun verifyDependencies()
    internal fun record(name: String, reason: String, framesText: String)
}
