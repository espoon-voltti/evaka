// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

plugins {
    `java-platform`
}

object Version {
    const val bouncyCastle = "1.68"
    const val flyingSaucer = "9.1.20"
    const val fuel = "2.3.1"
    const val mockito = "3.7.7"
}

javaPlatform {
    allowDependencies()
}

dependencies {
    constraints {
        api("com.auth0:java-jwt:3.13.0")
        api("com.github.kittinunf.fuel:fuel:${Version.fuel}")
        api("com.github.kittinunf.fuel:fuel-jackson:${Version.fuel}")
        api("com.google.guava:guava:30.1.1-jre")
        api("com.nhaarman:mockito-kotlin:1.6.0")
        api("com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0")
        api("com.zaxxer:HikariCP:4.0.2")
        api("io.github.microutils:kotlin-logging-jvm:2.0.4")
        api("io.javalin:javalin:3.13.3")
        api("io.springfox:springfox-swagger2:3.0.0")
        api("javax.annotation:javax.annotation-api:1.3.2")
        api("javax.jws:javax.jws-api:1.1")
        api("javax.xml.ws:jaxws-api:2.3.1")
        api("net.bytebuddy:byte-buddy:1.10.21")
        api("net.logstash.logback:logstash-logback-encoder:6.6")
        api("net.rakugakibox.spring.boot:logback-access-spring-boot-starter:2.7.1")
        api("org.apache.commons:commons-pool2:2.8.1")
        api("org.apache.commons:commons-text:1.9")
        api("org.apache.wss4j:wss4j-ws-security-dom:2.3.0")
        api("org.bouncycastle:bcpkix-jdk15on:${Version.bouncyCastle}")
        api("org.bouncycastle:bcprov-jdk15on:${Version.bouncyCastle}")
        api("org.flywaydb:flyway-core:7.5.3")
        api("org.glassfish.jaxb:jaxb-runtime:2.3.2")
        api("org.jetbrains:annotations:20.1.0")
        api("org.mockito:mockito-core:${Version.mockito}")
        api("org.mockito:mockito-junit-jupiter:${Version.mockito}")
        api("org.postgresql:postgresql:42.2.19")
        api("org.skyscreamer:jsonassert:1.5.0")
        api("org.thymeleaf.extras:thymeleaf-extras-java8time:3.0.3.RELEASE")
        api("org.thymeleaf:thymeleaf:3.0.11.RELEASE")
        api("org.xhtmlrenderer:flying-saucer-core:${Version.flyingSaucer}")
        api("org.xhtmlrenderer:flying-saucer-pdf-openpdf:${Version.flyingSaucer}")
        api("redis.clients:jedis:3.5.1")
    }

    api(platform("com.amazonaws:aws-java-sdk-bom:1.11.959"))
    api(platform("com.fasterxml.jackson:jackson-bom:2.12.1"))
    api(platform("org.jdbi:jdbi3-bom:3.18.0"))
    api(platform("org.jetbrains.kotlin:kotlin-bom:1.4.30"))
    api(platform("org.junit:junit-bom:5.7.1"))
    api(platform("org.springframework.boot:spring-boot-dependencies:2.4.3"))
    api(platform("org.testcontainers:testcontainers-bom:1.15.2"))
}
