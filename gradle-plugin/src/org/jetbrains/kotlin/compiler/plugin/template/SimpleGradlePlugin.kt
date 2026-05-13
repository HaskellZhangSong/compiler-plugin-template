package dev.songzh.functiontracer

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import dev.songzh.functiontracer.BuildConfig.ANNOTATIONS_LIBRARY_COORDINATES
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import java.io.File
import java.util.Properties

@Suppress("unused") // Used via reflection.
class FunctionTracerPlugin : KotlinCompilerPluginSupportPlugin {
    override fun apply(target: Project) {
        val extension = target.extensions.create("functionTracer", FunctionTracerExtension::class.java)

        // K2/Native in-process compilation does not call CommandLineProcessor.processOption()
        // for -P plugin:... args.  Write a properties file before every Kotlin compile task so
        // FunctionTracerIrExtension can read the options regardless of the backend.
        target.tasks.withType(KotlinCompilationTask::class.java).configureEach { task ->
            task.doFirst {
                val optionsDir = target.layout.buildDirectory.dir("function-tracer").get().asFile
                optionsDir.mkdirs()
                Properties().apply {
                    setProperty("traceAll", extension.traceAll.get().toString())
                    setProperty("logFile",  extension.logFile.get())
                }.store(File(optionsDir, "options.properties").outputStream(),
                    "Function tracer compiler plugin options")
            }
        }
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

    override fun getCompilerPluginId(): String = BuildConfig.KOTLIN_PLUGIN_ID

    override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
        groupId = BuildConfig.KOTLIN_PLUGIN_GROUP,
        artifactId = BuildConfig.KOTLIN_PLUGIN_NAME,
        version = BuildConfig.KOTLIN_PLUGIN_VERSION,
    )

    override fun applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>
    ): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project
        val extension = project.extensions.getByType(FunctionTracerExtension::class.java)

        @Suppress("DEPRECATION")
        kotlinCompilation.dependencies { implementation(ANNOTATIONS_LIBRARY_COORDINATES) }
        @Suppress("DEPRECATION")
        if (kotlinCompilation.implementationConfigurationName == "metadataCompilationImplementation") {
            project.dependencies.add("commonMainImplementation", ANNOTATIONS_LIBRARY_COORDINATES)
        }

        // These SubpluginOptions become -P plugin:id:key=value compiler args.
        // On JVM/JS the Kotlin compiler calls CommandLineProcessor.processOption() for each.
        // On K2/Native the fallback options.properties file (written in apply()) is used.
        return project.provider {
            listOf(
                SubpluginOption(key = "traceAll", value = extension.traceAll.get().toString()),
                SubpluginOption(key = "logFile",  value = extension.logFile.get()),
            )
        }
    }
}
