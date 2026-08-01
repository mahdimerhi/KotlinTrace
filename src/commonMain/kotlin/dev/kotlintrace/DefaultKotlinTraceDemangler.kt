// SPDX-License-Identifier: Apache-2.0

package dev.kotlintrace

public class DefaultKotlinTraceDemangler : KotlinTraceDemangler {

    private val frameRegex = Regex(
        """^kfun:(.+)\.([^()\s]+)\([^)]*\)\s*\((.*):(\d+)\)$"""
    )

    override fun demangle(rawFrame: String): KotlinTraceFrame? {
        val match = frameRegex.matchEntire(rawFrame) ?: return null
        val declaringClass = match.groupValues[1]
            .substringBeforeLast("#<modules>")
            .trimEnd('.')
        return KotlinTraceFrame(
            declaringClass = declaringClass,
            functionName = match.groupValues[2],
            fileName = match.groupValues[3].substringAfterLast('/'),
            lineNumber = match.groupValues[4].toIntOrNull(),
        )
    }
}
