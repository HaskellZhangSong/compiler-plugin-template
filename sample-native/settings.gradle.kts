pluginManagement {
    // Makes the gradle-plugin available as a Kotlin compiler plugin (plugin resolution).
    includeBuild("..")

    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

// A second includeBuild at the settings level is required so that Gradle can
// substitute *library* dependencies (compiler-plugin JAR + plugin-annotations)
// that are added by the Gradle plugin to the compiler classpath / source set.
includeBuild("..")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "sample-native"
