package org.jetbrains.kotlin.compiler.plugin.template

/**
 * Marks a function for call tracing.
 *
 * When this annotation is applied to a function, the compiler plugin will
 * automatically inject trace logging at function entry and exit points.
 *
 * Example:
 * ```kotlin
 * @Trace
 * fun compute(x: Int): Int {
 *     return x * 2
 * }
 * // Output at runtime:
 * // >>> [TRACE] Entering compute
 * // <<< [TRACE] Exiting compute
 * ```
 *
 * To trace ALL functions in a module without annotation, set `traceAll = true`
 * in the Gradle plugin configuration:
 * ```kotlin
 * functionTracer {
 *     traceAll = true
 * }
 * ```
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
public annotation class Trace

