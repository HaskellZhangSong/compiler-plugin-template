package dev.songzh.functiontracer

import java.io.File

public actual fun traceLog(message: String, logFile: String) {
    if (logFile.isEmpty()) {
        println(message)
    } else {
        File(logFile).appendText(message + "\n")
    }
}

