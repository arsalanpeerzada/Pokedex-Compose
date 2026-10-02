import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// Release signing comes from keystore.properties, which Git ignores (see keystore.properties.example).
// Without it, release builds are simply unsigned, which is what CI builds.
val keystore = Properties().apply {
    rootProject.file("keystore.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}

android {
    namespace = "dev.pokedex.app"
    compileSdk = 37

    defaultConfig {
        // Settled before the first test release; changing it later would create a new app on Play.
        applicationId = "io.github.arsalanpeerzada.pokedex"
        minSdk = 26
        targetSdk = 37
        // Feature-complete for v1; 1.0.0 follows a check on a real device.
        versionCode = 2
        versionName = "0.9.0"
    }

    signingConfigs {
        if (keystore.isNotEmpty()) {
            create("release") {
                storeFile = rootProject.file(keystore.getProperty("storeFile"))
                storePassword = keystore.getProperty("storePassword")
                keyAlias = keystore.getProperty("keyAlias")
                keyPassword = keystore.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:data"))
    implementation(project(":feature:today"))
    implementation(project(":feature:pokedex"))
    implementation(project(":feature:detail"))
    implementation(project(":feature:collection"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:onboarding"))
    implementation(project(":feature:teams"))
    implementation(project(":feature:widget"))
    implementation(project(":feature:reminder"))
    implementation(project(":feature:wallpaper"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.compose.material3.adaptive.navigation3)
    implementation(libs.coil.network.okhttp)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
