plugins {
    id("pingo.android.library")
    id("pingo.android.compose")
}

android {
    namespace = "com.pingo.core.ui"
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:designsystem"))
    testImplementation(libs.junit)
}
