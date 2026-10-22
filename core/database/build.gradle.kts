plugins {
    id("pingo.android.library")
    id("pingo.android.room")
    id("pingo.android.hilt")
}

android {
    namespace = "com.pingo.core.database"
    sourceSets.getByName("androidTest").assets.srcDir("$projectDir/schemas")
}

dependencies {
    implementation(project(":core:model"))
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}
