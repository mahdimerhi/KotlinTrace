import SwiftUI
import FirebaseCore
import SampleShared
import Sentry
import Bugsnag

@main
struct KotlinTraceSampleApp: App {
    init() {
        FirebaseApp.configure()
        SentrySDK.start { options in
            options.dsn = "https://d9bf81e1d587e1474836ad18d7191474@o4511848408612864.ingest.de.sentry.io/4511848417919056"
            options.environment = "simulator-demo"
            // KotlinTrace already reports the crash with demangled Kotlin frames
            // (see the -sentrycrash path); Sentry's own crash handler would add
            // a duplicate native SIGABRT event with mangled symbol names.
            options.enableCrashHandler = false
        }
        let bugsnagConfig = BugsnagConfiguration("b87963e4a334a82334826cf84bab8ab3")
        // Same reasoning as Sentry above: KotlinTrace reports the crash; Bugsnag's
        // own error detection would add a duplicate raw event.
        bugsnagConfig.autoDetectErrors = false
        Bugsnag.start(with: bugsnagConfig)
        if CommandLine.arguments.contains("-crash") {
            DispatchQueue.main.asyncAfter(deadline: .now() + 15) {
                CrashBot.shared.setupCrashlytics()
                CrashBot.shared.logCrash()
                CrashBot.shared.triggerCrash()
            }
        } else if CommandLine.arguments.contains("-sentrycrash") {
            DispatchQueue.main.asyncAfter(deadline: .now() + 15) {
                CrashBot.shared.setupSentry()
                CrashBot.shared.triggerCrash()
            }
        } else if CommandLine.arguments.contains("-bugsnagcrash") {
            DispatchQueue.main.asyncAfter(deadline: .now() + 15) {
                CrashBot.shared.setupBugsnag()
                CrashBot.shared.triggerCrash()
            }
        } else if CommandLine.arguments.contains("-rawcrash") {
            // Mirrors the "Crash (raw, no KotlinTrace)" button: no install call,
            // so the K/N default uncaught-exception handler runs and the
            // resulting fatal report keeps mangled frames.
            DispatchQueue.main.asyncAfter(deadline: .now() + 15) {
                CrashBot.shared.triggerCrash()
            }
        }
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
