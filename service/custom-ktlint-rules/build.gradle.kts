// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jlleitschuh.gradle.ktlint")
}

dependencies {
    api(platform(project(":evaka-bom")))
    implementation(platform(project(":evaka-bom")))
    testImplementation(platform(project(":evaka-bom")))

    // Kotlin + core
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit5"))

    implementation("com.pinterest.ktlint:ktlint-cli-ruleset-core")
    implementation("com.pinterest.ktlint:ktlint-rule-engine-core")
    testImplementation("org.slf4j:slf4j-simple")
    testImplementation("com.pinterest.ktlint:ktlint-test")
}

ktlint {
    version.set(libs.versions.ktlint.asProvider().get())
}
