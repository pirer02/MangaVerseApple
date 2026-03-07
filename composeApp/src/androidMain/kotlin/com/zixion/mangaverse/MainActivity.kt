package com.zixion.mangaverse

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.zixion.mangaverse.network.AndroidContext
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    companion object {
        // Un "canal de comunicación" para avisar a nuestro AuthManager cuando Google responda
        var googleSignInCallback: ((String?) -> Unit)? = null
    }

    // 1. Lanzador de la ventana de elegir cuenta de Google
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            // ¡Éxito! Le pasamos el Token de Google a nuestro callback
            googleSignInCallback?.invoke(account.idToken)
        } catch (e: Exception) {
            e.printStackTrace()
            // El usuario canceló o hubo un error
            googleSignInCallback?.invoke(null)
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        programarActualizacionEnSegundoPlano()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- LÍNEAS DE CONTEXTO ---
        AndroidContext.context = applicationContext
        AndroidContext.activity = this // AÑADIDO PARA EL LOGIN DE GOOGLE

        gestionarPermisos()

        setContent {
            App()
        }
    }

    // Función que llamaremos desde nuestro AuthManager
    fun lanzarLoginGoogle() {
        // Cogemos el Client ID que generó el plugin google-services automáticamente
        val webClientId = getString(resources.getIdentifier("default_web_client_id", "string", packageName))

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()

        val client = GoogleSignIn.getClient(this, gso)
        client.signOut() // Fuerza a mostrar siempre la ventana de elegir cuenta
        googleSignInLauncher.launch(client.signInIntent)
    }

    private fun gestionarPermisos() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                programarActualizacionEnSegundoPlano()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            programarActualizacionEnSegundoPlano()
        }
    }

    private fun programarActualizacionEnSegundoPlano() {
        val restricciones = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val peticionTrabajo = PeriodicWorkRequestBuilder<CacheUpdateWorker>(15, TimeUnit.MINUTES).setConstraints(restricciones).build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "MangaCacheUpdateWork",
            ExistingPeriodicWorkPolicy.KEEP,
            peticionTrabajo
        )
    }
}