plugins {
    id("pingo.android.library")
}

android {
    namespace = "com.pingo.core.testing"
}

dependencies {
    api(project(":core:common"))
    api(libs.junit)
    api(libs.kotlinx.coroutines.test)
}
