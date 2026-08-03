# KotlinTrace — KMP Crash Trace Readability SDK (working title)

> Planning document + task list for building a KMP library that makes Kotlin/Native
> crash stack traces readable in Firebase Crashlytics, Sentry, and Bugsnag.

---

## 1. Problem Statement

Kotlin Multiplatform (KMP) apps crash in shared Kotlin code on iOS, but the crash
reports shown in Firebase Crashlytics / Sentry / Bugsnag are unreadable (raw
`Kotlin_`-mangled symbols, no source file/line). Native Swift crashes are fine.
The readability layer is missing from every major crash-reporting vendor.

## 2. Validated Market Evidence (as of 2026-08-01)

| Source | Status | Detail |
|---|---|---|
| [firebase/firebase-ios-sdk#15512](https://github.com/firebase/firebase-ios-sdk/issues/15512) | **Open**, no assignee, ~8 mo | Kotlin crash on iOS = unreadable stack trace. Reporter tried NSExceptionKt + CrashKiOS, both "don't work (probably outdated libs)". Firebase labels it "feature request", no commitment. |
| [getsentry/sentry-kotlin-multiplatform#476](https://github.com/getsentry/sentry-kotlin-multiplatform/issues/476) | **Closed** via PR #554 (0.27.0, Jun 2 2026) | Fixed a *narrow* CMP 1.9.x symptom: Compose crashes routed through Cocoa's C++ monitor show `ExceptionObjHolderImpl`. Added **opt-in** `enableUnhandledCppExceptionMonitoring = false`. Not general symbolication; default preserves old behavior. |
| [touchlab/CrashKiOS](https://github.com/touchlab/CrashKiOS) | Effectively unmaintained | 338★, 14 open issues. #88 Kotlin 2.4.0 unsupported (open since Jun 5 2026). #84 newest versions **not on Maven Central** (open since Sep 28 2025 → users frozen on old artifacts). #85 `setUnhandledExceptionHook` broken on current Kotlin. |
| bugsnag/bugsnag-kotlin-multiplatform | Immature | ~8★, 0 forks. |
| [Kotzilla](https://blog.kotzilla.io/the-best-observability-tools-for-kotlin-multiplatform-in-2026) | Beta | iOS crash reporting beta; coupled to Koin. |
| [Luciq KMP SDK](https://www.luciq.ai/blog/kotlin-multiplatform-mobile-observability-sdk) (Instabug) | Productized | Symbolicated Kotlin traces + session replay + bug reporting. **Closed source**, heavy commercial suite. |
| [measure.sh](https://measure.sh/) | Open source | Self-hosted mobile monitoring backend, Adaptive Capture. Backend-focused; not the client readability layer. |
| OpenTelemetry Kotlin SDK (Embrace, 2026-03-24) | Accepted | Standardizes observability *plumbing*, not crash trace readability. |

### Key conclusions
- Everyone builds **capture/backend**; nobody maintains the **client-side Kotlin-trace readability layer** across vendors.
- Incumbents are either ignoring it (Firebase), patching one symptom at a time (Sentry), or stale (CrashKiOS).
- The differentiator = **breadth**: one maintained SDK that fixes readability for Firebase + Sentry + Bugsnag simultaneously, which no incumbent provides.

## 3. How It Works (core insight)

Vendors already capture crashes. The gap is the **format of the trace sent to them**.

- Client SDK (`appleMain`): install Kotlin/Native uncaught-exception hook, demangle
  `Kotlin_`-prefixed symbols, normalize Kotlin/Native frame metadata (file/line),
  and feed a *cleaned* report into the vendor's native API.
- No backend required for the baseline fix. True address→symbol resolution via dSYM
  remains the vendor's job; we fix the Kotlin frame layer client-side.

## 4. Architecture

```
┌──────────────────────────────────────────────────────┐
│  commonMain: KotlinTrace                              │
│  KotlinTrace.install(options)      ← single entry     │
│  ├─ Kotlin/Native uncaught-exception hook             │
│  └─ Kotlin frame demangling/normalization             │
└──────────┬────────────────────────────────────────────┘
           │
   ┌───────┼───────────┐
   ▼       ▼           ▼
crashlytics  sentry   bugsnag   (native cinterop / vendor adapters)
module     module     module
```

## 5. Proposed API

```kotlin
// commonMain
KotlinTrace.install(
    KotlinTraceOptions(
        backends = setOf(Backend.Crashlytics, Backend.Sentry, Backend.Bugsnag),
        demangle = true,
        includeKotlinSourceLocation = true,
        // future: coroutine diagnostic module hooks
    )
)
```

### Adapter usage (actual, as built)

```kotlin
// App startup (iOS). One call per adapter module the app depends on.
KotlinTrace.installCrashlytics() // extension in :kotlintrace-crashlytics
```

- `KotlinTrace.install()` merges `backends` across calls and installs the
  Kotlin/Native uncaught-exception hook exactly once (hook ownership stays in core).
- Adapter modules register a reporter via `KotlinTrace.registerBackend(...)` and
  call `KotlinTrace.install(...)` themselves, so `installCrashlytics()` alone is
  enough for the common single-backend case; combining backends works because
  installs merge.
- `installCrashlytics()` fails fast at startup if Firebase Crashlytics is not
  linked (clear NSException via `FIRCheckCrashlyticsDependencies()`).

## 6. MVP Scope (v0.1)

- [x] 1. Common `install()` + `KotlinTraceOptions`
- [x] 2. Crashlytics adapter: rewrite Kotlin traces client-side
- [ ] 3. Sentry adapter: rewrite Kotlin traces client-side
- [ ] 4. Sample CMP app that throws in shared Kotlin → before/after evidence
- [ ] 5. CI: Kotlin/Native test asserting demangled output

## 7. Non-Goals (v0.1)

- No custom crash-capture/backend (vendors already capture).
- No dSYM/address symbolication (vendor-side job).
- No session replay / APM / breadcrumbs.
- No Android/JVM targets yet (single-platform iOS-focused MVP).

---

# ROADMAP

## Phase 0 — Repo & Tooling (current)

- [x] 0.3 Init git locally, sensible `.gitignore` (Gradle/Kotlin/Xcode/build dirs)
- [x] 0.4 Add `README.md` (short, public-facing) + `LICENSE` (Apache-2.0)
- [x] 0.5 Confirm Java version (21) and Gradle version for KMP (9.6.1 wrapper, Kotlin 2.3.21)
- [ ] 0.1 Decide repo name, visibility (public/private), GitHub org/owner
- [ ] 0.2 Create repo on GitHub (web UI) — **user creates at push time**

## Phase 1 — Library Skeleton (buildable, testable)

- [x] 1.1 Gradle wrapper + `settings.gradle.kts` (version catalogs)
- [x] 1.2 Root `build.gradle.kts` (KMP plugin applied, single-module root)
- [x] 1.3 Targets: iosArm64, iosSimulatorArm64, iosX64, jvm (jvm for host tests)
- [x] 1.4 `commonMain`: `KotlinTrace`, `KotlinTraceOptions`, `Backend` enum,
       `KotlinTraceFrame` + `KotlinTraceDemangler` interface + `DefaultKotlinTraceDemangler`
- [x] 1.5 `appleMain` + `jvmMain`: `PlatformHook` expect/actual seam (real
       `setUnhandledExceptionHook` wiring deferred to Phase 2)
- [x] 1.6 `commonTest`: demangler unit tests (run on JVM host)
- [x] 1.7 Verify: `./gradlew build` passes (JVM tests green; iOS targets compile;
       iOS simulator tests disabled on this host — no simulator SDK installed)
- [ ] 1.8 **Hand-off: user reviews, commits, pushes to GitHub**

## Phase 2 — Vendor Adapters (MVP features)

- [x] 2.1 Crashlytics adapter module (`:kotlintrace-crashlytics`), cinterop to
       `FirebaseCrashlytics`, rewrite trace before upload
- [ ] 2.2 Sentry adapter module (`:kotlintrace-sentry`)
- [ ] 2.3 Bugsnag adapter module (`:kotlintrace-bugsnag`) *(lowest priority)*
- [ ] 2.4 Sample CMP iOS app with a crash button; run it twice — once with the
       adapter linked, once without — and capture before/after screenshots of
       the Crashlytics report (mangled vs demangled frames). This is the
       user-visible proof-of-value feature; **in progress (user decided to do it
       before 2.2)**.
- [ ] 2.5 **Hand-off per feature: user commits**

### Phase 2 decisions made (2026-08-01, Crashlytics first)

- **Fatal semantics**: uncaught Kotlin exceptions are recorded via the **public**
  `recordExceptionModel:` API (the path Firebase's own engineers recommend in
  firebase-ios-sdk#15512). Tradeoff accepted for v0.1: the readable report shows
  as a *non-fatal* "recorded exception" in Crashlytics, and the app still aborts
  (K/N runtime behavior), so the native crash also appears as an unreadable
  fatal. The CrashKiOS-style private `FIRCLSExceptionRecordNSException` path
  (fatal + persisted, but address-only → needs dSYMs, brittle) is explicitly
  **not** used.
- **No compile-time Firebase dependency**: `crashlytics.def` resolves
  `FIRCrashlytics`/`FIRExceptionModel`/`FIRStackFrame` at runtime
  (`NSClassFromString` + `methodForSelector`, pattern from CrashKiOS, Apache-2.0).
  Works with SPM, CocoaPods, or manual Firebase integration; no linker flags needed.
- **Hook ownership stays in core**: exactly one `setUnhandledExceptionHook`
  call, in `PlatformHook.apple.kt`; adapters only register reporters
  (`KotlinTrace.registerBackend`). Previous hook is chained after reporting.
- **Demangler upgraded to the real K/N frame format**: verified empirically
  (kotlinc-native 2.3.21) that `stackTraceToString()` prints
  `at <i> <lib> <addr> kfun:...#fn(args){} + <off> (/abs/path/File.kt:line:col)`
  with absolute paths, `line:column`, and `#`-prefixed top-level/constructor
  names. The old simplified format is still accepted. `(File.kt:line)` only
  appears in builds with debug info (`-g`); release builds have no file/line.
- **Simulator source info (KT-75992)**: on iOS simulators the runtime's default
  CoreSymbolication backend never resolves `(File.kt:line)` (no symbolication
  session is available). Fix, shipped in core `appleMain`:
  - cinterop (`src/nativeInterop/cinterop/sourceinfohook.{def,h}`) exposes the
    runtime's writable dispatcher `Kotlin_getSourceInfo_Function` and the
    bundled libbacktrace backend `Kotlin_getSourceInfo_libbacktrace`
    (libbacktrace is K/N's default on macOS/desktop and reads DWARF itself).
  - `SourceInfoHook.install()` (called from `PlatformHook.install()`) points the
    dispatcher at the libbacktrace backend. Compile-time gated to simulators
    only (`TARGET_OS_SIMULATOR` in the header); device builds keep the working
    CoreSymbolication backend.
  - **App-side prerequisite**: ld64 strips `__debug_*` from app binaries no
    matter the `DEBUG_INFORMATION_FORMAT` (verified empirically), so the app
    must ship its dSYM inside the bundle as
    `<App>/<App>.dSYM/Contents/Resources/DWARF/<App>` — libbacktrace's
    path-based dSYM lookup (`macho_add_dsym`). The sample's Xcode project has a
    build phase that copies `$(DWARF_DSYM_FOLDER_PATH)/<App>.app.dSYM` into the
    bundle. Without it, simulators show no file:line (same as before the fix).
  - Verified on the iOS 27.0 simulator: `printStackTrace()` and uncaught
    exception both resolve real file:line for Kotlin frames (and Swift frames).

### Sample app (roadmap 2.4) — current

Built in-repo (uncommitted):
- `:sample:shared` KMP module (jvm + 3 iOS targets, static `SampleShared.framework`)
  with `CrashBot`: `setupCrashlytics()` wraps `KotlinTrace.installCrashlytics()`;
  `triggerCrash()` throws on a Kotlin/Native thread so the uncaught-exception
  hook fires (an exception crossing the Swift boundary becomes an NSException
  and bypasses the hook).
- `sample/ios/`: SwiftUI app (`KotlinTraceSampleApp.swift`, `ContentView.swift`)
  with two buttons:
  - "Crash (demangled via KotlinTrace)" → `setupCrashlytics()` + `triggerCrash()`
  - "Crash (raw, no KotlinTrace)" → `triggerCrash()` only
  One build, two reports: same app binary always links the adapter; the "raw"
  run just skips the install call.
- `GoogleService-Info.plist` is gitignored (secret).

User-side steps (must be done manually — no xcodegen, so the Xcode project is
hand-created in Xcode; the `.pbxproj` is too fragile to generate):
1. Firebase console: create a project → add an iOS app with bundle id
   `dev.kotlintrace.sample` → download `GoogleService-Info.plist` into
   `sample/ios/` (gitignored). The app calls `FirebaseApp.configure()` at
   launch (`KotlinTraceSampleApp.swift`), so the plist must be added to the
   app target (File → Add Files… → select plist → "Copy items if needed"
   unchecked is fine since it lives in the project dir → add to target).
2. Xcode: create a new iOS App project (SwiftUI, bundle id
   `dev.kotlintrace.sample`) inside `sample/ios/`; replace the generated
   `KotlinTraceSampleApp.swift` / `ContentView.swift` with the repo's versions.
3. Add the Firebase SPM package (`https://github.com/firebase-ios-sdk/firebase-ios-sdk`,
   product `FirebaseCrashlytics`) to the app target.
4. Run-script build phase (before "Compile Sources") to build + embed the
   Kotlin framework:
   ```
   cd "$SRCROOT/.."
   export JAVA_HOME="$HOME/Library/Java/JavaVirtualMachines/jbr-21.0.9/Contents/Home"
   ./gradlew :sample:shared:embedAndSignAppleFrameworkForXcode
   ```
   (JAVA_HOME line only needed if gradlew reports "Unable to locate a Java Runtime".)
   After the first build succeeds, link the framework in the app target:
   "Frameworks, Libraries, and Embedded Content" → "+" → "Add Other…" →
   `sample/shared/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)/SampleShared.framework`
   → set it to **Do Not Embed** (the framework is static; the script phase only
   makes it available to the linker, it must not be embedded).
5. Run-script build phase (after the embed phase) to upload dSYMs:
   `${BUILD_DIR%Build/*}/SourcePackages/checkouts/firebase-ios-sdk/Crashlytics/run`,
   with target build setting `DEBUG_INFORMATION_FORMAT = dwarf-with-dsym`.
6. Run-script build phase (last) to embed the dSYM in the app bundle for the
   libbacktrace source-info hook (simulator file:line; see "Simulator source
   info" above). Copies `$(DWARF_DSYM_FOLDER_PATH)/$(PRODUCT_NAME).app.dSYM`
   into the bundle as `$(EXECUTABLE_NAME).dSYM`
   (→ `<App>/<App>.dSYM/Contents/Resources/DWARF/<App>`).
7. Run on the iOS simulator; tap "Crash (raw)" → relaunch → tap
   "Crash (demangled)" → relaunch; Crashlytics uploads both reports on the
   next launch.
8. Evidence: dashboard shows (a) a fatal report with raw `Kotlin_`-mangled
   frames (before), and (b) a readable non-fatal recorded exception with
   `CrashBot.crash` / file / line frames (after; the native abort also still
   logs the mangled fatal — known Phase-2 tradeoff). Console-side: the
   demangled run's `printStackTrace()` output already shows real
   `(CrashBot.kt:NN)` frames on the simulator.

Acceptance: readable Kotlin frames (function / file / line) visible in a
Crashlytics report.

## Phase 3 — Polish & Release

- [ ] 3.1 API validation (binary-compatibility-validator) + KDoc
- [ ] 3.2 Publish pipeline to Maven Central (Sonatype), versioning strategy
- [ ] 3.3 CI (GitHub Actions: build, test, publish snapshot)
- [ ] 3.4 Public docs site / README with usage + evidence screenshots
- [ ] 3.5 v0.1.0 release

## Phase 4 — Expansion (post-MVP, optional)

- [ ] 4.1 Android/JVM targets
- [ ] 4.2 Coroutine diagnostic module (`KotlinTraceDiagnostics`):
       CoroutineGuard-style scope-leak/hang detection under same umbrella
- [ ] 4.3 OTel-compatible span/telemetry export (vendor-neutral story)

---

# Best-Practice Constraints

- **Do not commit**: build outputs, `.gradle/`, `build/`, `.idea/`, `.DS_Store`,
  local.properties, Xcode `DerivedData`, keystores/secrets.
- Semantic commits, one concern per commit; **only the user commits and pushes**.
- Feature-branch workflow once CI exists; `main` always green.
- Version catalogs (`libs.versions.toml`) for dependency management — no raw
  hardcoded versions scattered in build files.
- Keep `commonMain` free of platform APIs; use `expect/actual` + appleMain.
- Every feature lands with a test; Kotlin/Native test targets must pass before hand-off.
- No comments in code unless they explain *why* (per project convention).

# Tech Stack / Environment

| Item | Value |
|---|---|
| OS | macOS (darwin), Apple Silicon (arm64) |
| JDK | 21 (JBR 21.0.9 at `~/Library/Java/JavaVirtualMachines/jbr-21.0.9/...`) |
| Build | Gradle 9.6.1 wrapper + Kotlin Multiplatform plugin |
| Kotlin | 2.3.21 (matches existing `kmp-sample` project) |
| Targets | iosArm64, iosSimulatorArm64, iosX64, jvm (jvm for host-runnable tests) |
| Xcode | 27.0 |
| License | Apache-2.0 |

# Skeleton Layout (current)

```
kotlintrace/
├── PROJECT.md            # planning + roadmap + todos
├── README.md             # public-facing intro
├── LICENSE               # Apache-2.0
├── build.gradle.kts      # KMP plugin, targets, sourceset wiring (core module)
├── settings.gradle.kts   # rootProject, repos, includes :kotlintrace-crashlytics, :sample:shared
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/          # Gradle 9.6.1 wrapper (committed)
├── src/                  # core library (artifact: kotlintrace)
│   ├── commonMain/kotlin/dev/kotlintrace/
│   │   ├── Backend.kt
│   │   ├── DefaultKotlinTraceDemangler.kt
│   │   ├── KotlinTrace.kt
│   │   ├── KotlinTraceDemangler.kt
│   │   ├── KotlinTraceFrame.kt
│   │   ├── KotlinTraceOptions.kt
│   │   ├── KotlinTraceReporter.kt   # backend → reporter dispatch
│   │   └── PlatformHook.kt          # expect seam
│   ├── appleMain/kotlin/dev/kotlintrace/PlatformHook.apple.kt   # real hook (setUnhandledExceptionHook)
│   ├── jvmMain/kotlin/dev/kotlintrace/PlatformHook.jvm.kt
│   └── commonTest/kotlin/dev/kotlintrace/
│       ├── DefaultKotlinTraceDemanglerTest.kt
│       └── KotlinTraceReporterTest.kt
└── kotlintrace-crashlytics/         # Crashlytics adapter (artifact: kotlintrace-crashlytics)
    ├── build.gradle.kts
    └── src/
        ├── commonMain/kotlin/dev/kotlintrace/crashlytics/
        │   ├── CrashlyticsBackend.kt         # KotlinTrace.installCrashlytics()
        │   ├── CrashlyticsExceptionReport.kt
        │   ├── CrashlyticsFrame.kt
        │   ├── CrashlyticsReportFormatter.kt # pure logic, JVM-tested
        │   └── CrashlyticsSink.kt            # expect seam
        ├── appleMain/kotlin/dev/kotlintrace/crashlytics/CrashlyticsSink.apple.kt  # cinterop calls
        ├── jvmMain/kotlin/dev/kotlintrace/crashlytics/CrashlyticsSink.jvm.kt      # no-op
        ├── nativeInterop/cinterop/crashlytics.def   # runtime-lookup shims
        └── commonTest/kotlin/dev/kotlintrace/crashlytics/CrashlyticsReportFormatterTest.kt
└── sample/                # roadmap 2.4 demo (artifact: kotlintrace-sample-shared)
    ├── shared/            # shared Kotlin demo code
    │   ├── build.gradle.kts
    │   └── src/
    │       ├── commonMain/kotlin/dev/kotlintrace/sample/CrashBot.kt
    │       └── commonTest/kotlin/dev/kotlintrace/sample/CrashBotTest.kt
    └── ios/               # SwiftUI app (Xcode project created locally, not committed)
        ├── KotlinTraceSampleApp.swift
        └── ContentView.swift
```

# Decisions to Confirm With User

- [x] Crashlytics fatal semantics: public `recordExceptionModel:` only (non-fatal
      report; native abort also logged unreadable). Private API rejected. *(2026-08-01)*
- [x] Module layout: root stays the core module; adapters are subprojects. *(2026-08-01)*
- [x] Adapter entry API: `KotlinTrace.installCrashlytics()` extension in the
      adapter module. *(2026-08-01)*
- [x] Ordering: sample app (2.4) before Sentry adapter (2.2) — proof-of-value
      first. *(2026-08-01)*
- [ ] Repo name (`KotlinTrace` vs alternative)
- [ ] Repo visibility: public vs private
- [ ] GitHub account/org to own it; create via web UI at push time
- [ ] Kotlin version pin is 2.3.21 (can bump to 2.4.10 later)
