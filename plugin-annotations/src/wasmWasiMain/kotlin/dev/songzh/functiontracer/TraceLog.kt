package dev.songzh.functiontracer

public actual fun traceLog(message: String, logFile: String): Unit = println(message)

/** No-op on Wasm/WASI — there are no file handles to close. */
public actual fun closeTraceLog(): Unit = Unit
