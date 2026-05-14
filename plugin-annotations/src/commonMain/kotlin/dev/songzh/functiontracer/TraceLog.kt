package dev.songzh.functiontracer

/**
 * Writes a trace [message] to [logFile] (appending) when [logFile] is non-empty,
 * or prints it to stdout when [logFile] is empty.
 *
 * On native targets this call is **non-blocking**: the message is enqueued and
 * written by a dedicated background thread in batches, eliminating per-call syscall
 * overhead in hot paths.
 */
public expect fun traceLog(message: String, logFile: String)

/**
 * Flushes all pending trace messages and releases open file handles.
 *
 * This is optional — all data is guaranteed to be written when the process exits
 * normally. Call this explicitly when you want to free file descriptors early or
 * ensure all buffered output is visible before the next operation.
 */
public expect fun closeTraceLog()
