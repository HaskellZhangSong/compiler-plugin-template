package dev.songzh.functiontracer

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * Gradle DSL extension for the Kotlin Function Tracer compiler plugin.
 *
 * ```kotlin
 * functionTracer {
 *     traceAll = true   // trace every non-inline function (default)
 *     traceAll = false  // opt-in mode: only @Trace-annotated functions are traced
 * }
 * ```
 */
open class FunctionTracerExtension @Inject constructor(objectFactory: ObjectFactory) {
    /**
     * When true, all non-inline, non-external functions are traced.
     * Defaults to true.
     */
    val traceAll: Property<Boolean> = objectFactory.property(Boolean::class.java).convention(true)
}
