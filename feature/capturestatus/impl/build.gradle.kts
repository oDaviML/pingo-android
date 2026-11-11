plugins {
    id("pingo.android.feature")
}

android {
    namespace = "com.pingo.feature.capturestatus.impl"
}

dependencies {
    implementation(project(":feature:capturestatus:api"))
    implementation(project(":core:data"))
    implementation(project(":core:datastore"))
    implementation(project(":core:notifications"))
    testImplementation(project(":core:testing"))
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.activity.compose)
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
