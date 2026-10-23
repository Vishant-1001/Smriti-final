// SMRITI Fit core: explicit-needs presentation solver (docs/flagship/FIT_SPEC.md).
// Pure Kotlin on the JVM: no Android, Compose, AWT or network. Used by :app (radio screen, behind
// FitFlags) and by :fit-bench (benchmark and validation harness).
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation(libs.junit)
}
