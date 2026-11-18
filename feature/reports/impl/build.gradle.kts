plugins {
    id("pingo.android.feature")
}

android {
    namespace = "com.pingo.feature.reports.impl"
}

dependencies {
    implementation(project(":feature:reports:api"))
    implementation(project(":core:common"))
    implementation(project(":core:data"))
    testImplementation(project(":core:testing"))
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(project(":core:common"))
    androidTestImplementation(project(":core:data"))
    androidTestImplementation(project(":core:database"))
    androidTestImplementation(project(":core:designsystem"))
    androidTestImplementation(project(":core:model"))
    androidTestImplementation(libs.room.ktx)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.activity.compose)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    debugImplementation(libs.compose.ui.test.manifest)
}
