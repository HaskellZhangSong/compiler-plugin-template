plugins {
    // Use the same Kotlin version as the plugin itself.
    kotlin("multiplatform") version "2.3.20"

    // Apply the local function-tracer compiler plugin (resolved via includeBuild in settings).
    id("org.jetbrains.kotlin.compiler.plugin.template")
}

group = "org.example"
version = "1.0.0"

kotlin {
    // ── macOS targets ──────────────────────────────────────────────────────────
    // macosX64 is deprecated in Kotlin 2.3 (tier-3); use macosArm64 on Apple Silicon.
    macosArm64 {
        binaries { executable { entryPoint = "org.example.main" } }
    }

    // ── Linux targets ─────────────────────────────────────────────────────────
    linuxArm64 {
        binaries { executable { entryPoint = "org.example.main" } }
    }
    linuxX64 {
        binaries { executable { entryPoint = "org.example.main" } }
    }

    sourceSets {
        commonMain.dependencies {
            // plugin-annotations is added automatically by the compiler plugin,
            // but we declare it explicitly here so the IDE can resolve @Trace.
            implementation("org.jetbrains.kotlin.compiler.plugin.template:plugin-annotations:0.1.0-SNAPSHOT")
        }
    }
}

// ── Plugin configuration ─────────────────────────────────────────────────────
functionTracer {
    // false  → only functions annotated with @Trace are instrumented (default)
    // true   → every non-inline function in the module is instrumented
    traceAll = true
}
