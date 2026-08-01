// SPDX-License-Identifier: Apache-2.0

package dev.kotlintrace

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DefaultKotlinTraceDemanglerTest {

    private val demangler = DefaultKotlinTraceDemangler()

    @Test
    fun demanglesSimpleFrame() {
        val frame = demangler.demangle("kfun:com.example.MainKt.main() (Main.kt:3)")

        assertEquals(
            KotlinTraceFrame("com.example.MainKt", "main", "Main.kt", 3),
            frame,
        )
    }

    @Test
    fun demanglesFunctionWithArgs() {
        val frame = demangler.demangle("kfun:dev.kotlintrace.Demangler#<modules>.parse(kotlin/String, kotlin/Int) (Demangler.kt:42)")

        assertEquals(
            KotlinTraceFrame("dev.kotlintrace.Demangler", "parse", "Demangler.kt", 42),
            frame,
        )
    }

    @Test
    fun returnsNullForNonKotlinFrame() {
        assertNull(demangler.demangle("0x1234abcd 0x5678ef01"))
    }

    @Test
    fun demanglesRealRuntimeFrame() {
        val frame = demangler.demangle(
            "    at 4   main.kexe   0x10204b533   kfun:#boomInner(kotlin.Int){} + 203 (/Users/mahdi/project/main.kt:17:11)"
        )

        assertEquals(
            KotlinTraceFrame(null, "boomInner", "main.kt", 17),
            frame,
        )
    }

    @Test
    fun demanglesRealRuntimeFrameWithClass() {
        val frame = demangler.demangle(
            "    at 1   main.kexe   0x10200ce7f   kfun:kotlin.Throwable#<init>(kotlin.String?){} + 95 (/Users/mahdi/project/kotlin/Throwable.kt:30:51)"
        )

        assertEquals(
            KotlinTraceFrame("kotlin.Throwable", "<init>", "Throwable.kt", 30),
            frame,
        )
    }

    @Test
    fun returnsNullForRealRuntimeFrameWithoutSourceLocation() {
        assertNull(demangler.demangle("    at 9   dyld   0x18dad3beb   start + 6687"))
    }
}
