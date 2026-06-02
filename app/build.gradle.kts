plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.frontend.petfinder"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.frontend.petfinder"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // Dependencias base (Compose, Core, UI, Testing)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // --- DEPENDENCIAS CORE DE PETFINDER ---

    // Consumo de la API RESTful (NestJS)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Google Maps y Geolocalización (Geofencing)
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.2.0")

    // Extensión oficial para usar Google Maps con Jetpack Compose
    implementation("com.google.maps.android:maps-compose:4.3.3")

    // Escaneo de Códigos QR físicos
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
    // Observa el ciclo de vida de TODA la app (foreground/background) para reconectar el socket
    implementation("androidx.lifecycle:lifecycle-process:2.8.4")
    // Navegación en Jetpack Compose
    implementation("androidx.navigation:navigation-compose:2.7.7")
    // Librería oficial de íconos de Material Design para Compose
    implementation("androidx.compose.material:material-icons-extended")
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("com.google.android.gms:play-services-location:21.2.0")
    // Cliente oficial de Socket.io
    implementation("io.socket:socket.io-client:2.1.0")
    // Gson (ya lo tienes, pero lo usaremos para parsear los eventos del socket)
    implementation("com.google.code.gson:gson:2.10.1")

    // Persistencia de sesión (JWT + refreshToken)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Firebase Cloud Messaging (push notifications)
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation("com.google.firebase:firebase-messaging-ktx")

    // Chrome Custom Tab para flujo OAuth de Google Sign-In
    implementation("androidx.browser:browser:1.8.0")
}