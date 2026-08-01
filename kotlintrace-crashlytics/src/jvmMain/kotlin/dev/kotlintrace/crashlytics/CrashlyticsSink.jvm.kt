// SPDX-License-Identifier: Apache-2.0

package dev.kotlintrace.crashlytics

internal actual object CrashlyticsSink {
    internal actual fun verifyDependencies() = Unit

    internal actual fun record(name: String, reason: String, framesText: String) = Unit
}
