// SPDX-License-Identifier: Apache-2.0

package dev.kotlintrace

/**
 * A crash-reporting backend that KotlinTrace can deliver readable Kotlin
 * crash reports to.
 *
 * Backends are activated by the matching `install*()` extension (for example
 * `KotlinTrace.installCrashlytics()`) and must not be constructed or
 * registered directly by consumers.
 */
public enum class Backend {
    /** Firebase Crashlytics. */
    CRASHLYTICS,

    /** Sentry (Apple SDK). */
    SENTRY,

    /** Bugsnag. */
    BUGSNAG,
}
