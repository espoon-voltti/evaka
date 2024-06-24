// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.voltti.logging.logback

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.read.ListAppender
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.MappingJsonFactory
import com.fasterxml.jackson.databind.json.JsonMapper
import java.util.UUID
import net.logstash.logback.encoder.CompositeJsonEncoder
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.slf4j.LoggerFactory

// DSL

fun withTestLoggers(block: (TestLoggers) -> Unit): Unit =
    setUpTestLoggers().let {
        try {
            block(it)
        } finally {
            it.stop()
        }
    }

private fun setUpTestLoggers() =
    TestLoggers(
        audit = TestLogger("VOLTTI_AUDIT_APPENDER"),
        default = TestLogger("VOLTTI_DEFAULT_APPENDER"),
        sanitized = TestLogger("VOLTTI_DEFAULT_APPENDER_SANITIZED")
    ).apply { start() }

class TestLogger(
    name: String
) {
    private val logger: Logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
    val encoder: CompositeJsonEncoder<ILoggingEvent>
    val appender: ListAppender<ILoggingEvent>

    init {
        (logger.getAppender(name) as ConsoleAppender).let { console ->
            encoder = console.encoder as CompositeJsonEncoder<ILoggingEvent>
            appender =
                ListAppender<ILoggingEvent>().apply {
                    console.copyOfAttachedFiltersList.forEach { filter -> addFilter(filter) }
                }
        }
    }

    fun start() {
        encoder.start()
        appender.start()
        logger.addAppender(appender)
    }

    fun stop() {
        logger.detachAppender(appender)
        appender.stop()
        encoder.stop()
    }
}

data class TestLoggers(
    val audit: TestLogger,
    val default: TestLogger,
    val sanitized: TestLogger
) {
    fun start() {
        audit.start()
        default.start()
        sanitized.start()
    }

    fun stop() {
        audit.stop()
        default.stop()
        sanitized.start()
    }
}

// assert utils

val testSSNs = listOf("130105-0872", "130105A087A", "130105a087a", "130105+0872")
const val redactedSSN = "REDACTED-SSN"
val UUIDWithSSNs =
    listOf("e1130765-2925-4549-b395-9abdd6c8e08a", "e1130765-b9b5-4549-b395-922222a8208a", "5c0250a8-a3fa-449c-a188-6010108a000a")

fun TestLoggers.assertAudit() = assertThat(audit.appender.list.map { it.toJson(audit.encoder).asMap(mapper) }).extracting(*auditEventProps)

fun TestLoggers.assertDefault() =
    assertThat(default.appender.list.map { it.toJson(default.encoder).asMap(mapper) }).extracting(*defaultEventProps)

fun TestLoggers.assertSanitized() =
    assertThat(sanitized.appender.list.map { it.toJson(sanitized.encoder).asMap(mapper) }).extracting(*defaultEventProps)

fun TestLoggers.withLatestDefault(block: (Map<String, Any>) -> Unit) =
    block(
        default.appender.list
            .last()
            .toJson(default.encoder)
            .asMap(mapper)
    )

fun TestLoggers.withLatestSanitized(block: (Map<String, Any>) -> Unit) =
    block(
        sanitized.appender.list
            .last()
            .toJson(sanitized.encoder)
            .asMap(mapper)
    )

data class AuditEvent(
    val userId: String,
    val userIdHash: String,
    val description: String,
    val eventCode: String,
    val targetId: Any,
    private val appName: String = "test-service",
    private val type: String = "app-audit-events",
    private val version: Int = 1
) {
    fun tuple() =
        Tuple(
            userId,
            description,
            eventCode,
            targetId,
            userIdHash,
            appName,
            EnvFields.appBuild,
            EnvFields.appCommit,
            EnvFields.env,
            EnvFields.hostIp,
            type,
            version
        )

    companion object {
        fun dummy(prefix: String) =
            AuditEvent(
                userId = "$prefix.userId",
                userIdHash = "$prefix.userIdHash",
                description = "$prefix.description",
                eventCode = "$prefix.someEvent",
                targetId = "$prefix.id"
            )
    }
}

private val auditEventProps =
    arrayOf(
        "userId",
        "description",
        "eventCode",
        "targetId",
        "userIdHash",
        "appName",
        "appBuild",
        "appCommit",
        "env",
        "hostIp",
        "type",
        "version"
    )

data class DefaultEvent(
    val userIdHash: String
) {
    val message: String = "${UUID.randomUUID()}.message"
    private val appName: String = "test-service"
    private val type: String = "app-misc"
    private val version: Int = 1

    fun tuple() =
        Tuple(
            message,
            appName,
            EnvFields.appBuild,
            EnvFields.appCommit,
            EnvFields.env,
            EnvFields.hostIp,
            userIdHash,
            type,
            version
        )
}

private val defaultEventProps =
    arrayOf(
        "message",
        "appName",
        "appBuild",
        "appCommit",
        "env",
        "hostIp",
        "userIdHash",
        "type",
        "version"
    )

private object EnvFields {
    const val appBuild = "APP_BUILD_IS_UNDEFINED"
    const val appCommit = "APP_COMMIT_IS_UNDEFINED"
    const val env = "VOLTTI_ENV_IS_UNDEFINED"
    const val hostIp = "HOST_IP_IS_UNDEFINED"
}

// json utils

fun Any.asMap(jsonMapper: JsonMapper = mapper): Map<String, Any> =
    jsonMapper.convertValue<Map<String, Any>>(this, object : TypeReference<Map<String, Any>>() {})

private fun ILoggingEvent.toJson(encoder: CompositeJsonEncoder<ILoggingEvent>): JsonNode = mapper.readTree(encoder.encode(this))

@Suppress("DEPRECATION")
private val factory = MappingJsonFactory().enable(JsonGenerator.Feature.ESCAPE_NON_ASCII) as JsonFactory
private val mapper = JsonMapper(factory)
