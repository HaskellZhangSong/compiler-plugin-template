package dev.songzh.functiontracer

// JS is single-threaded and has no filesystem access; always print to console.
public actual fun traceLog(message: String, logFile: String): Unit = println(message)

