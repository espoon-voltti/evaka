// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core

import evaka.core.shared.DatabaseTable
import evaka.core.shared.Id
import evaka.core.shared.domain.EvakaClock
import java.time.LocalDate

class AuditContext {
    @PublishedApi internal val ids = mutableMapOf<String, MutableSet<Id<*>>>()
    private val metaEntries = mutableMapOf<String, Any?>()

    /**
     * The earliest date the audited operation reaches into. Lets searches find operations that
     * target data far in the past (e.g. a year or more ago), which may warrant a security review.
     *
     * Contributed via [observeDate] from any layer; the context keeps the earliest value seen.
     */
    var minDate: LocalDate? = null
        private set

    inline fun <reified T : DatabaseTable> add(id: Id<T>): AuditContext {
        val key = T::class.simpleName!!.replaceFirstChar { it.lowercase() } + "Id"
        ids.getOrPut(key) { mutableSetOf() }.add(id)
        return this
    }

    inline fun <reified T : DatabaseTable> add(ids: Collection<Id<T>>): AuditContext {
        if (ids.isEmpty()) return this
        val key = T::class.simpleName!!.replaceFirstChar { it.lowercase() } + "Id"
        this.ids.getOrPut(key) { mutableSetOf() }.addAll(ids)
        return this
    }

    /**
     * Records a free-form metadata entry. May be called from any layer, letting deeper code
     * contribute descriptive details (e.g. the date range of an entity it is about to delete).
     * Writing the same key again overwrites the previous value.
     *
     * Never put sensitive data (free text, personal details, SSNs, notes) here.
     */
    fun addMeta(key: String, value: Any?): AuditContext {
        metaEntries[key] = value
        return this
    }

    /**
     * Records that the operation reaches the given [date], narrowing [minDate] toward the earliest
     * date contributed. Acts as an accumulator: the stored value is replaced only when [date] is
     * earlier than the current one. `null` is ignored, so callers can pass nullable entity dates
     * directly.
     */
    fun observeDate(date: LocalDate?): AuditContext {
        if (date == null) return this
        val current = minDate
        if (current == null || date < current) minDate = date
        return this
    }

    val context: Map<String, Set<Id<*>>>
        get() = ids.mapValues { it.value.toSet() }

    val meta: Map<String, Any?>
        get() = metaEntries.toMap()

    /** Emits the audit event for [event], using the accumulated context and the request [clock]. */
    fun log(event: Audit, clock: EvakaClock) =
        event.log(
            today = clock.today(),
            minDate = minDate,
            context = context.mapValues { (_, ids) -> ids.map { it.raw.toString() } },
            meta = meta,
        )
}
