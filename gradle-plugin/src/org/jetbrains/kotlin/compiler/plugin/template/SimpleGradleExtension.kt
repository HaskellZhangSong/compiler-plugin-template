package dev.songzh.functiontracer

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * Gradle DSL extension for the Kotlin Function Tracer compiler plugin.
 *
 * ```kotlin
 * functionTracer {
 *     traceAll = true                        // trace every non-inline function (default)
 *     traceAll = false                       // opt-in mode: only @Trace-annotated functions
 *     logFile  = "/tmp/trace.log"            // write to file instead of stdout (optional)
 * }
 * ```
 */
open class FunctionTracerExtension @Inject constructor(objectFactory: ObjectFactory) {
    /** When true, all non-inline, non-external functions are traced. Defaults to true. */
    val traceAll: Property<Boolean> = objectFactory.property(Boolean::class.java).convention(true)

    /**
     * Optional path to the log file.
     * When set, trace output is appended to this file instead of being printed to stdout.
     * Defaults to empty string (= stdout).
     */
    val logFile: Property<String> = objectFactory.property(String::class.java).convention("")
}
