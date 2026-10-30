// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.webpush

import evaka.core.PureJdbiTest
import evaka.core.application.ApplicationStatus
import evaka.core.application.ApplicationType
import evaka.core.application.persistence.daycare.Adult
import evaka.core.application.persistence.daycare.Child
import evaka.core.application.persistence.daycare.DaycareFormV0
import evaka.core.attachment.AttachmentParent
import evaka.core.attachment.insertAttachment
import evaka.core.document.DocumentTemplateContent
import evaka.core.document.childdocument.DocumentContent
import evaka.core.document.childdocument.DocumentStatus
import evaka.core.messaging.MessageType
import evaka.core.messaging.getCitizenMessageAccount
import evaka.core.messaging.insertMessage
import evaka.core.messaging.insertMessageContent
import evaka.core.messaging.insertRecipients
import evaka.core.messaging.insertThread
import evaka.core.messaging.markThreadRead
import evaka.core.messaging.upsertEmployeeMessageAccount
import evaka.core.messaging.upsertRecipientThreadParticipants
import evaka.core.shared.MessageThreadId
import evaka.core.shared.PedagogicalDocumentId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.dev.DevChildDocument
import evaka.core.shared.dev.DevChildDocumentPublishedVersion
import evaka.core.shared.dev.DevDocumentTemplate
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevGuardian
import evaka.core.shared.dev.DevPedagogicalDocument
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.insert
import evaka.core.shared.dev.insertTestApplication
import evaka.core.shared.domain.DateRange
import evaka.core.shared.domain.HelsinkiDateTime
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CitizenAppBadgeTest : PureJdbiTest(resetDbBeforeEach = true) {
    private val today = LocalDate.of(2026, 1, 1)
    private val now = HelsinkiDateTime.of(today, LocalTime.of(12, 0))
    private val citizen = DevPerson(ssn = "010180-1232")
    private val child = DevPerson(ssn = "010120A123U")
    private val employee = DevEmployee()

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(citizen, DevPersonType.ADULT)
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(DevGuardian(guardianId = citizen.id, childId = child.id))
            tx.insert(employee)
        }
    }

    @Test
    fun `a citizen with nothing to attend to has no badge`() {
        assertEquals(0, badgeCount())
    }

    @Test
    fun `unread messages and decisions waiting for an answer are summed`() {
        sendMessage()
        sendMessage()
        db.transaction { tx ->
            tx.insertTestApplication(
                type = ApplicationType.DAYCARE,
                status = ApplicationStatus.WAITING_CONFIRMATION,
                confidential = true,
                guardianId = citizen.id,
                childId = child.id,
                document =
                    DaycareFormV0(
                        type = ApplicationType.DAYCARE,
                        child = Child(dateOfBirth = child.dateOfBirth),
                        guardian = Adult(),
                    ),
            )
        }

        assertEquals(3, badgeCount())
    }

    @Test
    fun `a read message is not counted`() {
        val read = sendMessage()
        sendMessage()
        db.transaction { tx ->
            tx.markThreadRead(now, tx.getCitizenMessageAccount(citizen.id), read)
        }

        assertEquals(1, badgeCount())
    }

    @Test
    fun `unread documents are counted once each`() {
        db.transaction { tx ->
            val pedagogicalDocument =
                tx.insert(
                    DevPedagogicalDocument(
                        id = PedagogicalDocumentId(UUID.randomUUID()),
                        childId = child.id,
                        description = "description",
                    )
                )
            repeat(2) {
                tx.insertAttachment(
                    AuthenticatedUser.SystemInternalUser,
                    now,
                    "file.pdf",
                    "application/pdf",
                    AttachmentParent.PedagogicalDocument(pedagogicalDocument),
                    type = null,
                )
            }
            val template =
                tx.insert(
                    DevDocumentTemplate(
                        validity = DateRange(today, today),
                        content = DocumentTemplateContent(sections = emptyList()),
                    )
                )
            tx.insert(
                DevChildDocument(
                    status = DocumentStatus.DRAFT,
                    childId = child.id,
                    templateId = template,
                    content = DocumentContent(answers = emptyList()),
                    modifiedAt = now,
                    modifiedBy = AuthenticatedUser.SystemInternalUser.evakaUserId,
                    contentLockedAt = now,
                    contentLockedBy = null,
                    publishedVersions =
                        listOf(
                            DevChildDocumentPublishedVersion(
                                versionNumber = 1,
                                createdAt = now,
                                createdBy = AuthenticatedUser.SystemInternalUser.evakaUserId,
                                publishedContent = DocumentContent(answers = emptyList()),
                            )
                        ),
                )
            )
        }

        assertEquals(2, badgeCount())
    }

    @Test
    fun `a push shows at least one even when nothing is counted`() {
        assertEquals(1, pushAppBadge(0))
        assertEquals(3, pushAppBadge(3))
    }

    private fun badgeCount() = db.read { it.getCitizenAppBadgeCount(citizen.id, today) }

    private fun sendMessage(): MessageThreadId = db.transaction { tx ->
        val sender = tx.upsertEmployeeMessageAccount(employee.id)
        val contentId = tx.insertMessageContent("content", sender)
        val threadId =
            tx.insertThread(
                MessageType.MESSAGE,
                "title",
                urgent = false,
                sensitive = false,
                isCopy = false,
            )
        val messageId =
            tx.insertMessage(
                now = now,
                contentId = contentId,
                threadId = threadId,
                sender = sender,
                sentAt = now,
                recipientNames = emptyList(),
                municipalAccountName = "",
                serviceWorkerAccountName = "",
                financeAccountName = "",
            )
        tx.insertRecipients(listOf(messageId to setOf(tx.getCitizenMessageAccount(citizen.id))))
        tx.upsertRecipientThreadParticipants(contentId, now)
        threadId
    }
}
