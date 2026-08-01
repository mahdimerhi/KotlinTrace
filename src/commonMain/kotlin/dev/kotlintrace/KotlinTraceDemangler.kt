// SPDX-License-Identifier: Apache-2.0

package dev.kotlintrace

public fun interface KotlinTraceDemangler {
    public fun demangle(rawFrame: String): KotlinTraceFrame?
}
