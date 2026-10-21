plugins {
    id("pingo.android.library")
}

android {
    namespace = "com.pingo.core.datastore"
}

dependencies {
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
}
