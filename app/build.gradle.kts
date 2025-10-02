import java.util.Properties

plugins {
    id ("com.android.application")
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val AZURE_KEY = localProps.getProperty("AZURE_TRANSLATOR_KEY") ?: ""
val AZURE_REGION = localProps.getProperty("AZURE_TRANSLATOR_REGION") ?: ""

android {
    namespace = "vn.edu.usth.camer"
    compileSdk = 34

    defaultConfig {
        applicationId = "vn.edu.usth.camer"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }

        buildConfigField("String", "AZURE_TRANSLATOR_KEY", "\"$AZURE_KEY\"")
        buildConfigField("String", "AZURE_TRANSLATOR_REGION", "\"$AZURE_REGION\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles (getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
                targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
    androidResources {
        noCompress += setOf("tflite")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }


}

dependencies {
    implementation ("androidx.appcompat:appcompat:1.6.1")
    implementation ("com.google.android.material:material:1.11.0")
    implementation ("androidx.constraintlayout:constraintlayout:2.1.4")

    // CameraX
    implementation ("androidx.camera:camera-camera2:1.3.1")
    implementation ("androidx.camera:camera-lifecycle:1.3.1")
    implementation ("androidx.camera:camera-view:1.3.1")

    // MediaPipe
    implementation ("com.google.mediapipe:tasks-vision:0.10.9")

    // Retrofit
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")


    implementation("com.google.mediapipe:tasks-vision:0.10.14")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}