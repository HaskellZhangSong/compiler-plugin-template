package dev.songzh.functiontracer

import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.config.CompilerConfiguration

@Suppress("unused") // Used via reflection.
class FunctionTracerCommandLineProcessor : CommandLineProcessor {
    companion object {
        const val OPTION_TRACE_ALL = "traceAll"
        const val OPTION_LOG_FILE  = "logFile"
    }

    override val pluginId: String
        get() = BuildConfig.KOTLIN_PLUGIN_ID

    override val pluginOptions: Collection<CliOption>
        get() = listOf(
            CliOption(
                optionName = OPTION_TRACE_ALL,
                valueDescription = "<true|false>",
                description = "When true, trace ALL non-inline functions in the module instead of only @Trace-annotated ones.",
                required = false,
                allowMultipleOccurrences = false,
            ),
            CliOption(
                optionName = OPTION_LOG_FILE,
                valueDescription = "<path>",
                description = "Path to the log file. When set, trace output is appended to this file instead of stdout.",
                required = false,
                allowMultipleOccurrences = false,
            ),
        )

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) {
        when (option.optionName) {
            OPTION_TRACE_ALL -> configuration.put(TraceConfigurationKeys.TRACE_ALL, value.toBoolean())
            OPTION_LOG_FILE  -> configuration.put(TraceConfigurationKeys.LOG_FILE, value)
            else -> error("Unexpected config option: '${option.optionName}'")
        }
    }
}
