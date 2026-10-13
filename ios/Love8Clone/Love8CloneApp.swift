import SwiftUI

@main
struct Love8CloneApp: App {
    @StateObject private var model = AppModel.shared
    @Environment(\.scenePhase) private var scenePhase

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(model)
                .environmentObject(model.location)
                .preferredColorScheme(.light)
        }
        .onChange(of: scenePhase) { phase in
            switch phase {
            case .active: model.becameActive()
            case .background: model.enteredBackground()
            default: break
            }
        }
        .backgroundTask(.appRefresh(AppModel.refreshTaskId)) { @MainActor in
            await AppModel.shared.backgroundRefresh()
        }
    }
}
