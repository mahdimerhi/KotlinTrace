import SwiftUI
import SampleShared

struct ContentView: View {
    var body: some View {
        VStack(spacing: 24) {
            Button("Crash (demangled via KotlinTrace → Crashlytics)") {
                CrashBot.shared.setupCrashlytics()
                CrashBot.shared.triggerCrash()
            }
            Button("Crash (demangled via KotlinTrace → Sentry)") {
                CrashBot.shared.setupSentry()
                CrashBot.shared.triggerCrash()
            }
            Button("Crash (raw, no KotlinTrace)") {
                CrashBot.shared.triggerCrash()
            }
        }
        .padding()
    }
}

#Preview {
    ContentView()
}
