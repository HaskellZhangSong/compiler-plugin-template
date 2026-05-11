pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "kotlin-function-tracer"

include("compiler-plugin")
include("gradle-plugin")
include("plugin-annotations")
