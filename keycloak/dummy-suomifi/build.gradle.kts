// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadow)
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform(libs.http4k.bom))
    implementation(libs.http4k.core)
    implementation(libs.jboss.logging)
    implementation(libs.keycloak.common)
    implementation(libs.keycloak.saml.core)
    implementation(libs.slf4j.api)
    implementation(libs.slf4j.simple)
    implementation(libs.jackson.kotlin)
    implementation(libs.unbescape)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget(libs.versions.java.get())
    }
}

application {
    mainClass = "dummy_suomifi.AppKt"
}
