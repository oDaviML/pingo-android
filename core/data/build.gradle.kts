plugins {
    id("pingo.android.library")
}

android {
    namespace = "com.pingo.core.data"
}

dependencies {
    api(project(":core:model"))
    implementation(libs.kotlinx.coroutines.android)
}
