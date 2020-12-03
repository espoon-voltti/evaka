// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.allopen")
    id("org.jetbrains.kotlin.plugin.spring")

    id("com.github.ben-manes.versions")
    id("org.jlleitschuh.gradle.ktlint")
}

repositories {
    jcenter()
    mavenCentral()
}

dependencies {
    // Kotlin + core
    implementation(platform(kotlin("bom")))
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))

    // Logging
    implementation("io.github.microutils:kotlin-logging:${Version.kotlinLogging}")
    implementation("net.logstash.logback:logstash-logback-encoder:${Version.logstashEncoder}")
    implementation("net.rakugakibox.spring.boot:logback-access-spring-boot-starter:${Version.logbackSpringBoot}")

    // Spring
    implementation(platform("org.springframework.boot:spring-boot-dependencies:${Version.springBoot}"))
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-web")

    // Jackson
    implementation(platform("com.fasterxml.jackson:jackson-bom:${Version.jackson}"))
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // Auth0 Spring Security
    implementation("com.auth0:auth0-spring-security-api:${Version.auth0SpringSecurity}")
    implementation("com.auth0:java-jwt:${Version.auth0Jwt}")
    implementation("com.auth0:jwks-rsa:0.13.0")

    testImplementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.skyscreamer:jsonassert:1.5.0")
}

allOpen {
    annotation("org.springframework.boot.test.context.TestConfiguration")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = Version.java
        allWarningsAsErrors = true
    }
}

tasks {
    test {
        systemProperty("spring.profiles.active", "test")
    }
}
