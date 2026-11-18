import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

internal object AndroidConfig {
    const val TEST_INSTRUMENTATION_RUNNER = "androidx.test.runner.AndroidJUnitRunner"
    const val GROUP_PREFIX = "com.pingo"
    const val PROJECT_VERSION = "0.2.0"
}

internal val Project.libs: VersionCatalog
    get() = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

internal fun Project.configureModuleCoordinates() {
    val segments = path.split(":").filter { it.isNotBlank() }
    group = segments.joinToString(".") { it.replace("-", "") }.let { "${AndroidConfig.GROUP_PREFIX}.$it" }
    version = AndroidConfig.PROJECT_VERSION
}

internal fun Project.catalogVersion(name: String): String = libs.findVersion(name).get().requiredVersion

internal fun Project.compileSdkVersion(): Int = catalogVersion("androidCompileSdk").toInt()

internal fun Project.minSdkVersion(): Int = catalogVersion("androidMinSdk").toInt()

internal fun Project.targetSdkVersion(): Int = catalogVersion("androidTargetSdk").toInt()

internal fun Project.jvmTargetVersion(): String = catalogVersion("jvmTarget")

internal fun Project.javaVersion(): JavaVersion = JavaVersion.toVersion(jvmTargetVersion())

internal fun Project.configureKotlinCompilerOptions() {
    tasks.withType(KotlinCompile::class.java).configureEach { task ->
        task.compilerOptions.jvmTarget.set(JvmTarget.fromTarget(jvmTargetVersion()))
    }
}

internal fun Project.configureJvmJavaCompatibility() {
    extensions.getByType(JavaPluginExtension::class.java).apply {
        sourceCompatibility = javaVersion()
        targetCompatibility = javaVersion()
    }
    configureKotlinCompilerOptions()
}
