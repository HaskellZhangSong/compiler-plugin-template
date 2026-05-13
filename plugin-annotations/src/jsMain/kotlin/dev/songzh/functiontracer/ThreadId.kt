package dev.songzh.functiontracer

// JS is single-threaded; return a fixed identifier.
public actual fun traceCurrentThreadId(): String = "0"

