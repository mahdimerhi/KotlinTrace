// SPDX-License-Identifier: Apache-2.0

package dev.kotlintrace

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KotlinTraceReportFormatterTest {

    private val formatter = KotlinTraceReportFormatter()

    @Test
    fun formatsRealKotlinNativeTrace() {
        val trace = """
            kotlin.IllegalStateException: boom 42
                at 4   main.kexe   0x10204b533   kfun:#boomInner(kotlin.Int){} + 203 (/Users/mahdi/project/main.kt:17:11)
                at 6   main.kexe   0x10204b237   kfun:#main(){} + 295 (/Users/mahdi/project/main.kt:3:9)
        """.trimIndent()

        assertEquals(
            listOf(
                KotlinTraceReportFrame(symbol = "boomInner", file = "main.kt", line = 17),
                KotlinTraceReportFrame(symbol = "main", file = "main.kt", line = 3),
            ),
            formatter.formatStackTrace(trace),
        )
    }

    @Test
    fun nativeFramesAreDroppedWhenDemanglingIsEnabled() {
        val trace = """
            kotlin.IllegalStateException: boom 42
                at 8   main.kexe   0x10207d163   Init_and_run_start + 99
                at 9   dyld   0x18dad3beb   start + 6687
        """.trimIndent()

        assertEquals(emptyList(), formatter.formatStackTrace(trace))
    }

    @Test
    fun sourceLocationCanBeDisabled() {
        val formatter = KotlinTraceReportFormatter(includeSourceLocation = false)
        val trace = """
            kotlin.IllegalStateException: boom
                at 4   main.kexe   0x10204b533   kfun:#boomInner(kotlin.Int){} + 203 (/Users/mahdi/project/main.kt:17:11)
        """.trimIndent()

        assertEquals(
            listOf(KotlinTraceReportFrame(symbol = "boomInner", file = null, line = null)),
            formatter.formatStackTrace(trace),
        )
    }

    @Test
    fun demanglingCanBeDisabled() {
        val formatter = KotlinTraceReportFormatter(demangler = null)
        val trace = """
            kotlin.IllegalStateException: boom
                at 4   main.kexe   0x10204b533   kfun:#boomInner(kotlin.Int){} + 203 (/Users/mahdi/project/main.kt:17:11)
        """.trimIndent()

        assertEquals(
            listOf(
                KotlinTraceReportFrame(
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
                KotlinTraceReportFrame(symbol = "dev.kotlintrace.Demangler.parse", file = "Demangler.kt", line = 42),
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
                KotlinTraceReportFrame(symbol = "dev.kotlintrace.sample.CrashBot.crash", file = null, line = null),
                KotlinTraceReportFrame(symbol = "dev.kotlintrace.sample.crashOnBackgroundThread\$crashTrampoline", file = null, line = null),
            ),
            formatter.formatStackTrace(trace),
        )
    }

    @Test
    fun bootstrapFramesAreDroppedWhenDemanglingIsEnabled() {
        val trace = """
            kotlin.IllegalStateException: boom
                at 0   KotlinTraceSample.debug.dylib       0x100d4d377        kfun:kotlin.Throwable#<init>(kotlin.String?){} + 99
                at 1   KotlinTraceSample.debug.dylib       0x100d4d400        kfun:kotlin.Exception#<init>(kotlin.String?){} + 80
                at 2   KotlinTraceSample.debug.dylib       0x100d4d488        kfun:kotlin.IllegalStateException#<init>(kotlin.String?){} + 61
                at 3   KotlinTraceSample.debug.dylib       0x100d4d4f0        kfun:kotlin.Function0#invoke(){}1:0-trampoline + 92
                at 4   KotlinTraceSample.debug.dylib       0x100d2ca57        kfun:dev.kotlintrace.sample.CrashBot#crash(){}kotlin.Throwable + 159
        """.trimIndent()

        assertEquals(
            listOf(
                KotlinTraceReportFrame(symbol = "dev.kotlintrace.sample.CrashBot.crash", file = null, line = null),
            ),
            formatter.formatStackTrace(trace),
        )
    }

    @Test
    fun appConstructorFramesAreKept() {
        val trace = """
            kotlin.IllegalStateException: boom
                at 0   KotlinTraceSample.debug.dylib       0x100d4d377        kfun:dev.kotlintrace.sample.CrashBot#<init>(){} + 33
        """.trimIndent()

        assertEquals(
            listOf(
                KotlinTraceReportFrame(symbol = "dev.kotlintrace.sample.CrashBot.<init>", file = null, line = null),
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
        // A JVM stack trace contains no kfun: frames, and the default
        // demangler drops non-demangled frames, so the report is empty here.
        assertEquals(emptyList(), report.frames)
    }

    @Test
    fun rawFormatterPassesThroughJvmFrames() {
        val throwable = IllegalStateException("boom 42")
        val report = KotlinTraceReportFormatter(demangler = null).format(throwable)

        val frames = report.framesText.split("\n").filter { it.isNotEmpty() }
        assertTrue(frames.isNotEmpty())
        assertTrue(frames.first().startsWith("at dev.kotlintrace"))
    }

    @Test
    fun framesEncodeSymbolFileLine() {
        assertEquals(
            "dev.kotlintrace.MainKt.main|Main.kt|3",
            KotlinTraceExceptionReport(
                name = "x",
                reason = "y",
                frames = listOf(KotlinTraceReportFrame(symbol = "dev.kotlintrace.MainKt.main", file = "Main.kt", line = 3)),
            ).framesText,
        )
    }

    @Test
    fun framesEncodeWithoutFileOrLine() {
        assertEquals(
            "Init_and_run_start||",
            KotlinTraceExceptionReport(
                name = "x",
                reason = "y",
                frames = listOf(KotlinTraceReportFrame(symbol = "Init_and_run_start", file = null, line = null)),
            ).framesText,
        )
    }
}
