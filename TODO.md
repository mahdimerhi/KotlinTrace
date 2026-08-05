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

- [x] **Sentry adapter (roadmap 2.2)**
  - `:kotlintrace-sentry` module, runtime-lookup cinterop (`sentry.def`), zero
    compile-time Sentry dependency (chosen over the official KMP SDK)
  - Formatter extracted into core (`KotlinTraceReportFormatter` /
    `KotlinTraceExceptionReport` / `KotlinTraceReportFrame`) — shared by both
    sinks; `CrashlyticsBackend` uses it; tests moved to core commonTest
  - `KotlinTrace.installSentry()` entry point, fail-fast dependency check
  - Sample: Sentry SPM package + dSYM upload phase (sentry-wizard), SentrySDK
    start with `enableCrashHandler = false`, `-sentrycrash` launch arg, Sentry
    crash button; `-ObjC` linker flag (static-Sentry dead-stripping fix)
  - shim handles sentry-cocoa 9.x Swift class (`Sentry.SentrySDK`) + async
    transport (`flush:` before the process dies)
  - Evidence: `-sentrycrash` run on the iOS 27.0 sim → `captureEvent:` +
    `flush:`, envelope uploaded, `status 200`; sentry.io project `apple-ios`
    shows demangled `IllegalStateException` with `CrashBot.kt:NN` frames
  - **Acceptance met**: one KotlinTrace event per crash, demangled frames in
    Sentry

- [x] **Bugsnag adapter (roadmap 2.3)**
  - `:kotlintrace-bugsnag` module, runtime-lookup cinterop (mirrors
    Crashlytics/Sentry), `installBugsnag()` on `KotlinTrace`
  - Evidence (`-bugsnagcrash`, iOS 27.0 sim): `notifyError:block:` with
    `event.deliveryStrategy = StoreAndSend` + `unhandled = YES` — payload is
    written to `v1/events/*.json` AND upload is attempted synchronously inside
    the 1 s crash-time window; a stored event survives process death and is
    retried on the next launch (verified: file deleted only on upload success,
    leftover file from a timed-out run flushed on relaunch)
  - Captured payload: `unhandled: true`, demangled `IllegalStateException`,
    `CrashBot.kt:33` / `CrashBot.kt:29` frames, 13 frames total
  - **Acceptance met**: one KotlinTrace event per crash, demangled frames, no
    duplicate native event (Bugsnag `autoDetectErrors = false` in the sample)

- [x] **Crash report readability fixes** (device-verified iPhone X, iOS 16.7;
  Xcode 26.6. `./gradlew build` green)
  - One event per crash: hook reports then `kotlin.system.exitProcess(1)`
    instead of chaining `abort()` (previously produced a duplicate mangled
    SIGABRT via Crashlytics' mach handler — verified before/after)
  - Noise frames dropped when demangling enabled (non-`kfun:` bridge frames)
  - Runtime bootstrap frames dropped: `kotlin.*.<init>` constructors and
    `kotlin.Function0.invoke` trampolines → report leads with the first app
    frame (`CrashBot.crash`), not `kotlin.Throwable.<init>`
  - Source info enabled on device (libbacktrace hook, sim-only guard removed);
    dSYM-embed phase now covers all configurations
  - **Acceptance met**: single readable non-fatal event with demangled frames +
    `(CrashBot.kt:33)` on device; raw button still produces the native SIGABRT

- [x] **Binary-compatibility-validator + KDoc (roadmap 3.1)**
  - BCV plugin 0.18.1 applied at root only (auto-configures subprojects;
    applying per-module double-applies and fails configuring `bcv-rt-jvm-cp`);
    `ignoredProjects` = sample/shared; JVM dumps committed for `kotlintrace`,
    `kotlintrace-crashlytics`, `kotlintrace-sentry`, `kotlintrace-bugsnag`;
    `apiCheck` wired into `check`. Kotlin ABI (klib) validation not yet
    enabled — kotlinlang needs it experimental opt-in.
  - KDoc on the full public surface: `Backend`, `KotlinTrace` (+ install /
    registerBackend), `KotlinTraceOptions`, `KotlinTraceDemangler`,
    `DefaultKotlinTraceDemangler`, `KotlinTraceFrame`,
    `KotlinTraceExceptionReport`, `KotlinTraceReportFrame`,
    `KotlinTraceReportFormatter`, plus the three `install*()` adapters.

- [x] **CI (GitHub Actions)**
  - `.github/workflows/ci.yml`: on push to `main` / PR — macOS runner, JDK
    17, cached Gradle + Kotlin/Native toolchain, `./gradlew build`
    (JVM tests + `apiCheck` + all iOS targets). Sanity: the branch that
    enables CI must include the BCV+KDoc commit, otherwise `build` fails on
    the missing `.api` dumps.

- [x] **Publish to Maven Central (roadmap 3.2)** — **v0.1.0 live (2026-08-06)**
  - Coordinates: `io.github.mahdimerhi:kotlintrace[:crashlytics|sentry|bugsnag]:0.1.0`
    (+ `-jvm`, `-iosarm64`, `-iosx64`, `-iossimulatorarm64` variants), verified
    200 on `repo1.maven.org`; deployment state PUBLISHED on the portal.
  - Namespace: `dev.kotlintrace` rejected by the portal (needs `kotlintrace.dev`
    DNS TXT) → switched to `io.github.mahdimerhi` (auto-verified via GitHub
    login). Kotlin packages stay `dev.kotlintrace`.
  - `gradle/publish.gradle` (Groovy convention): maven-publish + signing +
    POM/SCM metadata + `centralStaging` repo; sources jars per target
    (`withSourcesJar(true)` — KGP 2.3 boolean), javadoc jar = Dokka HTML
    (`dokkaGenerateHtml`; Dokka's Javadoc generator doesn't support KMP).
  - Signing: 4096-bit RSA key `22F6AFEE9974A188` (revocation cert in
    `~/.gnupg/openpgp-revocs.d/`); key rides env vars
    (`ORG_GRADLE_PROJECT_signingKey` / `signingPassword` in `~/.zshrc`) —
    `gradle.properties` escaping breaks armored newlines. Public key uploaded
    to keys.openpgp.org.
  - Release flow: `./gradlew assembleCentralBundle` → `scripts/publish-central.sh`
    (needs `SONATYPE_TOKEN`). Gotchas encoded in the script: upload returns the
    deploymentId as plain text; status is a POST (GET 500s); staging dir must be
    wiped before re-assembling (`cleanCentralStaging` task) or stale
    namespace artifacts poison the bundle.

## Later (roadmap Phase 4)

- [ ] Android/JVM targets (4.1)
- [ ] Coroutine diagnostic module (4.2)
- [ ] OTel-compatible export (4.3)

---

## Verification

- `./gradlew build` (full build + `apiCheck`) and `./gradlew jvmTest`.
- iOS K/N target *test* tasks stay disabled by choice — evidence runs instead on
  real iOS builds (simulator + device); see PROJECT.md "Sample app" / 1.7.
- Sample Xcode project is hand-created (no xcodegen) — see PROJECT.md "Sample app".
