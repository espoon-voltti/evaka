// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

plugins {
    java
}

val generatedSources = "$buildDir/generated/sources/java/main"
val wsdl2java: Configuration by configurations.creating

sourceSets {
    main {
        java.srcDir(generatedSources)
    }
}

dependencies {
    implementation(platform(project(":evaka-bom")))
    implementation("jakarta.annotation:jakarta.annotation-api")
    implementation("jakarta.jws:jakarta.jws-api")
    implementation("jakarta.xml.ws:jakarta.xml.ws-api")

    wsdl2java(platform(project(":evaka-bom")))
    wsdl2java("org.slf4j:slf4j-simple")
    wsdl2java("jakarta.jws:jakarta.jws-api")
    wsdl2java("jakarta.xml.ws:jakarta.xml.ws-api")
    wsdl2java("org.apache.cxf:cxf-tools-wsdlto-frontend-jaxws")
    wsdl2java("org.apache.cxf:cxf-tools-wsdlto-databinding-jaxb")
}

val wsdl2javaTask = tasks.register<JavaExec>("wsdl2java") {
    val wsdl = "$projectDir/src/main/resources/wsdl/service.wsdl"

    mainClass.set("org.apache.cxf.tools.wsdlto.WSDLToJava")
    classpath = wsdl2java
    args = arrayListOf(
        "-d",
        generatedSources,
        "-p",
        "fi.espoo.evaka.vtjclient.soap",
        "-mark-generated",
        "-autoNameResolution",
        wsdl
    )
    inputs.files(wsdl)
    outputs.dir(generatedSources)
}

tasks.getByName<JavaCompile>("compileJava") {
    dependsOn(wsdl2javaTask)
}
