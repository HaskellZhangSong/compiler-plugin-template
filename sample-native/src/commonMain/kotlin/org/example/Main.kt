package org.example

import org.jetbrains.kotlin.compiler.plugin.template.Trace

// ── Top-level traced functions ────────────────────────────────────────────────

@Trace
fun computeFactorial(n: Long): Long {
    if (n <= 1) return 1L
    // exit is inserted before return branch
    var res: Long = n * computeFactorial(n - 1)
    return res
}

@Trace
fun computeFactorial2(n: Long): Long {
    if (n <= 1) return 1L
    // println is inserted before return statement
    return n * computeFactorial2(n - 1)
}

fun notrace() {
    println("funtion without @Trace annotation")
}

@Trace
fun runDemo() {
    println("========================================")
    println("  Kotlin/Native Function Call Tracer")
    println("========================================")
    println()

    // ── Calculator demo ───────────────────────────────────────────────────────
    println("── Calculator ───────────────────────────")
    val calc = Calculator()
    val sum = calc.add(10, 3)
    println("  add(10, 3)      = $sum")
    println()

    val diff = calc.subtract(10, 3)
    println("  subtract(10, 3) = $diff")
    println()

    val product = calc.multiply(10, 3)
    println("  multiply(10, 3) = $product")
    println()

    val quotient = calc.divide(10, 3)
    println("  divide(10, 3)   = $quotient")
    println()

    // isEven is NOT traced — no trace output expected.
    println("  isEven(4)       = ${calc.isEven(4)}  (not traced)")
    println()

    // ── Greeter demo ──────────────────────────────────────────────────────────
    println("── Greeter ──────────────────────────────")
    val greeter = Greeter("Hello")
    println("  ${greeter.greet("World")}")
    println()
    println("  ${greeter.greetOrDefault(null)}")
    println()
    println("  ${greeter.greetOrDefault("Kotlin Native")}")
    println()

    // ── Factorial demo (recursive traced function) ────────────────────────────
    println("── Factorial ────────────────────────────")
    val result = computeFactorial(5)
    println("  factorial(5) = $result")
    println()

    println("── Factorial2 ────────────────────────────")
    val result2 = computeFactorial2(5)
    println("  factorial(5) = $result2")
    println()

    println("── notrace ────────────────────────────")
    notrace()
}

fun main() {
    runDemo()
}

