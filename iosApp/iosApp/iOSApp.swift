import SwiftUI
import BalanceTracker

@main
struct iOSApp: App {
    init() {
        AppModuleKt.startKoin()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
