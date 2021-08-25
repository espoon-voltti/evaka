// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.jvm")

    id("org.jmailen.kotlinter")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform(project(":evaka-bom")))
    testImplementation(platform(project(":evaka-bom")))

    implementation(project(":"))
    implementation("io.github.microutils:kotlin-logging-jvm")

    // Kotlin + core
    implementation(kotlin("stdlib-jdk8"))
    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit5"))

}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = Version.java
        allWarningsAsErrors = true
    }
}

tasks {
    test {
        useJUnitPlatform()
    }

    create("codegen", JavaExec::class) {
        mainClass.set("evaka.codegen.cli.GenerateKt")
        classpath = sourceSets["main"].runtimeClasspath
        workingDir = projectDir.parentFile
    }
    create("codegenCheck", JavaExec::class) {
        mainClass.set("evaka.codegen.cli.CheckKt")
        classpath = sourceSets["main"].runtimeClasspath
        workingDir = projectDir.parentFile
    }
}
