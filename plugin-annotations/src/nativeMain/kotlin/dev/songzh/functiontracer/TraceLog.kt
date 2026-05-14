package dev.songzh.functiontracer

import kotlinx.cinterop.*
import platform.posix.*

// ---------------------------------------------------------------------------
// Async writer thread
//
// Producer threads call traceLog() which simply appends to a shared ArrayList
// under a POSIX mutex and signals the condition variable.
//
// A single background thread wakes up, grabs the whole batch in one lock, then
// writes everything to the appropriate files outside the lock.  File handles are
// kept open across batches so every write is just fputs + fflush-per-batch.
// ---------------------------------------------------------------------------

/**
 * A log entry: the destination file path (empty = stdout) and the message text.
 *
 * Stored as two interleaved elements in [AsyncWriter.buf] — even index = path,
 * odd index = message — to avoid allocating a wrapper object per log call.
 */

@OptIn(ExperimentalForeignApi::class)
private object AsyncWriter {
    // Synchronisation primitives on the native heap — they live for the whole
    // process so we never free them.
    val mutex: pthread_mutex_t = nativeHeap.alloc()
    val cond:  pthread_cond_t  = nativeHeap.alloc()
    val tid:   pthread_tVar    = nativeHeap.alloc()

    /** Pending (path, message) pairs, interleaved: [path0, msg0, path1, msg1, …]. */
    val buf: ArrayList<String> = ArrayList(10 * 1024)

    @kotlin.concurrent.Volatile var shutdown = false
    @kotlin.concurrent.Volatile var stopped  = false

    init {
        pthread_mutex_init(mutex.ptr, null)
        pthread_cond_init(cond.ptr, null)

        // Start the background writer thread.
        pthread_create(tid.ptr, null, staticCFunction { _: COpaquePointer? ->
            AsyncWriter.loop()
            null
        }, null)

        // Register a best-effort drain at normal process exit so callers don't
        // have to remember to call closeTraceLog().
        atexit(staticCFunction<Unit> { AsyncWriter.stop() })
    }

    /** Enqueue one (path, message) pair — called from any thread. Non-blocking. */
    fun enqueue(path: String, message: String) {
        pthread_mutex_lock(mutex.ptr)
        buf.add(path)
        buf.add(message)
        pthread_cond_signal(cond.ptr)
        pthread_mutex_unlock(mutex.ptr)
    }

    /**
     * Signal shutdown, wait for the writer to drain and exit, then return.
     * Safe to call more than once — subsequent calls are no-ops.
     */
    fun stop() {
        if (stopped) return
        stopped = true
        pthread_mutex_lock(mutex.ptr)
        shutdown = true
        pthread_cond_signal(cond.ptr)
        pthread_mutex_unlock(mutex.ptr)
        pthread_join(tid.value, null)
    }

    /** Body of the background writer thread. */
    fun loop() {
        // Each writer thread owns its file-handle map — no locking needed here.
        val files = HashMap<String, CPointer<FILE>>()
        val batch = ArrayList<String>(512)

        while (true) {
            // ---- wait for work ----
            pthread_mutex_lock(mutex.ptr)
            while (buf.isEmpty() && !shutdown) {
                pthread_cond_wait(cond.ptr, mutex.ptr)
            }
            // Swap out the buffer in O(1) — producers can keep enqueuing
            // immediately while we write the current snapshot.
            batch.addAll(buf)
            buf.clear()
            val done = shutdown
            pthread_mutex_unlock(mutex.ptr)

            // ---- write batch outside the lock ----
            var i = 0
            while (i < batch.size) {
                val path = batch[i]
                val msg  = batch[i + 1]
                i += 2

                if (path.isEmpty()) {
                    println(msg)
                } else {
                    var f = files[path]
                    if (f == null) {
                        f = fopen(path, "a")
                        if (f != null) files[path] = f
                    }
                    f?.let { fputs(msg + "\n", it) }
                }
            }
            batch.clear()

            // One fflush per file per batch — far cheaper than fflush-per-line.
            files.values.forEach { fflush(it) }

            if (done) break
        }

        // Drain complete — close every file handle cleanly.
        files.values.forEach { fclose(it) }
    }
}

// ---------------------------------------------------------------------------
// Public API
// ---------------------------------------------------------------------------

public actual fun traceLog(message: String, logFile: String) {
    AsyncWriter.enqueue(logFile, message)
}

/**
 * Blocks until all enqueued trace messages have been written to disk and all
 * log file handles have been closed.  Subsequent [traceLog] calls after this
 * point are silently dropped (the writer thread has already exited).
 *
 * Calling this is **optional** — a best-effort drain is registered via
 * `atexit` automatically when the first [traceLog] call occurs.
 */
public actual fun closeTraceLog() {
    AsyncWriter.stop()
}
