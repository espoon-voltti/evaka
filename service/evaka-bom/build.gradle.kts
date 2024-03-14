// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

plugins {
    `java-platform`
}

javaPlatform {
    allowDependencies()
}

dependencies {
    constraints {
        api("com.auth0:java-jwt:4.4.0")
        api("com.github.kagkarlsson:db-scheduler:13.0.0")
        api(libs.fuel)
        api(libs.fuel.jackson)
        api("com.google.guava:guava:33.1.0-jre")
        api("com.networknt:json-schema-validator:1.3.0")
        api("com.zaxxer:HikariCP:5.1.0")
        api("io.github.microutils:kotlin-logging-jvm:3.0.5")
        api(libs.opentracing.api)
        api(libs.opentracing.util)
        api("jakarta.annotation:jakarta.annotation-api:2.1.1")
        api("jakarta.jws:jakarta.jws-api:3.0.0")
        api("jakarta.xml.ws:jakarta.xml.ws-api:4.0.0")
        api("net.logstash.logback:logstash-logback-encoder:7.4")
        api("nz.net.ultraq.thymeleaf:thymeleaf-layout-dialect:3.3.0")
        api("org.apache.commons:commons-text:1.11.0")
        api("org.apache.commons:commons-imaging:1.0-alpha3")
        api("org.apache.tika:tika-core:2.9.0")
        api("org.apache.wss4j:wss4j-ws-security-dom:3.0.1")
        api(libs.bouncycastle.bcpkix)
        api(libs.bouncycastle.bcprov)
        api(libs.flyway.core)
        api(libs.flyway.database.postgresql)
        api("org.glassfish.jaxb:jaxb-runtime:4.0.1")
        api("org.jetbrains:annotations:24.1.0")
        api("org.jsoup:jsoup:1.17.1")
        api(libs.mockito.core)
        api(libs.mockito.junit.jupiter)
        api("org.mockito.kotlin:mockito-kotlin:5.2.1")
        api("org.postgresql:postgresql:42.7.0")
        api("org.skyscreamer:jsonassert:1.5.1")
        api("org.thymeleaf.extras:thymeleaf-extras-java8time:3.0.4.RELEASE")
        api("org.thymeleaf:thymeleaf:3.1.2.RELEASE")
        api(libs.flyingsaucer.core)
        api(libs.flyingsaucer.pdf.openpdf)
        api(libs.ktlint.cli.ruleset.core)
        api(libs.ktlint.rule.engine.core)
        api(libs.ktlint.test)
        api("org.apache.santuario:xmlsec:4.0.0")
    }

    api(platform("com.fasterxml.jackson:jackson-bom:2.17.0"))
    api(platform("org.apache.cxf:cxf-bom:4.0.3"))
    // Spring Boot specifies a version constraint for Jetty, but we have other libraries relying
    // on an older version -> we enforce a specific Jetty BOM version and ignore Spring Boot
    api(enforcedPlatform("org.eclipse.jetty:jetty-bom:11.0.20"))
    api(platform("org.jdbi:jdbi3-bom:3.45.0"))
    api(platform(libs.kotlin.bom))
    api(platform("org.junit:junit-bom:5.10.0"))
    api(platform(libs.spring.boot.dependencies))
    api(platform("software.amazon.awssdk:bom:2.25.1"))
}
