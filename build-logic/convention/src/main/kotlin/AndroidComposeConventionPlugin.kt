import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
            pluginManager.withPlugin("com.android.application") {
                extensions.getByType(ApplicationExtension::class.java).buildFeatures.compose = true
            }
            pluginManager.withPlugin("com.android.library") {
                extensions.getByType(LibraryExtension::class.java).buildFeatures.compose = true
            }
            dependencies.add("implementation", dependencies.platform(libs.findLibrary("compose-bom").get()))
            dependencies.add("implementation", libs.findLibrary("compose-ui").get())
            dependencies.add("implementation", libs.findLibrary("compose-ui-tooling-preview").get())
            dependencies.add("implementation", libs.findLibrary("compose-foundation").get())
            dependencies.add("debugImplementation", libs.findLibrary("compose-ui-tooling").get())
            dependencies.add("androidTestImplementation", dependencies.platform(libs.findLibrary("compose-bom").get()))
            dependencies.add("androidTestImplementation", libs.findLibrary("compose-ui-test-junit4").get())
        }
    }
}
