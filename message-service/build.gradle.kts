// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        jcenter()
    }

    dependencies {
        // needed by wsdl2java
        classpath("javax.jws:javax.jws-api:1.1")
        classpath("javax.xml.ws:jaxws-api:2.3.1")
    }
}

plugins {
    id("org.flywaydb.flyway") version Version.flyway
    id("org.jetbrains.kotlin.jvm") version Version.kotlin
    id("org.jetbrains.kotlin.plugin.allopen") version Version.kotlin
    id("org.jetbrains.kotlin.plugin.spring") version Version.kotlin
    id("org.springframework.boot") version Version.springBoot

    id("com.github.ben-manes.versions") version Version.GradlePlugin.versions
    id("org.jlleitschuh.gradle.ktlint") version Version.GradlePlugin.ktlint
    id("no.nils.wsdl2java") version "0.10"

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

sourceSets["main"].java.srcDir("src/main/sfi-messages-client")

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

    // Logging
    implementation("io.github.microutils:kotlin-logging:${Version.kotlinLogging}")
    implementation("net.rakugakibox.spring.boot:logback-access-spring-boot-starter:2.7.1")

    // Spring
    implementation(platform("org.springframework.boot:spring-boot-dependencies:${Version.springBoot}"))
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-web-services")
    implementation("org.springframework.ws:spring-ws-security")
    implementation("org.springframework.ws:spring-ws-support")

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

    // Database-related dependencies
    implementation("com.zaxxer:HikariCP:${Version.hikariCp}")
    implementation("org.postgresql:postgresql:${Version.postgresDriver}")
    implementation("org.flywaydb:flyway-core:${Version.flyway}")

    // JDBI
    implementation(platform("org.jdbi:jdbi3-bom:${Version.jdbi}"))
    implementation("org.jdbi:jdbi3-core")
    implementation("org.jdbi:jdbi3-jackson2")
    implementation("org.jdbi:jdbi3-kotlin")
    implementation("org.jdbi:jdbi3-postgres")

    // Voltti
    implementation(project(":service-lib"))

    // Miscellaneous
    implementation("com.auth0:auth0-spring-security-api:${Version.auth0SpringSecurity}")
    implementation("com.auth0:java-jwt:${Version.auth0Jwt}")
    implementation("javax.annotation:javax.annotation-api:1.3.2")
    implementation("io.springfox:springfox-swagger2:${Version.springFox}")
    implementation("javax.jws:javax.jws-api:1.1")
    implementation("javax.xml.ws:jaxws-api:2.3.1")
    implementation("org.apache.commons:commons-text:${Version.apacheCommonsText}")
    implementation("org.apache.wss4j:wss4j-ws-security-dom:${Version.apacheWss4j}")
    implementation("org.glassfish.jaxb:jaxb-runtime:${Version.jaxbRuntime}")
    implementation("org.bouncycastle:bcprov-jdk15on:${Version.bouncyCastle}")
    implementation("org.bouncycastle:bcpkix-jdk15on:${Version.bouncyCastle}")

    // JUnit
    testImplementation("org.junit.jupiter:junit-jupiter")

    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:${Version.mockitoKotlin}")
    testImplementation("net.bytebuddy:byte-buddy:${Version.byteBuddy}")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")

    integrationTestImplementation(platform("org.testcontainers:testcontainers-bom:${Version.testContainers}"))
    integrationTestImplementation("org.testcontainers:postgresql")
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
        allWarningsAsErrors = true
    }
}

ktlint {
    version.set("0.37.2")
}

project.wsdl2javaExt {
    cxfVersion = "3.3.7"
}

tasks {
    wsdl2java {
        wsdlsToGenerate = arrayListOf(
            arrayListOf(
                "-p",
                "fi.espoo.evaka.msg.sficlient.soap",
                "-mark-generated",
                "-autoNameResolution",
                "$projectDir/src/main/resources/wsdl/Viranomaispalvelut.wsdl"
            )
        )
        generatedWsdlDir = file("$projectDir/src/main/sfi-messages-client")
        wsdlDir = file("$projectDir/src/main/resources/wsdl")
        // disable source generation unless the wsdl2java task is triggered directly. It's an ugly hack, we know.
        enabled = project.gradle.startParameter.taskNames.contains("wsdl2java")
    }
}

tasks {
    test {
        systemProperty("spring.profiles.active", "test")
    }

    create("integrationTest", Test::class) {
        group = "verification"
        systemProperty("spring.profiles.active", "integration-test")
        testClassesDirs = sourceSets["integrationTest"].output.classesDirs
        classpath = sourceSets["integrationTest"].runtimeClasspath
        shouldRunAfter("test")
        outputs.upToDateWhen { false }
    }

    bootRun {
        systemProperty("spring.profiles.active", "dev,local")
    }
}
