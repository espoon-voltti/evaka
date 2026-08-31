// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import evaka.core.shared.Id
import java.io.PrintWriter
import java.io.StringWriter
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.logstash.logback.argument.StructuredArgument
import org.slf4j.LoggerFactory
import tools.jackson.databind.json.JsonMapper

data class CapturedAuditEvent(val fields: Map<String, Any?>, val mdc: Map<String, String>) {
    val eventCode: String
        get() = fields["eventCode"] as String

    /** Context ids by key, e.g. `employeeId` to the raw ids logged under it. */
    val context: Map<String, Set<String>>
        get() =
            (fields["context"] as? Map<*, *>).orEmpty().entries.associate { (key, value) ->
                key as String to (value as Collection<*>).map { it as String }.toSet()
            }

    val meta: Map<String, Any?>
        get() = (fields["meta"] as? Map<*, *>).orEmpty().mapKeys { it.key as String }

    val minDate: LocalDate?
        get() = (fields["minDate"] as String?)?.let(LocalDate::parse)

    /** Null for events emitted outside a request, e.g. by a scheduled job. */
    val userId: String?
        get() = mdc["userId"]

    fun containsId(id: Id<*>): Boolean = context.values.any { it.contains(id.raw.toString()) }

    /**
     * Asserts that the event context is exactly the one [expected] builds. The expectation is
     * written the same way as in production code, e.g. `assertContext { add(unitId).add(employeeId)
     * }`, so the keys are derived from the id types instead of being spelled out.
     */
    fun assertContext(expected: AuditContext.() -> Unit) = apply {
        val expectedContext =
            AuditContext().apply(expected).context.mapValues { (_, ids) ->
                ids.map { it.raw.toString() }.toSet()
            }
        assertEquals(expectedContext, context, "unexpected context in $eventCode event")
    }

    /**
     * Asserts that the event meta is exactly [expected]. Dates, enums and ids may be given as-is:
     * they are converted to the form they take in the log entry.
     */
    fun assertMeta(vararg expected: Pair<String, Any?>) = apply {
        assertEquals(
            expected.associate { (key, value) -> key to logValueOf(value) },
            meta,
            "unexpected meta in $eventCode event",
        )
    }

    fun assertMinDate(expected: LocalDate?) = apply {
        assertEquals(expected, minDate, "unexpected minDate in $eventCode event")
    }
}

/** The value as it appears in a log entry after JSON serialization. */
private fun logValueOf(value: Any?): Any? =
    when (value) {
        is LocalDate -> value.toString()
        is Enum<*> -> value.name
        is Id<*> -> value.raw.toString()
        is AuditChange -> mapOf("old" to logValueOf(value.old), "new" to logValueOf(value.new))
        is Collection<*> -> value.map { logValueOf(it) }
        else -> value
    }

/**
 * Captures audit events emitted during a test, decoded to the structured fields they would produce
 * in the audit log entry. Attach with [attach] (e.g. in a @BeforeEach) and detach with [detach]
 * (@AfterEach).
 */
class AuditLogCapture : AppenderBase<ILoggingEvent>() {
    private val mapper = JsonMapper()
    private val captured = mutableListOf<CapturedAuditEvent>()

    override fun append(eventObject: ILoggingEvent) {
        if (eventObject.markerList?.any { it.name == "AUDIT_EVENT" } != true) return
        val arg = eventObject.argumentArray?.firstOrNull() as? StructuredArgument ?: return
        val json =
            StringWriter().use { sw ->
                val pw = PrintWriter(sw, true)
                mapper.createGenerator(pw).use { generator ->
                    generator.run {
                        writeStartObject()
                        arg.writeTo(this)
                        writeEndObject()
                    }
                }
                sw.toString()
            }
        @Suppress("UNCHECKED_CAST")
        val fields = mapper.readValue(json, Map::class.java) as Map<String, Any?>
        synchronized(captured) {
            captured.add(CapturedAuditEvent(fields, eventObject.mdcPropertyMap.toMap()))
        }
    }

    fun events(event: Audit): List<CapturedAuditEvent> =
        synchronized(captured) { captured.filter { it.fields["eventCode"] == event.name } }

    /** The only captured [event]; fails if there is not exactly one. */
    fun event(event: Audit): CapturedAuditEvent {
        val events = events(event)
        assertEquals(1, events.size, "expected exactly one ${event.name} event, got ${events.size}")
        return events.single()
    }

    /** The only captured [event] with [id] in its context; fails if there is not exactly one. */
    fun event(event: Audit, id: Id<*>): CapturedAuditEvent {
        val events = events(event).filter { it.containsId(id) }
        assertEquals(
            1,
            events.size,
            "expected exactly one ${event.name} event with id $id, got ${events.size}",
        )
        return events.single()
    }

    fun assertNoEvents(event: Audit) {
        assertTrue(events(event).isEmpty(), "expected no ${event.name} events")
    }

    fun clear() {
        synchronized(captured) { captured.clear() }
    }

    fun attach() {
        // test classes use @TestInstance(PER_CLASS), so the same capture instance is reused
        // across test methods and must start each test empty
        clear()
        start()
        rootLogger().addAppender(this)
    }

    fun detach() {
        rootLogger().detachAppender(this)
        stop()
    }

    private fun rootLogger() = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
}
