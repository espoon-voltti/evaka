// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.logging

import ch.qos.logback.access.common.PatternLayoutEncoder
import ch.qos.logback.access.common.spi.IAccessEvent
import ch.qos.logback.access.tomcat.LogbackValve
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.Context
import ch.qos.logback.core.encoder.Encoder
import ch.qos.logback.core.filter.Filter
import ch.qos.logback.core.spi.FilterReply
import fi.espoo.evaka.shared.auth.getAuthenticatedUser
import fi.espoo.evaka.shared.logging.getBestMatchingPattern
import fi.espoo.evaka.shared.logging.getPathVariables
import fi.espoo.voltti.logging.MdcKey
import java.time.Instant
import java.time.ZoneOffset
import net.logstash.logback.composite.AbstractJsonProvider
import net.logstash.logback.composite.JsonProviders
import net.logstash.logback.encoder.AccessEventCompositeJsonEncoder
import org.springframework.core.env.Environment
import tools.jackson.core.JsonGenerator

private object AccessLogging

fun defaultAccessLoggingValve(env: Environment) =
    LogbackValve().apply {
        filename = "we-use-programmatic-config.xml"

        val outputJson =
            env.activeProfiles.contains("production") || env.activeProfiles.contains("json-logging")

        val encoder =
            if (outputJson) {
                val staticFields =
                    arrayOf(
                        "type" to "app-requests-received",
                        "loggerName" to AccessLogging::class.java.name,
                        "version" to 1,
                        "appBuild" to System.getenv("APP_BUILD"),
                        "appCommit" to System.getenv("APP_COMMIT"),
                        "appName" to env.getProperty("spring.application.name"),
                        "env" to System.getenv("VOLTTI_ENV"),
                        "hostIp" to System.getenv("HOST_IP"),
                    )
                createJsonEncoder { event ->
                    val user = event.request.getAuthenticatedUser()
                    val timestamp = Instant.ofEpochMilli(event.timeStamp).atOffset(ZoneOffset.UTC)

                    val httpRoute = event.request.getBestMatchingPattern() ?: ""

                    val pathParams =
                        event.request.getPathVariables().map { (name, value) ->
                            "${MdcKey.HTTP_PATH_PARAM.key}.$name" to value
                        }

                    sequenceOf(
                        *staticFields,
                        "@timestamp" to timestamp,
                        "clientIp" to event.remoteHost,
                        "contentLength" to event.contentLength,
                        "httpMethod" to event.method,
                        "httpRoute" to httpRoute,
                        "path" to event.requestURI,
                        "queryString" to event.queryString,
                        "responseTime" to event.elapsedTime,
                        "statusCode" to event.statusCode,
                        "traceId" to event.getRequestHeader("X-Request-ID"),
                        "userIdHash" to user?.rawIdHash?.toString(),
                        "userId" to user?.rawId().toString(),
                    ) + pathParams
                }
            } else {
                createPatternLayoutEncoder("common")
            }

        addAppender(createConsoleAppender(encoder))
    }

fun Context.createConsoleAppender(encoder: Encoder<IAccessEvent>) =
    ConsoleAppender<IAccessEvent>().apply {
        this.encoder = encoder
        addFilter(
            AccessLoggingFilter().apply {
                this.context = this@createConsoleAppender
                start()
            }
        )
        this.context = this@createConsoleAppender
        start()
    }

fun Context.createPatternLayoutEncoder(pattern: String) =
    PatternLayoutEncoder().apply {
        this.pattern = pattern
        this.context = this@createPatternLayoutEncoder
        start()
    }

fun Context.createJsonEncoder(
    collectFields: (event: IAccessEvent) -> Sequence<Pair<String, Any?>>
) =
    AccessEventCompositeJsonEncoder().apply {
        this.providers =
            JsonProviders<IAccessEvent>().apply {
                addProvider(
                    object : AbstractJsonProvider<IAccessEvent>() {
                        override fun writeTo(generator: JsonGenerator, event: IAccessEvent) {
                            collectFields(event).forEach { (name, value) ->
                                generator.writePOJOProperty(name, value)
                            }
                        }
                    }
                )
                this.setContext(this@createJsonEncoder)
                start()
            }
        this.context = this@createJsonEncoder
        start()
    }

class AccessLoggingFilter : Filter<IAccessEvent>() {
    override fun decide(event: IAccessEvent): FilterReply =
        when (event.request.requestURI) {
            "/health" -> FilterReply.DENY
            "/actuator/health" -> FilterReply.DENY
            else -> FilterReply.NEUTRAL
        }
}
