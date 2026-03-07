import Foundation
import FirebaseAuth
import FirebaseFirestore
import GoogleSignIn
import FirebaseCore
import UIKit
import ComposeApp // El nombre correcto que descubrimos en tu build.gradle

class AppleFirebaseManager: NSObject, IosFirebaseDelegate {
    
    // 1. Iniciar sesión con Google
    func iniciarSesionGoogle() async throws -> UsuarioFirebase? {
        // Buscamos la pantalla principal del iPhone para poner la ventanita encima
        guard let rootViewController = await MainActor.run(resultType: UIViewController?.self, body: {
            guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene else { return nil }
            return windowScene.windows.first?.rootViewController
        }) else {
            return nil
        }
        
        // Sacamos el ID secreto que guardamos en tu archivo .plist
        guard let clientID = FirebaseApp.app()?.options.clientID else { return nil }
        let config = GIDConfiguration(clientID: clientID)
        GIDSignIn.sharedInstance.configuration = config
        
        // Mostramos la ventana nativa de Google y esperamos a que el usuario termine
        let result = try await GIDSignIn.sharedInstance.signIn(withPresenting: rootViewController)
        
        // Sacamos los tokens de Google
        guard let idToken = result.user.idToken?.tokenString else { return nil }
        let accessToken = result.user.accessToken.tokenString
        
        // Convertimos los tokens a credenciales de Firebase
        let credential = GoogleAuthProvider.credential(withIDToken: idToken, accessToken: accessToken)
        
        // Iniciamos sesión en Firebase
        let authResult = try await Auth.auth().signIn(with: credential)
        let firebaseUser = authResult.user
        
        // ---> AQUÍ ESTÁ LA CORRECCIÓN: Usando las etiquetas uid, email y nombre <---
        return UsuarioFirebase(
            uid: firebaseUser.uid,
            email: firebaseUser.email ?? "",
            nombre: firebaseUser.displayName ?? ""
        )
    }
    
    // 2. Cerrar sesión
    func cerrarSesion() {
        do {
            try Auth.auth().signOut()
            GIDSignIn.sharedInstance.signOut()
        } catch {
            print("Error al cerrar sesión: \(error.localizedDescription)")
        }
    }
    
    // 3. Obtener el usuario actual
    func obtenerUsuarioActual() -> UsuarioFirebase? {
        guard let user = Auth.auth().currentUser else { return nil }
        
        // ---> AQUÍ ESTÁ LA CORRECCIÓN TAMBIÉN <---
        return UsuarioFirebase(
            uid: user.uid,
            email: user.email ?? "",
            nombre: user.displayName ?? ""
        )
    }
    
    // 4. Guardar datos en la nube (Firestore)
    func guardarDatosEnNube(datosJson: String) async throws {
        guard let user = Auth.auth().currentUser else { return }
        let db = Firestore.firestore()
        let docData: [String: Any] = ["jsonData": datosJson]
        try await db.collection("usuarios").document(user.uid).setData(docData)
    }
    
    // 5. Cargar datos de la nube (Firestore)
    func cargarDatosDeNube() async throws -> String? {
        guard let user = Auth.auth().currentUser else { return nil }
        let db = Firestore.firestore()
        let document = try await db.collection("usuarios").document(user.uid).getDocument()
        
        if document.exists {
            return document.data()?["jsonData"] as? String
        } else {
            return nil
        }
    }
    
    // 6. Eliminar cuenta
    func eliminarCuenta() async throws -> KotlinBoolean {
        guard let user = Auth.auth().currentUser else {
            return KotlinBoolean(value: false)
        }
        let db = Firestore.firestore()
        
        do {
            try await db.collection("usuarios").document(user.uid).delete()
            try await user.delete()
            return KotlinBoolean(value: true)
        } catch {
            return KotlinBoolean(value: false)
        }
    }
}
