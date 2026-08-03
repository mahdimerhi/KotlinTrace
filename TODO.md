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
- [x] **Simulator source info (KT-75992)** (commit `f1bba4d`)
  - Simulator's CoreSymbolication backend never resolves `(File.kt:line)`; the
    runtime's source-info dispatcher (`Kotlin_getSourceInfo_Function`) is now
    redirected to the bundled **libbacktrace** backend via cinterop
    (`src/nativeInterop/cinterop/sourceinfohook.{def,h}`,
    `SourceInfoHook.apple.kt`), installed from `PlatformHook.install()`,
    compile-time gated to simulators only (`TARGET_OS_SIMULATOR`)
  - Prerequisite (app side): embed the dSYM in the app bundle as
    `<App>/<App>.dSYM/Contents/Resources/DWARF/<App>` — libbacktrace's
    path-based dSYM lookup (Xcode's ld64 strips `__debug_*` from the app
    binary regardless of `DEBUG_INFORMATION_FORMAT`; verified empirically)
  - Verified on iOS 27.0 sim: normal-flow `printStackTrace()` and uncaught
    exception both resolve real file:line, incl. Swift frames
- [x] **Sample app (roadmap 2.4)** (commit `dbc72bb`) — proves the adapter works
  - `:sample:shared` KMP module (jvm + iOS, static `SampleShared.framework`)
    with `CrashBot` (`setupCrashlytics()` wraps `KotlinTrace.installCrashlytics()`;
    `triggerCrash()` throws on a Kotlin/Native thread so the hook fires) +
    `CrashBotTest`
  - SwiftUI app in `sample/ios/` with two buttons — "Crash (demangled via
    KotlinTrace)" and "Crash (raw)"; `GoogleService-Info.plist` gitignored;
    Xcode project hand-created (KGP embed phase, Crashlytics upload phase,
    dSYM-embed phase for the libbacktrace hook)
  - Firebase project + iOS app (bundle id `dev.kotlintrace.sample`) +
    `GoogleService-Info.plist` in place
  - Evidence captured in Crashlytics dashboard: mangled `kfun:` fatal report
    vs readable non-fatal recorded exception `IllegalStateException` with
    demangled names + `(CrashBot.kt:NN)` frames
  - **Acceptance met**: readable Kotlin frames (function / file / line)
    visible in a Crashlytics report

## Next up
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
- iOS *test* tasks are intentionally disabled (sample evidence runs on the
  iOS 27.0 simulator instead); re-enabling is a separate decision

## Environment notes

- iOS 27.0 simulator runtime installed (device `84A8EC82-8361-4BEC-86FA-234A2393DA2E`);
  iOS K/N *test* tasks stay disabled by choice, see Verification commands
- CocoaPods 1.16.2 installed (needed only if the sample app or a future
  adapter uses the cocoapods plugin)
- **xcodegen NOT installed** → sample app Xcode project is hand-created in
  Xcode (see PROJECT.md "Sample app" section for the exact steps)
- Firebase Crashlytics integration of the adapter requires NO Firebase
  dependency at build time (runtime lookup) — only the app itself must link it
