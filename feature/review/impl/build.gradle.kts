plugins {
    id("pingo.android.feature")
}

android {
    namespace = "com.pingo.feature.review.impl"
}

dependencies {
    implementation(project(":feature:review:api"))
}
