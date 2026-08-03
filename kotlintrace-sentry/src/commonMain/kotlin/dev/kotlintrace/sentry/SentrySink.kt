// SPDX-License-Identifier: Apache-2.0

package dev.kotlintrace.sentry

internal expect object SentrySink {
    internal fun verifyDependencies()
    internal fun record(name: String, reason: String, framesText: String)
}
