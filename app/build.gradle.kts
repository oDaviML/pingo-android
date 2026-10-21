plugins {
    id("pingo.android.application")
    id("pingo.android.compose")
    id("pingo.android.hilt")
}

android {
    namespace = "com.pingo"
    defaultConfig {
        applicationId = "com.pingo"
        versionCode = 1
        versionName = "0.1.0"
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:ui"))
    implementation(project(":sync"))
    implementation(project(":feature:onboarding:api"))
    implementation(project(":feature:onboarding:impl"))
    implementation(project(":feature:home:api"))
    implementation(project(":feature:home:impl"))
    implementation(project(":feature:transactions:api"))
    implementation(project(":feature:transactions:impl"))
    implementation(project(":feature:accounts:api"))
    implementation(project(":feature:accounts:impl"))
    implementation(project(":feature:reports:api"))
    implementation(project(":feature:reports:impl"))
    implementation(project(":feature:review:api"))
    implementation(project(":feature:review:impl"))
    implementation(project(":feature:capturestatus:api"))
    implementation(project(":feature:capturestatus:impl"))
    implementation(project(":feature:settings:api"))
    implementation(project(":feature:settings:impl"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.hilt.androidx.navigation.compose)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}
