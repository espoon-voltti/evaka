// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.citizenwebpush

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.messaging.MessageType
import fi.espoo.evaka.messaging.getAccountNames
import fi.espoo.evaka.messaging.getCitizenMessageAccount
import fi.espoo.evaka.messaging.insertMessage
import fi.espoo.evaka.messaging.insertMessageContent
import fi.espoo.evaka.messaging.insertRecipients
import fi.espoo.evaka.messaging.insertThread
import fi.espoo.evaka.messaging.upsertEmployeeMessageAccount
import fi.espoo.evaka.shared.MessageAccountId
import fi.espoo.evaka.shared.MessageId
import fi.espoo.evaka.shared.config.testFeatureConfig
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class CitizenPushSenderIntegrationTest : PureJdbiTest(resetDbBeforeEach = true) {
    private val now = HelsinkiDateTime.of(LocalDate.of(2026, 4, 14), LocalTime.of(12, 0))

    @Test
    fun `getCitizenPushRecipients returns reply-all account ids excluding self`() {
        val employee = DevEmployee(firstName = "Emp", lastName = "Loyee")
        val citizenA = DevPerson(firstName = "Citizen", lastName = "A")
        val citizenB = DevPerson(firstName = "Citizen", lastName = "B")

        val (messageId, employeeAccount, citizenAAccount, citizenBAccount) =
            db.transaction { tx ->
                tx.insert(employee)
                tx.insert(citizenA, DevPersonType.ADULT)
                tx.insert(citizenB, DevPersonType.ADULT)
                val employeeAccount = tx.upsertEmployeeMessageAccount(employee.id)
                val citizenAAccount = tx.getCitizenMessageAccount(citizenA.id)
                val citizenBAccount = tx.getCitizenMessageAccount(citizenB.id)

                val recipientIds = setOf(citizenAAccount, citizenBAccount)
                val contentId = tx.insertMessageContent("Hello", employeeAccount)
                val threadId =
                    tx.insertThread(
                        MessageType.MESSAGE,
                        "Hello thread",
                        urgent = false,
                        sensitive = false,
                        isCopy = false,
                    )
                val messageId =
                    tx.insertMessage(
                        now = now,
                        contentId = contentId,
                        threadId = threadId,
                        sender = employeeAccount,
                        sentAt = now,
                        recipientNames =
                            tx.getAccountNames(
                                recipientIds,
                                testFeatureConfig.serviceWorkerMessageAccountName,
                                testFeatureConfig.financeMessageAccountName,
                            ),
                        municipalAccountName = "Espoo",
                        serviceWorkerAccountName = "Espoon palveluohjaus",
                        financeAccountName = "Espoon asiakasmaksut",
                    )
                tx.insertRecipients(listOf(messageId to recipientIds))
                Tuple4(messageId, employeeAccount, citizenAAccount, citizenBAccount)
            }

        val rows = db.read { it.getCitizenPushRecipients(listOf(messageId)) }

        assertEquals(2, rows.size)
        val rowA = rows.single { it.personId == citizenA.id }
        val rowB = rows.single { it.personId == citizenB.id }
        assertEquals(
            setOf<MessageAccountId>(employeeAccount, citizenBAccount),
            rowA.replyRecipientAccountIds.toSet(),
        )
        assertEquals(
            setOf<MessageAccountId>(employeeAccount, citizenAAccount),
            rowB.replyRecipientAccountIds.toSet(),
        )
    }

    @Test
    fun `getCitizenPushRecipients returns empty reply-all list when thread has only the recipient`() {
        // Defensive edge case: thread in which the only participant is the citizen themself.
        // The schema allows it when the citizen's own account is both sender and the sole
        // recipient,
        // so we construct it directly via the low-level inserters. The reply-all set, after
        // removing
        // the recipient's own account, should be empty.
        val citizen = DevPerson(firstName = "Lone", lastName = "Citizen")

        val messageId =
            db.transaction { tx ->
                tx.insert(citizen, DevPersonType.ADULT)
                val citizenAccount = tx.getCitizenMessageAccount(citizen.id)
                val contentId = tx.insertMessageContent("Alone", citizenAccount)
                val threadId =
                    tx.insertThread(
                        MessageType.MESSAGE,
                        "Alone thread",
                        urgent = false,
                        sensitive = false,
                        isCopy = false,
                    )
                val messageId =
                    tx.insertMessage(
                        now = now,
                        contentId = contentId,
                        threadId = threadId,
                        sender = citizenAccount,
                        sentAt = now,
                        recipientNames =
                            tx.getAccountNames(
                                setOf(citizenAccount),
                                testFeatureConfig.serviceWorkerMessageAccountName,
                                testFeatureConfig.financeMessageAccountName,
                            ),
                        municipalAccountName = "Espoo",
                        serviceWorkerAccountName = "Espoon palveluohjaus",
                        financeAccountName = "Espoon asiakasmaksut",
                    )
                tx.insertRecipients(listOf(messageId to setOf(citizenAccount)))
                messageId
            }

        val rows = db.read { it.getCitizenPushRecipients(listOf(messageId)) }

        assertEquals(1, rows.size)
        assertEquals(citizen.id, rows.single().personId)
        assertEquals(emptySet<MessageAccountId>(), rows.single().replyRecipientAccountIds.toSet())
    }

    @Test
    fun `getCitizenPushRecipients excludes draft messages from reply-all set`() {
        val employee = DevEmployee(firstName = "Emp", lastName = "Loyee")
        val citizenA = DevPerson(firstName = "Citizen", lastName = "A")
        val citizenB = DevPerson(firstName = "Citizen", lastName = "B")
        val citizenC = DevPerson(firstName = "Citizen", lastName = "C") // draft author

        data class DraftTestFixture(
            val realMessageId: MessageId,
            val employeeAccount: MessageAccountId,
            val citizenBAccount: MessageAccountId,
        )

        val fixture =
            db.transaction { tx ->
                tx.insert(employee)
                tx.insert(citizenA, DevPersonType.ADULT)
                tx.insert(citizenB, DevPersonType.ADULT)
                tx.insert(citizenC, DevPersonType.ADULT)
                val employeeAccount = tx.upsertEmployeeMessageAccount(employee.id)
                val citizenAAccount = tx.getCitizenMessageAccount(citizenA.id)
                val citizenBAccount = tx.getCitizenMessageAccount(citizenB.id)
                val citizenCAccount = tx.getCitizenMessageAccount(citizenC.id)

                val recipientIds = setOf(citizenAAccount, citizenBAccount)
                val contentId = tx.insertMessageContent("Hello", employeeAccount)
                val threadId =
                    tx.insertThread(
                        MessageType.MESSAGE,
                        "Draft-leak test thread",
                        urgent = false,
                        sensitive = false,
                        isCopy = false,
                    )
                // Real sent message from employee to A and B
                val realMessageId =
                    tx.insertMessage(
                        now = now,
                        contentId = contentId,
                        threadId = threadId,
                        sender = employeeAccount,
                        sentAt = now,
                        recipientNames =
                            tx.getAccountNames(
                                recipientIds,
                                testFeatureConfig.serviceWorkerMessageAccountName,
                                testFeatureConfig.financeMessageAccountName,
                            ),
                        municipalAccountName = "Espoo",
                        serviceWorkerAccountName = "Espoon palveluohjaus",
                        financeAccountName = "Espoon asiakasmaksut",
                    )
                tx.insertRecipients(listOf(realMessageId to recipientIds))

                // Draft reply from citizen C — inserted with sentAt = now, then nullified
                val draftContentId = tx.insertMessageContent("Draft reply", citizenCAccount)
                val draftMessageId =
                    tx.insertMessage(
                        now = now,
                        contentId = draftContentId,
                        threadId = threadId,
                        sender = citizenCAccount,
                        sentAt = now,
                        recipientNames =
                            tx.getAccountNames(
                                setOf(employeeAccount),
                                testFeatureConfig.serviceWorkerMessageAccountName,
                                testFeatureConfig.financeMessageAccountName,
                            ),
                        municipalAccountName = "Espoo",
                        serviceWorkerAccountName = "Espoon palveluohjaus",
                        financeAccountName = "Espoon asiakasmaksut",
                    )
                tx.insertRecipients(listOf(draftMessageId to setOf(employeeAccount)))
                // Retroactively nullify sent_at to simulate a draft (never actually sent)
                tx.createUpdate {
                        sql("UPDATE message SET sent_at = NULL WHERE id = ${bind(draftMessageId)}")
                    }
                    .execute()

                DraftTestFixture(realMessageId, employeeAccount, citizenBAccount)
            }

        val rows = db.read { it.getCitizenPushRecipients(listOf(fixture.realMessageId)) }

        assertEquals(2, rows.size)
        val rowA = rows.single { it.personId == citizenA.id }
        // Citizen C must NOT appear — C's only participation is via a draft (sent_at IS NULL)
        assertEquals(
            setOf<MessageAccountId>(fixture.employeeAccount, fixture.citizenBAccount),
            rowA.replyRecipientAccountIds.toSet(),
        )
    }

    private data class Tuple4(
        val messageId: MessageId,
        val employeeAccount: MessageAccountId,
        val citizenAAccount: MessageAccountId,
        val citizenBAccount: MessageAccountId,
    )
}
