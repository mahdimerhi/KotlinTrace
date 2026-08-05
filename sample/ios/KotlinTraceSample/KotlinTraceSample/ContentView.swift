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
            Button("Crash (demangled via KotlinTrace → Bugsnag)") {
                CrashBot.shared.setupBugsnag()
                CrashBot.shared.triggerCrash()
            }
            Button("Crash (raw, no KotlinTrace)") {
                CrashBot.shared.triggerCrash()
            }
        }
        .buttonStyle(.borderedProminent)
        .tint(Color(red: 1.0, green: 0.55, blue: 0.1))
        .font(.headline)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.black)
        .foregroundStyle(.white)
        .preferredColorScheme(.dark)
    }
}

#Preview {
    ContentView()
}
