// RUN_PIPELINE_TILL: FRONTEND

import dev.songzh.functiontracer.Trace

// @Trace on a non-function target should produce no diagnostic (annotation is @Target(FUNCTION)).
// The plugin only instruments functions; class members are fine.

@Trace
fun add(a: Int, b: Int): Int = a + b
