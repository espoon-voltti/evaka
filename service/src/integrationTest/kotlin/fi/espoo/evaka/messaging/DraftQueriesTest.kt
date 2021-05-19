// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.messaging.message.DraftContent
import fi.espoo.evaka.messaging.message.deleteDraft
import fi.espoo.evaka.messaging.message.getDrafts
import fi.espoo.evaka.messaging.message.initDraft
import fi.espoo.evaka.messaging.message.upsertDraft
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.insertTestEmployee
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class DraftQueriesTest : PureJdbiTest() {

    private val accountId: UUID = UUID.randomUUID()

    @BeforeEach
    internal fun setUp() {
        db.transaction { tx ->
            val employeeId = tx.insertTestEmployee(DevEmployee(firstName = "Firstname", lastName = "Employee"))
            tx.createUpdate("INSERT INTO message_account (id, employee_id) VALUES (:id, :employeeId)")
                .bind("id", accountId)
                .bind("employeeId", employeeId)
                .execute()
        }
    }

    @AfterEach
    internal fun tearDown() {
        db.transaction { it.resetDatabase() }
    }

    @Test
    fun `draft queries`() {
        val content = "Content"
        val title = "Hello"
        val recipients = setOf(UUID.randomUUID(), UUID.randomUUID())

        val id = db.transaction { it.initDraft(accountId) }

        assertNotNull(id)
        assertContent(DraftContent())

        upsert(id, DraftContent())
        assertContent(DraftContent())

        upsert(id, DraftContent("Foo", content = null, recipients))
        assertContent(DraftContent(title = "Foo", recipients = recipients))

        upsert(id, DraftContent(title, content, recipients))
        assertContent(DraftContent(title, content, recipients))

        db.transaction { it.deleteDraft(accountId, id) }
        assertEquals(0, db.read { it.getDrafts(accountId) }.size)
    }

    private fun upsert(draftId: UUID, content: DraftContent) =
        db.transaction { it.upsertDraft(accountId, draftId, content) }

    private fun assertContent(expected: DraftContent) =
        assertEquals(expected, db.read { it.getDrafts(accountId)[0] })
}
