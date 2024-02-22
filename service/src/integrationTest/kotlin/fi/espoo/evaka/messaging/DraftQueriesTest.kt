// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.shared.MessageAccountId
import fi.espoo.evaka.shared.MessageDraftId
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.insert
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DraftQueriesTest : PureJdbiTest(resetDbBeforeEach = true) {

    private val accountId: MessageAccountId = MessageAccountId(UUID.randomUUID())

    @BeforeEach
    fun setUp() {
        db.transaction { tx ->
            val employeeId = tx.insert(DevEmployee(firstName = "Firstname", lastName = "Employee"))
            @Suppress("DEPRECATION")
            tx.createUpdate(
                    "INSERT INTO message_account (id, employee_id, type) VALUES (:id, :employeeId, 'PERSONAL')"
                )
                .bind("id", accountId)
                .bind("employeeId", employeeId)
                .execute()
        }
    }

    @Test
    fun `draft queries`() {
        val content = "Content"
        val title = "Hello"
        val type = MessageType.MESSAGE
        val recipients =
            setOf(MessageAccountId(UUID.randomUUID()), MessageAccountId(UUID.randomUUID()))
        val recipientNames = listOf("Auringonkukat", "Hippiäiset")

        val id = db.transaction { it.initDraft(accountId) }

        val fullContent =
            UpdatableDraftContent(
                type = type,
                title = title,
                content = content,
                recipientIds = recipients,
                recipientNames = recipientNames,
                urgent = false,
                sensitive = false
            )
        update(id, fullContent)
        assertContent(fullContent)

        update(id, fullContent.copy(title = "Dogs"))
        assertContent(fullContent.copy(title = "Dogs"))

        db.transaction { it.deleteDraft(accountId, id) }
        assertEquals(0, db.read { it.getDrafts(accountId) }.size)
    }

    private fun update(draftId: MessageDraftId, content: UpdatableDraftContent) =
        db.transaction { it.updateDraft(accountId, draftId, content) }

    private fun assertContent(expected: UpdatableDraftContent) {
        val actual = db.read { it.getDrafts(accountId)[0] }
        assertEquals(expected.title, actual.title)
        assertEquals(expected.type, actual.type)
        assertEquals(expected.content, actual.content)
        assertEquals(expected.recipientIds, actual.recipientIds)
        assertEquals(expected.recipientNames, actual.recipientNames)
    }
}
