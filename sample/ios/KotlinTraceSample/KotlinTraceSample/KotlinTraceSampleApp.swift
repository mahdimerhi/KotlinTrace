import SwiftUI
import FirebaseCore
import SampleShared

@main
struct KotlinTraceSampleApp: App {
    init() {
        FirebaseApp.configure()
        if CommandLine.arguments.contains("-crash") {
            DispatchQueue.main.asyncAfter(deadline: .now() + 15) {
                CrashBot.shared.setupCrashlytics()
                CrashBot.shared.logCrash()
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
