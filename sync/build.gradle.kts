plugins {
    id("pingo.android.library")
    id("pingo.android.hilt")
}

android {
    namespace = "com.pingo.sync"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:data"))
    implementation(project(":core:datastore"))
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.hilt.androidx.work)
    add("ksp", libs.hilt.androidx.compiler)
    testImplementation(libs.androidx.work.testing)
}
