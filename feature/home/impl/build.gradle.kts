plugins {
    id("pingo.android.feature")
}

android {
    namespace = "com.pingo.feature.home.impl"
}

dependencies {
    implementation(project(":feature:home:api"))
    implementation(project(":core:data"))
    implementation(project(":core:datastore"))
    implementation(project(":core:notifications"))
    testImplementation(project(":core:testing"))
    testImplementation(libs.junit)
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
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
