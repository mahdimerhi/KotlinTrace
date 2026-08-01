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

## 6. MVP Scope (v0.1)

- [ ] 1. Common `install()` + `KotlinTraceOptions`
- [ ] 2. Crashlytics adapter: rewrite Kotlin traces client-side
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

- [ ] 2.1 Crashlytics adapter module (`:kotlintrace-crashlytics`), cinterop to
       `FirebaseCrashlytics`, rewrite trace before upload
- [ ] 2.2 Sentry adapter module (`:kotlintrace-sentry`)
- [ ] 2.3 Bugsnag adapter module (`:kotlintrace-bugsnag`) *(lowest priority)*
- [ ] 2.4 Adapter tests + demo CMP sample app with before/after traces
- [ ] 2.5 **Hand-off per feature: user commits**

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
├── build.gradle.kts      # KMP plugin, targets, sourceset wiring
├── settings.gradle.kts   # rootProject, repos, version catalog wiring
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/          # Gradle 9.6.1 wrapper (committed)
└── src/
    ├── commonMain/kotlin/dev/kotlintrace/
    │   ├── Backend.kt
    │   ├── DefaultKotlinTraceDemangler.kt
    │   ├── KotlinTrace.kt
    │   ├── KotlinTraceDemangler.kt
    │   ├── KotlinTraceFrame.kt
    │   ├── KotlinTraceOptions.kt
    │   └── PlatformHook.kt        # expect seam
    ├── appleMain/kotlin/dev/kotlintrace/PlatformHook.apple.kt
    ├── jvmMain/kotlin/dev/kotlintrace/PlatformHook.jvm.kt
    └── commonTest/kotlin/dev/kotlintrace/DefaultKotlinTraceDemanglerTest.kt
```

# Decisions to Confirm With User

- [ ] Repo name (`KotlinTrace` vs alternative)
- [ ] Repo visibility: public vs private
- [ ] GitHub account/org to own it; create via web UI at push time
- [ ] Kotlin version pin is 2.3.21 (can bump to 2.4.10 later)
