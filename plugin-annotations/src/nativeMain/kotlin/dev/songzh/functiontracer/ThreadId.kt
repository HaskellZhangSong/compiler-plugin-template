package dev.songzh.functiontracer

@OptIn(kotlin.native.concurrent.ObsoleteWorkersApi::class)
public actual fun traceCurrentThreadId(): String =
    kotlin.native.concurrent.Worker.current.id.toString().padStart(4)

