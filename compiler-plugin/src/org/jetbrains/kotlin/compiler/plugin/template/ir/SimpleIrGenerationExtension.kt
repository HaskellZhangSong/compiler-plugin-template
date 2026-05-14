package dev.songzh.functiontracer.ir

import dev.songzh.functiontracer.TraceConfigurationKeys
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import java.io.File
import java.util.Properties

class FunctionTracerIrExtension(
    private val configuration: CompilerConfiguration,
) : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val opts = resolveOptions()
        moduleFragment.transformChildrenVoid(
            FunctionTracerTransformer(pluginContext, opts.traceAll, opts.logFile)
        )
    }

    // ---------------------------------------------------------------------------
    // Option resolution
    // ---------------------------------------------------------------------------

    private fun resolveOptions(): TraceOptions {
        // Primary path: CompilerConfiguration keys set by CommandLineProcessor.processOption().
        // This works for JVM and JS compilations.
        val cfgTraceAll = configuration.get(TraceConfigurationKeys.TRACE_ALL)
        val cfgLogFile  = configuration.get(TraceConfigurationKeys.LOG_FILE)
        if (cfgTraceAll != null && cfgLogFile != null) {
            return TraceOptions(traceAll = cfgTraceAll, logFile = cfgLogFile)
        }

        // Fallback path: for K2/Native in-process compilation the Kotlin compiler does
        // NOT call CommandLineProcessor.processOption() for -P plugin:... args, so the
        // configuration keys above are never populated.  The Gradle plugin writes
        // build/function-tracer/options.properties just before every compile task runs;
        // we locate it by walking up from the first source root.
        return loadFromPropertiesFile() ?: TraceOptions(traceAll = true, logFile = "")
    }

    private fun loadFromPropertiesFile(): TraceOptions? {
        val roots = sourceRootPaths()
        for (rootPath in roots) {
            val start = File(rootPath).let { if (it.isDirectory) it else it.parentFile ?: it }
            var dir: File? = start
            while (dir != null) {
                val f = File(dir, "build/function-tracer/options.properties")
                if (f.isFile) {
                    val props = Properties().apply { f.inputStream().use { load(it) } }
                    return TraceOptions(
                        traceAll = props.getProperty("traceAll")?.toBooleanStrictOrNull() ?: true,
                        logFile  = props.getProperty("logFile") ?: "",
                    )
                }
                dir = dir.parentFile
            }
        }
        return null
    }

    /** Extracts file paths from content roots via reflection (avoids hard dependency on CLI types). */
    private fun sourceRootPaths(): List<String> = runCatching {
        val mapField = configuration::class.java.getDeclaredField("map").also { it.isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val map = mapField.get(configuration) as Map<Any, Any?>
        val roots = map.entries.firstOrNull { it.key.toString() == "content roots" }?.value as? Iterable<*>
            ?: return emptyList()
        roots.mapNotNull { root ->
            root?.let { r ->
                runCatching {
                    r::class.java.methods.firstOrNull { it.name == "getPath" && it.parameterCount == 0 }
                        ?.invoke(r) as? String
                }.getOrNull()
            }
        }
    }.getOrDefault(emptyList())
}

private data class TraceOptions(val traceAll: Boolean, val logFile: String)
