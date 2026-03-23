pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "PageTurner"
include(":app")

// Feature modules
include(":feature:onboarding")
include(":feature:swipedeck")
include(":feature:tasteprofile")
include(":feature:readinglist")
include(":feature:bookdetail")

// Core modules
include(":core:network")
include(":core:ai")
include(":core:data")
include(":core:domain")
include(":core:ui")
include(":core:analytics")
include(":core:logging")
include(":core:testing")
