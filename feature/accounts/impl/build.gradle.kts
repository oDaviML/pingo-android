plugins {
    id("pingo.android.feature")
}

android {
    namespace = "com.pingo.feature.accounts.impl"
}

dependencies {
    implementation(project(":feature:accounts:api"))
}
