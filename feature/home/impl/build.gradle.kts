plugins {
    id("pingo.android.feature")
}

android {
    namespace = "com.pingo.feature.home.impl"
}

dependencies {
    implementation(project(":feature:home:api"))
    implementation(project(":core:notifications"))
    testImplementation(project(":core:testing"))
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.activity.compose)
    debugImplementation(libs.compose.ui.test.manifest)
}
