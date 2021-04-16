// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        mavenCentral()
    }

    dependencies {
        // needed by wsdl2java
        classpath("javax.jws:javax.jws-api:1.1")
        classpath("javax.xml.ws:jaxws-api:2.3.1")

        classpath("com.pinterest:ktlint:${Version.ktlint}")
    }
}

plugins {
    id("org.flywaydb.flyway") version Version.GradlePlugin.flyway
    id("org.jetbrains.kotlin.jvm") version Version.GradlePlugin.kotlin
    id("org.jetbrains.kotlin.plugin.allopen") version Version.GradlePlugin.kotlin
    id("org.jetbrains.kotlin.plugin.spring") version Version.GradlePlugin.kotlin
    id("org.springframework.boot") version Version.GradlePlugin.springBoot

    id("com.github.ben-manes.versions") version Version.GradlePlugin.versions
    id("org.jmailen.kotlinter") version Version.GradlePlugin.kotlinter
    id("no.nils.wsdl2java") version "0.10"
    id("org.owasp.dependencycheck") version Version.GradlePlugin.owasp

    idea
}

repositories {
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

val ktlint by configurations.creating

dependencies {
    implementation(platform(project(":evaka-bom")))
    testImplementation(platform(project(":evaka-bom")))
    integrationTestImplementation(platform(project(":evaka-bom")))

    // Kotlin + core
    implementation(kotlin("stdlib-jdk8"))

    // Logging
    implementation("io.github.microutils:kotlin-logging-jvm")
    implementation("net.rakugakibox.spring.boot:logback-access-spring-boot-starter")

    // Spring
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-web-services")
    implementation("org.springframework.ws:spring-ws-security")
    implementation("org.springframework.ws:spring-ws-support")

    // Jackson
    implementation("com.fasterxml.jackson.core:jackson-core")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("com.fasterxml.jackson.module:jackson-module-jaxb-annotations")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    runtimeOnly("com.fasterxml.jackson.datatype:jackson-datatype-jdk8")

    // AWS SDK
    implementation("com.amazonaws:aws-java-sdk-s3")
    implementation("com.amazonaws:aws-java-sdk-sts")

    // Database-related dependencies
    implementation("com.zaxxer:HikariCP")
    implementation("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")

    // JDBI
    implementation("org.jdbi:jdbi3-core")
    implementation("org.jdbi:jdbi3-jackson2")
    implementation("org.jdbi:jdbi3-kotlin")
    implementation("org.jdbi:jdbi3-postgres")

    // Voltti
    implementation(project(":service-lib")) {
        exclude("com.auth0", "auth0-spring-security-api")
        exclude("org.springframework.boot", "spring-boot-starter-security")
    }

    // Miscellaneous
    implementation("com.auth0:java-jwt")
    implementation("javax.annotation:javax.annotation-api")
    implementation("io.springfox:springfox-swagger2")
    implementation("javax.jws:javax.jws-api")
    implementation("javax.xml.ws:jaxws-api")
    implementation("org.apache.commons:commons-text")
    implementation("org.apache.wss4j:wss4j-ws-security-dom")
    implementation("org.glassfish.jaxb:jaxb-runtime")
    implementation("org.bouncycastle:bcprov-jdk15on")
    implementation("org.bouncycastle:bcpkix-jdk15on")

    // JUnit
    testImplementation("org.junit.jupiter:junit-jupiter")

    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin")
    testImplementation("net.bytebuddy:byte-buddy")
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    integrationTestImplementation("org.testcontainers:postgresql")

    ktlint("com.pinterest:ktlint:${Version.ktlint}")
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

project.wsdl2javaExt {
    cxfVersion = "3.4.2"
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

    create("ktlintApplyToIdea", JavaExec::class) {
        main = "com.pinterest.ktlint.Main"
        classpath = ktlint
        args = listOf("applyToIDEAProject", "-y")
    }

    dependencyCheck {
        failBuildOnCVSS = 0.0f
        analyzers.apply {
            assemblyEnabled = false
            nodeAuditEnabled = false
            nodeEnabled = false
            nuspecEnabled = false
        }
        suppressionFile = "$projectDir/owasp-suppressions.xml"
    }
}
