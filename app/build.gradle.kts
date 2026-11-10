import java.util.Properties
import java.io.FileInputStream

val keystoreProperties = Properties().apply {
    val f = rootProject.file("keystore.properties")
    require(f.exists()) { "keystore.properties not found" }
    load(FileInputStream(f))
}

plugins {
    id("com.android.application")
    // id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("dagger.hilt.android.plugin")
    id("kotlin-parcelize")
}

android {
    namespace = "com.bodepudi.vexnotebooktracker"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.bodepudi.vexnotebooktracker" // this is what Google Play cares about and must be unique
        minSdk = 24
        targetSdk = 36
        versionCode = 3 // this is what Google Play cares about
        versionName = "1.2"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Generate native debug symbols for all ABIs
        // This helps with crash analysis in Play Console
        ndk {
            debugSymbolLevel = "FULL"
        }
    }

    // Add packaging configuration for 16KB page size support
    packaging {
        jniLibs {
            useLegacyPackaging = false
            excludes += setOf(
                "**/libtoolChecker.so"
            )
        }
    }

    signingConfigs {
        create("release") {
            storeFile     = file(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties["storePassword"] as String
            keyAlias      = keystoreProperties["keyAlias"] as String
            keyPassword   = keystoreProperties["keyPassword"] as String
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "API_BASE_URL", "\"https://api.vexteams.org\"")
            signingConfig = signingConfigs.getByName("release")

            // Generate full debug symbols for better crash analysis in production
            ndk {
                debugSymbolLevel = "FULL"
            }
        }
        debug {
            buildConfigField("String", "API_BASE_URL", "\"https://api-dev.vexteams.org\"")

            // Also generate symbols for debug builds (optional, but helpful)
            ndk {
                debugSymbolLevel = "FULL"
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        // dataBinding is not used anywhere in this project (no <layout> roots, no DataBindingUtil).
        // AGP 8.11 ships databinding-ktx built with Kotlin 2.1, which the Kotlin 1.9 compiler cannot read.
        dataBinding = false
        buildConfig = true
    }

    ksp {
        arg("dagger.hilt.android.internal.disableAndroidSuperclassValidation", "true")
    }

    lint {
        disable.add("ResAuto")
        abortOnError = false
        warningsAsErrors = false
    }
}

dependencies {
    // Core Android
    //implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")

    // Architecture Components
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.7")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.fragment:fragment-ktx:1.8.5")

    // Navigation
    implementation("androidx.navigation:navigation-fragment-ktx:2.8.4")
    implementation("androidx.navigation:navigation-ui-ktx:2.8.4")

    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.48.1")
    ksp("com.google.dagger:hilt-compiler:2.48.1")

    // Security
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("com.google.android.play:integrity:1.4.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    implementation("io.jsonwebtoken:jjwt-impl:0.12.6")
    implementation("io.jsonwebtoken:jjwt-gson:0.12.6")
    implementation("com.auth0.android:jwtdecode:2.0.0")

    // Optional - THESE ARE LIKELY THE PROBLEM
    // TRY COMMENTING OUT rootbeer-lib temporarily
    // implementation("com.scottyab:rootbeer-lib:0.1.0")
    implementation("com.google.firebase:firebase-installations:17.0.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}