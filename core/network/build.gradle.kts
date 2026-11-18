plugins {
    id("pingo.android.library")
    id("pingo.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.pingo.core.network"
}

dependencies {
    implementation(project(":core:auth"))
    implementation(project(":core:common"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.test.runner)
}
