// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.allopen")
    id("org.jetbrains.kotlin.plugin.spring")

    id("com.github.ben-manes.versions")
    id("org.jlleitschuh.gradle.ktlint")
}

dependencies {
    implementation(platform(project(":evaka-bom")))
    testImplementation(platform(project(":evaka-bom")))

    // Kotlin + core
    implementation(kotlin("stdlib-jdk8"))
    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit5"))

    // Logging
    implementation("io.github.microutils:kotlin-logging-jvm")
    implementation("net.logstash.logback:logstash-logback-encoder")

    // Spring
    implementation("org.springframework.boot:spring-boot-starter-web")

    // Jackson
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // Auth0 JWT
    implementation("com.auth0:java-jwt")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.skyscreamer:jsonassert")
}

ktlint {
    version.set(
        libs.versions.ktlint
            .asProvider()
            .get()
    )
}
