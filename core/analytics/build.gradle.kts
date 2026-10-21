plugins {
    id("pingo.android.library")
}

android {
    namespace = "com.pingo.core.analytics"
}

dependencies {
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
}
