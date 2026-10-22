pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "pingo-android"

include(":app")
include(":spike:capture")
include(":core:model")
include(":core:common")
include(":core:designsystem")
include(":core:ui")
include(":core:data")
include(":core:database")
include(":core:datastore")
include(":core:network")
include(":core:notifications")
include(":core:parser")
include(":core:auth")
include(":core:analytics")
include(":core:testing")
include(":sync")
include(":feature:onboarding:api")
include(":feature:onboarding:impl")
include(":feature:home:api")
include(":feature:home:impl")
include(":feature:transactions:api")
include(":feature:transactions:impl")
include(":feature:accounts:api")
include(":feature:accounts:impl")
include(":feature:reports:api")
include(":feature:reports:impl")
include(":feature:review:api")
include(":feature:review:impl")
include(":feature:capturestatus:api")
include(":feature:capturestatus:impl")
include(":feature:settings:api")
include(":feature:settings:impl")
