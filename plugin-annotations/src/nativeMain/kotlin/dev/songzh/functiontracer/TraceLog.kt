package dev.songzh.functiontracer

import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fputs

@OptIn(ExperimentalForeignApi::class)
public actual fun traceLog(message: String, logFile: String) {
    if (logFile.isEmpty()) {
        println(message)
    } else {
        val file = fopen(logFile, "a")
        if (file != null) {
            fputs(message + "\n", file)
            fclose(file)
        }
    }
}

