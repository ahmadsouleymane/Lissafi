import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.lissafi.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.lissafi.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 3
        versionName = "1.0"
    }

    packaging {
        jniLibs {
            // APK distribué hors Play Store → packaging legacy (libs compressées,
            // APK plus léger pour les téléchargements sur réseau mobile).
            // Les libs ML Kit 17.3+ et CameraX 1.4.2+ sont alignées 16 KB.
            useLegacyPackaging = true
        }
    }

    signingConfigs {
        create("release") {
            // Signature release depuis keystore.properties (jamais commité).
            // Si le fichier est absent, le build release échoue volontairement.
            val props = Properties()
            val propsFile = rootProject.file("keystore.properties")
            if (propsFile.exists()) props.load(propsFile.inputStream())
            storeFile = rootProject.file(props.getProperty("storeFile", "app/lissafi-release.keystore"))
            storePassword = props.getProperty("storePassword", "")
            keyAlias = props.getProperty("keyAlias", "lissafi")
            keyPassword = props.getProperty("keyPassword", "")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

dependencies {
    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.activity.compose)

    // Icônes Lucide (style Vercel/Linear) — ImageVectors via composables.icons.Lucide
    implementation(libs.composables.lucide)

    // Graphiques Vico (écran Activité)
    implementation(libs.vico.compose.m3)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.ui.tooling)

    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // CameraX + ML Kit barcode
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mlkit.barcode.scanning)

    // WorkManager (sync background)
    implementation(libs.androidx.work.runtime)

    // Ktor (HTTP client pour appels REST Supabase)
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.json)

    // KotlinX Serialization
    implementation(libs.kotlinx.serialization.json)

    // Firebase Cloud Messaging (notifications push)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    // Chiffrement des préférences (tokens de session GoTrue au repos)
    implementation(libs.androidx.security.crypto)

    // Core library desugaring
    coreLibraryDesugaring(libs.desugar.jdk.libs)
}
