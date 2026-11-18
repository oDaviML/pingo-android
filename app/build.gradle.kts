import java.util.Properties

plugins {
    id("pingo.android.application")
    id("pingo.android.compose")
    id("pingo.android.hilt")
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

fun localProperty(key: String, fallback: String): String = localProperties.getProperty(key) ?: System.getenv(key) ?: fallback

val versaoDoBuild = providers.gradleProperty("pingo.versionName").orNull ?: "0.4.0"

android {
    namespace = "com.pingo"
    defaultConfig {
        applicationId = "com.pingo"
        versionCode = 4
        versionName = versaoDoBuild
        buildConfigField("String", "AUTH0_DOMAIN", "\"${localProperty("pingo.auth0.domain", "")}\"")
        buildConfigField("String", "AUTH0_CLIENT_ID", "\"${localProperty("pingo.auth0.clientId", "")}\"")
        buildConfigField("String", "AUTH0_SCHEME", "\"${localProperty("pingo.auth0.scheme", "pingo")}\"")
        buildConfigField("String", "AUTH0_AUDIENCE", "\"${localProperty("pingo.auth0.audience", "https://api.pingo.app/")}\"")
        buildConfigField(
            "String",
            "AUTH0_GOOGLE_WEB_CLIENT_ID",
            "\"${localProperty("pingo.auth0.googleWebClientId", "")}\"",
        )
        buildConfigField("String", "API_BASE_URL", "\"${localProperty("pingo.api.baseUrl", "http://10.0.2.2:8080/")}\"")
        manifestPlaceholders["auth0Domain"] = localProperty("pingo.auth0.domain", "")
        manifestPlaceholders["auth0Scheme"] = localProperty("pingo.auth0.scheme", "pingo")
    }
    buildTypes {
        release {
            buildConfigField("String", "API_BASE_URL", "\"https://api-pingo.davimartinslage.com.br/\"")
        }
    }
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core:analytics"))
    implementation(project(":core:auth"))
    implementation(project(":core:common"))
    implementation(project(":core:datastore"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:network"))
    implementation(project(":core:notifications"))
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
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.hilt.androidx.navigation.compose)
    implementation(libs.hilt.androidx.work)
    implementation(libs.androidx.work.runtime.ktx)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(project(":core:auth"))
    androidTestImplementation(project(":core:common"))
    androidTestImplementation(project(":core:designsystem"))
    androidTestImplementation(project(":core:ui"))
    androidTestImplementation(project(":feature:accounts:impl"))
    androidTestImplementation(project(":feature:capturestatus:impl"))
    androidTestImplementation(project(":feature:transactions:impl"))
    androidTestImplementation(libs.androidx.activity.compose)
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
    androidTestImplementation(project(":core:data"))
    androidTestImplementation(project(":core:database"))
    androidTestImplementation(project(":core:datastore"))
    androidTestImplementation(project(":core:model"))
    androidTestImplementation(project(":core:network"))
    androidTestImplementation(project(":core:notifications"))
    androidTestImplementation(project(":core:parser"))
    androidTestImplementation(project(":sync"))
    androidTestImplementation(libs.room.ktx)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.kotlinx.serialization.json)
}
