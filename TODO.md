# KotlinTrace — TODO / Work Checklist

> Living task list. PROJECT.md holds the architecture + full roadmap;
> this file tracks what is in flight and what comes next, per feature.
> Rule: one feature per commit, only the user commits and pushes.

## Legend
- `[x]` done and committed
- `[ ]` next up / planned

---

## Done

- [x] **Phase 0/1 — Repo + skeleton** (commit `91e3b40`)
  - Gradle 9.6.1 + Kotlin 2.3.21, version catalogs, jvm + 3 iOS targets
  - Core API: `KotlinTrace.install()`, `KotlinTraceOptions`, `Backend`,
    `KotlinTraceFrame`, demangler interface + default impl
  - `PlatformHook` expect/actual seam
- [x] **Phase 2.1 — Crashlytics adapter** (commit `acb6495`)
  - `:kotlintrace-crashlytics` module, runtime-lookup cinterop (`crashlytics.def`,
    zero Firebase compile-time dependency)
  - Real `setUnhandledExceptionHook` wiring in core, `KotlinTraceReporter` dispatch,
    `KotlinTrace.installCrashlytics()` entry point
  - Demangler upgraded to the real K/N frame format (verified empirically)
  - 17 tests green on JVM; all iOS targets compile + link

## Next up

- [ ] **Sample app (roadmap 2.4) — recommended next: proves the adapter works**
  - Small iOS app (Xcode project or KMP shared framework) with a "Crash" button
    that throws in shared Kotlin code
  - Run twice: once with `kotlintrace-crashlytics` linked, once without
  - Capture before/after evidence: mangled report vs demangled report in
    Crashlytics dashboard
  - Needs: Firebase project + `GoogleService-Info.plist`, iOS simulator
    (runtime is downloading), dSYM upload for the non-Kotlin frames
  - Acceptance: readable Kotlin frames (function / file / line) visible in a
    Crashlytics report
- [ ] **Sentry adapter (roadmap 2.2)**
  - Reuses core pipeline (formatter + reporter); only the sink changes
  - Research first: Sentry KMP SDK (getsentry/sentry-kotlin-multiplatform) vs
    runtime-lookup cinterop like Crashlytics
- [ ] **Bugsnag adapter (roadmap 2.3)** — lowest priority

## Later (roadmap Phase 3/4)

- [ ] CI (GitHub Actions: build, test, publish snapshot)
- [ ] Binary-compatibility-validator + KDoc (3.1)
- [ ] Publish pipeline to Maven Central (3.2)
- [ ] Android/JVM targets (4.1)
- [ ] Coroutine diagnostic module (4.2)
- [ ] OTel-compatible export (4.3)

---

## Verification commands (this Mac)

- `./gradlew build` — full build (JVM tests + all iOS targets compile/link)
- `./gradlew jvmTest` — pure-logic tests (JVM host)
- iOS *test* tasks are intentionally disabled (no simulator runtime installed);
  do not re-enable

## Environment notes

- No iOS simulator runtime → iOS tests can't execute here yet (user is
  downloading it; SDKs 27.0 already present)
- CocoaPods 1.16.2 installed (needed only if the sample app or a future
  adapter uses the cocoapods plugin)
- Firebase Crashlytics integration of the adapter requires NO Firebase
  dependency at build time (runtime lookup) — only the app itself must link it
