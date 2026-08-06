plugins {
    id("com.android.application") version "8.8.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.25" apply false
    id("com.google.devtools.ksp") version "1.9.25-1.0.20" apply false  // KSP version
    id("com.google.dagger.hilt.android") version "2.48.1" apply false  // Hilt version
    // REMOVE: kotlin-kapt plugin if it exists
}
