plugins {
    id("pingo.android.feature")
}

android {
    namespace = "com.pingo.feature.transactions.impl"
}

dependencies {
    implementation(project(":feature:transactions:api"))
}
