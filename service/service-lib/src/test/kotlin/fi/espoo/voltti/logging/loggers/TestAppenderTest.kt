// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.voltti.logging.loggers

import fi.espoo.voltti.logging.utils.clearTestMessages
import fi.espoo.voltti.logging.utils.getJson
import fi.espoo.voltti.logging.utils.getTestMessages
import fi.espoo.voltti.logging.utils.setupTestAppender
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import mu.KLogger
import mu.KotlinLogging
import net.logstash.logback.argument.StructuredArguments
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert

private val logger = KotlinLogging.logger {}.also(KLogger::setupTestAppender)

/** Test the test appender implementation */
class TestAppenderTest {
    @AfterEach
    fun clearTestMessages() {
        logger.clearTestMessages()
    }

    @Test
    fun `logged lines can be read back in correct order`() {
        val line1 = "Line 1"
        val line2 = "Line 2"

        logger.info { line1 }
        logger.info { line2 }

        assertEquals(2, logger.getTestMessages().size)
        assertEquals(line1, logger.getTestMessages()[0])
        assertEquals(line2, logger.getTestMessages()[1])
    }

    @Test
    fun `lines are logged regardless of log level`() {
        val line = "Line"

        logger.info { line }
        logger.warn { line }
        logger.error { line }

        assertEquals(3, logger.getTestMessages().size)
        assertTrue(logger.getTestMessages().all { it == line })
    }

    @Test
    fun `test rendering structured arguments to json`() {
        logger.info("message", StructuredArguments.entries(mapOf("one" to "1", "two" to "2")))
        JSONAssert.assertEquals("""{ "one": "1", "two": "2"}""", logger.getJson(), true)
    }
}
