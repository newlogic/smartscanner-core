pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        mavenLocal()
        maven { url = uri("https://jitpack.io") }
        // For idpass-lite-java-android if needed
    }
}
rootProject.name = "idpass-smart-scanner"
include(":app")
include(":core-lib")
include(":smartscanner-mrz-parser")
include(":smartscanner-android-api")
