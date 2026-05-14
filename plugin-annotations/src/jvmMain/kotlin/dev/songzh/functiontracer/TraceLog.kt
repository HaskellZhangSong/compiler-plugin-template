package dev.songzh.functiontracer

import java.io.BufferedWriter
import java.io.FileWriter
import java.util.concurrent.ConcurrentHashMap

/** One open [BufferedWriter] per log-file path, shared across all threads. */
private val writers = ConcurrentHashMap<String, BufferedWriter>()

/**
 * Explicitly flushes and closes all open log file handles for this JVM process.
 * Optional — the JVM shuts them down at exit, and every write is flushed immediately.
 */
public fun closeTraceLog() {
    writers.values.forEach { it.close() }
    writers.clear()
}

public actual fun traceLog(message: String, logFile: String) {
    if (logFile.isEmpty()) {
        println(message)
        return
    }
    val writer = writers.getOrPut(logFile) {
        BufferedWriter(FileWriter(logFile, /* append = */ true))
    }
    synchronized(writer) {
        writer.write(message)
        writer.newLine()
        writer.flush()
    }
}
