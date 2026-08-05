# KotlinTrace

Readable Kotlin/Native crash stack traces for Firebase Crashlytics, Sentry, and Bugsnag.

Kotlin Multiplatform apps crash in shared Kotlin code on iOS, but the reports shown in
major crash-reporting vendors are unreadable (raw `Kotlin_`-mangled symbols, no source
file/line). KotlinTrace fixes the readability layer client-side — no backend required.

## Status

Crashlytics, Sentry, and Bugsnag adapters shipped: Kotlin/Native uncaught-exception hook, client-side
frame demangling, and real source `(File.kt:line)` on device and simulator — proven
end-to-end in the sample app (see [PROJECT.md](PROJECT.md)).

## Usage

### 1. Add the adapter module

```kotlin
// root build.gradle.kts — one dependency per vendor you use
implementation(project(":kotlintrace-crashlytics")) // Firebase Crashlytics
implementation(project(":kotlintrace-sentry"))      // Sentry
```

### 2. Call it once, at app startup, on iOS

The uncaught-exception hook must be installed **before any Kotlin code can crash** —
call it as early as possible in your app's launch path. One call is enough:

```kotlin
// Shared Kotlin, e.g. called from your SwiftUI App.init() / AppDelegate:
KotlinTrace.installCrashlytics()   // extension from :kotlintrace-crashlytics
```

or from Swift, via a small wrapper in your shared module (as the sample does):

```swift
// KotlinTraceSampleApp.swift
@main
struct MyApp: App {
    init() {
        CrashBot.shared.setupCrashlytics() // wraps KotlinTrace.installCrashlytics()
    }
    // ...
}
```

That single call registers the Crashlytics reporter, installs the hook exactly once
(calls are idempotent — repeated or multi-adapter installs merge safely), and fails
fast at startup with a clear error if Firebase Crashlytics is not linked. The Sentry
adapter works the same way: `KotlinTrace.installSentry()` (it expects sentry-cocoa to
be linked — see PROJECT.md for the `-ObjC` requirement with the static SPM product).

### 3. Options

```kotlin
KotlinTrace.installCrashlytics(
    demangle = true,                      // demangle Kotlin/Native symbol names
    includeKotlinSourceLocation = true,   // keep (File.kt:line) in frames
)
```

### Low-level core API (usually not needed)

`KotlinTrace.install(KotlinTraceOptions(backends = setOf(Backend.CRASHLYTICS)))` only
installs the hook; it does **not** register any vendor reporter. The per-adapter
extensions above (`installCrashlytics()`, `installSentry()`, and the future
`installBugsnag()`) do both — use them. Multiple adapters = call each extension once.

## License

Apache-2.0 — see [LICENSE](LICENSE). Copyright (c) 2026 Mahdi Merhi.
