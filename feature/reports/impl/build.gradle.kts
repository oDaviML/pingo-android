plugins {
    id("pingo.android.feature")
}

android {
    namespace = "com.pingo.feature.reports.impl"
}

dependencies {
    implementation(project(":feature:reports:api"))
}
