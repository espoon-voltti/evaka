// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        jcenter()
    }
}

plugins {
    id("org.jetbrains.kotlin.jvm") version Version.kotlin
    id("org.jetbrains.kotlin.plugin.allopen") version Version.kotlin
    id("org.jetbrains.kotlin.plugin.spring") version Version.kotlin
    id("org.springframework.boot") version Version.springBoot
    id("org.flywaydb.flyway") version Version.flyway

    id("com.github.ben-manes.versions") version Version.GradlePlugin.versions
    id("io.gitlab.arturbosch.detekt") version Version.GradlePlugin.detekt
    id("org.jlleitschuh.gradle.ktlint") version Version.GradlePlugin.ktlint

    idea
}

repositories {
    jcenter()
    mavenCentral()
}

sourceSets {
    create("integrationTest") {
        compileClasspath += main.get().output + test.get().output
        runtimeClasspath += main.get().output + test.get().output
    }
}

val integrationTestImplementation: Configuration by configurations.getting {
    extendsFrom(configurations.testImplementation.get())
}

configurations["integrationTestRuntimeOnly"].extendsFrom(configurations.testRuntimeOnly.get())

idea {
    module {
        testSourceDirs =
            testSourceDirs + sourceSets["integrationTest"].withConvention(KotlinSourceSet::class) { kotlin.srcDirs }
        testResourceDirs = testResourceDirs + sourceSets["integrationTest"].resources.srcDirs
    }
}

dependencies {
    // Kotlin + core
    implementation(platform(kotlin("bom")))
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Version.kotlinx}")

    // Logging
    implementation("io.github.microutils:kotlin-logging:${Version.kotlinLogging}")
    implementation("net.rakugakibox.spring.boot:logback-access-spring-boot-starter:${Version.logbackSpringBoot}")

    // Spring
    implementation(platform("org.springframework.boot:spring-boot-dependencies:${Version.springBoot}"))
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-web-services")
    implementation("org.springframework.ws:spring-ws-security")
    implementation("org.springframework.ws:spring-ws-support")
    implementation("org.springframework.boot:spring-boot-devtools")

    // Database-related dependencies
    implementation("com.zaxxer:HikariCP:${Version.hikariCp}")
    implementation("org.flywaydb:flyway-core:${Version.flyway}")
    implementation("org.postgresql:postgresql:${Version.postgresDriver}")
    implementation("redis.clients:jedis:${Version.jedis}")

    // JDBI
    implementation(platform("org.jdbi:jdbi3-bom:${Version.jdbi}"))
    implementation("org.jdbi:jdbi3-core")
    implementation("org.jdbi:jdbi3-jackson2")
    implementation("org.jdbi:jdbi3-kotlin")
    implementation("org.jdbi:jdbi3-postgres")

    // Fuel
    implementation("com.github.kittinunf.fuel:fuel:${Version.fuel}")
    implementation("com.github.kittinunf.fuel:fuel-jackson:${Version.fuel}")

    // Jackson
    implementation(platform("com.fasterxml.jackson:jackson-bom:${Version.jackson}"))
    implementation("com.fasterxml.jackson.core:jackson-core")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("com.fasterxml.jackson.module:jackson-module-jaxb-annotations")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    runtimeOnly("com.fasterxml.jackson.datatype:jackson-datatype-jdk8")

    // AWS SDK
    implementation(platform("com.amazonaws:aws-java-sdk-bom:${Version.awsSdk}"))
    implementation("com.amazonaws:aws-java-sdk-s3")
    implementation("com.amazonaws:aws-java-sdk-sts")
    implementation("com.amazonaws:aws-java-sdk-ses")

    // Voltti
    implementation(project(":service-lib"))

    // Flying Saucer <=>
    implementation("org.thymeleaf:thymeleaf:3.0.11.RELEASE")
    implementation("org.thymeleaf.extras:thymeleaf-extras-java8time:3.0.3.RELEASE")
    implementation("org.xhtmlrenderer:flying-saucer-core:${Version.flyingSaucer}")
    implementation("org.xhtmlrenderer:flying-saucer-pdf-openpdf:${Version.flyingSaucer}")

    // Miscellaneous
    implementation("com.auth0:auth0-spring-security-api:${Version.auth0SpringSecurity}")
    implementation("com.auth0:java-jwt:${Version.auth0Jwt}")
    implementation("javax.annotation:javax.annotation-api:1.3.2")
    implementation("org.apache.commons:commons-pool2:${Version.commonsPool2}")
    implementation("org.glassfish.jaxb:jaxb-runtime:2.3.2")
    implementation("org.bouncycastle:bcprov-jdk15on:${Version.bouncyCastle}")
    implementation("org.bouncycastle:bcpkix-jdk15on:${Version.bouncyCastle}")

    // JUnit
    testImplementation(platform("org.junit:junit-bom:${Version.junit}"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    testImplementation("com.nhaarman:mockito-kotlin:${Version.mockitoKotlin}")
    testImplementation("net.bytebuddy:byte-buddy:${Version.byteBuddy}")
    testImplementation("net.logstash.logback:logstash-logback-encoder:${Version.logstashEncoder}")
    testImplementation("org.jetbrains:annotations:${Version.jetbrainsAnnotations}")
    testImplementation("org.mockito:mockito-core:${Version.mockito}")
    testImplementation("org.mockito:mockito-junit-jupiter:${Version.mockito}")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.ws:spring-ws-test")

    integrationTestImplementation("io.javalin:javalin:${Version.javalin}")
    integrationTestImplementation(platform("org.testcontainers:testcontainers-bom:${Version.testContainers}"))
    integrationTestImplementation("org.testcontainers:postgresql")

    implementation(project(":vtjclient"))

    add("ktlint", files("custom-ktlint-rules/custom-ktlint-rules.jar"))
}

allOpen {
    annotation("org.springframework.boot.test.context.TestConfiguration")
}

allprojects {
    tasks.withType<JavaCompile> {
        sourceCompatibility = Version.java
        targetCompatibility = Version.java
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = Version.java
        allWarningsAsErrors = name != "compileIntegrationTestKotlin"
    }
}

detekt {
    config = files("$projectDir/detekt.yml")
}

ktlint {
    version.set("0.37.2")
}

tasks {
    test {
        useJUnitPlatform()
        systemProperty("spring.profiles.active", "test")
    }

    create("integrationTest", Test::class) {
        useJUnitPlatform()
        group = "verification"
        systemProperty("spring.profiles.active", "integration-test")
        testClassesDirs = sourceSets["integrationTest"].output.classesDirs
        classpath = sourceSets["integrationTest"].runtimeClasspath
        shouldRunAfter("test")
        outputs.upToDateWhen { false }
    }

    bootRun {
        // If you want to develop against VTJ, add vtj-dev here
        systemProperty("spring.profiles.active", "local")
    }

    create("bootRunTest", org.springframework.boot.gradle.tasks.run.BootRun::class) {
        main = "fi.espoo.evaka.MainKt"
        classpath = sourceSets["main"].runtimeClasspath
        systemProperty("spring.profiles.active", "local")
        systemProperty("spring.datasource.url", "jdbc:postgresql://localhost:15432/evaka_it")
        systemProperty("spring.datasource.username", "evaka_it")
        systemProperty("spring.datasource.password", "evaka_it")
        systemProperty("flyway.username", "evaka_it")
        systemProperty("flyway.password", "evaka_it")
    }
}
