// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.voltti.logging.logback

import fi.espoo.voltti.logging.loggers.info
import java.util.UUID
import mu.KLogger
import mu.KMarkerFactory
import mu.KotlinLogging
import net.logstash.logback.argument.StructuredArguments
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.slf4j.MDC

private val logger = KotlinLogging.logger {}
private val auditMarker = KMarkerFactory.getMarker("AUDIT_EVENT")

private fun KLogger.audit(
    msg: String,
    args: Map<String, Any>
) = info(auditMarker, msg, StructuredArguments.e(args))

class AppenderTest {
    private val meta = TestMeta.random()
    private val userIdHash = UUID.randomUUID().toString()

    @AfterEach
    fun afterEach() {
        MDC.clear()
    }

    @Test
    fun `audit appender`() {
        listOf("test1", "test2").map { prefix ->
            withTestLoggers {
                MDC.put("userId", "$prefix.userId")
                MDC.put("userIdHash", "$prefix.userIdHash")

                logger.audit(
                    "$prefix.description",
                    mapOf(
                        "eventCode" to "$prefix.someEvent",
                        "targetId" to "$prefix.id"
                    )
                )

                it.assertAudit().containsExactly(AuditEvent.dummy(prefix).tuple())
                it.assertDefault().isEmpty()
            }
        }
    }

    @Test
    fun `default appender`() {
        withTestLoggers {
            MDC.put("userIdHash", userIdHash)

            val event1 = DefaultEvent(userIdHash)
            val exception = TestException()
            logger.error(event1.message, StructuredArguments.e(mapOf("meta" to meta)), exception)
            it.assertDefault().containsExactly(event1.tuple())
            it.withLatestDefault { actual -> defaultErrorAssertions(actual, meta, exception) }

            val event2 = DefaultEvent(userIdHash)
            logger.info(event2.message)
            it.assertDefault().containsExactly(event1.tuple(), event2.tuple())
            it.withLatestDefault { actual -> defaultInfoAssertions(actual) }

            it.assertAudit().isEmpty()
        }
    }

    @Test
    fun `sanitized appender`() {
        withTestLoggers {
            MDC.put("userIdHash", userIdHash)

            val event1 = DefaultEvent(userIdHash)
            val exception = TestExceptionSensitive()
            logger.error(event1.message, StructuredArguments.e(mapOf("meta" to meta)), exception)
            it.assertSanitized().containsExactly(event1.tuple())
            it.withLatestSanitized { actual ->
                defaultErrorAssertions(actual, meta, exception)
                assertThat(actual["stackTrace"] as String).doesNotContain(testSSNs[0])
                assertThat(actual["stackTrace"] as String).contains(redactedSSN)
            }

            val event2 = DefaultEvent(userIdHash)
            logger.info(event2.message)
            it.assertSanitized().containsExactly(event1.tuple(), event2.tuple())
            it.withLatestSanitized { actual -> defaultInfoAssertions(actual) }

            testSSNs.forEach { ssn ->
                logger.info(mapOf("body" to """{"ssn": "$ssn"}""")) { "Accidental SSN logging: $ssn}" }
                it.withLatestSanitized { actual ->
                    assertThat(actual["message"] as String).doesNotContain(ssn)
                    assertThat(actual["message"] as String).contains(redactedSSN)
                    assertThat((actual["meta"] as Map<*, *>)["body"] as String).doesNotContain(ssn)
                    assertThat((actual["meta"] as Map<*, *>)["body"] as String).contains(redactedSSN)
                }
            }

            UUIDWithSSNs.forEach { uuid ->
                logger.info(mapOf("body" to """{"id": "$uuid"}""")) { "UUID has SSN in it: $uuid" }
                it.withLatestSanitized { actual ->
                    assertThat(actual["message"] as String).contains(uuid)
                    assertThat(actual["message"] as String).doesNotContain(redactedSSN)
                    assertThat((actual["meta"] as Map<*, *>)["body"] as String).contains(uuid)
                    assertThat((actual["meta"] as Map<*, *>)["body"] as String).doesNotContain(redactedSSN)
                }
            }

            it.assertAudit().isEmpty()
        }
    }

    private fun defaultInfoAssertions(actual: Map<String, Any>) {
        assertThat(actual["logLevel"]).isEqualTo("INFO")
        assertThat(actual["@timestamp"] as String).isNotBlank
        assertThat(actual["exception"]).isNull()
        assertThat(actual["stackTrace"]).isNull()
        assertThat(actual["meta"]).isNull()
    }

    private fun defaultErrorAssertions(
        actual: Map<String, Any>,
        meta: TestMeta,
        exception: RuntimeException
    ) {
        assertThat(actual["logLevel"]).isEqualTo("ERROR")
        assertThat(actual["@timestamp"] as String).isNotBlank
        assertThat(actual["exception"]).isEqualTo(exception::class.java.simpleName)
        assertThat(actual["stackTrace"] as String).isNotBlank
        assertThat(actual["meta"]).isEqualTo(meta.asMap())
    }
}

data class TestMeta(
    val key1: String,
    val key2: String
) {
    companion object {
        fun random() = TestMeta(UUID.randomUUID().toString(), UUID.randomUUID().toString())
    }
}

class TestException : RuntimeException("BOOM!")

class TestExceptionSensitive : RuntimeException("BOOM! (social_security_number)=(${testSSNs[0]})")
