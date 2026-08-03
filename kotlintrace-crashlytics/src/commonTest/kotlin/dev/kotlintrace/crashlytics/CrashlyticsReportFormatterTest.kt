// SPDX-License-Identifier: Apache-2.0

package dev.kotlintrace.crashlytics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CrashlyticsReportFormatterTest {

    private val formatter = CrashlyticsReportFormatter()

    @Test
    fun formatsRealKotlinNativeTrace() {
        val trace = """
            kotlin.IllegalStateException: boom 42
                at 4   main.kexe   0x10204b533   kfun:#boomInner(kotlin.Int){} + 203 (/Users/mahdi/project/main.kt:17:11)
                at 6   main.kexe   0x10204b237   kfun:#main(){} + 295 (/Users/mahdi/project/main.kt:3:9)
        """.trimIndent()

        assertEquals(
            listOf(
                CrashlyticsFrame(symbol = "boomInner", file = "main.kt", line = 17),
                CrashlyticsFrame(symbol = "main", file = "main.kt", line = 3),
            ),
            formatter.formatStackTrace(trace),
        )
    }

    @Test
    fun nativeFramesPassThroughAsRawSymbols() {
        val trace = """
            kotlin.IllegalStateException: boom 42
                at 8   main.kexe   0x10207d163   Init_and_run_start + 99
                at 9   dyld   0x18dad3beb   start + 6687
        """.trimIndent()

        assertEquals(
            listOf(
                CrashlyticsFrame(symbol = "at 8   main.kexe   0x10207d163   Init_and_run_start + 99", file = null, line = null),
                CrashlyticsFrame(symbol = "at 9   dyld   0x18dad3beb   start + 6687", file = null, line = null),
            ),
            formatter.formatStackTrace(trace),
        )
    }

    @Test
    fun sourceLocationCanBeDisabled() {
        val formatter = CrashlyticsReportFormatter(includeSourceLocation = false)
        val trace = """
            kotlin.IllegalStateException: boom
                at 4   main.kexe   0x10204b533   kfun:#boomInner(kotlin.Int){} + 203 (/Users/mahdi/project/main.kt:17:11)
        """.trimIndent()

        assertEquals(
            listOf(CrashlyticsFrame(symbol = "boomInner", file = null, line = null)),
            formatter.formatStackTrace(trace),
        )
    }

    @Test
    fun demanglingCanBeDisabled() {
        val formatter = CrashlyticsReportFormatter(demangler = null)
        val trace = """
            kotlin.IllegalStateException: boom
                at 4   main.kexe   0x10204b533   kfun:#boomInner(kotlin.Int){} + 203 (/Users/mahdi/project/main.kt:17:11)
        """.trimIndent()

        assertEquals(
            listOf(
                CrashlyticsFrame(
                    symbol = "at 4   main.kexe   0x10204b533   kfun:#boomInner(kotlin.Int){} + 203 (/Users/mahdi/project/main.kt:17:11)",
                    file = null,
                    line = null,
                ),
            ),
            formatter.formatStackTrace(trace),
        )
    }

    @Test
    fun formatsClassFrameWithArgsAndModules() {
        val trace = """
            kotlin.IllegalStateException: boom
                at 2   main.kexe   0x10200d317   kfun:dev.kotlintrace.Demangler#<modules>.parse(kotlin/String, kotlin/Int) + 95 (/Users/mahdi/project/Demangler.kt:42:9)
        """.trimIndent()

        assertEquals(
            listOf(
                CrashlyticsFrame(symbol = "dev.kotlintrace.Demangler.parse", file = "Demangler.kt", line = 42),
            ),
            formatter.formatStackTrace(trace),
        )
    }

    @Test
    fun formatsRuntimeTraceWithoutSourceLocations() {
        val trace = """
            kotlin.IllegalStateException: KotlinTrace sample crash from shared Kotlin (CrashBot)
                at 0   KotlinTraceSample.debug.dylib       0x100d4d377        kfun:kotlin.Throwable#<init>(kotlin.String?){} + 99
                at 4   KotlinTraceSample.debug.dylib       0x100d2ca57        kfun:dev.kotlintrace.sample.CrashBot#crash(){}kotlin.Throwable + 159
                at 9   KotlinTraceSample.debug.dylib       0x100d2c747        kfun:dev.kotlintrace.sample.crashOnBackgroundThread${'$'}crashTrampoline#internal + 95
                at 11  libsystem_pthread.dylib             0x1004d267f        _pthread_start + 103
        """.trimIndent()

        assertEquals(
            listOf(
                CrashlyticsFrame(symbol = "kotlin.Throwable.<init>", file = null, line = null),
                CrashlyticsFrame(symbol = "dev.kotlintrace.sample.CrashBot.crash", file = null, line = null),
                CrashlyticsFrame(symbol = "dev.kotlintrace.sample.crashOnBackgroundThread\$crashTrampoline", file = null, line = null),
                CrashlyticsFrame(symbol = "at 11  libsystem_pthread.dylib             0x1004d267f        _pthread_start + 103", file = null, line = null),
            ),
            formatter.formatStackTrace(trace),
        )
    }

    @Test
    fun buildReportFromThrowable() {
        val throwable = IllegalStateException("boom 42")

        val report = formatter.format(throwable)

        assertEquals("IllegalStateException", report.name)
        assertEquals("boom 42", report.reason)
        val frames = report.framesText.split("\n").filter { it.isNotEmpty() }
        assertTrue(frames.isNotEmpty())
        assertTrue(frames.first().startsWith("at dev.kotlintrace"))
    }

    @Test
    fun framesEncodeSymbolFileLine() {
        assertEquals(
            "dev.kotlintrace.MainKt.main|Main.kt|3",
            CrashlyticsFrame(symbol = "dev.kotlintrace.MainKt.main", file = "Main.kt", line = 3).encode(),
        )
    }

    @Test
    fun framesEncodeWithoutFileOrLine() {
        assertEquals(
            "Init_and_run_start||",
            CrashlyticsFrame(symbol = "Init_and_run_start", file = null, line = null).encode(),
        )
    }
}
