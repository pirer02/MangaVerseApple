package com.zixion.mangaverse.network

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.zixion.mangaverse.models.UsuarioFirebase
import com.zixion.mangaverse.MainActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await // Asegúrate de tener esta dependencia o usa el estilo addOnCompleteListener

actual class AuthManager {
    private val auth = FirebaseAuth.getInstance()

    actual suspend fun iniciarSesionGoogle(): UsuarioFirebase? = suspendCancellableCoroutine { continuation ->
        val activity = AndroidContext.activity as? MainActivity
        if (activity == null) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        MainActivity.googleSignInCallback = { idToken ->
            if (idToken != null) {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(credential)
                    .addOnSuccessListener { result ->
                        val user = result.user
                        if (user != null) {
                            continuation.resume(UsuarioFirebase(user.uid, user.email ?: "", user.displayName ?: ""))
                        } else {
                            continuation.resume(null)
                        }
                    }
                    .addOnFailureListener { continuation.resume(null) }
            } else {
                continuation.resume(null)
            }
        }
        activity.lanzarLoginGoogle()
    }

    actual fun cerrarSesion() {
        auth.signOut()
    }

    actual fun obtenerUsuarioActual(): UsuarioFirebase? {
        val user = auth.currentUser ?: return null
        return UsuarioFirebase(user.uid, user.email ?: "", user.displayName ?: "")
    }

    actual suspend fun guardarDatosEnNube(datosJson: String) = suspendCancellableCoroutine { continuation ->
        val user = auth.currentUser
        if (user == null) {
            continuation.resume(Unit)
            return@suspendCancellableCoroutine
        }
        val db = FirebaseFirestore.getInstance()
        val docData = hashMapOf("jsonData" to datosJson)
        db.collection("usuarios").document(user.uid).set(docData)
            .addOnCompleteListener { continuation.resume(Unit) }
    }

    actual suspend fun cargarDatosDeNube(): String? = suspendCancellableCoroutine { continuation ->
        val user = auth.currentUser
        if (user == null) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }
        val db = FirebaseFirestore.getInstance()
        db.collection("usuarios").document(user.uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    continuation.resume(doc.getString("jsonData"))
                } else {
                    continuation.resume(null)
                }
            }
            .addOnFailureListener { continuation.resume(null) }
    }

    // --- NUEVA IMPLEMENTACIÓN DE ELIMINACIÓN ---
    actual suspend fun eliminarCuenta(): Boolean = suspendCancellableCoroutine { continuation ->
        val user = auth.currentUser
        if (user == null) {
            continuation.resume(false)
            return@suspendCancellableCoroutine
        }

        val uid = user.uid
        val db = FirebaseFirestore.getInstance()

        // 1. Primero intentamos borrar los datos de Firestore
        db.collection("usuarios").document(uid).delete()
            .addOnCompleteListener { taskData ->
                // Independientemente de si falló borrar el JSON (por si no tenía), intentamos borrar el usuario
                user.delete()
                    .addOnSuccessListener {
                        continuation.resume(true)
                    }
                    .addOnFailureListener {
                        // Aquí suele fallar si la sesión es antigua (requiere re-login)
                        continuation.resume(false)
                    }
            }
    }
}