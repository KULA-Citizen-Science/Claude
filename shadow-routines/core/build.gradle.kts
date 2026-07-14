plugins {
    alias(libs.plugins.kotlin.jvm)
}

// No explicit jvmToolchain: Kotlin's default target (bytecode 1.8) stays comfortably
// below :app's JVM 17 target so the Android module can consume this module's classes
// without a bytecode-version mismatch, on any JDK >= 17 building this project.

dependencies {
    testImplementation(libs.junit)
}
