import androidx.room.gradle.RoomExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidRoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("androidx.room")
            pluginManager.apply("com.google.devtools.ksp")
            val schemasDirectory = file("schemas").absolutePath
            extensions.getByType(RoomExtension::class.java).schemaDirectory(schemasDirectory)
            dependencies.add("implementation", libs.findLibrary("room-runtime").get())
            dependencies.add("implementation", libs.findLibrary("room-ktx").get())
            dependencies.add("ksp", libs.findLibrary("room-compiler").get())
            dependencies.add("implementation", libs.findLibrary("sqlcipher-android").get())
            dependencies.add("implementation", libs.findLibrary("androidx-sqlite").get())
            dependencies.add("testImplementation", libs.findLibrary("room-testing").get())
        }
    }
}
