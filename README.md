# KotlinTrace

**Readable Kotlin crash stack traces for Firebase Crashlytics, Sentry, and Bugsnag.**

Kotlin Multiplatform apps crash in shared Kotlin code on iOS, but the reports shown in
major crash-reporting vendors are unreadable — raw `kfun:`-mangled symbols, no source
file/line. KotlinTrace fixes the readability layer **client-side**, with no backend:
one call at startup, and the same crash shows demangled function names and
`(File.kt:line)` locations.

Works with the crash reporter you already use — Firebase Crashlytics, Sentry, or
Bugsnag. No new backend, no vendor lock-in.

## How it looks

**Before** (what vendors normally receive):

```
IllegalStateException: Uncaught Kotlin exception
    at 0   iosApp   0x1020e88e7   kfun:kotlin.Exception#<init>(kotlin/String?;kotlin.Throwable?){} + 119
    at 1   iosApp   0x1020d3273   kfun:dev.kotlintrace.sample.CrashBot#crash(){}kotlin.Throwable + 159
    ... and 38 more stack frames
```

**After** (with KotlinTrace):

```
IllegalStateException: Uncaught Kotlin exception
    at CrashBot.crash (CrashBot.kt:33)
    at crashOnBackgroundThread$crashTrampoline (MainKt.kt:21)
```

## Quick start

KotlinTrace is iOS-only (Kotlin/Native). Add one dependency per vendor you use, call
one function at startup — done.

### 1. Add the dependency

```kotlin
// shared module build.gradle.kts — commonMain.dependencies
implementation("io.github.mahdimerhi:kotlintrace-crashlytics:0.1.0")  // Firebase Crashlytics
implementation("io.github.mahdimerhi:kotlintrace-sentry:0.1.0")       // Sentry
implementation("io.github.mahdimerhi:kotlintrace-bugsnag:0.1.0")      // Bugsnag
```

Add only the modules whose SDKs you already link. All three work side by side if
you use multiple reporters.

### 2. Call it once at app startup

Install the hook **before any Kotlin code can crash** — the earlier, the better.
One call is enough; repeated calls and multi-adapter installs merge safely.

```kotlin
// Shared Kotlin, e.g. called from your SwiftUI App.init()
fun setupCrashReporting() {
    KotlinTrace.installCrashlytics() // or installSentry() / installBugsnag()
}
```

From Swift, call it through a tiny wrapper in your shared module:

```swift
@main
struct MyApp: App {
    init() {
        setupCrashReporting() // → KotlinTrace.installCrashlytics()
    }
}
```

### 3. Verify

Crash the app in shared Kotlin code (uncaught exception), relaunch, and check your
Crashlytics / Sentry / Bugsnag dashboard: the event now shows demangled Kotlin
functions with `(File.kt:line)`.

## Options

```kotlin
KotlinTrace.installSentry(
    demangle = true,                      // default: true
    includeKotlinSourceLocation = true,   // default: true
)
// same parameters on installCrashlytics() and installBugsnag()
```

## How it works

- **Client-side rewrite.** KotlinTrace rewrites the Kotlin stack-trace frames before
  the event reaches your vendor's native crash API. No backend involved.
- **One clean event per crash.** It reports once, flushes the transport, then exits —
  so the OS-native abort (which would add a second, mangled event) never fires.
- **Friendly noise dropped.** Runtime bootstrap frames (K/N runtime init, trampolines)
  and non-Kotlin bridge frames are removed from the report.
- **`(File.kt:line)`** resolves via the K/N runtime's DWARF capabilities on device
  and simulator. Release builds without debug info fall back to function names only.

## Adapters

| Module | Entry point | Vendor SDK |
|---|---|---|
| `:kotlintrace-crashlytics` | `KotlinTrace.installCrashlytics()` | Firebase Crashlytics |
| `:kotlintrace-sentry` | `KotlinTrace.installSentry()` | sentry-cocoa |
| `:kotlintrace-bugsnag` | `KotlinTrace.installBugsnag()` | bugsnag-cocoa |

The core engine (`:kotlintrace`) holds the uncaught-exception hook + demangler; the
adapters wire the vendors. The core's low-level API (`KotlinTrace.install(...)` +
`KotlinTraceOptions`) is there if you want to write your own sink — most apps just
use the `install*()` extensions above.

## Requirements

- iOS app (Kotlin/Native targets: iosArm64, iosSimulatorArm64, iosX64)
- Kotlin 2.x Multiplatform project
- The vendor SDK linked in the app as usual (Firebase Crashlytics / sentry-cocoa /
  bugsnag-cocoa)
- Sentry apps: disable Sentry's own crash handler so it doesn't add a duplicate
  mangled native event (`options.enableCrashHandler = false`)

## Docs

- [PROJECT.md](PROJECT.md) — architecture, roadmap, integration details
- [TODO.md](TODO.md) — release checklist

## License

Apache-2.0 — see [LICENSE](LICENSE). Copyright (c) 2026 Mahdi Merhi.
