plugins {
    id("pingo.android.library")
}

android {
    namespace = "com.pingo.core.network"
}

dependencies {
    implementation(libs.kotlinx.coroutines.android)
}
