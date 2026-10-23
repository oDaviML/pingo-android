import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            configureModuleCoordinates()
            pluginManager.apply("com.android.library")
            pluginManager.apply("org.jetbrains.kotlin.android")
            extensions.getByType(LibraryExtension::class.java).apply {
                compileSdk = compileSdkVersion()
                defaultConfig {
                    minSdk = minSdkVersion()
                    testInstrumentationRunner = AndroidConfig.TEST_INSTRUMENTATION_RUNNER
                    manifestPlaceholders.putIfAbsent("auth0Domain", "")
                    manifestPlaceholders.putIfAbsent("auth0Scheme", "pingo")
                }
                compileOptions {
                    sourceCompatibility = javaVersion()
                    targetCompatibility = javaVersion()
                }
            }
            configureKotlinCompilerOptions()
            dependencies.add("testImplementation", libs.findLibrary("junit").get())
        }
    }
}
