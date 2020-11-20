// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.voltti.logging.loggers

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import fi.espoo.voltti.logging.utils.clearTestMessages
import fi.espoo.voltti.logging.utils.getTestAppender
import fi.espoo.voltti.logging.utils.getTestMessages
import fi.espoo.voltti.logging.utils.setupTestAppender
import mu.KLogger
import mu.KotlinLogging
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner

private val logger = KotlinLogging.logger {}.also(KLogger::setupTestAppender)
private const val message = "test message"
private val initialLogLevel = (logger.underlyingLogger as Logger).level

@RunWith(SpringRunner::class)
class AppMiscLoggersTest {
    @Before
    fun before() {
        // To avoid noise in other tests, just raise the log level for these tests that require TRACE
        (logger.underlyingLogger as Logger).level = Level.TRACE
    }

    @After
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
        val args = mapOf("argKey" to "argVal", "meta" to mapOf("metaArg1" to "metaVal1", "metaArg2" to "metaVal2"))
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
        val args = mapOf("argKey" to "argVal", "meta" to mapOf("metaArg1" to "metaVal1", "metaArg2" to "metaVal2"))
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
        val args = mapOf("argKey" to "argVal", "meta" to mapOf("metaArg1" to "metaVal1", "metaArg2" to "metaVal2"))
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
        val args = mapOf("argKey" to "argVal", "meta" to mapOf("metaArg1" to "metaVal1", "metaArg2" to "metaVal2"))
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
        val args = mapOf("argKey" to "argVal", "meta" to mapOf("metaArg1" to "metaVal1", "metaArg2" to "metaVal2"))
        logger.error(args) { message }

        val event = logger.getTestAppender().getEvents().first()
        compareArgs(args, event)
    }

    private fun compareArgs(expectedArgs: Map<String, Any>, event: ILoggingEvent) {
        assertEquals(expectedArgs.toString(), event.argumentArray.first().toString())
    }
}
