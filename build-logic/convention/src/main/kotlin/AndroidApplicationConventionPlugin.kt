import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            configureModuleCoordinates()
            pluginManager.apply("com.android.application")
            pluginManager.apply("org.jetbrains.kotlin.android")
            extensions.getByType(ApplicationExtension::class.java).apply {
                compileSdk = compileSdkVersion()
                defaultConfig {
                    minSdk = minSdkVersion()
                    targetSdk = targetSdkVersion()
                    testInstrumentationRunner = AndroidConfig.TEST_INSTRUMENTATION_RUNNER
                }
                compileOptions {
                    sourceCompatibility = javaVersion()
                    targetCompatibility = javaVersion()
                }
            }
            configureKotlinCompilerOptions()
        }
    }
}
