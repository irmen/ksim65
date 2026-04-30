plugins {
    kotlin("jvm") version "2.3.20"
    application
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

group = "net.razorvine"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.11.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    implementation("net.razorvine:ksim65:2.0")
}

tasks.test {
    useJUnitPlatform()
    dependsOn("assembleTestProgram")
    systemProperty("test.program.path", "../testprogram/test.prg")
}

tasks.register<Exec>("assembleTestProgram") {
    description = "Assembles the testprogram.asm into test.prg using 64tass"
    group = "build"
    workingDir = file("../testprogram")
    commandLine("64tass", "-o", "test.prg", "testprogram.asm")
    // Define inputs/outputs for incremental build support
    inputs.file(file("../testprogram/testprogram.asm"))
    outputs.file(file("../testprogram/test.prg"))
}

application {
    mainClass.set("net.razorvine.MainKt")
}

tasks.register<JavaExec>("runWithTestPrg") {
    description = "run the test program with '../testprogram/test.prg' as an argument"
    group = "application"
    dependsOn("assembleTestProgram")
    mainClass.set("net.razorvine.MainKt")
    classpath = sourceSets.main.get().runtimeClasspath
    args("../testprogram/test.prg")
}