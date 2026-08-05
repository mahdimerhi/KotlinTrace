// SPDX-License-Identifier: Apache-2.0

package dev.kotlintrace.bugsnag

internal expect object BugsnagSink {
    internal fun verifyDependencies()
    internal fun record(name: String, reason: String, framesText: String)
}
