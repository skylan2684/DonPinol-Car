plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.donpinol.car"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.donpinol.car"
        minSdk = 26
        targetSdk = 34
        versionCode = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()
        versionName = "1.0." + (System.getenv("GITHUB_RUN_NUMBER") ?: "0")
    }

    buildFeatures {
        buildConfig = true
    }

    flavorDimensions += "destino"
    productFlavors {
        create("radio") {
            dimension = "destino"
            buildConfigField("boolean", "MUSIC_ACCESS", "true")
        }
        create("telefono") {
            dimension = "destino"
            buildConfigField("boolean", "MUSIC_ACCESS", "false")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}
