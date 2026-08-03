// SPDX-License-Identifier: Apache-2.0

package dev.kotlintrace

public class DefaultKotlinTraceDemangler : KotlinTraceDemangler {

    // Real Kotlin/Native runtime format, e.g.:
    // "    at 4   main.kexe   0x10204b533   kfun:#boomInner(kotlin.Int){} + 203 (/abs/path/main.kt:17:11)"
    // kfun symbols may contain spaces inside argument lists, e.g. "kfun:a.B#b(kotlin/String, kotlin/Int)".
    private val runtimeFrameRegex = Regex(
        """^at\s+\d+\s+\S+\s+0x[0-9a-fA-F]+\s+(kfun:(?:[^()\s]|\([^)]*\))+)(?:\s+\+\s*\d+)?(?:\s+\((.*):(\d+):(\d+)\))?\s*$"""
    )

    // Plain "kfun:..." line without the runtime prefix, kept because older
    // Kotlin/Native toolchains and some vendor reports print this shape:
    // "kfun:com.example.MainKt.main() (Main.kt:3)"
    private val plainFrameRegex = Regex(
        """^kfun:(.+)\.([^()\s]+)\([^)]*\)\s*\((.*):(\d+)\)$"""
    )

    private val visibilitySuffixes = setOf("internal", "external", "private", "protected")

    override fun demangle(rawFrame: String): KotlinTraceFrame? {
        val frame = rawFrame.trim()

        runtimeFrameRegex.matchEntire(frame)?.let { match ->
            val (declaringClass, functionName) = parseSymbol(match.groupValues[1]) ?: return null
            val location = match.groupValues[2].ifEmpty { null }
            return KotlinTraceFrame(
                declaringClass = declaringClass,
                functionName = functionName,
                fileName = location?.substringAfterLast('/'),
                lineNumber = location?.let { match.groupValues[3].toIntOrNull() },
            )
        }

        val plain = plainFrameRegex.matchEntire(frame) ?: return null
        return KotlinTraceFrame(
            declaringClass = plain.groupValues[1].substringBeforeLast("#<modules>").trimEnd('.'),
            functionName = plain.groupValues[2],
            fileName = plain.groupValues[3].substringAfterLast('/'),
            lineNumber = plain.groupValues[4].toIntOrNull(),
        )
    }

    private fun parseSymbol(symbol: String): Pair<String?, String>? {
        val body = symbol.removePrefix("kfun:").substringBefore('(')
        val hashIndex = body.indexOf('#')
        if (hashIndex < 0) {
            val dotIndex = body.lastIndexOf('.')
            if (dotIndex <= 0) return null
            return body.substring(0, dotIndex) to body.substring(dotIndex + 1)
        }
        val tail = body.substring(hashIndex + 1)
        if (tail in visibilitySuffixes) {
            val dotIndex = body.lastIndexOf('.', hashIndex - 1)
            if (dotIndex <= 0) return null
            return body.substring(0, dotIndex) to body.substring(dotIndex + 1, hashIndex)
        }
        val declaringClass = body.substring(0, hashIndex).ifEmpty { null }
        return declaringClass to tail.substringAfter('.')
    }
}
