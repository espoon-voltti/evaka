// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

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
    java
    id("no.nils.wsdl2java") version "0.10"
}

repositories {
    jcenter()
    mavenCentral()
}

dependencies {
    implementation("javax.jws:javax.jws-api:1.1")
    implementation("javax.xml.ws:jaxws-api:2.3.1")
}

project.wsdl2javaExt {
    cxfVersion = "3.3.7"
}

tasks {
    wsdl2java {
        wsdlsToGenerate = arrayListOf(
            arrayListOf(
                "-p",
                "fi.espoo.evaka.vtjclient.soap",
                "-mark-generated",
                "-autoNameResolution",
                "$projectDir/src/main/resources/wsdl/service.wsdl"
            )
        )
        generatedWsdlDir = file("$projectDir/src/main/java")
        wsdlDir = file("$projectDir/src/main/resources/wsdl")
        // disable source generation unless the wsdl2java task is triggered directly. It's an ugly hack, we know.
        enabled = project.gradle.startParameter.taskNames.contains("wsdl2java")
    }
}
