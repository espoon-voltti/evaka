// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.shared.MessageAccountId
import fi.espoo.evaka.shared.MessageDraftId
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.insertTestEmployee
import fi.espoo.evaka.shared.dev.resetDatabase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals

class DraftQueriesTest : PureJdbiTest() {

    private val accountId: MessageAccountId = MessageAccountId(UUID.randomUUID())

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
        val type = MessageType.MESSAGE
        val recipients = setOf(MessageAccountId(UUID.randomUUID()), MessageAccountId(UUID.randomUUID()))
        val recipientNames = listOf("Auringonkukat", "Hippiäiset")

        val id = db.transaction { it.initDraft(accountId) }

        val fullContent = UpsertableDraftContent(
            type = type,
            title = title,
            content = content,
            recipientIds = recipients,
            recipientNames = recipientNames
        )
        upsert(id, fullContent)
        assertContent(fullContent)

        upsert(id, fullContent.copy(title = "Dogs"))
        assertContent(fullContent.copy(title = "Dogs"))

        db.transaction { it.deleteDraft(accountId, id) }
        assertEquals(0, db.read { it.getDrafts(accountId) }.size)
    }

    private fun upsert(draftId: MessageDraftId, content: UpsertableDraftContent) =
        db.transaction { it.upsertDraft(accountId, draftId, content) }

    private fun assertContent(expected: UpsertableDraftContent) {
        val actual = db.read { it.getDrafts(accountId)[0] }
        assertEquals(expected.title, actual.title)
        assertEquals(expected.type, actual.type)
        assertEquals(expected.content, actual.content)
        assertEquals(expected.recipientIds, actual.recipientIds)
        assertEquals(expected.recipientNames, actual.recipientNames)
    }
}
