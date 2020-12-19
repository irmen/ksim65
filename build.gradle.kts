import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.*
import kotlin.math.max


plugins {
    // Apply the Kotlin JVM plugin to add support for Kotlin on the JVM.
    kotlin("jvm") version "1.4.20"
    `maven-publish`
    application
    id("org.jetbrains.dokka") version "0.10.0"
    id("com.jfrog.bintray") version "1.8.4"
}

allprojects {
    val versionProps = Properties().also {
        it.load(File("$projectDir/src/main/resources/version.properties").inputStream())
    }
    version = versionProps["version"] as String
    group = "net.razorvine"
    base.archivesBaseName = "ksim65"

    repositories {
        // Use jcenter for resolving dependencies.
        // You can declare any Maven/Ivy/file repository here.
        mavenLocal()
        jcenter()
        maven("https://jitpack.io")
    }
}


dependencies {
    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

    // Use the Kotlin JDK 8 standard library.
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // Use the Kotlin test library.
    testImplementation("org.jetbrains.kotlin:kotlin-test")

    // Use the Kotlin JUnit5 integration.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.4.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.4.0")

    subprojects.forEach {
        archives(it)
    }
}

tasks {
    named<Test>("test") {
        useJUnitPlatform()
        dependsOn("cleanTest")
        testLogging.events("failed")

        // parallel tests.
        systemProperty("junit.jupiter.execution.parallel.enabled", "true")
        systemProperty("junit.jupiter.execution.parallel.mode.default", "concurrent")
        maxParallelForks = max(1, Runtime.getRuntime().availableProcessors()/2)
    }

    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "11"
    }

    named<DokkaTask>("dokka") {
        outputFormat = "html"
        outputDirectory = "$buildDir/kdoc"
        configuration {
            skipEmptyPackages = true
        }
    }
}

val c64emuScript by tasks.registering(CreateStartScripts::class) {
    outputDir = File(project.buildDir, "bin")
    applicationName = "c64emu"
    mainClassName = "razorvine.c64emu.C64MainKt"
    classpath = project.tasks["jar"].outputs.files+project.configurations.runtimeClasspath.get()
}

val ehbasicScript by tasks.registering(CreateStartScripts::class) {
    outputDir = File(project.buildDir, "bin")
    applicationName = "ehbasic"
    mainClassName = "razorvine.examplemachines.EhBasicMainKt"
    classpath = project.tasks["jar"].outputs.files+project.configurations.runtimeClasspath.get()
}

application {
    applicationName = "ksim65vm"
    mainClassName = "razorvine.examplemachines.MachineMainKt"
    applicationDistribution.into("bin") {
        from(c64emuScript, ehbasicScript)
        fileMode = 493
    }
}

val sourcesJar by tasks.registering(Jar::class) {
    dependsOn("classes")
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

val dokkaDocs by tasks.registering(Jar::class) {
    dependsOn("dokka")
    archiveClassifier.set("kdoc")
    from(fileTree(File(project.buildDir, "kdoc")))
}

publishing {
    repositories {
        mavenLocal()
    }
    publications {
        register("mavenJava", MavenPublication::class) {
            from(components["java"])
            artifact(sourcesJar.get())
            artifact(dokkaDocs.get())
        }
    }
}

bintray {
    user = System.getenv("BINTRAY_USER")
    key = System.getenv("BINTRAY_KEY")

    setPublications(* publishing.publications.names.toTypedArray())
    // setConfigurations("archives")
    pkg = PackageConfig().also {
        it.name = "ksim65"
        it.repo = "maven"
        it.setLicenses("MIT")
        it.vcsUrl = "https://github.com/irmen/ksim65.git"
        it.setLabels("6502", "retro", "emulation", "c64")
        it.githubRepo = it.vcsUrl
        it.version = VersionConfig().also {
            it.gpg = GpgConfig().also {
                it.sign = true
            }
        }
    }
}
