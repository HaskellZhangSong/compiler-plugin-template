package dev.songzh.functiontracer

import java.io.BufferedWriter
import java.io.FileWriter
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

// ---------------------------------------------------------------------------
// Async writer (JVM)
//
// A single-threaded ExecutorService serialises all file writes on a dedicated
// daemon thread.  traceLog() submits a task and returns immediately; the
// executor drains its queue in order, keeping one BufferedWriter open per
// destination file.
// ---------------------------------------------------------------------------

private object Writer {
    /** Accessed only from the single writer thread — no synchronisation needed. */
    val files = HashMap<String, BufferedWriter>()

    val executor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "function-tracer-writer").also { it.isDaemon = true }
    }

    init {
        // Best-effort drain on normal JVM exit (mirrors native's atexit hook).
        Runtime.getRuntime().addShutdownHook(
            Thread({ closeTraceLog() }, "function-tracer-shutdown")
        )
    }
}

public actual fun traceLog(message: String, logFile: String) {
    Writer.executor.execute {
        if (logFile.isEmpty()) {
            println(message)
        } else {
            val w = Writer.files.getOrPut(logFile) {
                BufferedWriter(FileWriter(logFile, /* append = */ true))
            }
            w.write(message)
            w.newLine()
            // No flush per write — the 8 KB BufferedWriter drains automatically
            // when full.  Force a flush + close via closeTraceLog().
        }
    }
}

/**
 * Waits for all pending trace messages to be written to disk, then flushes and
 * closes every log file handle.
 *
 * Calling this is **optional** — a shutdown hook flushes everything on normal
 * JVM exit.  Call it explicitly when you need data on disk immediately.
 * Safe to call multiple times; subsequent calls return immediately.
 */
public actual fun closeTraceLog() {
    if (Writer.executor.isShutdown) return
    Writer.executor.execute {
        Writer.files.values.forEach { runCatching { it.flush(); it.close() } }
        Writer.files.clear()
    }
    Writer.executor.shutdown()
    Writer.executor.awaitTermination(30, TimeUnit.SECONDS)
}
