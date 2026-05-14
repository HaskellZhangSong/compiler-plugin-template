package dev.songzh.functiontracer

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.FILE
import platform.posix.fclose
import platform.posix.fflush
import platform.posix.fopen
import platform.posix.fputs

/**
 * Per-OS-thread cached file handle. Each thread that writes to a log file gets its own
 * [FILE*] so no mutex is needed. [fflush] is used after every write to prevent data loss,
 * so the OS-level close at process exit is sufficient for clean shutdown. Call
 * [closeTraceLog] explicitly if you need to release the handle early.
 */
@kotlin.native.concurrent.ThreadLocal
@OptIn(ExperimentalForeignApi::class)
private var cachedHandle: CPointer<FILE>? = null

@kotlin.native.concurrent.ThreadLocal
private var cachedPath: String = ""

/**
 * Explicitly flushes and closes the trace log file handle for the current thread.
 * This is optional — the OS will close the descriptor at process exit and all data
 * is flushed on every [traceLog] call, so no data will be lost even without calling this.
 */
@OptIn(ExperimentalForeignApi::class)
public fun closeTraceLog() {
    cachedHandle?.let { fclose(it) }
    cachedHandle = null
    cachedPath = ""
}

@OptIn(ExperimentalForeignApi::class)
public actual fun traceLog(message: String, logFile: String) {
    if (logFile.isEmpty()) {
        println(message)
        return
    }

    // Re-open only when the destination changes (e.g. log rotation or path switch).
    if (cachedHandle == null || cachedPath != logFile) {
        cachedHandle?.let { fclose(it) }
        cachedHandle = fopen(logFile, "a")
        cachedPath = logFile
    }

    cachedHandle?.let { file ->
        fputs(message + "\n", file)
        // Flush immediately so data is visible even if the process crashes.
        fflush(file)
    }
}
