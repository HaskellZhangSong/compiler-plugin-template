package dev.songzh.functiontracer

// JS is single-threaded and has no filesystem access; always print to console.
public actual fun traceLog(message: String, logFile: String): Unit = println(message)

/** No-op on JS — there are no file handles to close. */
public actual fun closeTraceLog(): Unit = Unit
