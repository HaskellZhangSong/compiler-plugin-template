package dev.songzh.functiontracer

@Suppress("DEPRECATION")
public actual fun traceCurrentThreadId(): String =
    Thread.currentThread().id.toString()

