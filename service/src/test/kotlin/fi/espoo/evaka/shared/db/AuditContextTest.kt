// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.db

import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.PlacementId
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class AuditContextTest {
    @Test
    fun `add groups placement ids under placementId key`() {
        val ctx = AuditContext()
        val id = PlacementId(UUID.randomUUID())
        ctx.add(id)
        assertEquals(mapOf("placementId" to setOf(id)), ctx.context)
    }

    @Test
    fun `childId and personId both group under personId key`() {
        val ctx = AuditContext()
        val childId = ChildId(UUID.randomUUID())
        val personId = PersonId(UUID.randomUUID())
        ctx.add(childId)
        ctx.add(personId)
        // Both ChildId and PersonId are Id<DatabaseTable.Person>
        assertEquals(mapOf("personId" to setOf(childId, personId)), ctx.context)
    }

    @Test
    fun `multiple adds to same key accumulate`() {
        val ctx = AuditContext()
        val id1 = PlacementId(UUID.randomUUID())
        val id2 = PlacementId(UUID.randomUUID())
        ctx.add(id1)
        ctx.add(id2)
        assertEquals(mapOf("placementId" to setOf(id1, id2)), ctx.context)
    }

    @Test
    fun `add collection accumulates all ids`() {
        val ctx = AuditContext()
        val ids = setOf(PlacementId(UUID.randomUUID()), PlacementId(UUID.randomUUID()))
        ctx.add(ids)
        assertEquals(mapOf("placementId" to ids), ctx.context)
    }

    @Test
    fun `add empty collection is a no-op`() {
        val ctx = AuditContext()
        ctx.add(emptyList<PlacementId>())
        assertEquals(emptyMap(), ctx.context)
    }

    @Test
    fun `adding same id twice deduplicates`() {
        val ctx = AuditContext()
        val id = PlacementId(UUID.randomUUID())
        ctx.add(id)
        ctx.add(id)
        assertEquals(mapOf("placementId" to setOf(id)), ctx.context)
    }

    @Test
    fun `empty context returns empty map`() {
        val ctx = AuditContext()
        assertEquals(emptyMap(), ctx.context)
    }

    @Test
    fun `add returns this for chaining`() {
        val ctx = AuditContext()
        val placementId = PlacementId(UUID.randomUUID())
        val childId = ChildId(UUID.randomUUID())
        val result = ctx.add(placementId).add(childId)
        assertSame(ctx, result)
        assertEquals(
            mapOf("placementId" to setOf(placementId), "personId" to setOf(childId)),
            ctx.context,
        )
    }
}
