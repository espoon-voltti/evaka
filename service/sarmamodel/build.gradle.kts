// SPDX-FileCopyrightText: 2024 City of Espoo
// SPDX-License-Identifier: LGPL-2.1-or-later

plugins {
    java
}

val generatedSources = layout.buildDirectory.dir("generated/sources/java/main")
val xsd2java: Configuration by configurations.creating

sourceSets {
    main {
        java.srcDir(generatedSources)
    }
}

dependencies {
    xsd2java("com.sun.xml.bind:jaxb-xjc:4.0.5")
    xsd2java("com.sun.xml.bind:jaxb-impl:4.0.5")
    implementation("jakarta.xml.bind:jakarta.xml.bind-api:4.0.3")
}

tasks.register<JavaExec>("generateJaxb") {
    val schemaDir = project.file("src/main/schema")
    
    mainClass.set("com.sun.tools.xjc.Driver")
    classpath = xsd2java
    args = listOf(
        "-d", generatedSources.get().toString(),
        "-p", "fi.espoo.evaka.sarma.model",
        schemaDir.toString()
    )

    inputs.dir(schemaDir)
    outputs.dir(generatedSources)
}

tasks.named("compileJava") {
    dependsOn("generateJaxb")
}