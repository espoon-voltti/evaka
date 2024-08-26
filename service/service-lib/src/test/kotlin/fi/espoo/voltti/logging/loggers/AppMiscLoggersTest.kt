// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.voltti.logging.loggers

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.ThrowableProxy
import fi.espoo.voltti.logging.utils.clearTestMessages
import fi.espoo.voltti.logging.utils.getTestAppender
import fi.espoo.voltti.logging.utils.getTestMessages
import fi.espoo.voltti.logging.utils.setupTestAppender
import kotlin.test.assertEquals
import mu.KLogger
import mu.KotlinLogging
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private val logger = KotlinLogging.logger {}.also(KLogger::setupTestAppender)
private const val message = "test message"
private val initialLogLevel = (logger.underlyingLogger as Logger).level

class AppMiscLoggersTest {
    @BeforeEach
    fun before() {
        // To avoid noise in other tests, just raise the log level for these tests that require
        // TRACE
        (logger.underlyingLogger as Logger).level = Level.TRACE
    }

    @AfterEach
    fun clear() {
        logger.clearTestMessages()
        (logger.underlyingLogger as Logger).level = initialLogLevel
    }

    @Test
    fun `trace log message logged`() {
        logger.trace(mapOf("argKey" to "argVal")) { message }
        assertEquals(message, logger.getTestMessages().first())
    }

    @Test
    fun `arguments added to trace log entry`() {
        val args = mapOf("metaArg1" to "metaVal1", "metaArg2" to "metaVal2")
        logger.trace(args) { message }

        val event = logger.getTestAppender().getEvents().first()
        compareArgs(args, event)
    }

    @Test
    fun `debug log message logged`() {
        logger.debug(mapOf("argKey" to "argVal")) { message }
        assertEquals(message, logger.getTestMessages().first())
    }

    @Test
    fun `arguments added to debug log entry`() {
        val args = mapOf("metaArg1" to "metaVal1", "metaArg2" to "metaVal2")
        logger.debug(args) { message }

        val event = logger.getTestAppender().getEvents().first()
        compareArgs(args, event)
    }

    @Test
    fun `info log message logged`() {
        logger.info(mapOf("argKey" to "argVal")) { message }
        assertEquals(message, logger.getTestMessages().first())
    }

    @Test
    fun `arguments added to info log entry`() {
        val args = mapOf("metaArg1" to "metaVal1", "metaArg2" to "metaVal2")
        logger.info(args) { message }

        val event = logger.getTestAppender().getEvents().first()
        compareArgs(args, event)
    }

    @Test
    fun `warn log message logged`() {
        logger.warn(mapOf("argKey" to "argVal")) { message }
        assertEquals(message, logger.getTestMessages().first())
    }

    @Test
    fun `arguments added to warn log entry`() {
        val args = mapOf("metaArg1" to "metaVal1", "metaArg2" to "metaVal2")
        logger.warn(args) { message }

        val event = logger.getTestAppender().getEvents().first()
        compareArgs(args, event)
    }

    @Test
    fun `error log message logged`() {
        logger.error(mapOf("argKey" to "argVal")) { message }
        assertEquals(message, logger.getTestMessages().first())
    }

    @Test
    fun `arguments added to error log entry`() {
        val args = mapOf("metaArg1" to "metaVal1", "metaArg2" to "metaVal2")
        logger.error(args) { message }

        val event = logger.getTestAppender().getEvents().first()
        compareArgs(args, event)
    }

    @Test
    fun `throwable and all arguments included in error log entry`() {
        val exception = RuntimeException("This is a test exception")
        val args = mapOf("metaArg1" to "metaVal1", "metaArg2" to "metaVal2")
        logger.error(exception, args) { message }

        val event = logger.getTestAppender().getEvents().first()
        assertEquals(message, event.message)
        compareArgs(args, event)
        assertEquals((event.throwableProxy as ThrowableProxy).throwable, exception)
    }

    private fun compareArgs(expectedArgs: Map<String, Any>, event: ILoggingEvent) {
        assertEquals(
            mapOf("meta" to expectedArgs.toString()).toString(),
            event.argumentArray.first().toString(),
        )
    }
}
