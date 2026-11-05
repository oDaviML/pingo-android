plugins {
    id("pingo.android.library")
    id("pingo.android.hilt")
}

android {
    namespace = "com.pingo.core.data"
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:auth"))
    implementation(project(":core:common"))
    implementation(project(":core:database"))
    implementation(project(":core:datastore"))
    implementation(project(":core:network"))
    implementation(libs.room.runtime)
    implementation(libs.kotlinx.coroutines.android)
    androidTestImplementation(libs.androidx.test.runner)
}
