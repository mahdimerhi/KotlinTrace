// SPDX-License-Identifier: Apache-2.0

package dev.kotlintrace.crashlytics

public data class CrashlyticsExceptionReport(
    public val name: String,
    public val reason: String,
    public val framesText: String,
)
