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
// Built from source via the git submodule at smartscanner-mrz-parser/ (Gradle module lives in its parser/ dir).
project(":smartscanner-mrz-parser").projectDir = file("smartscanner-mrz-parser/parser")
include(":smartscanner-android-api")
