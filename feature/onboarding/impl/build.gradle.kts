plugins {
    id("pingo.android.feature")
}

android {
    namespace = "com.pingo.feature.onboarding.impl"
}

dependencies {
    implementation(project(":feature:onboarding:api"))
}
