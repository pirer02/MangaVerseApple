import SwiftUI
import FirebaseCore
import GoogleSignIn // <-- NUEVO: Para manejar el cierre de la ventana de Google
import ComposeApp   // <-- NUEVO: Para acceder al puente de Kotlin

class AppDelegate: NSObject, UIApplicationDelegate {
  func application(_ application: UIApplication,
                   didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {
      
    FirebaseApp.configure()
      
    // <-- LA LÍNEA MÁGICA: Conectamos el enchufe de Kotlin con nuestra nueva clase de Swift
    IosFirebaseBridge.shared.delegate = AppleFirebaseManager()
      
    return true
  }
}

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate

    var body: some Scene {
        WindowGroup {
            ContentView()
                // <-- NUEVO: Esto captura el enlace de retorno cuando Google termina el login
                .onOpenURL { url in
                    GIDSignIn.sharedInstance.handle(url)
                }
        }
    }
}
