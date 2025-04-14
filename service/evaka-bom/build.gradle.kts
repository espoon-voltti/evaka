// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

plugins { `java-platform` }

javaPlatform { allowDependencies() }

dependencies {
    constraints {
        api("ch.qos.logback.access:logback-access-tomcat:2.0.6")

        // These constraints are needed for CVE fixes
        api("ch.qos.logback:logback-classic:1.5.18")
        api("ch.qos.logback:logback-core:1.5.18")

        api("com.auth0:java-jwt:4.5.0")
        api("com.github.kagkarlsson:db-scheduler:15.3.0")
        api(libs.fuel)
        api(libs.fuel.jackson)
        api("com.github.mwiede:jsch:0.2.25")
        api("com.google.guava:guava:33.4.7-jre")
        api("com.networknt:json-schema-validator:1.5.6")
        api("com.zaxxer:HikariCP:6.3.0")
        api("io.github.oshai:kotlin-logging-jvm:7.0.6")
        api("io.kotest:kotest-property:5.9.1")
        api("io.mockk:mockk:1.13.17")
        api("jakarta.annotation:jakarta.annotation-api:3.0.0")
        api("jakarta.jws:jakarta.jws-api:3.0.0")
        api("jakarta.xml.ws:jakarta.xml.ws-api:4.0.2")
        api("net.logstash.logback:logstash-logback-encoder:8.1")
        api("nz.net.ultraq.thymeleaf:thymeleaf-layout-dialect:3.4.0")
        api("org.apache.commons:commons-csv:1.14.0")
        api("org.apache.commons:commons-text:1.13.1")
        api("org.apache.commons:commons-imaging:1.0.0-alpha5")
        api("org.apache.httpcomponents:httpclient:4.5.14")
        api("org.apache.tika:tika-core:3.1.0")
        api(libs.bouncycastle.bcpkix)
        api(libs.bouncycastle.bcprov)
        api(libs.flyway.core)
        api(libs.flyway.database.postgresql)
        api("org.glassfish.jaxb:jaxb-runtime:4.0.5")
        api("org.jetbrains:annotations:26.0.2")
        api("org.jsoup:jsoup:1.19.1")
        api(libs.mockito.core)
        api(libs.mockito.junit.jupiter)
        api("org.mockito.kotlin:mockito-kotlin:5.4.0")
        api("org.postgresql:postgresql:42.7.5")
        api("org.skyscreamer:jsonassert:1.5.3")
        api("org.thymeleaf.extras:thymeleaf-extras-java8time:3.0.4.RELEASE")
        api("org.thymeleaf:thymeleaf:3.1.3.RELEASE")
        api(libs.flyingsaucer.core)
        api(libs.flyingsaucer.pdf.openpdf)
        api(libs.ktlint.cli.ruleset.core)
        api(libs.ktlint.rule.engine.core)
        api(libs.ktlint.test)
    }

    api(platform("com.fasterxml.jackson:jackson-bom:2.18.3"))
    api(platform("com.squareup.okhttp3:okhttp-bom:4.12.0"))
    api(platform("io.opentelemetry:opentelemetry-bom:1.49.0"))
    api(platform("io.netty:netty-bom:4.2.0.Final"))
    api(platform("org.apache.cxf:cxf-bom:4.1.1"))
    api(platform("org.jdbi:jdbi3-bom:3.49.0"))
    api(platform(libs.kotlin.bom))
    api(platform("org.junit:junit-bom:5.12.2"))
    api(platform(libs.spring.boot.dependencies))
    api(platform("software.amazon.awssdk:bom:2.29.1"))
}
