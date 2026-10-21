plugins {
    id("pingo.android.feature")
}

android {
    namespace = "com.pingo.feature.home.impl"
}

dependencies {
    implementation(project(":feature:home:api"))
}
