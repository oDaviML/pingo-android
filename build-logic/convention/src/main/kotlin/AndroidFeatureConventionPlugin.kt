import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("pingo.android.library")
            pluginManager.apply("pingo.android.compose")
            pluginManager.apply("pingo.android.hilt")
            dependencies.add("implementation", project(":core:model"))
            dependencies.add("implementation", project(":core:ui"))
            dependencies.add("implementation", project(":core:designsystem"))
            dependencies.add("implementation", libs.findLibrary("androidx-navigation-compose").get())
            dependencies.add("implementation", libs.findLibrary("androidx-lifecycle-runtime-compose").get())
            dependencies.add("implementation", libs.findLibrary("hilt-androidx-navigation-compose").get())
        }
    }
}
