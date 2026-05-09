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
 *     // Trace only functions annotated with @Trace (default)
 *     traceAll = false
 *
 *     // OR trace every non-inline function in the module
 *     traceAll = true
 * }
 * ```
 */
open class SimpleGradleExtension @Inject constructor(objectFactory: ObjectFactory) {
    /**
     * When true, ALL non-inline, non-external functions in the compiled module
     * are traced, regardless of whether they carry the @Trace annotation.
     * Defaults to false (annotation-based opt-in).
     */
    val traceAll: Property<Boolean> = objectFactory.property(Boolean::class.java).convention(false)
}
