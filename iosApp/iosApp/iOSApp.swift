import SwiftUI
import FirebaseCore
import GoogleSignIn
import ComposeApp // <-- Usa el nombre que te funcionó (ComposeApp o Shared)
import BackgroundTasks
import UserNotifications

class AppDelegate: NSObject, UIApplicationDelegate {
    
    // Este es el mismo ID que pusimos en el Paso 2
    let taskIdentifier = "com.zixion.mangaverse.updateCache"

    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {

        FirebaseApp.configure()
        
        // Conectamos el puente de Firebase que hicimos
        IosFirebaseBridge.shared.delegate = AppleFirebaseManager()
        
        // 1. Pedir permiso al usuario para mostrarle Notificaciones
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { granted, error in
            if granted {
                print("Permiso de notificaciones concedido")
            }
        }
        
        // 2. Registrar la tarea en segundo plano
        BGTaskScheduler.shared.register(forTaskWithIdentifier: taskIdentifier, using: nil) { task in
            // Cuando iOS decida despertar la app, llamará a esta función
            self.manejarActualizacionEnSegundoPlano(task: task as! BGAppRefreshTask)
        }

        return true
    }
    
    // Esta función se ejecuta cada vez que la app pasa a segundo plano (se minimiza)
    func applicationDidEnterBackground(_ application: UIApplication) {
        programarProximaActualizacion()
    }

    func manejarActualizacionEnSegundoPlano(task: BGAppRefreshTask) {
        // 1. Programamos la SIGUIENTE alarma para el futuro
        programarProximaActualizacion()

        // 2. Si iOS nos dice "te has quedado sin tiempo, apágate", le hacemos caso
        task.expirationHandler = {
            task.setTaskCompleted(success: false)
        }

        // 3. Llamamos al "Cerebro" de Kotlin
        Task {
            do {
                // Aquí usamos nuestro código compartido de Kotlin Multiplatform
                let novedades = try await MangaUpdateChecker.shared.buscarNovedades()
                
                // Si ha encontrado mangas nuevos, lanzamos la notificación
                if !novedades.isEmpty {
                    mostrarNotificacionLocal(novedades: novedades)
                }
                
                // Le decimos a iOS que hemos terminado con éxito
                task.setTaskCompleted(success: true)
            } catch {
                task.setTaskCompleted(success: false)
            }
        }
    }

    func programarProximaActualizacion() {
        let request = BGAppRefreshTaskRequest(identifier: taskIdentifier)
        // Le decimos a Apple: "Por favor, no me despiertes antes de 15 minutos (900 segundos)"
        request.earliestBeginDate = Date(timeIntervalSinceNow: 60 * 60)
        
        do {
            try BGTaskScheduler.shared.submit(request)
        } catch {
            print("No se pudo programar la tarea: \(error)")
        }
    }

    func mostrarNotificacionLocal(novedades: [String]) {
        let content = UNMutableNotificationContent()
        
        // Adaptamos el texto igual que en Android
        content.title = novedades.count == 1 ? "¡Nuevo capítulo!" : "¡Actualizaciones de Biblioteca!"
        
        // Como las notificaciones en iOS no tienen "InboxStyle" nativo tan fácil como Android,
        // juntamos todos los mangas encontrados en un solo bloque de texto.
        content.body = novedades.joined(separator: "\n")
        content.sound = .default

        let request = UNNotificationRequest(identifier: UUID().uuidString, content: content, trigger: nil)
        UNUserNotificationCenter.current().add(request)
    }
}

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate

    var body: some Scene {
        WindowGroup {
            ContentView()
                // Esto captura el enlace de retorno cuando Google termina el login
                .onOpenURL { url in
                    GIDSignIn.sharedInstance.handle(url)
                }
        }
    }
}
