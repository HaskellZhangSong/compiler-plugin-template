package dev.songzh.functiontracer

import dev.songzh.functiontracer.runners.AbstractJsBoxTest
import dev.songzh.functiontracer.runners.AbstractJvmBoxTest
import dev.songzh.functiontracer.runners.AbstractJvmDiagnosticTest
import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5

fun main() {
    generateTestGroupSuiteWithJUnit5 {
        testGroup(testDataRoot = "compiler-plugin/testData", testsRoot = "compiler-plugin/test-gen") {
            testClass<AbstractJvmDiagnosticTest> {
                model("diagnostics")
            }

            testClass<AbstractJvmBoxTest> {
                model("box")
            }

            testClass<AbstractJsBoxTest> {
                model("box")
            }
        }
    }
}
