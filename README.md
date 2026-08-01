# KotlinTrace

Readable Kotlin/Native crash stack traces for Firebase Crashlytics, Sentry, and Bugsnag.

Kotlin Multiplatform apps crash in shared Kotlin code on iOS, but the reports shown in
major crash-reporting vendors are unreadable (raw `Kotlin_`-mangled symbols, no source
file/line). KotlinTrace fixes the readability layer client-side — no backend required.

## Status

Skeleton phase. See [PROJECT.md](PROJECT.md) for the full roadmap and validated
market research.

## Usage

```kotlin
KotlinTrace.install(
    KotlinTraceOptions(
        backends = setOf(Backend.CRASHLYTICS, Backend.SENTRY),
    )
)
```

## License

Apache-2.0 — see [LICENSE](LICENSE). Copyright (c) 2026 Mahdi Merhi.
