// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.voltti.logging.utils

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.json.JsonMapper
import java.io.PrintWriter
import java.io.StringWriter
import mu.KLogger
import net.logstash.logback.argument.StructuredArgument
import org.json.JSONObject

const val appenderName = "TestAppender"

class TestAppender : AppenderBase<ILoggingEvent>() {
    init {
        start()
    }

    private var events: MutableList<ILoggingEvent> = mutableListOf()

    fun getEvents() = events.toList()

    override fun getName(): String = appenderName

    override fun append(eventObject: ILoggingEvent) {
        events.add(eventObject)
    }

    fun getMessages(): List<String> = this.events.mapNotNull(ILoggingEvent::getMessage).toList()

    fun clear() {
        this.events = mutableListOf()
    }

    fun getJson(): JSONObject = JSONObject(getJsonString())

    fun getJsonString(): String =
        events
            .first()
            .argumentArray
            .first()
            .let { it as StructuredArgument }
            .let { args ->
                StringWriter().use { sw ->
                    val pw = PrintWriter(sw, true)
                    JsonFactory().createGenerator(pw).use { generator ->
                        generator.run {
                            codec = JsonMapper()
                            writeStartObject()
                            args.writeTo(this)
                            writeEndObject()
                        }
                    }
                    sw.buffer.toString()
                }
            }
}

fun KLogger.setupTestAppender() {
    (this.underlyingLogger as Logger).addAppender(TestAppender())
}

fun KLogger.getTestAppender(): TestAppender = (this.underlyingLogger as Logger).getAppender(appenderName) as TestAppender

fun KLogger.getTestMessages(): List<String> = this.getTestAppender().getMessages()

fun KLogger.getJson(): JSONObject = this.getTestAppender().getJson()

fun KLogger.clearTestMessages() {
    this.getTestAppender().clear()
}
