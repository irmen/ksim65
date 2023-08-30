import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.*
import kotlin.math.max


plugins {
    // Apply the Kotlin JVM plugin to add support for Kotlin on the JVM.
    kotlin("jvm") version "1.9.10"
    `maven-publish`
    application
    java
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}


allprojects {
    val versionProps = Properties().also {
        it.load(File("$projectDir/src/main/resources/version.properties").inputStream())
    }
    version = versionProps["version"] as String
    group = "net.razorvine"
    // base.archivesBaseName = "ksim65"

    repositories {
        // You can declare any Maven/Ivy/file repository here.
        mavenLocal()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

dependencies {
    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // Use the Kotlin JDK 8 standard library.
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // Use the Kotlin test library.
    testImplementation("org.jetbrains.kotlin:kotlin-test")

    // Use the Kotlin JUnit5 integration.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    subprojects.forEach {
        implementation(it)
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
}

val c64emuScript by tasks.registering(CreateStartScripts::class) {
    outputDir = File(project.buildDir, "bin")
    applicationName = "c64emu"
    mainClass.set("razorvine.c64emu.C64MainKt")
    classpath = project.tasks["jar"].outputs.files+project.configurations.runtimeClasspath.get()
}

val ehbasicScript by tasks.registering(CreateStartScripts::class) {
    outputDir = File(project.buildDir, "bin")
    applicationName = "ehbasic"
    mainClass.set("razorvine.examplemachines.EhBasicMainKt")
    classpath = project.tasks["jar"].outputs.files+project.configurations.runtimeClasspath.get()
}

application {
    applicationName = "ksim65vm"
    mainClass.set("razorvine.examplemachines.MachineMainKt")
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

publishing {
    repositories {
        mavenLocal()
    }
    publications {
        register("mavenJava", MavenPublication::class) {
            from(components["java"])
            artifact(sourcesJar.get())
        }
    }
}
