plugins {
    id("pingo.android.library")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.pingo.core.parser"
}

dependencies {
    api(project(":core:model"))
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
}
