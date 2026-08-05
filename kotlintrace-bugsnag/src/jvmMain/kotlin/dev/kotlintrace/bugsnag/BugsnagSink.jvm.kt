// SPDX-License-Identifier: Apache-2.0

package dev.kotlintrace.bugsnag

internal actual object BugsnagSink {
    internal actual fun verifyDependencies() = Unit
    internal actual fun record(name: String, reason: String, framesText: String) = Unit
}
