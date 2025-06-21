import java.util.*
import kotlin.math.max


plugins {
    // Apply the Kotlin JVM plugin to add support for Kotlin on the JVM.
    kotlin("jvm") version "2.1.21"
    // `maven-publish`
    application
    java
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
        // maven("https://jitpack.io")
    }
}

dependencies {
    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    // implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    // Use the Kotlin JDK 8 standard library.
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // Use the Kotlin test library.
    testImplementation("org.jetbrains.kotlin:kotlin-test")

    // Use the Kotlin JUnit5 integration.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // implementation("com.github.jitpack:gradle-simple:1.0")

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
}


val c64emuScript by tasks.registering(CreateStartScripts::class) {
    outputDir = project.layout.buildDirectory.dir("bin").get().asFile
    applicationName = "c64emu"
    mainClass.set("razorvine.c64emu.C64MainKt")
    classpath = project.tasks["jar"].outputs.files+project.configurations.runtimeClasspath.get()
}

val ehbasicScript by tasks.registering(CreateStartScripts::class) {
    outputDir = project.layout.buildDirectory.dir("bin").get().asFile
    applicationName = "ehbasic"
    mainClass.set("razorvine.examplemachines.EhBasicMainKt")
    classpath = project.tasks["jar"].outputs.files+project.configurations.runtimeClasspath.get()
}

application {
    applicationName = "ksim65vm"
    mainClass.set("razorvine.examplemachines.MachineMainKt")
    applicationDistribution.into("bin") {
        from(c64emuScript, ehbasicScript)
        filePermissions {
            user {
                read=true
                execute=true
                write=true
            }
            other.execute = true
            group.execute = true
        }
    }
}

val sourcesJar by tasks.registering(Jar::class) {
    dependsOn("classes")
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

//publishing {
//    repositories {
//        mavenLocal()
//    }
//    publications {
//        register("mavenJava", MavenPublication::class) {
//            from(components["java"])
//            artifact(sourcesJar.get())
//        }
//    }
//}
