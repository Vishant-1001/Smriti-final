// SMRITI Fit: explicit-needs presentation solver (docs/flagship/FIT_SPEC.md).
// Pure Kotlin on the JVM: no Android, Compose or network. The benchmark measures text with the
// app's own bundled fonts (java.awt, headless) and real strings from app/src/main/res.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation(libs.junit)
}

val appRes = rootProject.file("app/src/main/res").absolutePath

tasks.withType<Test>().configureEach {
    systemProperty("java.awt.headless", "true")
    systemProperty("smriti.res", appRes)
}

fun JavaExec.fitBench(mode: String, out: String) {
    group = "verification"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("app.smriti.fit.sim.FitBenchmarkKt")
    jvmArgs("-Djava.awt.headless=true", "-Dfile.encoding=UTF-8")
    args(mode, appRes, out)
}

// Development split only (seeds 1-2000, development content pool).
tasks.register<JavaExec>("runFitDev") { fitBench("dev", rootProject.file("docs/flagship/FIT_DEV_RESULTS.md").absolutePath) }
// Pre-registered evaluation (seeds 3001-8000, held-out content pool). See docs/flagship/FIT_PROTOCOL.md.
tasks.register<JavaExec>("runFitEval") { fitBench("eval", rootProject.file("docs/flagship/FIT_EVAL_RESULTS.md").absolutePath) }
// Human-readable demo: one screen under four different explicit profiles, with explanations.
tasks.register<JavaExec>("runFitDemo") { fitBench("demo", rootProject.file("docs/flagship/FIT_DEMO.md").absolutePath) }
// Development only: concrete violating instances, for checking the geometry model (writes to build/).
tasks.register<JavaExec>("runFitExamples") { fitBench("examples", layout.buildDirectory.file("fit-examples.md").get().asFile.absolutePath) }
