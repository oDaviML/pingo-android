plugins {
    id("pingo.android.library")
    id("pingo.android.hilt")
}

android {
    namespace = "com.pingo.core.notifications"
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:data"))
    implementation(project(":core:datastore"))
    implementation(project(":core:parser"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(project(":core:testing"))
    testImplementation(libs.kotlinx.coroutines.test)
}
