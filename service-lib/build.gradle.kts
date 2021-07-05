// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.allopen")
    id("org.jetbrains.kotlin.plugin.spring")

    id("com.github.ben-manes.versions")
    id("org.jmailen.kotlinter")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform(project(":evaka-bom")))
    testImplementation(platform(project(":evaka-bom")))

    // Kotlin + core
    implementation(kotlin("stdlib-jdk8"))
    api(kotlin("test"))
    api(kotlin("test-junit5"))

    // Logging
    implementation("io.github.microutils:kotlin-logging-jvm")
    implementation("net.logstash.logback:logstash-logback-encoder")
    implementation("net.rakugakibox.spring.boot:logback-access-spring-boot-starter")

    // Spring
    implementation("org.springframework.boot:spring-boot-starter-web")

    // Jackson
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // Auth0 JWT
    implementation("com.auth0:java-jwt")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.skyscreamer:jsonassert")
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
        filter {
            isFailOnNoMatchingTests = false
        }
    }
}
