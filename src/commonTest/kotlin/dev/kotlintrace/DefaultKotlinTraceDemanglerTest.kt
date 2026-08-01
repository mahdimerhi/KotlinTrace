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
}
