// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.shared.MessageAccountId
import fi.espoo.evaka.shared.MessageDraftId
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.RealEvakaClock
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DraftQueriesTest : PureJdbiTest(resetDbBeforeEach = true) {

    private val accountId: MessageAccountId = MessageAccountId(UUID.randomUUID())
    private val clock = RealEvakaClock()

    @BeforeEach
    fun setUp() {
        db.transaction { tx ->
            val employeeId = tx.insert(DevEmployee(firstName = "Firstname", lastName = "Employee"))
            tx.execute {
                sql(
                    "INSERT INTO message_account (id, employee_id, type) VALUES (${bind(accountId)}, ${bind(employeeId)}, 'PERSONAL')"
                )
            }
        }
    }

    @Test
    fun `draft queries`() {
        val content = "Content"
        val title = "Hello"
        val type = MessageType.MESSAGE
        val recipients =
            setOf(
                DraftRecipient(MessageAccountId(UUID.randomUUID()), false),
                DraftRecipient(MessageAccountId(UUID.randomUUID()), false),
            )
        val recipientNames = listOf("Auringonkukat", "Hippiäiset")

        val id = db.transaction { it.initDraft(accountId) }

        val fullContent =
            UpdatableDraftContent(
                type = type,
                title = title,
                content = content,
                recipients = recipients,
                recipientNames = recipientNames,
                urgent = false,
                sensitive = false,
            )
        update(id, fullContent)
        assertContent(fullContent)

        update(id, fullContent.copy(title = "Dogs"))
        assertContent(fullContent.copy(title = "Dogs"))

        db.transaction { it.deleteDraft(accountId, id) }
        assertEquals(0, db.read { it.getDrafts(accountId) }.size)
    }

    private fun update(draftId: MessageDraftId, content: UpdatableDraftContent) =
        db.transaction { it.updateDraft(accountId, draftId, content, clock.now()) }

    private fun assertContent(expected: UpdatableDraftContent) {
        val actual = db.read { it.getDrafts(accountId)[0] }
        assertEquals(expected.title, actual.title)
        assertEquals(expected.type, actual.type)
        assertEquals(expected.content, actual.content)
        assertEquals(expected.recipients, actual.recipients)
        assertEquals(expected.recipientNames, actual.recipientNames)
    }
}
