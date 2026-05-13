package dev.songzh.functiontracer

/**
 * Returns a string identifying the current thread (or worker/coroutine context).
 * The format is platform-specific.
 */
public expect fun traceCurrentThreadId(): String

