plugins {
    id("pingo.android.feature")
}

android {
    namespace = "com.pingo.feature.settings.impl"
}

dependencies {
    implementation(project(":feature:settings:api"))
}
