package dev.songzh.functiontracer

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import dev.songzh.functiontracer.ir.FunctionTracerIrExtension
import org.jetbrains.kotlin.config.CompilerConfiguration

class FunctionTracerComponentRegistrar : CompilerPluginRegistrar() {
    override val pluginId: String
        get() = BuildConfig.KOTLIN_PLUGIN_ID
    override val supportsK2: Boolean
        get() = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        // Default to true: trace all functions unless explicitly turned off.
        val traceAll = configuration.get(TraceConfigurationKeys.TRACE_ALL, true)
        IrGenerationExtension.registerExtension(FunctionTracerIrExtension(traceAll))
    }
}
