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

// v2 diagnosis of the v1 confidence gate (development seeds only).
tasks.register<JavaExec>("runScopeDiagnosis") {
    group = "verification"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("app.smriti.scope.sim.DiagnosisMainKt")
    args(rootProject.file("docs/scope/v2/DIAGNOSIS.md").absolutePath)
}

// Re-runs the ORIGINAL v1 benchmark into build/ (never overwrites the committed v1 results) for an integrity diff.
tasks.register<JavaExec>("reproduceScopeV1") {
    group = "verification"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("app.smriti.scope.sim.BenchmarkMainKt")
    args(layout.buildDirectory.file("v1-reproduction/SCOPE_BENCHMARK_RESULTS.md").get().asFile.absolutePath)
}

// Protocol v2 (docs/scope/v2/SCOPE_PROTOCOL_v2.md): development tuning, then fresh held-out test.
tasks.register<JavaExec>("runScopeV2Tuning") {
    group = "verification"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("app.smriti.scope.sim.BenchmarkV2MainKt")
    args("tune", file("src/main/resources/scope_v2_frozen.properties").absolutePath, rootProject.file("docs/scope/v2/TUNING_DEV.md").absolutePath)
}
tasks.register<JavaExec>("runScopeV2Test") {
    group = "verification"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("app.smriti.scope.sim.BenchmarkV2MainKt")
    args("test", file("src/main/resources/scope_v2_frozen.properties").absolutePath, rootProject.file("docs/scope/v2/SCOPE_BENCHMARK_RESULTS_v2.md").absolutePath)
}
