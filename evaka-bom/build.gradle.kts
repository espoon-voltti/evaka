// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

plugins {
    `java-platform`
}

object Version {
    const val bouncyCastle = "1.70"
    const val cxf = "3.5.1"
    const val flyingSaucer = "9.1.22"
    const val fuel = "2.3.1"
    const val mockito = "4.4.0"
}

repositories {
    mavenCentral()
}

javaPlatform {
    allowDependencies()
}

dependencies {
    constraints {
        api("com.auth0:java-jwt:3.19.1")
        api("com.github.kagkarlsson:db-scheduler:11.0")
        api("com.github.kittinunf.fuel:fuel:${Version.fuel}")
        api("com.github.kittinunf.fuel:fuel-jackson:${Version.fuel}")
        api("com.google.guava:guava:30.1.1-jre")
        api("com.zaxxer:HikariCP:5.0.1")
        api("dev.akkinoc.spring.boot:logback-access-spring-boot-starter:3.2.5")
        api("io.github.microutils:kotlin-logging-jvm:2.1.21")
        api("io.javalin:javalin:4.4.0")
        api("javax.annotation:javax.annotation-api:1.3.2")
        api("javax.jws:javax.jws-api:1.1")
        api("javax.xml.ws:jaxws-api:2.3.1")
        api("net.bytebuddy:byte-buddy:1.12.1")
        api("net.logstash.logback:logstash-logback-encoder:7.0.1")
        api("org.apache.cxf:cxf-tools-wsdlto-databinding-jaxb:${Version.cxf}") // not included in cxf-bom
        api("org.apache.commons:commons-pool2:2.11.1")
        api("org.apache.commons:commons-text:1.9")
        api("org.apache.santuario:xmlsec:2.2.3")
        api("org.apache.tika:tika-core:2.4.1")
        api("org.apache.wss4j:wss4j-ws-security-dom:2.3.0")
        api("org.bouncycastle:bcpkix-jdk15on:${Version.bouncyCastle}")
        api("org.bouncycastle:bcprov-jdk15on:${Version.bouncyCastle}")
        api("org.flywaydb:flyway-core:8.5.11")
        api("org.glassfish.jaxb:jaxb-runtime:2.3.2")
        api("org.jetbrains:annotations:23.0.0")
        api("org.mockito:mockito-core:${Version.mockito}")
        api("org.mockito:mockito-junit-jupiter:${Version.mockito}")
        api("org.mockito.kotlin:mockito-kotlin:4.0.0")
        api("org.postgresql:postgresql:42.3.3")
        api("org.skyscreamer:jsonassert:1.5.0")
        api("org.thymeleaf.extras:thymeleaf-extras-java8time:3.0.4.RELEASE")
        api("org.thymeleaf:thymeleaf:3.0.15.RELEASE")
        api("org.xhtmlrenderer:flying-saucer-core:${Version.flyingSaucer}")
        api("org.xhtmlrenderer:flying-saucer-pdf-openpdf:${Version.flyingSaucer}")
        api("redis.clients:jedis:4.2.1")
    }

    api(platform("com.fasterxml.jackson:jackson-bom:2.13.3"))
    api(platform("org.apache.cxf:cxf-bom:${Version.cxf}"))
    api(platform("org.jdbi:jdbi3-bom:3.28.0"))
    api(platform("org.jetbrains.kotlin:kotlin-bom:1.6.21"))
    api(platform("org.junit:junit-bom:5.8.2"))
    api(platform("org.springframework.boot:spring-boot-dependencies:2.7.1"))
    api(platform("software.amazon.awssdk:bom:2.17.164"))
}
