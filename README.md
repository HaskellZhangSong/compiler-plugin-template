# Kotlin Compiler Plugin template

This is a template project for writing a compiler plugin for the Kotlin compiler.

## Details

This project has three modules:
- The [`:compiler-plugin`](compiler-plugin/src) module contains the compiler plugin itself.
- The [`:plugin-annotations`](plugin-annotations/src/commonMain/kotlin) module contains annotations which can be used in
user code for interacting with compiler plugin.
- The [`:gradle-plugin`](gradle-plugin/src) module contains a simple Gradle plugin to add the compiler plugin and
annotation dependency to a Kotlin project. 

Extension point registration:
- K2 Frontend (FIR) extensions can be registered in `SimplePluginRegistrar`.
- All other extensions (including K1 frontend and backend) can be registered in `SimplePluginComponentRegistrar`.

## Function tracer changes

| File | What changed |
| --- | --- |
| `compiler-plugin/src/org/jetbrains/kotlin/compiler/plugin/template/TraceConfigurationKeys.kt` | Added `TRACE_ALL` compiler configuration key. |
| `compiler-plugin/src/org/jetbrains/kotlin/compiler/plugin/template/SimpleCommandLineProcessor.kt` | Added `--traceAll` CLI option and stores it in compiler configuration. |
| `compiler-plugin/src/org/jetbrains/kotlin/compiler/plugin/template/SimplePluginComponentRegistrar.kt` | Reads `traceAll` from config and passes it to IR extension registration. |
| `compiler-plugin/src/org/jetbrains/kotlin/compiler/plugin/template/ir/FunctionTracerTransformer.kt` | Implements IR transformation to inject function entry/exit trace `println` calls. |
| `compiler-plugin/src/org/jetbrains/kotlin/compiler/plugin/template/ir/SimpleIrGenerationExtension.kt` | Runs `FunctionTracerTransformer` with `transformChildrenVoid`. |
| `plugin-annotations/src/commonMain/kotlin/org/jetbrains/kotlin/compiler/plugin/template/Trace.kt` | Added `@Trace` annotation for opt-in tracing. |
| `gradle-plugin/src/org/jetbrains/kotlin/compiler/plugin/template/SimpleGradleExtension.kt` | Added Gradle DSL property `traceAll`. |
| `gradle-plugin/src/org/jetbrains/kotlin/compiler/plugin/template/SimpleGradlePlugin.kt` | Added `functionTracer {}` extension and passes `traceAll` as subplugin option. |


gradle-plugin — DSL

| File | What changed |
| --- | --- |
| `SimpleGradleExtension.kt` | Adds `traceAll: Property<Boolean>` (default `false`). |
| `SimpleGradlePlugin.kt` | Passes `traceAll` as a `SubpluginOption`; DSL block renamed to `functionTracer`. |

## Tests

The [Kotlin compiler test framework][test-framework] is set up for this project.
To create a new test, add a new `.kt` file in a [compiler-plugin/testData](compiler-plugin/testData) sub-directory:
`testData/box` for codegen tests and `testData/diagnostics` for diagnostics tests.
The generated JUnit 5 test classes will be updated automatically when tests are next run.
They can be manually updated with the `generateTests` Gradle task as well.
To aid in running tests, it is recommended to install the [Kotlin Compiler DevKit][test-plugin] IntelliJ plugin,
which is pre-configured in this repository.

[//]: # (Links)

## Build Plugin

```sh
./gradlew :compiler-plugin:compileKotlin
```

## Run Trace Example
```bash
cd sample-native
./gradlew runDebugExecutableMacosArm64
```

## Debug

```sh
cd sample-native
 ./gradlew assemble -Dorg.gradle.debug=true --no-daemon -Pkotlin.compiler.execution.strategy=in-process      
```
Then use remote debugger to connect to the process on port 5005 in IntelliJ.

[test-framework]: https://github.com/JetBrains/kotlin/blob/master/compiler/test-infrastructure/ReadMe.md
[test-plugin]: https://github.com/JetBrains/kotlin-compiler-devkit
