import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-parcelize")
    id("kotlin-kapt")
    id("androidx.navigation.safeargs.kotlin")
}

android {
    namespace = "com.example.travelcompanion"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.travelcompanion"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val apikeysPropertiesFile = rootProject.file("apikeys.properties")
        val apikeysProperties = Properties()
        apikeysProperties.load(FileInputStream(apikeysPropertiesFile))

        val apiKeyGoogle = apikeysProperties.getProperty("API_KEY_GOOGLE")
        val apiKeyOrs = apikeysProperties.getProperty("API_KEY_ORS")

        buildConfigField("String", "API_KEY_GOOGLE", "\"$apiKeyGoogle\"")
        buildConfigField("String", "API_KEY_ORS", "\"$apiKeyOrs\"")

        manifestPlaceholders["API_KEY_GOOGLE"] = apikeysProperties["API_KEY_GOOGLE"] as String
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.osmdroid.android)
    implementation(libs.osmdroid.wms)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.play.services.maps)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.androidx.room.runtime.android)
    implementation(libs.androidx.coordinatorlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.play.services.location)
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.work.testing)
    implementation(libs.core)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    kapt(libs.androidx.room.compiler)
    // OkHttp
    implementation(libs.okhttp)

// JSON (se non usi una libreria esterna)
    implementation(libs.json)

// Kotlin Coroutines (se non già incluse)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.glide)
    kapt(libs.compiler)
    implementation(libs.mpandroidchart)
    implementation(libs.play.services.maps.v1820)
    implementation(libs.android.maps.utils)
    implementation(libs.androidx.work.runtime.ktx)

    testImplementation (libs.mockito.core)
    testImplementation (libs.mockito.kotlin)
    testImplementation (libs.junit)
    testImplementation (libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.work.testing.v290)
    androidTestImplementation(libs.androidx.junit.v115)
    androidTestImplementation(libs.androidx.core)
}
