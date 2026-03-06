import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)

    // Plugin para serialización (JSON)
    kotlin("plugin.serialization") version "2.0.20"
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)

            // Motor de Internet para Android
            implementation("io.ktor:ktor-client-okhttp:2.3.12")

            // 1. Dependencias de WorkManager (Kotlin + Coroutines support)
            implementation("androidx.work:work-runtime-ktx:2.9.0")

            // 2. Integración de Koin con WorkManager
            implementation("io.insert-koin:koin-androidx-workmanager:3.5.3")
        }

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)

            // Componentes de UI Tooling
            implementation(compose.components.uiToolingPreview)

            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            // Librería de Iconos Extendidos
            implementation(compose.materialIconsExtended)

            // 1. Navegación (Voyager)
            implementation("cafe.adriel.voyager:voyager-navigator:1.1.0-beta02")
            implementation("cafe.adriel.voyager:voyager-transitions:1.1.0-beta02")

            // 2. Conexión a Internet (Ktor)
            implementation("io.ktor:ktor-client-core:2.3.12")
            implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
            implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")

            // 3. Imágenes (Coil 3)
            implementation("io.coil-kt.coil3:coil-compose:3.0.0-alpha06")
            implementation("io.coil-kt.coil3:coil-network-ktor:3.0.0-alpha06")

            // 4. Sistema de archivos multiplataforma (Okio)
            implementation("com.squareup.okio:okio:3.9.0")



        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        iosMain.dependencies {
            // Motor de Internet para iOS
            implementation("io.ktor:ktor-client-darwin:2.3.12")
        }
    }
}

android {
    namespace = "com.zixion.mangaverse"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.zixion.mangaverse"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(libs.compose.uiTooling)
}