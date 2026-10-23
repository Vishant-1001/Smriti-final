// SCOPE v2.1 research engine. Pure Kotlin on the JVM: no Android, Compose or network.
// Not a dependency of :app, so it cannot affect gameplay (scope.v2.enabled = false).
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation(libs.junit)
}

// Runs the frozen benchmark protocol and writes docs/scope/SCOPE_BENCHMARK_RESULTS.md
tasks.register<JavaExec>("runScopeBenchmark") {
    group = "verification"
    description = "Runs the SCOPE synthetic benchmark (B0/B1/B2/B3) with frozen seeds."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("app.smriti.scope.sim.BenchmarkMainKt")
    args(rootProject.file("docs/scope/SCOPE_BENCHMARK_RESULTS.md").absolutePath)
}
