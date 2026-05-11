package org.jetbrains.kotlin.compiler.plugin.template

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * Gradle DSL extension for the function tracer compiler plugin.
 *
 * Usage in build.gradle.kts:
 * ```kotlin
 * functionTracer {
 *     // Trace every non-inline function in the module (default)
 *     traceAll = true
 *
 *     // OR opt into annotation-based tracing only
 *     traceAll = false
 * }
 * ```
 */
open class SimpleGradleExtension @Inject constructor(objectFactory: ObjectFactory) {
    /**
     * When true, all non-inline, non-external functions in the compiled module
     * are traced, regardless of @Trace annotation.
     * Defaults to true.
     */
    val traceAll: Property<Boolean> = objectFactory.property(Boolean::class.java).convention(true)
}
