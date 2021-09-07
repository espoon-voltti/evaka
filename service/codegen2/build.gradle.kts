// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.springframework.boot")
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
    implementation(kotlin("reflect"))

    // Spring
    api("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.springframework.boot:spring-boot-starter-web")

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

    create("codegen2", JavaExec::class) {
        mainClass.set("evaka.codegen2.cli.GenerateKt")
        classpath = sourceSets["main"].runtimeClasspath
        workingDir = projectDir.parentFile
    }

    create("codegen2Check", JavaExec::class) {
        mainClass.set("evaka.codegen2.cli.CheckKt")
        classpath = sourceSets["main"].runtimeClasspath
        workingDir = projectDir.parentFile
    }
}
