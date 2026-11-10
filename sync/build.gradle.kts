plugins {
    id("pingo.android.library")
    id("pingo.android.hilt")
}

android {
    namespace = "com.pingo.sync"
}

dependencies {
    implementation(project(":core:auth"))
    implementation(project(":core:common"))
    implementation(project(":core:data"))
    implementation(project(":core:database"))
    implementation(project(":core:datastore"))
    implementation(project(":core:network"))
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.hilt.androidx.work)
    implementation(libs.room.ktx)
    add("ksp", libs.hilt.androidx.compiler)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotlinx.serialization.json)
    testImplementation(libs.androidx.work.testing)
    androidTestImplementation(libs.androidx.work.testing)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}
