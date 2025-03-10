// SPDX-FileCopyrightText: 2024 City of Espoo
// SPDX-License-Identifier: LGPL-2.1-or-later

plugins {
    java
    id("org.unbroken-dome.xjc") version "2.0.0"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform(project(":evaka-bom")))
    implementation("jakarta.xml.bind:jakarta.xml.bind-api")
    xjcTool("com.sun.xml.bind:jaxb-xjc:4.0.5")
    xjcTool("com.sun.xml.bind:jaxb-impl:3.0.2")
}

sourceSets {
    main {
        xjcTargetPackage.set("fi.espoo.evaka.sarma.model")
    }
}

tasks.named("compileJava") {
    dependsOn("xjcGenerate")
} 