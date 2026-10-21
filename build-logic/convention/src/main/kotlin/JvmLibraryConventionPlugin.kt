import org.gradle.api.Plugin
import org.gradle.api.Project

class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            configureModuleCoordinates()
            pluginManager.apply("org.jetbrains.kotlin.jvm")
            configureJvmJavaCompatibility()
            dependencies.add("testImplementation", libs.findLibrary("junit").get())
        }
    }
}
