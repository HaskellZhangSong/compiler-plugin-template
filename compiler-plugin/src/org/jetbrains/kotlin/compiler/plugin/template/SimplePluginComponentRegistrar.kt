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
        // Pass the configuration by reference. Options (traceAll, logFile) are read lazily
        // inside FunctionTracerIrExtension.generate(), which runs after all CLI option
        // processing is complete. The file-based fallback handles the K2/Native case where
        // CommandLineProcessor.processOption() is not called by the compiler.
        IrGenerationExtension.registerExtension(FunctionTracerIrExtension(configuration))
    }
}
