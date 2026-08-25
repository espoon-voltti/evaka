// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.dataremoval

import evaka.core.DataRemovalEnv
import evaka.core.FullApplicationTest
import evaka.core.application.ApplicationType
import evaka.core.application.notes.createApplicationNote
import evaka.core.application.persistence.daycare.Adult
import evaka.core.application.persistence.daycare.Apply
import evaka.core.application.persistence.daycare.Child as ApplicationFormChild
import evaka.core.application.persistence.daycare.DaycareFormV0
import evaka.core.attachment.AttachmentParent
import evaka.core.attachment.insertAttachment
import evaka.core.messaging.MessageController
import evaka.core.messaging.MessageRecipient
import evaka.core.messaging.MessageType
import evaka.core.messaging.ReplyToMessageBody
import evaka.core.messaging.UpdatableDraftContent
import evaka.core.messaging.createDaycareGroupMessageAccount
import evaka.core.messaging.createMunicipalMessageAccount
import evaka.core.messaging.deleteMessageThreadsOfExpiredChildren
import evaka.core.messaging.getCitizenMessageAccount
import evaka.core.messaging.upsertEmployeeMessageAccount
import evaka.core.pis.service.insertGuardian
import evaka.core.placement.PlacementController
import evaka.core.shared.ApplicationId
import evaka.core.shared.AttachmentId
import evaka.core.shared.ChildId
import evaka.core.shared.MessageAccountId
import evaka.core.shared.MessageContentId
import evaka.core.shared.MessageDraftId
import evaka.core.shared.MessageThreadId
import evaka.core.shared.PlacementId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.auth.UserRole
import evaka.core.shared.auth.insertDaycareAclRow
import evaka.core.shared.db.QuerySql
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevDaycareGroup
import evaka.core.shared.dev.DevDaycareGroupPlacement
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.DevPlacement
import evaka.core.shared.dev.insert
import evaka.core.shared.dev.insertTestApplication
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import evaka.core.shared.security.PilotFeature
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.util.ReflectionTestUtils

private data class SentMessage(
    val contentId: MessageContentId,
    val threadIds: List<MessageThreadId>,
    val attachmentIds: Set<AttachmentId>,
)

private data class Sender(val user: AuthenticatedUser.Employee, val account: MessageAccountId)

class MessageDataRemovalIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var dataRemovalService: DataRemovalService
    @Autowired private lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>
    @Autowired private lateinit var messageController: MessageController
    @Autowired private lateinit var placementController: PlacementController

    private val today = LocalDate.of(2026, 5, 7)
    private val now = HelsinkiDateTime.of(today, LocalTime.of(2, 0))
    private val clock = MockEvakaClock(now)

    private val bulletinExpiresBefore = now.minusYears(5)
    private val bulletinRecipientExpireDate = today.minusYears(5)
    private val draftExpiresBefore = now.minusYears(1)
    private val applicationExpireDate = today.minusYears(10)

    // A placement whose child left care over five years ago
    private val expiredPlacementPeriod =
        FiniteDateRange(today.minusYears(6), bulletinRecipientExpireDate.minusDays(1))
    // A placement that is still going on
    private val ongoingPlacementPeriod = FiniteDateRange(today.minusYears(6), today.plusYears(1))
    private val sendTimeOverFiveYearsAgo =
        HelsinkiDateTime.of(expiredPlacementPeriod.start.plusDays(1), LocalTime.of(12, 0))
    private val sendTimeWithinFiveYears =
        HelsinkiDateTime.of(bulletinRecipientExpireDate.plusDays(1), LocalTime.of(12, 0))

    // Owns the municipal message account and corrects placements
    private val admin = DevEmployee(roles = setOf(UserRole.ADMIN))
    // Sends the bulletins of their own unit
    private val unitStaff = DevEmployee()
    private val careArea = DevCareArea()
    private val daycare =
        DevDaycare(areaId = careArea.id, enabledPilotFeatures = setOf(PilotFeature.MESSAGING))
    private val daycareGroup = DevDaycareGroup(daycareId = daycare.id)
    private val child = DevPerson()
    private val guardian = DevPerson()

    private lateinit var staffSender: Sender
    private lateinit var municipalSender: Sender
    private lateinit var guardianAccount: MessageAccountId

    @BeforeEach
    fun setup() {
        db.transaction { tx ->
            tx.insert(admin)
            tx.insert(unitStaff)
            tx.insert(careArea)
            tx.insert(daycare)
            tx.insert(daycareGroup)
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(guardian, DevPersonType.ADULT)
            tx.insertGuardian(guardian.id, child.id)
            // A personal account reaches a child only through an ACL row in the child's unit.
            // The role is STAFF rather than UNIT_SUPERVISOR, because supervisors receive a copy
            // of every bulletin sent to their unit, and only the copy tests want one.
            tx.insertDaycareAclRow(daycare.id, unitStaff.id, UserRole.STAFF)
            staffSender = Sender(unitStaff.user, tx.upsertEmployeeMessageAccount(unitStaff.id))
            municipalSender = Sender(admin.user, tx.createMunicipalMessageAccount())
            guardianAccount = tx.getCitizenMessageAccount(guardian.id)
        }
    }

    private fun insertGroupPlacement(childId: ChildId, period: FiniteDateRange): PlacementId =
        db.transaction { tx ->
            val placementId =
                tx.insert(
                    DevPlacement(
                        childId = childId,
                        unitId = daycare.id,
                        startDate = period.start,
                        endDate = period.end,
                    )
                )
            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placementId,
                    daycareGroupId = daycareGroup.id,
                    startDate = period.start,
                    endDate = period.end,
                )
            )
            placementId
        }

    private fun insertSibling(period: FiniteDateRange): ChildId {
        val sibling = DevPerson()
        db.transaction { tx ->
            tx.insert(sibling, DevPersonType.CHILD)
            tx.insertGuardian(guardian.id, sibling.id)
        }
        insertGroupPlacement(sibling.id, period)
        return sibling.id
    }

    private fun insertChildOfAnotherGuardian(period: FiniteDateRange): ChildId {
        val otherChild = DevPerson()
        val otherGuardian = DevPerson()
        db.transaction { tx ->
            tx.insert(otherChild, DevPersonType.CHILD)
            tx.insert(otherGuardian, DevPersonType.ADULT)
            tx.insertGuardian(otherGuardian.id, otherChild.id)
        }
        insertGroupPlacement(otherChild.id, period)
        return otherChild.id
    }

    private fun sendMessage(
        sentAt: HelsinkiDateTime,
        recipients: List<MessageRecipient> = listOf(MessageRecipient.Child(child.id)),
        sender: Sender = staffSender,
        type: MessageType = MessageType.BULLETIN,
        attachmentCount: Int = 0,
    ): SentMessage {
        val sendClock = MockEvakaClock(sentAt)
        val draftId =
            messageController.initDraftMessage(
                dbInstance(),
                sender.user,
                sendClock,
                sender.account,
            )
        val attachmentIds =
            (1..attachmentCount).map { insertDraftAttachment(draftId, sentAt, sender.user) }.toSet()
        val contentId =
            messageController
                .createMessage(
                    dbInstance(),
                    sender.user,
                    sendClock,
                    sender.account,
                    null,
                    MessageController.PostMessageBody(
                        title = "title",
                        content = "content",
                        type = type,
                        urgent = false,
                        sensitive = false,
                        recipients = recipients.toSet(),
                        recipientNames = listOf("Recipient"),
                        attachmentIds = attachmentIds,
                        draftId = draftId,
                    ),
                )
                .createdId ?: error("Message had no recipients")
        runSendingJobs()
        return SentMessage(contentId, ageThreadsTo(contentId, sentAt), attachmentIds)
    }

    private fun replyToThread(
        threadId: MessageThreadId,
        sentAt: HelsinkiDateTime,
        sender: Sender = staffSender,
        recipients: Set<MessageAccountId> = setOf(guardianAccount),
    ) {
        messageController.replyToThread(
            dbInstance(),
            sender.user,
            MockEvakaClock(sentAt),
            sender.account,
            threadId,
            ReplyToMessageBody(content = "reply", recipientAccountIds = recipients),
        )
        runSendingJobs()
    }

    private fun runSendingJobs() {
        asyncJobRunner.runPendingJobsSync(clock)
    }

    // message_thread.created comes from the column default, which no production code and
    // therefore no mocked clock can affect
    private fun ageThreadsTo(
        contentId: MessageContentId,
        sentAt: HelsinkiDateTime,
    ): List<MessageThreadId> = db.transaction { tx ->
        tx.createUpdate {
                sql(
                    """
UPDATE message_thread
SET created = ${bind(sentAt)}
WHERE id = ANY(SELECT thread_id FROM message WHERE content_id = ${bind(contentId)})
RETURNING id
"""
                )
            }
            .executeAndReturnGeneratedKeys()
            .toList<MessageThreadId>()
    }

    private fun createDraft(
        createdAt: HelsinkiDateTime,
        type: MessageType = MessageType.MESSAGE,
        modifiedAt: HelsinkiDateTime = createdAt,
    ): MessageDraftId {
        val draftId =
            messageController.initDraftMessage(
                dbInstance(),
                staffSender.user,
                MockEvakaClock(createdAt),
                staffSender.account,
            )
        messageController.updateDraftMessage(
            dbInstance(),
            staffSender.user,
            MockEvakaClock(modifiedAt),
            staffSender.account,
            draftId,
            UpdatableDraftContent(
                type = type,
                title = "title",
                content = "content",
                urgent = false,
                sensitive = false,
                recipients = emptySet(),
                recipientNames = emptyList(),
            ),
        )
        // message_draft.created_at comes from the column default, which no production code and
        // therefore no mocked clock can affect
        db.transaction { tx ->
            tx.execute {
                sql(
                    "UPDATE message_draft SET created_at = ${bind(createdAt)} WHERE id = ${bind(draftId)}"
                )
            }
        }
        return draftId
    }

    private fun insertDraftAttachment(
        draftId: MessageDraftId,
        uploadedAt: HelsinkiDateTime,
        uploadedBy: AuthenticatedUser.Employee = staffSender.user,
    ): AttachmentId = db.transaction { tx ->
        tx.insertAttachment(
            uploadedBy,
            uploadedAt,
            "attachment.pdf",
            "application/pdf",
            AttachmentParent.MessageDraft(draftId),
            type = null,
        )
    }

    private fun createGroupMessageAccount(): MessageAccountId = db.transaction { tx ->
        tx.createDaycareGroupMessageAccount(daycareGroup.id)
    }

    private fun staffCopyThreadIds(): List<MessageThreadId> = db.read { tx ->
        tx.createQuery { sql("SELECT id FROM message_thread WHERE is_copy") }
            .toList<MessageThreadId>()
    }

    private fun setThreadApplication(threadId: MessageThreadId, applicationId: ApplicationId) {
        db.transaction { tx ->
            tx.execute {
                sql(
                    "UPDATE message_thread SET application_id = ${bind(applicationId)} WHERE id = ${bind(threadId)}"
                )
            }
        }
    }

    private fun insertApplicationNote(
        applicationId: ApplicationId,
        contentId: MessageContentId,
    ) {
        db.transaction { tx ->
            tx.createApplicationNote(
                now = now,
                applicationId = applicationId,
                content = "content",
                createdBy = unitStaff.evakaUserId,
                messageContentId = contentId,
            )
        }
    }

    private fun linkThreadToApplication(
        threadId: MessageThreadId,
        applicationId: ApplicationId,
        contentId: MessageContentId,
    ) {
        setThreadApplication(threadId, applicationId)
        insertApplicationNote(applicationId, contentId)
    }

    private fun deleteExpiredBulletinThreads(limit: Int = 100) =
        dataRemovalService.deleteExpiredBulletinThreads(
            db,
            now,
            recipientExpireDate = bulletinRecipientExpireDate,
            expiresBefore = bulletinExpiresBefore,
            limit = limit,
        )

    private fun deleteMessageThreadsOfExpiredChildren(
        expiredChildIds: List<ChildId>,
        limit: Int = 100,
    ) =
        dataRemovalService.deleteMessageThreadsOfExpiredChildren(
            db,
            now,
            expiredChildIdsQuery = expiredChildIdsQuery(expiredChildIds),
            limit = limit,
        )

    private fun expiredChildIdsQuery(childIds: List<ChildId>) = QuerySql {
        sql("SELECT id FROM child WHERE id = ANY(${bind(childIds)})")
    }

    private fun deleteExpiredMessageDrafts(limit: Int = 100) =
        dataRemovalService.deleteExpiredMessageDrafts(
            db,
            now,
            expiresBefore = draftExpiresBefore,
            limit = limit,
        )

    @Test
    fun `deleteExpiredBulletinThreads deletes a bulletin thread with its messages, recipients, participants, children and content`() {
        insertGroupPlacement(child.id, expiredPlacementPeriod)
        sendMessage(sentAt = sendTimeOverFiveYearsAgo)

        deleteExpiredBulletinThreads()

        assertEquals(0, rowCount("message_thread"))
        assertEquals(0, rowCount("message"))
        assertEquals(0, rowCount("message_recipients"))
        assertEquals(0, rowCount("message_thread_participant"))
        assertEquals(0, rowCount("message_thread_children"))
        assertEquals(0, rowCount("message_content"))
    }

    @Test
    fun `deleteExpiredBulletinThreads keeps a bulletin whose child left care exactly five years ago`() {
        insertGroupPlacement(
            child.id,
            FiniteDateRange(expiredPlacementPeriod.start, bulletinRecipientExpireDate),
        )
        sendMessage(sentAt = sendTimeOverFiveYearsAgo)

        deleteExpiredBulletinThreads()

        assertEquals(1, rowCount("message_thread"))
        assertEquals(1, rowCount("message_content"))
    }

    @Test
    fun `deleteExpiredBulletinThreads takes the latest placement of its children as the expiry anchor`() {
        // Both children have left care, but the later of the two placements ended on the expiry
        // date rather than before it
        val leftEarlier = DevPerson()
        db.transaction { tx ->
            tx.insert(leftEarlier, DevPersonType.CHILD)
            tx.insertGuardian(guardian.id, leftEarlier.id)
        }
        insertGroupPlacement(
            leftEarlier.id,
            FiniteDateRange(
                expiredPlacementPeriod.start,
                expiredPlacementPeriod.start.plusMonths(6),
            ),
        )
        insertGroupPlacement(
            child.id,
            FiniteDateRange(expiredPlacementPeriod.start, bulletinRecipientExpireDate),
        )
        sendMessage(
            sentAt = sendTimeOverFiveYearsAgo,
            recipients =
                listOf(
                    MessageRecipient.Child(leftEarlier.id),
                    MessageRecipient.Child(child.id),
                ),
        )
        assertEquals(1, rowCount("message_thread"))
        assertEquals(2, rowCount("message_thread_children"), "both children are on one thread")

        deleteExpiredBulletinThreads()

        assertEquals(1, rowCount("message_thread"))
        assertEquals(1, rowCount("message_content"))
    }

    @Test
    fun `deleteExpiredBulletinThreads keeps an age-expired bulletin whose child is still in care`() {
        // The age limit only decides when no placement is found, so it can never delete a
        // bulletin whose children left care less than the recipient retention period ago
        insertGroupPlacement(child.id, ongoingPlacementPeriod)
        sendMessage(sentAt = sendTimeOverFiveYearsAgo)

        deleteExpiredBulletinThreads()

        assertEquals(1, rowCount("message_thread"))
        assertEquals(1, rowCount("message_content"))
    }

    @Test
    fun `deleteExpiredBulletinThreads keeps a bulletin whose child has no placements left until it is five years old`() {
        // Without placements there is nothing to measure the recipient retention from
        val placementId = insertGroupPlacement(child.id, ongoingPlacementPeriod)
        val withinAgeLimit = sendMessage(sentAt = sendTimeWithinFiveYears)
        sendMessage(sentAt = sendTimeOverFiveYearsAgo)
        placementController.deletePlacement(dbInstance(), admin.user, clock, placementId)

        deleteExpiredBulletinThreads()

        assertEquals(withinAgeLimit.threadIds, survivingMessageThreadIds())
    }

    @Test
    fun `deleteExpiredBulletinThreads deletes a municipal bulletin five years after it was sent`() {
        insertGroupPlacement(child.id, ongoingPlacementPeriod)
        sendMessage(
            sentAt = sendTimeOverFiveYearsAgo,
            sender = municipalSender,
            recipients = listOf(MessageRecipient.Unit(daycare.id)),
        )
        assertEquals(
            0,
            rowCount("message_thread_children"),
            "a municipal bulletin records no children",
        )

        deleteExpiredBulletinThreads()

        assertEquals(0, rowCount("message_thread"))
        assertEquals(0, rowCount("message_content"))
    }

    @Test
    fun `deleteExpiredBulletinThreads keeps a municipal bulletin until it is five years old`() {
        insertGroupPlacement(child.id, ongoingPlacementPeriod)
        sendMessage(
            sentAt = sendTimeWithinFiveYears,
            sender = municipalSender,
            recipients = listOf(MessageRecipient.Unit(daycare.id)),
        )

        deleteExpiredBulletinThreads()

        assertEquals(1, rowCount("message_thread"))
        assertEquals(1, rowCount("message_content"))
    }

    @Test
    fun `deleteExpiredBulletinThreads deletes a bulletin with a recent follow-up whose child placement ended before the expiry date`() {
        // A follow-up must not restart the retention of a bulletin that is anchored to
        // placements, unlike one expiring by age
        insertGroupPlacement(child.id, expiredPlacementPeriod)
        val sent = sendMessage(sentAt = sendTimeOverFiveYearsAgo)
        replyToThread(sent.threadIds.single(), sentAt = now.minusYears(1))

        deleteExpiredBulletinThreads()

        assertEquals(0, rowCount("message_thread"))
        assertEquals(0, rowCount("message_content"))
    }

    @Test
    fun `deleteExpiredBulletinThreads deletes a staff copy only after the bulletin it copies`() {
        createGroupMessageAccount()
        insertGroupPlacement(child.id, expiredPlacementPeriod)
        val sent =
            sendMessage(
                sentAt = sendTimeOverFiveYearsAgo,
                recipients = listOf(MessageRecipient.Group(daycareGroup.id)),
                attachmentCount = 1,
            )
        val copyId = staffCopyThreadIds().single()

        deleteExpiredBulletinThreads()

        assertEquals(listOf(copyId), survivingMessageThreadIds())
        // The copy still uses the content, so neither it nor its attachment is removed yet
        assertEquals(1, rowCount("message_content"))
        assertTrue(scheduledAttachmentDeletionIds().isEmpty())

        deleteExpiredBulletinThreads()

        assertEquals(0, rowCount("message_thread"))
        assertEquals(0, rowCount("message_content"))
        assertEquals(
            sent.attachmentIds.map { it.toString() }.toSet(),
            scheduledAttachmentDeletionIds(),
        )
    }

    @Test
    fun `deleteExpiredBulletinThreads keeps a staff copy while the bulletin it copies is retained`() {
        createGroupMessageAccount()
        insertGroupPlacement(child.id, ongoingPlacementPeriod)
        sendMessage(
            sentAt = sendTimeOverFiveYearsAgo,
            recipients = listOf(MessageRecipient.Group(daycareGroup.id)),
        )

        deleteExpiredBulletinThreads()

        assertEquals(2, rowCount("message_thread"))
        assertEquals(1, rowCount("message_content"))
    }

    @Test
    fun `deleteExpiredBulletinThreads keeps a content shared by a thread that is still retained`() {
        val otherChild = DevPerson()
        val otherGuardian = DevPerson()
        db.transaction { tx ->
            tx.insert(otherChild, DevPersonType.CHILD)
            tx.insert(otherGuardian, DevPersonType.ADULT)
            tx.insertGuardian(otherGuardian.id, otherChild.id)
        }
        insertGroupPlacement(child.id, expiredPlacementPeriod)
        insertGroupPlacement(otherChild.id, ongoingPlacementPeriod)
        sendMessage(
            sentAt = sendTimeOverFiveYearsAgo,
            recipients =
                listOf(MessageRecipient.Child(child.id), MessageRecipient.Child(otherChild.id)),
            attachmentCount = 1,
        )

        deleteExpiredBulletinThreads()

        assertEquals(1, rowCount("message_thread"))
        assertEquals(1, rowCount("message_content"))
        assertTrue(scheduledAttachmentDeletionIds().isEmpty())
    }

    @Test
    fun `deleteExpiredBulletinThreads enqueues DeleteAttachment for each attachment of an expired bulletin`() {
        insertGroupPlacement(child.id, expiredPlacementPeriod)
        val sent = sendMessage(sentAt = sendTimeOverFiveYearsAgo, attachmentCount = 2)

        deleteExpiredBulletinThreads()

        assertEquals(
            sent.attachmentIds.map { it.toString() }.toSet(),
            scheduledAttachmentDeletionIds(),
        )
    }

    @Test
    fun `deleteExpiredBulletinThreads keeps only the thread whose own follow-up is recent`() {
        // A recent follow-up must protect its own thread and no other thread of the same batch.
        // Only a thread expiring by age can be postponed by a follow-up, so the placement the
        // threads were anchored to is deleted.
        val placementId = insertGroupPlacement(child.id, ongoingPlacementPeriod)
        sendMessage(sentAt = sendTimeOverFiveYearsAgo)
        val withFollowUp = sendMessage(sentAt = sendTimeOverFiveYearsAgo)
        replyToThread(withFollowUp.threadIds.single(), sentAt = now.minusYears(1))
        placementController.deletePlacement(dbInstance(), admin.user, clock, placementId)

        deleteExpiredBulletinThreads()

        assertEquals(withFollowUp.threadIds, survivingMessageThreadIds())
        // The retained thread keeps both its messages and contents; the deleted one leaves none
        assertEquals(2, rowCount("message"))
        assertEquals(2, rowCount("message_content"))
    }

    @Test
    fun `deleteExpiredBulletinThreads deletes every content of an expired thread whose follow-up has also expired`() {
        val placementId = insertGroupPlacement(child.id, ongoingPlacementPeriod)
        val sent = sendMessage(sentAt = sendTimeOverFiveYearsAgo, attachmentCount = 1)
        replyToThread(sent.threadIds.single(), sentAt = sendTimeOverFiveYearsAgo.plusDays(1))
        placementController.deletePlacement(dbInstance(), admin.user, clock, placementId)

        deleteExpiredBulletinThreads()

        assertEquals(0, rowCount("message_thread"))
        assertEquals(0, rowCount("message"))
        assertEquals(0, rowCount("message_content"))
        assertEquals(
            sent.attachmentIds.map { it.toString() }.toSet(),
            scheduledAttachmentDeletionIds(),
        )
    }

    @Test
    fun `deleteExpiredBulletinThreads keeps an equally old regular message thread`() {
        insertGroupPlacement(child.id, expiredPlacementPeriod)
        sendMessage(sentAt = sendTimeOverFiveYearsAgo, type = MessageType.MESSAGE)

        deleteExpiredBulletinThreads()

        assertEquals(1, rowCount("message_thread"))
        assertEquals(1, rowCount("message"))
        assertEquals(1, rowCount("message_content"))
    }

    @Test
    fun `deleteExpiredBulletinThreads keeps an expired bulletin thread that is linked to an application`() {
        // Bulletins linked to an application exist only because of an earlier bug, so the link
        // can only be created directly. The note that such a message also creates can be
        // deleted by an employee, so the link alone has to keep the thread.
        insertGroupPlacement(child.id, expiredPlacementPeriod)
        val sent = sendMessage(sentAt = sendTimeOverFiveYearsAgo, attachmentCount = 1)
        val applicationId = insertApplication()
        setThreadApplication(sent.threadIds.single(), applicationId)

        deleteExpiredBulletinThreads()

        assertEquals(0, countNonNull("application_note", "message_content_id"))
        assertEquals(1, rowCount("message_thread"))
        assertEquals(1, rowCount("message"))
        assertEquals(1, rowCount("message_content"))
        assertEquals(1, countNonNull("attachment", "message_content_id"))
        assertTrue(scheduledAttachmentDeletionIds().isEmpty())
    }

    @Test
    fun `deleteExpiredBulletinThreads keeps a thread whose content an application note references`() {
        // Bulletins linked to an application exist only because of an earlier bug
        insertGroupPlacement(child.id, expiredPlacementPeriod)
        val sent = sendMessage(sentAt = sendTimeOverFiveYearsAgo, attachmentCount = 1)
        insertApplicationNote(insertApplication(), sent.contentId)

        deleteExpiredBulletinThreads()

        assertEquals(1, rowCount("message_thread"))
        assertEquals(1, rowCount("message"))
        assertEquals(1, rowCount("message_content"))
        assertEquals(1, countNonNull("application_note", "message_content_id"))
        assertTrue(scheduledAttachmentDeletionIds().isEmpty())
    }

    @Test
    fun `deleteExpiredBulletinThreads doesn't remove more threads than the limit`() {
        insertGroupPlacement(child.id, ongoingPlacementPeriod)
        repeat(3) {
            sendMessage(
                sentAt = sendTimeOverFiveYearsAgo,
                sender = municipalSender,
                recipients = listOf(MessageRecipient.Unit(daycare.id)),
            )
        }

        deleteExpiredBulletinThreads(limit = 2)

        assertEquals(1, rowCount("message_thread"))
        assertEquals(1, rowCount("message_content"))
    }

    @Test
    fun `deleteMessageThreadsOfExpiredChildren deletes a thread with its messages, recipients, participants, children and content`() {
        insertGroupPlacement(child.id, expiredPlacementPeriod)
        val sent =
            sendMessage(
                sentAt = sendTimeOverFiveYearsAgo,
                type = MessageType.MESSAGE,
                attachmentCount = 1,
            )
        replyToThread(sent.threadIds.single(), sentAt = now.minusYears(1))

        deleteMessageThreadsOfExpiredChildren(listOf(child.id))

        assertEquals(0, rowCount("message_thread"))
        assertEquals(0, rowCount("message"))
        assertEquals(0, rowCount("message_recipients"))
        assertEquals(0, rowCount("message_thread_participant"))
        assertEquals(0, rowCount("message_thread_children"))
        assertEquals(0, rowCount("message_content"))
        assertEquals(
            sent.attachmentIds.map { it.toString() }.toSet(),
            scheduledAttachmentDeletionIds(),
        )
    }

    @Test
    fun `deleteMessageThreadsOfExpiredChildren keeps the threads of a child who has not expired`() {
        insertGroupPlacement(child.id, expiredPlacementPeriod)
        val retained = sendMessage(sentAt = sendTimeOverFiveYearsAgo, type = MessageType.MESSAGE)
        val otherChild = insertChildOfAnotherGuardian(expiredPlacementPeriod)
        sendMessage(
            sentAt = sendTimeOverFiveYearsAgo,
            recipients = listOf(MessageRecipient.Child(otherChild)),
            type = MessageType.MESSAGE,
        )

        deleteMessageThreadsOfExpiredChildren(listOf(otherChild))

        assertEquals(retained.threadIds, survivingMessageThreadIds())
        assertEquals(1, rowCount("message_content"))
    }

    @Test
    fun `deleteMessageThreadsOfExpiredChildren deletes nothing while no child has expired`() {
        insertGroupPlacement(child.id, expiredPlacementPeriod)
        sendMessage(sentAt = sendTimeOverFiveYearsAgo, type = MessageType.MESSAGE)

        deleteMessageThreadsOfExpiredChildren(emptyList())

        assertEquals(1, rowCount("message_thread"))
        assertEquals(1, rowCount("message_content"))
    }

    @Test
    fun `deleteMessageThreadsOfExpiredChildren keeps a thread until its last child has expired`() {
        insertGroupPlacement(child.id, expiredPlacementPeriod)
        val sibling = insertSibling(expiredPlacementPeriod)
        sendMessage(
            sentAt = sendTimeOverFiveYearsAgo,
            recipients = listOf(MessageRecipient.Child(child.id), MessageRecipient.Child(sibling)),
            type = MessageType.MESSAGE,
        )
        assertEquals(1, rowCount("message_thread"))
        assertEquals(2, rowCount("message_thread_children"), "both children are on one thread")

        deleteMessageThreadsOfExpiredChildren(listOf(child.id))

        assertEquals(1, rowCount("message_thread"))
        assertEquals(1, rowCount("message_content"))

        deleteMessageThreadsOfExpiredChildren(listOf(child.id, sibling))

        assertEquals(0, rowCount("message_thread"))
        assertEquals(0, rowCount("message_content"))
    }

    @Test
    fun `deleteMessageThreadsOfExpiredChildren returns the children of a deleted thread`() {
        // The children of a thread must be read before the delete cascades them away, or the
        // audit trail of the removal loses them
        insertGroupPlacement(child.id, expiredPlacementPeriod)
        val sibling = insertSibling(expiredPlacementPeriod)
        val sent =
            sendMessage(
                sentAt = sendTimeOverFiveYearsAgo,
                recipients =
                    listOf(MessageRecipient.Child(child.id), MessageRecipient.Child(sibling)),
                type = MessageType.MESSAGE,
            )

        val batch = db.transaction { tx ->
            tx.deleteMessageThreadsOfExpiredChildren(
                expiredChildIdsQuery(listOf(child.id, sibling)),
                limit = 100,
            )
        }

        assertEquals(sent.threadIds, batch.threads.map { it.threadId })
        assertEquals(setOf(child.id, sibling), batch.threads.single().childIds.toSet())
    }

    @Test
    fun `deleteMessageThreadsOfExpiredChildren keeps a content shared by the thread of a child who has not expired`() {
        insertGroupPlacement(child.id, expiredPlacementPeriod)
        val otherChild = insertChildOfAnotherGuardian(expiredPlacementPeriod)
        val sent =
            sendMessage(
                sentAt = sendTimeOverFiveYearsAgo,
                recipients =
                    listOf(MessageRecipient.Child(child.id), MessageRecipient.Child(otherChild)),
                type = MessageType.MESSAGE,
                attachmentCount = 1,
            )
        assertEquals(2, sent.threadIds.size, "the children have guardians of their own")

        deleteMessageThreadsOfExpiredChildren(listOf(child.id))

        assertEquals(1, rowCount("message_thread"))
        assertEquals(1, rowCount("message_content"))
        assertTrue(scheduledAttachmentDeletionIds().isEmpty())
    }

    @Test
    fun `deleteMessageThreadsOfExpiredChildren deletes a bulletin of an expired child however recent it is`() {
        insertGroupPlacement(child.id, ongoingPlacementPeriod)
        sendMessage(sentAt = now.minusDays(1))

        deleteMessageThreadsOfExpiredChildren(listOf(child.id))

        assertEquals(0, rowCount("message_thread"))
        assertEquals(0, rowCount("message_content"))
    }

    @Test
    fun `deleteMessageThreadsOfExpiredChildren leaves a staff copy for the bulletin removal to delete`() {
        // A copy records no children of its own, so it outlives the bulletin of an expired child
        createGroupMessageAccount()
        insertGroupPlacement(child.id, ongoingPlacementPeriod)
        val sent =
            sendMessage(
                sentAt = sendTimeWithinFiveYears,
                recipients = listOf(MessageRecipient.Group(daycareGroup.id)),
                attachmentCount = 1,
            )
        val copyId = staffCopyThreadIds().single()

        deleteMessageThreadsOfExpiredChildren(listOf(child.id))

        assertEquals(listOf(copyId), survivingMessageThreadIds())
        assertEquals(1, rowCount("message_content"))

        deleteExpiredBulletinThreads()

        assertEquals(0, rowCount("message_thread"))
        assertEquals(0, rowCount("message_content"))
        assertEquals(
            sent.attachmentIds.map { it.toString() }.toSet(),
            scheduledAttachmentDeletionIds(),
        )
    }

    @Test
    fun `deleteMessageThreadsOfExpiredChildren keeps a thread that is linked to an application`() {
        insertGroupPlacement(child.id, expiredPlacementPeriod)
        val sent = sendMessage(sentAt = sendTimeOverFiveYearsAgo, type = MessageType.MESSAGE)
        setThreadApplication(sent.threadIds.single(), insertApplication())

        deleteMessageThreadsOfExpiredChildren(listOf(child.id))

        assertEquals(1, rowCount("message_thread"))
        assertEquals(1, rowCount("message_content"))
    }

    @Test
    fun `deleteMessageThreadsOfExpiredChildren keeps a thread whose content an application note references`() {
        insertGroupPlacement(child.id, expiredPlacementPeriod)
        val sent = sendMessage(sentAt = sendTimeOverFiveYearsAgo, type = MessageType.MESSAGE)
        insertApplicationNote(insertApplication(), sent.contentId)

        deleteMessageThreadsOfExpiredChildren(listOf(child.id))

        assertEquals(1, rowCount("message_thread"))
        assertEquals(1, rowCount("message_content"))
    }

    @Test
    fun `deleteMessageThreadsOfExpiredChildren doesn't remove more threads than the limit`() {
        insertGroupPlacement(child.id, expiredPlacementPeriod)
        repeat(3) { sendMessage(sentAt = sendTimeOverFiveYearsAgo, type = MessageType.MESSAGE) }

        deleteMessageThreadsOfExpiredChildren(listOf(child.id), limit = 2)

        assertEquals(1, rowCount("message_thread"))
        assertEquals(1, rowCount("message_content"))
    }

    @Test
    fun `deleteExpiredMessageDrafts deletes an expired draft and enqueues its attachment deletion`() {
        val draftId = createDraft(createdAt = draftExpiresBefore.minusDays(1))
        val attachmentId = insertDraftAttachment(draftId, now)

        deleteExpiredMessageDrafts()

        assertEquals(0, rowCount("message_draft"))
        assertEquals(setOf(attachmentId.toString()), scheduledAttachmentDeletionIds())
    }

    @Test
    fun `deleteExpiredMessageDrafts doesn't remove more drafts than the limit`() {
        repeat(3) { createDraft(createdAt = draftExpiresBefore.minusDays(1)) }

        deleteExpiredMessageDrafts(limit = 2)

        assertEquals(1, rowCount("message_draft"))
    }

    @Test
    fun `deleteExpiredMessageDrafts deletes an expired draft of every type`() {
        MessageType.entries.forEach {
            createDraft(createdAt = draftExpiresBefore.minusDays(1), type = it)
        }

        deleteExpiredMessageDrafts()

        assertEquals(0, rowCount("message_draft"))
    }

    @Test
    fun `deleteExpiredMessageDrafts deletes a draft created over a year ago even if it was modified today`() {
        createDraft(createdAt = draftExpiresBefore.minusDays(1), modifiedAt = now)

        deleteExpiredMessageDrafts()

        assertEquals(0, rowCount("message_draft"))
    }

    @Test
    fun `deleteExpiredMessageDrafts keeps a draft created less than a year ago`() {
        createDraft(createdAt = draftExpiresBefore)

        deleteExpiredMessageDrafts()

        assertEquals(1, rowCount("message_draft"))
    }

    @Test
    fun `deleteExpiredData removes a bulletin five years after its children left care`() {
        insertGroupPlacement(child.id, expiredPlacementPeriod)
        val sent = sendMessage(sentAt = sendTimeOverFiveYearsAgo, attachmentCount = 1)

        withLimit(1000) {
            dataRemovalService.deleteExpiredData(db, clock, AsyncJob.DeleteExpiredData)
        }

        assertEquals(0, rowCount("message_thread"))
        assertEquals(0, rowCount("message_content"))
        assertTrue(
            scheduledAttachmentDeletionIds().containsAll(sent.attachmentIds.map { it.toString() })
        )
    }

    @Test
    fun `deleteExpiredData removes expired bulletin threads and message drafts`() {
        insertGroupPlacement(child.id, expiredPlacementPeriod)
        val sent = sendMessage(sentAt = sendTimeOverFiveYearsAgo, attachmentCount = 1)
        val draftAttachment =
            insertDraftAttachment(createDraft(createdAt = now.minusYears(1).minusDays(1)), now)

        withLimit(1000) {
            dataRemovalService.deleteExpiredData(db, clock, AsyncJob.DeleteExpiredData)
        }

        assertEquals(0, rowCount("message_thread"))
        assertEquals(0, rowCount("message_content"))
        assertEquals(0, rowCount("message_draft"))
        assertEquals(
            (sent.attachmentIds + draftAttachment).map { it.toString() }.toSet(),
            scheduledAttachmentDeletionIds(),
        )
    }

    @Test
    fun `deleteExpiredData removes an expired bulletin thread once its application has been removed`() {
        // Bulletins linked to an application exist only because of an earlier bug
        insertGroupPlacement(child.id, expiredPlacementPeriod)
        val sent = sendMessage(sentAt = sendTimeOverFiveYearsAgo, attachmentCount = 1)
        linkThreadToApplication(
            sent.threadIds.single(),
            insertExpiredApplication(),
            sent.contentId,
        )

        withLimit(1000) {
            dataRemovalService.deleteExpiredData(db, clock, AsyncJob.DeleteExpiredData)
        }

        assertEquals(0, rowCount("application"))
        assertEquals(0, rowCount("application_note"))
        assertEquals(0, rowCount("message_thread"))
        assertEquals(0, rowCount("message"))
        assertEquals(0, rowCount("message_content"))
        assertTrue(
            scheduledAttachmentDeletionIds().containsAll(sent.attachmentIds.map { it.toString() })
        )
    }

    @Test
    fun `deleteExpiredData keeps a bulletin thread of exactly five years and a draft of exactly one year`() {
        insertGroupPlacement(child.id, ongoingPlacementPeriod)
        sendMessage(
            sentAt = now.minusYears(5),
            sender = municipalSender,
            recipients = listOf(MessageRecipient.Unit(daycare.id)),
        )
        createDraft(createdAt = now.minusYears(1))

        withLimit(1000) {
            dataRemovalService.deleteExpiredData(db, clock, AsyncJob.DeleteExpiredData)
        }

        assertEquals(1, rowCount("message_thread"))
        assertEquals(1, rowCount("message_draft"))
    }

    @Test
    fun `deleteExpiredData keeps a regular message thread of a child who left care over ten years ago`() {
        // Regular messages are retained as long as the data of their children, and the rule that
        // expires a child is not in use yet
        val leftCareTenYearsAgo = FiniteDateRange(today.minusYears(12), today.minusYears(11))
        insertGroupPlacement(child.id, leftCareTenYearsAgo)
        sendMessage(
            sentAt =
                HelsinkiDateTime.of(leftCareTenYearsAgo.start.plusDays(1), LocalTime.of(12, 0)),
            type = MessageType.MESSAGE,
        )

        withLimit(1000) {
            dataRemovalService.deleteExpiredData(db, clock, AsyncJob.DeleteExpiredData)
        }

        assertEquals(1, rowCount("message_thread"))
        assertEquals(1, rowCount("message_content"))
    }

    private fun insertApplication(childId: ChildId = child.id): ApplicationId =
        db.transaction { tx ->
            tx.insertTestApplication(
                type = ApplicationType.DAYCARE,
                guardianId = guardian.id,
                childId = childId,
                document =
                    DaycareFormV0(
                        type = ApplicationType.DAYCARE,
                        child = ApplicationFormChild(dateOfBirth = null),
                        guardian = Adult(),
                        apply = Apply(preferredUnits = listOf(daycare.id)),
                    ),
            )
        }

    // An application of a child who left care over ten years ago, which deleteExpiredData removes
    private fun insertExpiredApplication(): ApplicationId {
        val applicationChild = DevPerson()
        db.transaction { tx ->
            tx.insert(applicationChild, DevPersonType.CHILD)
            tx.insert(
                DevPlacement(
                    childId = applicationChild.id,
                    unitId = daycare.id,
                    startDate = applicationExpireDate.minusYears(1),
                    endDate = applicationExpireDate.minusDays(1),
                )
            )
        }
        return insertApplication(applicationChild.id)
    }

    private fun withLimit(limit: Int, block: () -> Unit) {
        ReflectionTestUtils.setField(
            dataRemovalService,
            "dataRemovalEnv",
            DataRemovalEnv(limit = limit),
        )
        block()
    }

    private fun rowCount(table: String): Int = db.read { tx ->
        tx.createQuery { sql("SELECT count(*) FROM $table") }.exactlyOne<Int>()
    }

    private fun countNonNull(table: String, column: String): Int = db.read { tx ->
        tx.createQuery { sql("SELECT count(*) FROM $table WHERE $column IS NOT NULL") }
            .exactlyOne<Int>()
    }

    private fun scheduledAttachmentDeletionIds(): Set<String> =
        db.read { tx ->
                tx.createQuery {
                        sql(
                            "SELECT payload::json->>'attachmentId' FROM async_job WHERE type = 'DeleteAttachment'"
                        )
                    }
                    .toList<String>()
            }
            .toSet()

    private fun survivingMessageThreadIds(): List<MessageThreadId> = db.read { tx ->
        tx.createQuery { sql("SELECT id FROM message_thread") }.toList<MessageThreadId>()
    }
}
