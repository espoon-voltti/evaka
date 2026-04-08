// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.db

import fi.espoo.evaka.shared.PlacementId
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class AuditedResultTest {
    @Test
    fun `logThenResult calls log callback and returns result`() {
        val ctx = AuditContext()
        val id = PlacementId(UUID.randomUUID())
        ctx.add(id)
        val audited = AuditedResult("hello", ctx)

        var capturedContext: AuditContext? = null
        val result = audited.logThenResult { context -> capturedContext = context }

        assertEquals("hello", result)
        assertSame(ctx, capturedContext)
    }

    @Test
    fun `destructuring works`() {
        val ctx = AuditContext()
        val audited = AuditedResult(42, ctx)
        val (result, context) = audited
        assertEquals(42, result)
        assertSame(ctx, context)
    }
}
