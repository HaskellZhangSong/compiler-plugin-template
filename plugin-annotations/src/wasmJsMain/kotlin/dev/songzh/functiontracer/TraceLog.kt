package dev.songzh.functiontracer

public actual fun traceLog(message: String, logFile: String): Unit = println(message)

