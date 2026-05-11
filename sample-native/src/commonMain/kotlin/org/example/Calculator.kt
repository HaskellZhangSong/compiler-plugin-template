package org.example

import dev.songzh.functiontracer.Trace

/**
 * A simple calculator that demonstrates function-call tracing on Kotlin/Native.
 *
 * Functions annotated with @Trace will have entry/exit println calls injected
 * by the compiler plugin at compile time — no runtime reflection is needed.
 */
class Calculator {

    @Trace
    fun add(a: Int, b: Int): Int {
        return a + b
    }

    @Trace
    fun subtract(a: Int, b: Int): Int {
        return a - b
    }

    @Trace
    fun multiply(a: Int, b: Int): Int {
        return a * b
    }

    @Trace
    fun divide(a: Int, b: Int): Int {
        require(b != 0) { "Division by zero" }
        return a / b
    }

    // Not annotated with @Trace — this function is NOT traced.
    fun isEven(n: Int): Boolean = n % 2 == 0
}

