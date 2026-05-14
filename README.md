# Kotlin Function Tracer

A Kotlin compiler plugin that automatically injects entry/exit trace logging into functions.  
Works on **JVM, JS, Wasm and Native** targets.

## Features

- Traces function entry (`>>> [TRACE] Entering …`) and exit (`<<< [TRACE] Exiting …`)
- Includes thread/worker ID in every trace line
- Configurable: trace **all** functions or only those annotated with `@Trace`
- Log to **stdout** (default) or to a **file**
- Zero runtime overhead on untraced functions — the compiler skips them entirely

---

## Quick start

### 1. Apply the plugin

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}
```

```kotlin
// build.gradle.kts
plugins {
    kotlin("multiplatform") version "2.3.20"   // or jvm / android
    id("dev.songzh.functiontracer") version "1.0.0"
}
```

### 2. Configure (optional)

```kotlin
functionTracer {
    // true  → instrument every non-inline function in the module (default)
    // false → only functions annotated with @Trace
    traceAll = true

    // Write trace output to a file instead of stdout (optional)
    logFile = "/tmp/trace.log"
}
```

### 3. Annotate (when `traceAll = false`)

```kotlin
import dev.songzh.functiontracer.Trace

@Trace
fun compute(x: Int): Int = x * 2
```

### 4. Run

Output written to stdout (or the configured file):

```
>>> [TRACE] Entering com.example.compute [thread=1]
<<< [TRACE] Exiting  com.example.compute [thread=1]
```

---

## File handle lifecycle

`traceLog` keeps an open file handle per thread (Native) or per JVM process (JVM) so  
`fopen`/file-open is called only once regardless of how many trace lines are written.  
All writes are flushed immediately — no data is lost if the process crashes.

Call `closeTraceLog()` (available on Native and JVM) to explicitly release the handle:

```kotlin
import dev.songzh.functiontracer.closeTraceLog

fun main() {
    runMyApp()
    closeTraceLog()   // optional; OS/JVM releases handles at process exit anyway
}
```

---

## Project structure

| Module | Purpose |
|---|---|
| [`compiler-plugin`](compiler-plugin/src) | IR transformer that injects trace calls |
| [`plugin-annotations`](plugin-annotations/src) | `@Trace` annotation + `traceLog` runtime helper (multiplatform) |
| [`gradle-plugin`](gradle-plugin/src) | Gradle DSL — `functionTracer { }` extension |

---

## Building

```sh
./gradlew build
```

## Running the Native sample

```sh
cd sample-native
./gradlew runDebugExecutableMacosArm64
```

## Running tests

```sh
./gradlew :compiler-plugin:test
```

Tests use the [Kotlin compiler test framework](https://github.com/JetBrains/kotlin/blob/master/compiler/test-infrastructure/ReadMe.md).  
Add `.kt` files under `compiler-plugin/testData/box` (codegen) or `compiler-plugin/testData/diagnostics`.  
Golden IR-dump files are created automatically on the first test run.

## Debugging (attach IntelliJ remote debugger)

```sh
cd sample-native
./gradlew assemble \
    -Dorg.gradle.debug=true \
    --no-daemon \
    -Pkotlin.compiler.execution.strategy=in-process
```

Connect a **Remote JVM Debug** run configuration to port **5005**.  
Install the [Kotlin Compiler DevKit](https://github.com/JetBrains/kotlin-compiler-devkit) plugin for the best IDE experience.

---

## License

Apache 2.0 — see [LICENSE.txt](LICENSE.txt).
