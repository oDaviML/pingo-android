plugins {
    id("pingo.android.library")
}

android {
    namespace = "com.pingo.core.model"
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.runtime)
}
