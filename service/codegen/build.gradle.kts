// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jlleitschuh.gradle.ktlint")
    id("com.ncorti.ktfmt.gradle")
}

dependencies {
    api(platform(project(":evaka-bom")))
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

tasks {
    register("codegen", JavaExec::class) {
        shouldRunAfter("assemble")
        mainClass.set("evaka.codegen.GenerateKt")
        classpath = sourceSets["main"].runtimeClasspath
        workingDir = projectDir.parentFile
    }
    register("codegenCheck", JavaExec::class) {
        mainClass.set("evaka.codegen.CheckKt")
        classpath = sourceSets["main"].runtimeClasspath
        workingDir = projectDir.parentFile
    }
}

ktfmt {
    kotlinLangStyle()
}

ktlint {
    version.set(libs.versions.ktlint.asProvider().get())
}
