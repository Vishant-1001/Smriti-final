pluginManagement {
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

rootProject.name = "Smriti"
include(":app")
// Experimental SCOPE research engine: pure Kotlin/JVM, not a dependency of :app (off the gameplay path).
include(":scope")
// SMRITI Fit: explicit-needs presentation solver. Pure Kotlin/JVM, no Android, Compose or network.
include(":fit")
