plugins {
    id("pingo.android.feature")
}

android {
    namespace = "com.pingo.feature.settings.impl"
}

dependencies {
    implementation(project(":feature:settings:api"))
    implementation(project(":core:common"))
    implementation(project(":core:data"))
    testImplementation(project(":core:testing"))
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.activity.compose)
    debugImplementation(libs.compose.ui.test.manifest)
}
