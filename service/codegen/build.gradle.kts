// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jmailen.kotlinter")
}

repositories {
    mavenCentral()
    maven("https://build.shibboleth.net/maven/releases") {
        content {
            includeGroup("net.shibboleth.utilities")
            includeGroup("org.opensaml")
        }
    }
}

dependencies {
    implementation(platform(project(":evaka-bom")))
    testImplementation(platform(project(":evaka-bom")))

    implementation(project(":"))
    implementation("io.github.microutils:kotlin-logging-jvm")

    // Kotlin + core
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit5"))

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

    create("codegen", JavaExec::class) {
        shouldRunAfter("assemble")
        mainClass.set("evaka.codegen.GenerateKt")
        classpath = sourceSets["main"].runtimeClasspath
        workingDir = projectDir.parentFile
    }
    create("codegenCheck", JavaExec::class) {
        mainClass.set("evaka.codegen.CheckKt")
        classpath = sourceSets["main"].runtimeClasspath
        workingDir = projectDir.parentFile
    }
}
