plugins {
    id("pingo.android.library")
}

android {
    namespace = "com.pingo.core.auth"
}

dependencies {
    implementation(libs.auth0)
    implementation(libs.androidx.biometric)
    implementation(libs.kotlinx.coroutines.android)
}
