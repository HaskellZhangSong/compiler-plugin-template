package dev.songzh.functiontracer

/**
 * Writes a trace [message] to [logFile] (appending) when [logFile] is non-empty,
 * or prints it to stdout when [logFile] is empty.
 */
public expect fun traceLog(message: String, logFile: String)

