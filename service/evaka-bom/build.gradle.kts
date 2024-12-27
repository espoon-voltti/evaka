// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

plugins { `java-platform` }

javaPlatform { allowDependencies() }

dependencies {
    constraints {
        api("ch.qos.logback.access:logback-access-tomcat:2.0.5")

        // These constraints are needed for CVE fixes
        api("ch.qos.logback:logback-classic:1.5.15")
        api("ch.qos.logback:logback-core:1.5.15")

        // These constraints are needed for CVE fixes
        api("org.apache.tomcat.embed:tomcat-embed-core:11.0.2")
        api("org.apache.tomcat.embed:tomcat-embed-el:11.0.2")
        api("org.apache.tomcat.embed:tomcat-embed-websocket:11.0.2")

        api("com.auth0:java-jwt:4.4.0")
        api("com.github.kagkarlsson:db-scheduler:15.1.1")
        api(libs.fuel)
        api(libs.fuel.jackson)
        api("com.google.guava:guava:33.4.0-jre")
        api("com.networknt:json-schema-validator:1.5.0")
        api("com.zaxxer:HikariCP:6.2.0")
        api("io.github.microutils:kotlin-logging-jvm:3.0.5")
        api("io.kotest:kotest-property:5.9.1")
        api("io.mockk:mockk:1.13.13")
        api("jakarta.annotation:jakarta.annotation-api:3.0.0")
        api("jakarta.jws:jakarta.jws-api:3.0.0")
        api("jakarta.xml.ws:jakarta.xml.ws-api:4.0.2")
        api("net.logstash.logback:logstash-logback-encoder:8.0")
        api("nz.net.ultraq.thymeleaf:thymeleaf-layout-dialect:3.3.0")
        api("org.apache.commons:commons-csv:1.12.0")
        api("org.apache.commons:commons-text:1.13.0")
        api("org.apache.commons:commons-imaging:1.0-alpha3")
        api("org.apache.tika:tika-core:3.0.0")
        api("org.apache.wss4j:wss4j-ws-security-dom:3.0.1")
        api(libs.bouncycastle.bcpkix)
        api(libs.bouncycastle.bcprov)
        api(libs.flyway.core)
        api(libs.flyway.database.postgresql)
        api("org.glassfish.jaxb:jaxb-runtime:4.0.5")
        api("org.jetbrains:annotations:26.0.0")
        api("org.jsoup:jsoup:1.18.1")
        api(libs.mockito.core)
        api(libs.mockito.junit.jupiter)
        api("org.mockito.kotlin:mockito-kotlin:5.4.0")
        api("org.postgresql:postgresql:42.7.4")
        api("org.skyscreamer:jsonassert:1.5.3")
        api("org.thymeleaf.extras:thymeleaf-extras-java8time:3.0.4.RELEASE")
        api("org.thymeleaf:thymeleaf:3.1.3.RELEASE")
        api(libs.flyingsaucer.core)
        api(libs.flyingsaucer.pdf.openpdf)
        api(libs.ktlint.cli.ruleset.core)
        api(libs.ktlint.rule.engine.core)
        api(libs.ktlint.test)
        api("org.apache.santuario:xmlsec:4.0.0")
    }

    api(platform("com.fasterxml.jackson:jackson-bom:2.18.2"))
    api(platform("com.squareup.okhttp3:okhttp-bom:4.12.0"))
    api(platform("io.opentelemetry:opentelemetry-bom:1.45.0"))
    api(platform("org.apache.cxf:cxf-bom:4.1.0"))
    // Spring Boot specifies a version constraint for Jetty, but we have other libraries relying
    // on an older version -> we enforce a specific Jetty BOM version and ignore Spring Boot
    api(enforcedPlatform("org.eclipse.jetty:jetty-bom:11.0.20"))
    api(platform("org.jdbi:jdbi3-bom:3.47.0"))
    api(platform(libs.kotlin.bom))
    api(platform("org.junit:junit-bom:5.11.4"))
    api(platform(libs.spring.boot.dependencies))
    api(platform("software.amazon.awssdk:bom:2.29.1"))
}
