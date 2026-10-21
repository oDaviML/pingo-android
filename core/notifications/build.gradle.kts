plugins {
    id("pingo.android.library")
}

android {
    namespace = "com.pingo.core.notifications"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
}
