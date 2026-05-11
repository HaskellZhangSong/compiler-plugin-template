package org.example

import dev.songzh.functiontracer.Trace

/**
 * A simple greeter that shows tracing on functions with String return values
 * and on functions with multiple return paths.
 */
class Greeter(private val prefix: String) {

    @Trace
    fun greet(name: String): String {
        return "$prefix, $name!"
    }

    @Trace
    fun greetOrDefault(name: String?): String {
        if (name == null || name.isBlank()) {
            return "$prefix, stranger!"
        }
        return "$prefix, $name!"
    }
}

