// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.bundling.BootJar

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath(files("custom-ktlint-rules/custom-ktlint-rules.jar"))
        classpath("com.pinterest:ktlint:${Version.ktlint}")
    }
}

plugins {
    id("org.jetbrains.kotlin.jvm") version Version.GradlePlugin.kotlin
    id("org.jetbrains.kotlin.plugin.allopen") version Version.GradlePlugin.kotlin
    id("org.jetbrains.kotlin.plugin.spring") version Version.GradlePlugin.kotlin
    id("org.springframework.boot") version Version.GradlePlugin.springBoot
    id("org.flywaydb.flyway") version Version.GradlePlugin.flyway

    id("com.github.ben-manes.versions") version Version.GradlePlugin.versions
    id("org.jmailen.kotlinter") version Version.GradlePlugin.kotlinter
    id("com.ncorti.ktfmt.gradle") version Version.GradlePlugin.ktfmt
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
    api(platform(project(":evaka-bom")))
    implementation(platform(project(":evaka-bom")))
    testImplementation(platform(project(":evaka-bom")))
    runtimeOnly(platform(project(":evaka-bom")))
    integrationTestImplementation(platform(project(":evaka-bom")))
    ktlint(platform(project(":evaka-bom")))

    // Kotlin + core
    api(kotlin("stdlib-jdk8"))
    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit5"))
    integrationTestImplementation(kotlin("test"))
    integrationTestImplementation(kotlin("test-junit5"))

    // Logging
    implementation("dev.akkinoc.spring.boot:logback-access-spring-boot-starter")
    implementation("io.github.microutils:kotlin-logging-jvm")

    // Spring
    api("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-web-services")
    implementation("org.springframework.ws:spring-ws-security")
    implementation("org.springframework.ws:spring-ws-support")
    implementation("org.springframework.boot:spring-boot-devtools")

    // Database-related dependencies
    implementation("com.zaxxer:HikariCP")
    implementation("org.flywaydb:flyway-core")
    implementation("org.postgresql:postgresql")
    implementation("redis.clients:jedis")

    // JDBI
    implementation("org.jdbi:jdbi3-core")
    implementation("org.jdbi:jdbi3-jackson2")
    implementation("org.jdbi:jdbi3-kotlin")
    implementation("org.jdbi:jdbi3-postgres")

    // Fuel
    implementation("com.github.kittinunf.fuel:fuel")
    implementation("com.github.kittinunf.fuel:fuel-jackson")

    // Jackson
    implementation("com.fasterxml.jackson.core:jackson-core")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("com.fasterxml.jackson.module:jackson-module-jaxb-annotations")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    runtimeOnly("com.fasterxml.jackson.datatype:jackson-datatype-jdk8")

    // AWS SDK
    implementation("software.amazon.awssdk:s3")
    implementation("software.amazon.awssdk:sts")
    implementation("software.amazon.awssdk:ses")

    // Voltti
    implementation(project(":service-lib"))

    // Flying Saucer <=>
    implementation("org.thymeleaf:thymeleaf")
    implementation("org.thymeleaf.extras:thymeleaf-extras-java8time")
    implementation("nz.net.ultraq.thymeleaf:thymeleaf-layout-dialect")
    implementation("org.xhtmlrenderer:flying-saucer-core")
    implementation("org.xhtmlrenderer:flying-saucer-pdf-openpdf")

    // Miscellaneous
    implementation("com.github.kagkarlsson:db-scheduler")
    implementation("com.auth0:java-jwt")
    implementation("javax.annotation:javax.annotation-api")
    implementation("org.apache.commons:commons-pool2")
    implementation("org.apache.commons:commons-text")
    implementation("org.glassfish.jaxb:jaxb-runtime")
    implementation("org.bouncycastle:bcprov-jdk15on")
    implementation("org.bouncycastle:bcpkix-jdk15on")
    implementation("org.apache.tika:tika-core")
    implementation("org.apache.commons:commons-imaging")

    // JUnit
    testImplementation("org.junit.jupiter:junit-jupiter")

    testImplementation("net.bytebuddy:byte-buddy")
    testImplementation("net.logstash.logback:logstash-logback-encoder")
    testImplementation("org.jetbrains:annotations")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
    testImplementation("org.mockito.kotlin:mockito-kotlin")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.ws:spring-ws-test")

    integrationTestImplementation("io.javalin:javalin")
    integrationTestImplementation("org.apache.cxf:cxf-rt-frontend-jaxws")
    integrationTestImplementation("org.apache.cxf:cxf-rt-transports-http")
    integrationTestImplementation("org.apache.cxf:cxf-rt-transports-http-jetty")
    integrationTestImplementation("org.apache.cxf:cxf-rt-ws-security")

    implementation(project(":sficlient"))
    implementation(project(":vtjclient"))

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
        allWarningsAsErrors = name != "compileIntegrationTestKotlin"
    }
}

tasks.getByName<Jar>("jar") {
    archiveClassifier.set("")
}

tasks.getByName<BootJar>("bootJar") {
    archiveClassifier.set("boot")
}

tasks {
    test {
        useJUnitPlatform()
        systemProperty("spring.profiles.active", "test")
        filter {
            isFailOnNoMatchingTests = false
        }
    }

    create("integrationTest", Test::class) {
        useJUnitPlatform()
        group = "verification"
        systemProperty("spring.profiles.active", "integration-test")
        testClassesDirs = sourceSets["integrationTest"].output.classesDirs
        classpath = sourceSets["integrationTest"].runtimeClasspath
        shouldRunAfter("test")
        outputs.upToDateWhen { false }
        filter {
            isFailOnNoMatchingTests = false
        }
    }

    bootRun {
        // If you want to develop against VTJ, add vtj-dev here
        systemProperty("spring.profiles.active", "local")
    }

    create("bootRunTest", org.springframework.boot.gradle.tasks.run.BootRun::class) {
        mainClass.set("fi.espoo.evaka.MainKt")
        classpath = sourceSets["main"].runtimeClasspath
        systemProperty("spring.profiles.active", "local")
        systemProperty("spring.datasource.url", "jdbc:postgresql://localhost:15432/evaka_it")
        systemProperty("spring.datasource.username", "evaka_it")
        systemProperty("spring.datasource.password", "evaka_it")
        systemProperty("flyway.username", "evaka_it")
        systemProperty("flyway.password", "evaka_it")
    }

    create("ktlintApplyToIdea", JavaExec::class) {
        mainClass.set("com.pinterest.ktlint.Main")
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

ktfmt {
    kotlinLangStyle()
}
