// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core

import evaka.core.shared.ChildId
import evaka.core.shared.PersonId
import evaka.core.shared.PlacementId
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class AuditContextTest {
    private val today = LocalDate.of(2026, 6, 24)

    private fun auditContext() = AuditContext()

    @Test
    fun `add groups placement ids under placementId key`() {
        val ctx = auditContext()
        val id = PlacementId(UUID.randomUUID())
        ctx.add(id)
        assertEquals(mapOf("placementId" to setOf(id)), ctx.context)
    }

    @Test
    fun `childId and personId both group under personId key`() {
        val ctx = auditContext()
        val childId = ChildId(UUID.randomUUID())
        val personId = PersonId(UUID.randomUUID())
        ctx.add(childId)
        ctx.add(personId)
        // Both ChildId and PersonId are Id<DatabaseTable.Person>
        assertEquals(mapOf("personId" to setOf(childId, personId)), ctx.context)
    }

    @Test
    fun `multiple adds to same key accumulate`() {
        val ctx = auditContext()
        val id1 = PlacementId(UUID.randomUUID())
        val id2 = PlacementId(UUID.randomUUID())
        ctx.add(id1)
        ctx.add(id2)
        assertEquals(mapOf("placementId" to setOf(id1, id2)), ctx.context)
    }

    @Test
    fun `add collection accumulates all ids`() {
        val ctx = auditContext()
        val ids = setOf(PlacementId(UUID.randomUUID()), PlacementId(UUID.randomUUID()))
        ctx.add(ids)
        assertEquals(mapOf("placementId" to ids), ctx.context)
    }

    @Test
    fun `add empty collection is a no-op`() {
        val ctx = auditContext()
        ctx.add(emptyList<PlacementId>())
        assertEquals(emptyMap(), ctx.context)
    }

    @Test
    fun `adding same id twice deduplicates`() {
        val ctx = auditContext()
        val id = PlacementId(UUID.randomUUID())
        ctx.add(id)
        ctx.add(id)
        assertEquals(mapOf("placementId" to setOf(id)), ctx.context)
    }

    @Test
    fun `empty context returns empty map`() {
        val ctx = auditContext()
        assertEquals(emptyMap(), ctx.context)
    }

    @Test
    fun `add returns this for chaining`() {
        val ctx = auditContext()
        val placementId = PlacementId(UUID.randomUUID())
        val childId = ChildId(UUID.randomUUID())
        val result = ctx.add(placementId).add(childId)
        assertSame(ctx, result)
        assertEquals(
            mapOf("placementId" to setOf(placementId), "personId" to setOf(childId)),
            ctx.context,
        )
    }

    @Test
    fun `meta is empty by default`() {
        val ctx = auditContext()
        assertEquals(emptyMap(), ctx.meta)
    }

    @Test
    fun `addMeta records entries`() {
        val ctx = auditContext()
        ctx.addMeta("category", "OTHER_ABSENCE")
        ctx.addMeta("date", "2020-01-01")
        assertEquals(mapOf("category" to "OTHER_ABSENCE", "date" to "2020-01-01"), ctx.meta)
    }

    @Test
    fun `addMeta overwrites an existing key`() {
        val ctx = auditContext()
        ctx.addMeta("count", 1)
        ctx.addMeta("count", 2)
        assertEquals(mapOf("count" to 2), ctx.meta)
    }

    @Test
    fun `addMeta returns this for chaining`() {
        val ctx = auditContext()
        val result = ctx.addMeta("a", 1).addMeta("b", 2)
        assertSame(ctx, result)
    }

    @Test
    fun `minDate is null by default`() {
        val ctx = auditContext()
        assertNull(ctx.minDate)
    }

    @Test
    fun `observeDate sets minDate when none is present`() {
        val ctx = auditContext()
        ctx.observeDate(LocalDate.of(2020, 6, 1))
        assertEquals(LocalDate.of(2020, 6, 1), ctx.minDate)
    }

    @Test
    fun `observeDate keeps the earliest date regardless of call order`() {
        val ctx = auditContext()
        ctx.observeDate(LocalDate.of(2020, 6, 1))
        ctx.observeDate(LocalDate.of(2018, 1, 1)) // earlier -> replaces
        ctx.observeDate(LocalDate.of(2022, 12, 31)) // later -> ignored
        assertEquals(LocalDate.of(2018, 1, 1), ctx.minDate)
    }

    @Test
    fun `observeDate ignores null`() {
        val ctx = auditContext()
        ctx.observeDate(LocalDate.of(2020, 6, 1))
        ctx.observeDate(null)
        assertEquals(LocalDate.of(2020, 6, 1), ctx.minDate)
    }

    @Test
    fun `observeDate returns this for chaining`() {
        val ctx = auditContext()
        val result = ctx.observeDate(LocalDate.of(2020, 6, 1)).observeDate(LocalDate.of(2019, 1, 1))
        assertSame(ctx, result)
        assertEquals(LocalDate.of(2019, 1, 1), ctx.minDate)
    }

    @Test
    fun `daysIntoHistory is null when minDate is null`() {
        assertNull(daysIntoHistory(minDate = null, today = today))
    }

    @Test
    fun `daysIntoHistory is zero when minDate is in the future`() {
        assertEquals(0L, daysIntoHistory(minDate = LocalDate.of(2026, 8, 1), today = today))
    }

    @Test
    fun `daysIntoHistory is zero when minDate is today`() {
        assertEquals(0L, daysIntoHistory(minDate = today, today = today))
    }

    @Test
    fun `daysIntoHistory counts days when minDate is in the past`() {
        assertEquals(1L, daysIntoHistory(minDate = LocalDate.of(2026, 6, 23), today = today))
        assertEquals(365L, daysIntoHistory(minDate = LocalDate.of(2025, 6, 24), today = today))
    }

    @Test
    fun `logFields assembles the accumulated context, meta, minDate and daysIntoHistory`() {
        val ctx = auditContext()
        val childId = ChildId(UUID.randomUUID())
        ctx.add(childId).addMeta("count", 3).observeDate(LocalDate.of(2025, 6, 24))

        val fields = ctx.logFields(AuditEvent.ChildServiceApplicationsRead, today)

        assertEquals(
            mapOf(
                "eventCode" to "ChildServiceApplicationsRead",
                "context" to mapOf("personId" to listOf(childId.raw.toString())),
                "minDate" to "2025-06-24",
                "daysIntoHistory" to 365L,
                "securityLevel" to "low",
                "securityEvent" to false,
                "meta" to mapOf("count" to 3),
            ),
            fields,
        )
    }

    @Test
    fun `logFields omits the meta key when no meta was recorded`() {
        val ctx = auditContext()
        val fields = ctx.logFields(AuditEvent.UnitServiceApplicationsRead, today)
        assertEquals(false, fields.containsKey("meta"))
        assertNull(fields["minDate"])
        assertNull(fields["daysIntoHistory"])
    }
}
