// SPDX-FileCopyrightText: 2024 City of Espoo
// SPDX-License-Identifier: LGPL-2.1-or-later

plugins {
    java
}

configurations {
    create("jaxb")
}

dependencies {
    "jaxb"("com.sun.xml.bind:jaxb-xjc:4.0.5")
    "jaxb"("com.sun.xml.bind:jaxb-impl:4.0.5")
    implementation("jakarta.xml.bind:jakarta.xml.bind-api:4.0.2")
}

tasks.register("generateJaxb") {
    val outputDir = layout.buildDirectory.dir("generated-sources/jaxb")
    val schemaDir = project.file("src/main/schema")
    
    inputs.dir(schemaDir)
    outputs.dir(outputDir)
    
    doLast {
        outputDir.get().asFile.mkdirs()
        
        ant.withGroovyBuilder {
            "taskdef"(
                "name" to "xjc",
                "classname" to "com.sun.tools.xjc.XJCTask",
                "classpath" to configurations["jaxb"].asPath
            )
            "xjc"(
                "destdir" to outputDir.get().asFile,
                "package" to "fi.espoo.evaka.sarma.model"
            ) {
                "schema"("dir" to schemaDir, "includes" to "**/*.xsd")
            }
        }
    }
}

tasks.named("compileJava") {
    dependsOn("generateJaxb")
    sourceSets.main.get().java.srcDir(layout.buildDirectory.dir("generated-sources/jaxb"))
} 