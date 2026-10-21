plugins {
    id("pingo.android.feature")
}

android {
    namespace = "com.pingo.feature.capturestatus.impl"
}

dependencies {
    implementation(project(":feature:capturestatus:api"))
}
