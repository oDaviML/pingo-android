plugins {
    id("pingo.android.application")
    id("pingo.android.compose")
}

android {
    namespace = "com.pingo.spike.capture"
    defaultConfig {
        applicationId = "com.pingo.spike"
        versionCode = 1
        versionName = "0.1.0"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.compose.material3)
    testImplementation(libs.junit)
}
