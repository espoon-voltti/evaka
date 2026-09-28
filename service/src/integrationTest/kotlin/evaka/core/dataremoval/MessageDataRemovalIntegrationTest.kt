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
import evaka.core.messaging.MessageService
import evaka.core.messaging.MessageType
import evaka.core.messaging.NewMessageStub
import evaka.core.messaging.ReplyToMessageBody
import evaka.core.messaging.UpdatableDraftContent
import evaka.core.messaging.createDaycareGroupMessageAccount
import evaka.core.messaging.createFinanceMessageAccount
import evaka.core.messaging.createMunicipalMessageAccount
import evaka.core.messaging.createServiceWorkerMessageAccount
import evaka.core.messaging.deleteExpiredRegularThreads
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
import evaka.core.shared.PersonId
import evaka.core.shared.PlacementId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.auth.UserRole
import evaka.core.shared.auth.insertDaycareAclRow
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevDaycareGroup
import evaka.core.shared.dev.DevDaycareGroupPlacement
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevFosterParent
import evaka.core.shared.dev.DevFridgeChild
import evaka.core.shared.dev.DevFridgePartnership
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
    @Autowired private lateinit var messageService: MessageService
    @Autowired private lateinit var placementController: PlacementController

    private val today = LocalDate.of(2026, 5, 7)
    private val now = HelsinkiDateTime.of(today, LocalTime.of(2, 0))
    private val clock = MockEvakaClock(now)

    private val bulletinExpiresBefore = now.minusYears(5)
    private val bulletinPlacementExpireDate = today.minusYears(5)
    private val draftExpiresBefore = now.minusYears(1)
    private val applicationExpireDate = today.minusYears(10)
    private val financePlacementExpireDate = today.minusYears(5)
    private val financeExpiresBefore = now.minusYears(5)
    private val regularPlacementExpireDate = today.minusYears(5)
    private val regularExpiresBefore = now.minusYears(10)

    // A placement whose child left care over five years ago
    private val expiredPlacementPeriod =
        FiniteDateRange(today.minusYears(6), bulletinPlacementExpireDate.minusDays(1))
    // A placement that is still going on
    private val ongoingPlacementPeriod = FiniteDateRange(today.minusYears(6), today.plusYears(1))
    // A placement that is still going on and started over ten years ago
    private val placementOngoingForOverTenYears =
        FiniteDateRange(today.minusYears(12), today.plusYears(1))
    private val sendTimeOverFiveYearsAgo =
        HelsinkiDateTime.of(expiredPlacementPeriod.start.plusDays(1), LocalTime.of(12, 0))
    private val sendTimeWithinFiveYears =
        HelsinkiDateTime.of(bulletinPlacementExpireDate.plusDays(1), LocalTime.of(12, 0))
    private val replyTimeOverFiveYearsAgo =
        HelsinkiDateTime.of(financePlacementExpireDate.minusDays(1), LocalTime.of(12, 0))
    private val sendTimeOverTenYearsAgo =
        HelsinkiDateTime.of(regularExpiresBefore.toLocalDate().minusYears(1), LocalTime.of(12, 0))
    private val sendTimeWithinTenYears =
        HelsinkiDateTime.of(regularExpiresBefore.toLocalDate().plusDays(1), LocalTime.of(12, 0))

    // Past SAFE_DATA_REMOVAL_AGE, so the guardianships of old placements can be removed
    private val childDateOfBirth = today.minusYears(13)

    // Owns the municipal message account and corrects placements
    private val admin = DevEmployee(roles = setOf(UserRole.ADMIN))
    // Sends the bulletins of their own unit
    private val unitStaff = DevEmployee()
    // Owns the service worker message account
    private val serviceWorker = DevEmployee(roles = setOf(UserRole.SERVICE_WORKER))
    // Owns the finance message account
    private val financeAdmin = DevEmployee(roles = setOf(UserRole.FINANCE_ADMIN))
    private val careArea = DevCareArea()
    private val daycare =
        DevDaycare(areaId = careArea.id, enabledPilotFeatures = setOf(PilotFeature.MESSAGING))
    // Staff copies are made only for the groups that exist on the send date
    private val daycareGroup =
        DevDaycareGroup(daycareId = daycare.id, startDate = placementOngoingForOverTenYears.start)
    private val child = DevPerson(dateOfBirth = childDateOfBirth)
    private val guardian = DevPerson()

    private lateinit var staffSender: Sender
    private lateinit var municipalSender: Sender
    private lateinit var serviceWorkerSender: Sender
    private lateinit var financeSender: Sender
    private lateinit var guardianAccount: MessageAccountId

    @BeforeEach
    fun setup() {
        db.transaction { tx ->
            tx.insert(admin)
            tx.insert(unitStaff)
            tx.insert(serviceWorker)
            tx.insert(financeAdmin)
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
            serviceWorkerSender = Sender(serviceWorker.user, tx.createServiceWorkerMessageAccount())
            financeSender = Sender(financeAdmin.user, tx.createFinanceMessageAccount())
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
        relatedApplicationId: ApplicationId? = null,
        markSent: Boolean = true,
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
                        relatedApplicationId = relatedApplicationId,
                    ),
                )
                .createdId ?: error("Message had no recipients")
        if (markSent) runSendingJobs()
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

    // Staff copies were made of regular messages too until 2024, but only bulletins get one now
    private fun turnIntoLegacyRegularMessage(sent: SentMessage) {
        db.transaction { tx ->
            tx.execute {
                sql(
                    """
UPDATE message_thread
SET message_type = 'MESSAGE'
WHERE id = ANY(SELECT thread_id FROM message WHERE content_id = ${bind(sent.contentId)})
"""
                )
            }
        }
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

    private fun sendServiceWorkerMessage(
        applicationId: ApplicationId,
        sentAt: HelsinkiDateTime = now,
        attachmentCount: Int = 0,
    ): SentMessage =
        sendMessage(
            sentAt = sentAt,
            recipients = listOf(MessageRecipient.Citizen(guardian.id)),
            sender = serviceWorkerSender,
            type = MessageType.MESSAGE,
            attachmentCount = attachmentCount,
            relatedApplicationId = applicationId,
        )

    private fun deleteExpiredApplications(limit: Int = 100) =
        dataRemovalService.deleteExpiredApplications(
            db,
            now,
            expireDate = applicationExpireDate,
            limit = limit,
        )

    private fun deleteExpiredServiceWorkerThreads(limit: Int = 100) =
        dataRemovalService.deleteExpiredServiceWorkerThreads(db, now, limit = limit)

    private fun deleteExpiredBulletinThreads(limit: Int = 100) =
        dataRemovalService.deleteExpiredBulletinThreads(
            db,
            now,
            placementExpireDate = bulletinPlacementExpireDate,
            expiresBefore = bulletinExpiresBefore,
            limit = limit,
        )

    private fun deleteExpiredRegularThreads(limit: Int = 100) =
        dataRemovalService.deleteExpiredRegularThreads(
            db,
            now,
            placementExpireDate = regularPlacementExpireDate,
            expiresBefore = regularExpiresBefore,
            limit = limit,
        )

    private fun sendFinanceMessage(
        sentAt: HelsinkiDateTime = now,
        attachmentCount: Int = 0,
        markSent: Boolean = true,
        recipient: PersonId = guardian.id,
    ): SentMessage =
        sendMessage(
            sentAt = sentAt,
            recipients = listOf(MessageRecipient.Citizen(recipient)),
            sender = financeSender,
            type = MessageType.MESSAGE,
            attachmentCount = attachmentCount,
            markSent = markSent,
        )

    private fun insertAdultWithoutChildren(): PersonId {
        val adult = DevPerson()
        db.transaction { tx -> tx.insert(adult, DevPersonType.ADULT) }
        return adult.id
    }

    private fun insertFosterParentOfChildWhoLeftCareOverTenYearsAgo(): PersonId {
        val leftCareOverTenYearsAgo = FiniteDateRange(today.minusYears(12), today.minusYears(11))
        insertGroupPlacement(child.id, leftCareOverTenYearsAgo)
        val fosterParent = DevPerson()
        db.transaction { tx ->
            tx.insert(fosterParent, DevPersonType.ADULT)
            tx.insert(
                DevFosterParent(
                    childId = child.id,
                    parentId = fosterParent.id,
                    validDuring = leftCareOverTenYearsAgo.asDateRange(),
                    modifiedAt = now,
                    modifiedBy = admin.evakaUserId,
                )
            )
        }
        return fosterParent.id
    }

    private fun deleteExpiredFinanceThreads(limit: Int = 100) =
        dataRemovalService.deleteExpiredFinanceThreads(
            db,
            now,
            placementExpireDate = financePlacementExpireDate,
            expiresBefore = financeExpiresBefore,
            limit = limit,
        )

    // The children are left unselected by default, which a citizen with several children could
    // do until November 2023
    private fun sendCitizenMessage(
        sentAt: HelsinkiDateTime,
        children: Set<ChildId> = emptySet(),
    ): SentMessage {
        val contentId = db.transaction { tx ->
            messageService
                .sendMessageAsCitizen(
                    tx,
                    sentAt,
                    guardianAccount,
                    NewMessageStub(
                        title = "title",
                        content = "content",
                        urgent = false,
                        sensitive = false,
                    ),
                    recipients = setOf(staffSender.account),
                    children = children,
                )
                .contentId
        }
        runSendingJobs()
        return SentMessage(contentId, ageThreadsTo(contentId, sentAt), emptySet())
    }

    private fun deleteExpiredMessageDrafts(limit: Int = 100) =
        dataRemovalService.deleteExpiredMessageDrafts(
            db,
            now,
            expiresBefore = draftExpiresBefore,
            limit = limit,
        )

    @Test
    fun `deleteExpiredBulletinThreads deletes a bulletin thread with its messages, recipients, participants, children and content five years after its children left care`() {
        insertGroupPlacement(child.id, expiredPlacementPeriod)
        val sent = sendMessage(sentAt = sendTimeOverFiveYearsAgo, attachmentCount = 2)
        // A follow-up doesn't postpone the removal of a bulletin anchored to placements, unlike
        // one expiring by age
        replyToThread(sent.threadIds.single(), sentAt = now.minusYears(1))

        deleteExpiredBulletinThreads()

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
    fun `deleteExpiredBulletinThreads keeps a bulletin whose child left care exactly five years ago`() {
        insertGroupPlacement(
            child.id,
            FiniteDateRange(expiredPlacementPeriod.start, bulletinPlacementExpireDate),
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
            FiniteDateRange(expiredPlacementPeriod.start, bulletinPlacementExpireDate),
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
    fun `deleteExpiredBulletinThreads deletes a bulletin whose child has no placements left once it has had no messages for five years`() {
        // Without placements there is nothing to measure the recipient retention from. A follow-up
        // must protect only its own thread, and only while it is recent.
        val placementId = insertGroupPlacement(child.id, ongoingPlacementPeriod)
        val withinAgeLimit = sendMessage(sentAt = sendTimeWithinFiveYears)
        val withRecentFollowUp = sendMessage(sentAt = sendTimeOverFiveYearsAgo)
        replyToThread(withRecentFollowUp.threadIds.single(), sentAt = now.minusYears(1))
        val withOldFollowUp = sendMessage(sentAt = sendTimeOverFiveYearsAgo)
        replyToThread(
            withOldFollowUp.threadIds.single(),
            sentAt = sendTimeOverFiveYearsAgo.plusDays(1),
        )
        placementController.deletePlacement(dbInstance(), admin.user, clock, placementId)

        deleteExpiredBulletinThreads()

        assertEquals(
            (withinAgeLimit.threadIds + withRecentFollowUp.threadIds).toSet(),
            survivingMessageThreadIds().toSet(),
        )
        assertEquals(3, rowCount("message_content"))
    }

    @Test
    fun `deleteExpiredBulletinThreads deletes a municipal bulletin five years after it was sent`() {
        insertGroupPlacement(child.id, ongoingPlacementPeriod)
        sendMessage(
            sentAt = sendTimeOverFiveYearsAgo,
            sender = municipalSender,
            recipients = listOf(MessageRecipient.Unit(daycare.id)),
        )
        val withinAgeLimit =
            sendMessage(
                sentAt = sendTimeWithinFiveYears,
                sender = municipalSender,
                recipients = listOf(MessageRecipient.Unit(daycare.id)),
            )
        assertEquals(
            0,
            rowCount("message_thread_children"),
            "a municipal bulletin records no children",
        )

        deleteExpiredBulletinThreads()

        assertEquals(withinAgeLimit.threadIds, survivingMessageThreadIds())
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
    fun `deleteExpiredBulletinThreads deletes a staff copy of a regular message once the message it copies is gone`() {
        createGroupMessageAccount()
        insertGroupPlacement(child.id, expiredPlacementPeriod)
        val sent =
            sendMessage(
                sentAt = sendTimeOverFiveYearsAgo,
                recipients = listOf(MessageRecipient.Group(daycareGroup.id)),
                attachmentCount = 1,
            )
        turnIntoLegacyRegularMessage(sent)
        val copyId = staffCopyThreadIds().single()

        deleteExpiredRegularThreads()

        assertEquals(listOf(copyId), survivingMessageThreadIds())

        deleteExpiredBulletinThreads()

        assertEquals(0, rowCount("message_thread"))
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
    fun `deleteExpiredBulletinThreads keeps an expired bulletin that an application references`() {
        // Bulletins linked to an application exist only because of an earlier bug, so the link
        // can only be created directly. The note that such a message also creates can be
        // deleted by an employee, so the link alone has to keep the thread.
        insertGroupPlacement(child.id, expiredPlacementPeriod)
        val linked = sendMessage(sentAt = sendTimeOverFiveYearsAgo, attachmentCount = 1)
        setThreadApplication(linked.threadIds.single(), insertApplication())
        val noted = sendMessage(sentAt = sendTimeOverFiveYearsAgo, attachmentCount = 1)
        insertApplicationNote(insertApplication(), noted.contentId)

        deleteExpiredBulletinThreads()

        assertEquals(2, rowCount("message_thread"))
        assertEquals(2, rowCount("message_content"))
        assertEquals(2, countNonNull("attachment", "message_content_id"))
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
    fun `deleteExpiredRegularThreads deletes a thread with its messages, recipients, participants, children and content five years after its children left care`() {
        insertGroupPlacement(child.id, expiredPlacementPeriod)
        val sent =
            sendMessage(
                sentAt = sendTimeOverFiveYearsAgo,
                type = MessageType.MESSAGE,
                attachmentCount = 1,
            )
        // A message sent after the children left care doesn't postpone the removal
        replyToThread(sent.threadIds.single(), sentAt = now.minusYears(1))

        deleteExpiredRegularThreads()

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
    fun `deleteExpiredRegularThreads keeps a thread whose child left care exactly five years ago`() {
        insertGroupPlacement(
            child.id,
            FiniteDateRange(expiredPlacementPeriod.start, regularPlacementExpireDate),
        )
        sendMessage(sentAt = sendTimeOverFiveYearsAgo, type = MessageType.MESSAGE)

        deleteExpiredRegularThreads()

        assertEquals(1, rowCount("message_thread"))
        assertEquals(1, rowCount("message_content"))
    }

    @Test
    fun `deleteExpiredRegularThreads keeps a thread over ten years old while its child is in care`() {
        // The age limit only decides when no placement is found
        insertGroupPlacement(child.id, placementOngoingForOverTenYears)
        sendCitizenMessage(sentAt = sendTimeOverTenYearsAgo, children = setOf(child.id))

        deleteExpiredRegularThreads()

        assertEquals(1, rowCount("message_thread"))
        assertEquals(1, rowCount("message_content"))
    }

    @Test
    fun `deleteExpiredRegularThreads takes the latest placement of its children as the expiry anchor`() {
        insertGroupPlacement(child.id, expiredPlacementPeriod)
        val sibling = insertSibling(ongoingPlacementPeriod)
        sendMessage(
            sentAt = sendTimeOverFiveYearsAgo,
            recipients = listOf(MessageRecipient.Child(child.id), MessageRecipient.Child(sibling)),
            type = MessageType.MESSAGE,
        )
        assertEquals(1, rowCount("message_thread"))
        assertEquals(2, rowCount("message_thread_children"), "both children are on one thread")

        deleteExpiredRegularThreads()

        assertEquals(1, rowCount("message_thread"))
        assertEquals(1, rowCount("message_content"))
    }

    @Test
    fun `deleteExpiredRegularThreads keeps the threads of a child who is still in care`() {
        insertGroupPlacement(child.id, ongoingPlacementPeriod)
        val retained = sendMessage(sentAt = sendTimeOverFiveYearsAgo, type = MessageType.MESSAGE)
        val otherChild = insertChildOfAnotherGuardian(expiredPlacementPeriod)
        sendMessage(
            sentAt = sendTimeOverFiveYearsAgo,
            recipients = listOf(MessageRecipient.Child(otherChild)),
            type = MessageType.MESSAGE,
        )

        deleteExpiredRegularThreads()

        assertEquals(retained.threadIds, survivingMessageThreadIds())
        assertEquals(1, rowCount("message_content"))
    }

    @Test
    fun `deleteExpiredRegularThreads returns the children of a deleted thread`() {
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
            tx.deleteExpiredRegularThreads(
                regularPlacementExpireDate,
                regularExpiresBefore,
                limit = 100,
            )
        }

        assertEquals(sent.threadIds, batch.threads.map { it.threadId })
        assertEquals(setOf(child.id, sibling), batch.threads.single().childIds.toSet())
    }

    @Test
    fun `deleteExpiredRegularThreads deletes a thread whose children have no placements once it has had no messages for ten years`() {
        sendCitizenMessage(sentAt = sendTimeOverTenYearsAgo, children = setOf(child.id))
        val withinAgeLimit =
            sendCitizenMessage(sentAt = sendTimeWithinTenYears, children = setOf(child.id))
        val withRecentReply =
            sendCitizenMessage(sentAt = sendTimeOverTenYearsAgo, children = setOf(child.id))
        replyToThread(withRecentReply.threadIds.single(), sentAt = sendTimeWithinTenYears)

        deleteExpiredRegularThreads()

        assertEquals(
            (withinAgeLimit.threadIds + withRecentReply.threadIds).toSet(),
            survivingMessageThreadIds().toSet(),
        )
    }

    @Test
    fun `deleteExpiredRegularThreads deletes a thread without recorded children ten years after its last message although a child of its citizen is in care`() {
        insertGroupPlacement(child.id, placementOngoingForOverTenYears)
        sendCitizenMessage(sentAt = sendTimeOverTenYearsAgo)

        deleteExpiredRegularThreads()

        assertEquals(0, rowCount("message_thread"))
        assertEquals(0, rowCount("message"))
        assertEquals(0, rowCount("message_recipients"))
        assertEquals(0, rowCount("message_thread_participant"))
        assertEquals(0, rowCount("message_content"))
    }

    @Test
    fun `deleteExpiredRegularThreads keeps bulletins, staff copies and the threads of the finance account`() {
        // Apart from the original of the copy, whose child is in care, every thread is over ten
        // years old and records no children, so only its type keeps it from this removal
        insertGroupPlacement(child.id, placementOngoingForOverTenYears)
        sendMessage(
            sentAt = sendTimeOverTenYearsAgo,
            sender = municipalSender,
            recipients = listOf(MessageRecipient.Unit(daycare.id)),
        )
        sendFinanceMessage(sentAt = sendTimeOverTenYearsAgo)
        createGroupMessageAccount()
        val copied =
            sendMessage(
                sentAt = sendTimeOverTenYearsAgo,
                recipients = listOf(MessageRecipient.Group(daycareGroup.id)),
            )
        turnIntoLegacyRegularMessage(copied)
        assertEquals(1, staffCopyThreadIds().size)
        val threadIds = survivingMessageThreadIds().toSet()
        assertEquals(4, threadIds.size)

        deleteExpiredRegularThreads()

        assertEquals(threadIds, survivingMessageThreadIds().toSet())
    }

    @Test
    fun `deleteExpiredRegularThreads keeps an expired thread that an application references`() {
        insertGroupPlacement(child.id, expiredPlacementPeriod)
        val linked = sendMessage(sentAt = sendTimeOverFiveYearsAgo, type = MessageType.MESSAGE)
        setThreadApplication(linked.threadIds.single(), insertApplication())
        val noted = sendMessage(sentAt = sendTimeOverFiveYearsAgo, type = MessageType.MESSAGE)
        insertApplicationNote(insertApplication(), noted.contentId)

        deleteExpiredRegularThreads()

        assertEquals(2, rowCount("message_thread"))
        assertEquals(2, rowCount("message_content"))
    }

    @Test
    fun `deleteExpiredRegularThreads doesn't remove more threads than the limit`() {
        insertGroupPlacement(child.id, expiredPlacementPeriod)
        repeat(3) { sendMessage(sentAt = sendTimeOverFiveYearsAgo, type = MessageType.MESSAGE) }

        deleteExpiredRegularThreads(limit = 2)

        assertEquals(1, rowCount("message_thread"))
        assertEquals(1, rowCount("message_content"))
    }

    @Test
    fun `deleteExpiredServiceWorkerThreads deletes a thread with its messages, recipients, participants and content once its application is gone`() {
        val sent = sendServiceWorkerMessage(insertExpiredApplication())
        // The reply is copied into a note of the application too, which goes with the application
        replyToThread(sent.threadIds.single(), now, sender = serviceWorkerSender)
        deleteExpiredApplications()

        deleteExpiredServiceWorkerThreads()

        assertEquals(0, rowCount("message_thread"))
        assertEquals(0, rowCount("message"))
        assertEquals(0, rowCount("message_recipients"))
        assertEquals(0, rowCount("message_thread_participant"))
        assertEquals(0, rowCount("message_content"))
    }

    @Test
    fun `deleteExpiredServiceWorkerThreads keeps a thread whose application still exists`() {
        sendServiceWorkerMessage(insertApplication())

        deleteExpiredServiceWorkerThreads()

        assertEquals(1, rowCount("message_thread"))
        assertEquals(1, rowCount("message_content"))
    }

    @Test
    fun `deleteExpiredServiceWorkerThreads keeps the threads of the other message accounts`() {
        insertGroupPlacement(child.id, expiredPlacementPeriod)
        sendMessage(sentAt = sendTimeOverFiveYearsAgo, type = MessageType.MESSAGE)
        sendMessage(sentAt = sendTimeOverFiveYearsAgo)
        // A finance thread is a message to a citizen without an application link or notes, which
        // makes the account type the only thing that separates it from a service worker thread
        sendMessage(
            sentAt = sendTimeOverFiveYearsAgo,
            recipients = listOf(MessageRecipient.Citizen(guardian.id)),
            sender = financeSender,
            type = MessageType.MESSAGE,
        )

        deleteExpiredServiceWorkerThreads()

        assertEquals(3, rowCount("message_thread"))
        assertEquals(3, rowCount("message_content"))
    }

    @Test
    fun `deleteExpiredServiceWorkerThreads doesn't remove more threads than the limit`() {
        val applicationId = insertExpiredApplication()
        repeat(3) { sendServiceWorkerMessage(applicationId) }
        deleteExpiredApplications()

        deleteExpiredServiceWorkerThreads(limit = 2)

        assertEquals(1, rowCount("message_thread"))
        assertEquals(1, rowCount("message_content"))
    }

    @Test
    fun `deleteExpiredFinanceThreads deletes a thread with its messages, recipients, participants and content five years after the children of its citizen left care`() {
        insertGroupPlacement(child.id, expiredPlacementPeriod)
        sendFinanceMessage(sentAt = sendTimeOverFiveYearsAgo)

        deleteExpiredFinanceThreads()

        assertEquals(0, rowCount("message_thread"))
        assertEquals(0, rowCount("message"))
        assertEquals(0, rowCount("message_recipients"))
        assertEquals(0, rowCount("message_thread_participant"))
        assertEquals(0, rowCount("message_content"))
    }

    @Test
    fun `deleteExpiredFinanceThreads keeps a thread until it has had no messages for five years although the children of its citizen left care earlier`() {
        insertGroupPlacement(child.id, expiredPlacementPeriod)
        val withinAgeLimit = sendFinanceMessage(sentAt = sendTimeWithinFiveYears)
        val withRecentReply = sendFinanceMessage(sentAt = sendTimeOverFiveYearsAgo)
        replyToThread(
            withRecentReply.threadIds.single(),
            sentAt = sendTimeWithinFiveYears,
            sender = financeSender,
        )
        val withOldReply = sendFinanceMessage(sentAt = sendTimeOverFiveYearsAgo)
        replyToThread(
            withOldReply.threadIds.single(),
            sentAt = replyTimeOverFiveYearsAgo,
            sender = financeSender,
        )

        deleteExpiredFinanceThreads()

        assertEquals(
            (withinAgeLimit.threadIds + withRecentReply.threadIds).toSet(),
            survivingMessageThreadIds().toSet(),
        )
        assertEquals(3, rowCount("message_content"))
    }

    @Test
    fun `deleteExpiredFinanceThreads keeps a thread whose citizen's child left care exactly five years ago`() {
        insertGroupPlacement(
            child.id,
            FiniteDateRange(expiredPlacementPeriod.start, financePlacementExpireDate),
        )
        sendFinanceMessage(sentAt = sendTimeOverFiveYearsAgo)

        deleteExpiredFinanceThreads()

        assertEquals(1, rowCount("message_thread"))
        assertEquals(1, rowCount("message_content"))
    }

    @Test
    fun `deleteExpiredFinanceThreads takes the latest placement of the children of its citizen as the expiry anchor`() {
        insertGroupPlacement(child.id, expiredPlacementPeriod)
        insertSibling(ongoingPlacementPeriod)
        sendFinanceMessage(sentAt = sendTimeOverFiveYearsAgo)

        deleteExpiredFinanceThreads()

        assertEquals(1, rowCount("message_thread"))
        assertEquals(1, rowCount("message_content"))
    }

    @Test
    fun `deleteExpiredFinanceThreads keeps a thread while a child of the partner of its citizen is in care`() {
        // The citizen has no placed child of their own, so only the family of the partner keeps
        // the thread
        val partner = DevPerson()
        val partnersChild = DevPerson()
        db.transaction { tx ->
            tx.insert(partner, DevPersonType.ADULT)
            tx.insert(partnersChild, DevPersonType.CHILD)
            tx.insert(
                DevFridgeChild(
                    childId = partnersChild.id,
                    headOfChild = partner.id,
                    startDate = ongoingPlacementPeriod.start,
                    endDate = ongoingPlacementPeriod.end,
                )
            )
            tx.insert(
                DevFridgePartnership(
                    first = partner.id,
                    second = guardian.id,
                    startDate = ongoingPlacementPeriod.start,
                    createdAt = now,
                )
            )
        }
        insertGroupPlacement(partnersChild.id, ongoingPlacementPeriod)
        sendFinanceMessage(sentAt = sendTimeOverFiveYearsAgo)

        deleteExpiredFinanceThreads()

        assertEquals(1, rowCount("message_thread"))
        assertEquals(1, rowCount("message_content"))
    }

    @Test
    fun `deleteExpiredFinanceThreads deletes a thread of a citizen who has no children at all`() {
        sendFinanceMessage(
            sentAt = sendTimeOverFiveYearsAgo,
            recipient = insertAdultWithoutChildren(),
        )

        deleteExpiredFinanceThreads()

        assertEquals(0, rowCount("message_thread"))
        assertEquals(0, rowCount("message_content"))
    }

    @Test
    fun `deleteExpiredFinanceThreads keeps the threads of the other message accounts`() {
        insertGroupPlacement(child.id, expiredPlacementPeriod)
        val retained =
            listOf(
                sendMessage(sentAt = sendTimeOverFiveYearsAgo, type = MessageType.MESSAGE),
                sendMessage(sentAt = sendTimeOverFiveYearsAgo),
                sendServiceWorkerMessage(insertApplication(), sentAt = sendTimeOverFiveYearsAgo),
            )
        sendFinanceMessage(sentAt = sendTimeOverFiveYearsAgo)

        deleteExpiredFinanceThreads()

        assertEquals(
            retained.flatMap { it.threadIds }.toSet(),
            survivingMessageThreadIds().toSet(),
        )
    }

    @Test
    fun `deleteExpiredFinanceThreads doesn't remove more threads than the limit`() {
        repeat(3) { sendFinanceMessage(sentAt = sendTimeOverFiveYearsAgo) }

        deleteExpiredFinanceThreads(limit = 2)

        assertEquals(1, rowCount("message_thread"))
        assertEquals(1, rowCount("message_content"))
    }

    @Test
    fun `deleteExpiredFinanceThreads keeps a thread whose messages are not marked sent yet while a child of its citizen is in care`() {
        // The participant row of the recipient is written by the job that marks the messages sent
        insertGroupPlacement(child.id, ongoingPlacementPeriod)
        sendFinanceMessage(sentAt = sendTimeOverFiveYearsAgo, markSent = false)

        deleteExpiredFinanceThreads()

        assertEquals(1, rowCount("message_thread"))
        assertEquals(1, rowCount("message_content"))
    }

    @Test
    fun `deleteExpiredFosterParents keeps foster parenthood while the finance thread removal of its parent is pending and deletes it afterwards`() {
        val fosterParent = insertFosterParentOfChildWhoLeftCareOverTenYearsAgo()
        // The participant row of the recipient is written by the job that marks the messages sent,
        // so the parent of an unsent thread is found only through its recipients
        sendFinanceMessage(
            sentAt = sendTimeOverFiveYearsAgo,
            markSent = false,
            recipient = fosterParent,
        )

        deleteExpiredFosterParents(
            db,
            expireDate = today.minusYears(10),
            citizenUserExpireDate = today.minusYears(1),
            limit = 100,
        )

        assertEquals(
            1,
            rowCount("foster_parent"),
            "foster parenthood is kept while a finance thread exists",
        )

        deleteExpiredFinanceThreads()
        deleteExpiredFosterParents(
            db,
            expireDate = today.minusYears(10),
            citizenUserExpireDate = today.minusYears(1),
            limit = 100,
        )

        assertEquals(0, rowCount("foster_parent"))
    }

    @Test
    fun `deleteExpiredGuardians keeps guardianship while the finance thread removal of its guardian is pending and deletes it afterwards`() {
        val leftCareOverTenYearsAgo = FiniteDateRange(today.minusYears(12), today.minusYears(11))
        insertGroupPlacement(child.id, leftCareOverTenYearsAgo)
        sendFinanceMessage(sentAt = sendTimeOverFiveYearsAgo)

        deleteExpiredGuardians(
            db,
            now,
            expireDate = today.minusYears(10),
            citizenUserExpireDate = today.minusYears(1),
            limit = 100,
        )

        assertEquals(1, rowCount("guardian"), "guardianship is kept while a finance thread exists")

        deleteExpiredFinanceThreads()
        deleteExpiredGuardians(
            db,
            now,
            expireDate = today.minusYears(10),
            citizenUserExpireDate = today.minusYears(1),
            limit = 100,
        )

        assertEquals(0, rowCount("guardian"))
    }

    @Test
    fun `deleteExpiredMessageDrafts deletes an expired draft of every type and enqueues its attachment deletion`() {
        val draftIds =
            MessageType.entries.map {
                createDraft(createdAt = draftExpiresBefore.minusDays(1), type = it)
            }
        val attachmentId = insertDraftAttachment(draftIds.first(), now)

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
    fun `deleteExpiredMessageDrafts deletes a draft created over a year ago even if it was modified today`() {
        createDraft(createdAt = draftExpiresBefore.minusDays(1), modifiedAt = now)

        deleteExpiredMessageDrafts()

        assertEquals(0, rowCount("message_draft"))
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
    fun `deleteExpiredData removes a service worker thread once its application has been removed`() {
        val sent = sendServiceWorkerMessage(insertExpiredApplication(), attachmentCount = 1)

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
    fun `deleteExpiredData keeps a service worker thread while its application is retained`() {
        insertGroupPlacement(child.id, ongoingPlacementPeriod)
        sendServiceWorkerMessage(insertApplication())

        withLimit(1000) {
            dataRemovalService.deleteExpiredData(db, clock, AsyncJob.DeleteExpiredData)
        }

        assertEquals(1, rowCount("application"))
        assertEquals(1, rowCount("message_thread"))
        assertEquals(1, rowCount("message_content"))
    }

    @Test
    fun `deleteExpiredData removes a regular message thread and its staff copy five years after its children left care`() {
        createGroupMessageAccount()
        insertGroupPlacement(child.id, expiredPlacementPeriod)
        val sent =
            sendMessage(
                sentAt = sendTimeOverFiveYearsAgo,
                recipients = listOf(MessageRecipient.Group(daycareGroup.id)),
                attachmentCount = 1,
            )
        turnIntoLegacyRegularMessage(sent)

        withLimit(1000) {
            dataRemovalService.deleteExpiredData(db, clock, AsyncJob.DeleteExpiredData)
        }

        assertEquals(0, rowCount("message_thread"))
        assertEquals(0, rowCount("message_content"))
        assertEquals(
            sent.attachmentIds.map { it.toString() }.toSet(),
            scheduledAttachmentDeletionIds(),
        )
    }

    @Test
    fun `deleteExpiredData removes a thread without recorded children ten years after its last message`() {
        insertGroupPlacement(child.id, placementOngoingForOverTenYears)
        sendCitizenMessage(sentAt = sendTimeOverTenYearsAgo)
        val withinAgeLimit = sendCitizenMessage(sentAt = sendTimeWithinTenYears)

        withLimit(1000) {
            dataRemovalService.deleteExpiredData(db, clock, AsyncJob.DeleteExpiredData)
        }

        assertEquals(withinAgeLimit.threadIds, survivingMessageThreadIds())
        assertEquals(1, rowCount("message_content"))
    }

    @Test
    fun `deleteExpiredData removes a finance thread five years after the children of its citizen left care`() {
        insertGroupPlacement(child.id, expiredPlacementPeriod)
        val sent = sendFinanceMessage(sentAt = sendTimeOverFiveYearsAgo, attachmentCount = 1)

        withLimit(1000) {
            dataRemovalService.deleteExpiredData(db, clock, AsyncJob.DeleteExpiredData)
        }

        assertEquals(0, rowCount("message_thread"))
        assertEquals(0, rowCount("message_content"))
        assertTrue(
            scheduledAttachmentDeletionIds().containsAll(sent.attachmentIds.map { it.toString() })
        )
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

    // An application of a child who left care over ten years ago, which the application removal
    // deletes
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
