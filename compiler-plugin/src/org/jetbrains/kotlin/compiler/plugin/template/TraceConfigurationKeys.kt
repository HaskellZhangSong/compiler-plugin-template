package org.jetbrains.kotlin.compiler.plugin.template

import org.jetbrains.kotlin.config.CompilerConfigurationKey

object TraceConfigurationKeys {
    /**
     * When true, ALL non-inline, non-external functions in the module are traced,
     * regardless of whether they carry the @Trace annotation.
     */
    val TRACE_ALL: CompilerConfigurationKey<Boolean> =
        CompilerConfigurationKey.create("trace all functions")
}

