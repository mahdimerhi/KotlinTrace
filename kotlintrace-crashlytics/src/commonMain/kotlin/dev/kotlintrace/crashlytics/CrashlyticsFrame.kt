// SPDX-License-Identifier: Apache-2.0

package dev.kotlintrace.crashlytics

public data class CrashlyticsFrame(
    public val symbol: String,
    public val file: String?,
    public val line: Int?,
) {
    internal fun encode(): String =
        listOf(symbol, file.orEmpty(), line?.toString().orEmpty()).joinToString("|")
}
