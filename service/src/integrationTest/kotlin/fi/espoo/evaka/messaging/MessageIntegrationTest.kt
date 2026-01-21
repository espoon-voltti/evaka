// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging

import fi.espoo.evaka.*
import fi.espoo.evaka.application.ApplicationType
import fi.espoo.evaka.application.notes.getApplicationNotes
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.attachment.AttachmentsController
import fi.espoo.evaka.daycare.CareType
import fi.espoo.evaka.insertServiceNeedOptions
import fi.espoo.evaka.messaging.MessageController.PostMessagePreflightResponse
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.serviceneed.ShiftCareType
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.AttachmentId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.MessageAccountId
import fi.espoo.evaka.shared.MessageContentId
import fi.espoo.evaka.shared.MessageDraftId
import fi.espoo.evaka.shared.MessageThreadFolderId
import fi.espoo.evaka.shared.MessageThreadId
import fi.espoo.evaka.shared.ServiceNeedOptionId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.*
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.*
import fi.espoo.evaka.shared.domain.*
import fi.espoo.evaka.shared.security.PilotFeature
import fi.espoo.evaka.test.validDaycareApplication
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockMultipartFile

class MessageIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired lateinit var attachmentsController: AttachmentsController
    @Autowired lateinit var messageController: MessageController
    @Autowired lateinit var messageControllerCitizen: MessageControllerCitizen
    @Autowired lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>
    private var asyncJobRunningEnabled = true

    private val clock = RealEvakaClock()

    private val groupId1 = GroupId(UUID.randomUUID())
    private val groupId2 = GroupId(UUID.randomUUID())
    private val employee1 =
        AuthenticatedUser.Employee(
            id = EmployeeId(UUID.randomUUID()),
            roles = setOf(UserRole.UNIT_SUPERVISOR),
        )
    private val employee2 =
        AuthenticatedUser.Employee(
            id = EmployeeId(UUID.randomUUID()),
            roles = setOf(UserRole.UNIT_SUPERVISOR),
        )
    private val serviceWorker =
        DevEmployee(
            firstName = "Service",
            lastName = "Worker",
            roles = setOf(UserRole.SERVICE_WORKER),
        )
    private val messager =
        DevEmployee(
            firstName = "Municipal",
            lastName = "Messager",
            roles = setOf(UserRole.MESSAGING),
        )
    private val financeAdmin =
        DevEmployee(
            firstName = "Finance",
            lastName = "Admin",
            roles = setOf(UserRole.FINANCE_ADMIN),
        )
    private val person1 = AuthenticatedUser.Citizen(id = testAdult_1.id, CitizenAuthLevel.STRONG)
    private val person2 = AuthenticatedUser.Citizen(id = testAdult_2.id, CitizenAuthLevel.STRONG)
    private val person3 = AuthenticatedUser.Citizen(id = testAdult_3.id, CitizenAuthLevel.STRONG)
    private val person4 = AuthenticatedUser.Citizen(id = testAdult_4.id, CitizenAuthLevel.STRONG)
    private val person5 = AuthenticatedUser.Citizen(id = testAdult_5.id, CitizenAuthLevel.STRONG)
    private val person6 = AuthenticatedUser.Citizen(id = testAdult_6.id, CitizenAuthLevel.STRONG)
    private val person7 = AuthenticatedUser.Citizen(id = testAdult_7.id, CitizenAuthLevel.STRONG)
    private val placementStart = LocalDate.of(2022, 5, 14)
    private val placementEnd = placementStart.plusMonths(1)
    private val sendTime = HelsinkiDateTime.of(placementStart, LocalTime.of(12, 11))
    private val readTime = sendTime.plusSeconds(30)

    private lateinit var employee1Account: MessageAccountId
    private lateinit var employee2Account: MessageAccountId
    private lateinit var group1Account: MessageAccountId
    private lateinit var group2Account: MessageAccountId
    private lateinit var person1Account: MessageAccountId
    private lateinit var person2Account: MessageAccountId
    private lateinit var person3Account: MessageAccountId
    private lateinit var person4Account: MessageAccountId
    private lateinit var person5Account: MessageAccountId
    private lateinit var person6Account: MessageAccountId
    private lateinit var person7Account: MessageAccountId
    private lateinit var serviceWorkerAccount: MessageAccountId
    private lateinit var municipalAccount: MessageAccountId
    private lateinit var financeAccount: MessageAccountId

    @BeforeEach
    fun setUp() {
        db.transaction { tx ->
            tx.insert(testArea)
            tx.insert(
                DevDaycare(
                    areaId = testArea.id,
                    id = testDaycare.id,
                    name = testDaycare.name,
                    enabledPilotFeatures = setOf(PilotFeature.MESSAGING),
                )
            )
            tx.insert(
                DevDaycare(
                    areaId = testArea.id,
                    id = testDaycare2.id,
                    name = testDaycare2.name,
                    type = setOf(CareType.FAMILY),
                    enabledPilotFeatures = setOf(PilotFeature.MESSAGING),
                )
            )

            fun insertGroup(id: GroupId, name: String? = null): MessageAccountId {
                tx.insert(
                    DevDaycareGroup(
                        id = id,
                        daycareId = testDaycare.id,
                        startDate = placementStart,
                        name = name ?: "Testiläiset",
                    )
                )
                return tx.createDaycareGroupMessageAccount(id)
            }
            group1Account = insertGroup(groupId1)
            group2Account = insertGroup(groupId2, "Group 2")

            fun insertPerson(person: DevPerson): MessageAccountId {
                val id = tx.insert(person, DevPersonType.ADULT)
                return tx.getCitizenMessageAccount(id)
            }
            person1Account = insertPerson(testAdult_1)
            person2Account = insertPerson(testAdult_2)
            person3Account = insertPerson(testAdult_3)
            person4Account = insertPerson(testAdult_4)
            person5Account = insertPerson(testAdult_5)
            person6Account = insertPerson(testAdult_6)
            person7Account = insertPerson(testAdult_7)

            val fridgeHeadId = person4.id

            tx.insertServiceNeedOptions()
            tx.insert(
                DevEmployee(id = employee1.id, firstName = "Firstname", lastName = "Employee")
            )

            // person 1 and 2 are guardians of child 1
            testChild_1.let {
                insertChild(tx, it, groupId1, optionId = snDefaultPartDayDaycare.id)
                tx.insertGuardian(person1.id, it.id)
                tx.insertGuardian(person2.id, it.id)
                tx.insert(
                    DevParentship(
                        childId = it.id,
                        headOfChildId = fridgeHeadId,
                        startDate = LocalDate.of(2019, 1, 1),
                        endDate = LocalDate.of(2019, 12, 31),
                    )
                ) // parentship alone does not allow messaging if not a guardian
            }

            // person 2 and 3 are guardian of child 3
            testChild_3.let {
                insertChild(tx, it, groupId1, optionId = snDefaultFiveYearOldsDaycare.id)
                tx.insertGuardian(person2.id, it.id)
                tx.insertGuardian(person3.id, it.id)
            }

            testChild_4.let {
                insertChild(
                    tx,
                    it,
                    groupId2,
                    optionId = snDefaultFiveYearOldsPartDayDaycare.id,
                    shiftCare = ShiftCareType.FULL,
                )
                tx.insertGuardian(person4.id, it.id)
            }

            testChild_5.let {
                insertChild(tx, it, groupId1)
                tx.insert(
                    DevParentship(
                        childId = it.id,
                        headOfChildId = fridgeHeadId,
                        startDate = LocalDate.of(2019, 1, 1),
                        endDate = LocalDate.of(2019, 12, 31),
                    )
                ) // no guardian, no messages
            }

            // person 3 and 5 are guardian of child 6
            testChild_6.let {
                insertChild(tx, it, groupId1)
                tx.insertGuardian(person3.id, it.id)
                tx.insertGuardian(person5.id, it.id)
            }

            // person 3 and 5 are guardian of child 6
            testChild_8.let {
                insertChild(
                    tx,
                    it,
                    groupId2,
                    daycareId = testDaycare2.id,
                    optionId = snDefaultPreschool.id,
                    shiftCare = ShiftCareType.INTERMITTENT,
                )
                tx.insertGuardian(person6.id, it.id)
                tx.insertGuardian(person7.id, it.id)
            }

            employee1Account = tx.upsertEmployeeMessageAccount(employee1.id)
            tx.insertDaycareAclRow(testDaycare.id, employee1.id, UserRole.UNIT_SUPERVISOR)
            tx.syncDaycareGroupAcl(
                testDaycare.id,
                employee1.id,
                listOf(groupId1, groupId2),
                clock.now(),
            )

            tx.insert(DevEmployee(id = employee2.id, firstName = "Foo", lastName = "Supervisor"))
            employee2Account = tx.upsertEmployeeMessageAccount(employee2.id)
            tx.insertDaycareAclRow(testDaycare2.id, employee2.id, UserRole.UNIT_SUPERVISOR)

            tx.insert(serviceWorker)
            serviceWorkerAccount = tx.createServiceWorkerMessageAccount()
            tx.insert(messager)
            municipalAccount = tx.createMunicipalMessageAccount()
            tx.insert(financeAdmin)
            financeAccount = tx.createFinanceMessageAccount()
        }
    }

    @Nested
    inner class SensitiveMessages {

        @Test
        fun `sensitive messages can be sent from personal accounts for single child recipients`() {
            postNewThread(
                title = "title",
                message = "Juhannus tulee pian",
                messageType = MessageType.MESSAGE,
                sender = employee1Account,
                recipients = listOf(MessageRecipient.Child(testChild_1.id)),
                user = employee1,
                sensitive = true,
            )

            // then the recipient can see it
            assertEquals(
                listOf(Pair(employee1Account, "Juhannus tulee pian")),
                getRegularMessageThreads(person1).first().toSenderContentPairs(),
            )
        }

        @Test
        fun `sensitive messages cannot be sent to multiple recipients`() {
            assertThrows<BadRequest> {
                postNewThread(
                    title = "title",
                    message = "content",
                    messageType = MessageType.MESSAGE,
                    sender = employee1Account,
                    recipients =
                        listOf(
                            MessageRecipient.Child(testChild_1.id),
                            MessageRecipient.Child(testChild_2.id),
                        ),
                    user = employee1,
                    sensitive = true,
                )
            }
        }

        @Test
        fun `sensitive messages cannot be sent to != child recipients`() {
            assertThrows<BadRequest> {
                postNewThread(
                    title = "title",
                    message = "content",
                    messageType = MessageType.MESSAGE,
                    sender = employee1Account,
                    recipients = listOf(MessageRecipient.Citizen(testAdult_1.id)),
                    user = employee1,
                    sensitive = true,
                )
            }
        }

        @Test
        fun `sensitive messages cannot from != personal accounts`() {
            assertThrows<BadRequest> {
                postNewThread(
                    title = "title",
                    message = "content",
                    messageType = MessageType.MESSAGE,
                    sender = group1Account,
                    recipients = listOf(MessageRecipient.Child(testAdult_1.id)),
                    user = employee1,
                    sensitive = true,
                )
            }
        }

        @Test
        fun `weak auth citizen sees redacted view of sensitive message from personal account to child`() {
            postNewThread(
                title = "Sensitive Title",
                message = "Sensitive content that should be hidden",
                messageType = MessageType.MESSAGE,
                sender = employee1Account,
                recipients = listOf(MessageRecipient.Child(testChild_1.id)),
                user = employee1,
                sensitive = true,
            )

            val weakAuthUser = AuthenticatedUser.Citizen(id = person1.id, CitizenAuthLevel.WEAK)
            val threads = getAllCitizenMessageThreads(weakAuthUser)

            assertEquals(1, threads.size)
            assertTrue(threads[0] is CitizenMessageThread.Redacted)
            val redacted = threads[0] as CitizenMessageThread.Redacted
            assertEquals(employee1Account, redacted.sender?.id)
            assertNotNull(redacted.sender)
            assertNotNull(redacted.lastMessageSentAt)
            assertTrue(redacted.hasUnreadMessages)
        }

        @Test
        fun `strong auth citizen sees full content of sensitive message`() {
            postNewThread(
                title = "Sensitive Title",
                message = "Sensitive content that should be visible",
                messageType = MessageType.MESSAGE,
                sender = employee1Account,
                recipients = listOf(MessageRecipient.Child(testChild_1.id)),
                user = employee1,
                sensitive = true,
            )

            val threads = getAllCitizenMessageThreads(person1)

            assertEquals(1, threads.size)
            assertTrue(threads[0] is CitizenMessageThread.Regular)
            val regular = threads[0] as CitizenMessageThread.Regular
            assertEquals("Sensitive Title", regular.title)
            assertEquals(MessageType.MESSAGE, regular.messageType)
            assertTrue(regular.sensitive)
            assertEquals(1, regular.messages.size)
            assertEquals("Sensitive content that should be visible", regular.messages[0].content)
            assertEquals(employee1Account, regular.messages[0].sender.id)
        }

        @Test
        fun `only guardians and foster parents receive sensitive messages about their child`() {
            val fosterParent = DevPerson(firstName = "Foster", lastName = "Parent")
            db.transaction { tx ->
                tx.insert(fosterParent, DevPersonType.ADULT)
                tx.getCitizenMessageAccount(fosterParent.id)
                tx.insert(
                    DevFosterParent(
                        childId = testChild_1.id,
                        parentId = fosterParent.id,
                        validDuring = DateRange(placementStart, placementEnd),
                        modifiedAt = HelsinkiDateTime.now(),
                        modifiedBy = EvakaUserId(employee1.id.raw),
                    )
                )
            }

            postNewThread(
                title = "Sensitive Title",
                message = "Sensitive content",
                messageType = MessageType.MESSAGE,
                sender = employee1Account,
                recipients = listOf(MessageRecipient.Child(testChild_1.id)),
                user = employee1,
                sensitive = true,
            )

            val person1Threads = getAllCitizenMessageThreads(person1)
            val person2Threads = getAllCitizenMessageThreads(person2)
            val fosterParentUser =
                AuthenticatedUser.Citizen(id = fosterParent.id, CitizenAuthLevel.STRONG)
            val fosterParentThreads = getAllCitizenMessageThreads(fosterParentUser)

            assertEquals(1, person1Threads.size)
            assertEquals(1, person2Threads.size)
            assertEquals(1, fosterParentThreads.size)

            val nonGuardianUser =
                AuthenticatedUser.Citizen(id = testAdult_5.id, CitizenAuthLevel.STRONG)
            val nonGuardianThreads = getAllCitizenMessageThreads(nonGuardianUser)
            assertEquals(0, nonGuardianThreads.size)
        }

        @Test
        fun `strong auth citizen can mark sensitive message as read`() {
            postNewThread(
                title = "Sensitive Title",
                message = "Sensitive content",
                messageType = MessageType.MESSAGE,
                sender = employee1Account,
                recipients = listOf(MessageRecipient.Child(testChild_1.id)),
                user = employee1,
                sensitive = true,
            )

            assertEquals(1, unreadMessagesCount(person1))
            assertEquals(1, getUnreadReceivedMessages(person1Account, person1).size)

            val threadId = getRegularMessageThreads(person1).first().id
            markThreadRead(person1, threadId)

            assertEquals(0, unreadMessagesCount(person1))
            assertEquals(0, getUnreadReceivedMessages(person1Account, person1).size)
        }

        @Test
        fun `strong auth citizen can archive sensitive message thread`() {
            postNewThread(
                title = "Sensitive Title",
                message = "Sensitive content",
                messageType = MessageType.MESSAGE,
                sender = employee1Account,
                recipients = listOf(MessageRecipient.Child(testChild_1.id)),
                user = employee1,
                sensitive = true,
            )

            val threadsBeforeArchive = getAllCitizenMessageThreads(person1)
            assertEquals(1, threadsBeforeArchive.size)

            val threadId = threadsBeforeArchive.first().id
            messageControllerCitizen.archiveThread(dbInstance(), person1, threadId)

            val threadsAfterArchive = getAllCitizenMessageThreads(person1)
            assertEquals(0, threadsAfterArchive.size)
        }

        @Test
        fun `citizen can view sensitive message after placement ends`() {
            val messageTime = HelsinkiDateTime.of(placementStart.plusDays(10), LocalTime.NOON)
            postNewThread(
                title = "Sensitive Title",
                message = "Sensitive content during active placement",
                messageType = MessageType.MESSAGE,
                sender = employee1Account,
                recipients = listOf(MessageRecipient.Child(testChild_1.id)),
                user = employee1,
                sensitive = true,
                now = messageTime,
            )

            val afterPlacementEnd = HelsinkiDateTime.of(placementEnd.plusDays(1), LocalTime.NOON)
            val threads = getAllCitizenMessageThreads(person1, now = afterPlacementEnd)

            assertEquals(1, threads.size)
            assertTrue(threads[0] is CitizenMessageThread.Regular)
            val regular = threads[0] as CitizenMessageThread.Regular
            assertEquals("Sensitive Title", regular.title)
            assertEquals("Sensitive content during active placement", regular.messages[0].content)
            assertTrue(regular.sensitive)
        }

        @Test
        fun `weak auth citizen sees redacted view of old sensitive messages after placement ends`() {
            val messageTime = HelsinkiDateTime.of(placementStart.plusDays(10), LocalTime.NOON)
            postNewThread(
                title = "Sensitive Title",
                message = "Sensitive content during active placement",
                messageType = MessageType.MESSAGE,
                sender = employee1Account,
                recipients = listOf(MessageRecipient.Child(testChild_1.id)),
                user = employee1,
                sensitive = true,
                now = messageTime,
            )

            val afterPlacementEnd = HelsinkiDateTime.of(placementEnd.plusDays(1), LocalTime.NOON)
            val weakAuthUser = AuthenticatedUser.Citizen(id = person1.id, CitizenAuthLevel.WEAK)
            val threads = getAllCitizenMessageThreads(weakAuthUser, now = afterPlacementEnd)

            assertEquals(1, threads.size)
            assertTrue(threads[0] is CitizenMessageThread.Redacted)
            val redacted = threads[0] as CitizenMessageThread.Redacted
            assertNotNull(redacted.sender)
            assertNotNull(redacted.lastMessageSentAt)
        }

        @Test
        fun `sensitive message cannot be sent to child with ended placement`() {
            val endedPlacementStart = LocalDate.now().minusMonths(2)
            val endedPlacementEnd = LocalDate.now().minusDays(1)
            val childWithEndedPlacement = DevPerson()
            val guardianOfEndedPlacement = DevPerson()

            db.transaction { tx ->
                tx.insert(childWithEndedPlacement, DevPersonType.CHILD)
                tx.insert(guardianOfEndedPlacement, DevPersonType.ADULT)
                tx.insertGuardian(guardianOfEndedPlacement.id, childWithEndedPlacement.id)

                val placementId =
                    tx.insert(
                        DevPlacement(
                            childId = childWithEndedPlacement.id,
                            unitId = testDaycare.id,
                            startDate = endedPlacementStart,
                            endDate = endedPlacementEnd,
                        )
                    )
                tx.insert(
                    DevDaycareGroupPlacement(
                        daycarePlacementId = placementId,
                        daycareGroupId = groupId1,
                        startDate = endedPlacementStart,
                        endDate = endedPlacementEnd,
                    )
                )
                tx.insert(
                    DevServiceNeed(
                        confirmedBy = employee1.evakaUserId,
                        placementId = placementId,
                        startDate = endedPlacementStart,
                        endDate = endedPlacementEnd,
                        optionId = snDefaultDaycare.id,
                    )
                )
            }

            val preflightResponse =
                postNewThreadPreflightCheck(
                    user = employee1,
                    sender = employee1Account,
                    recipients = listOf(MessageRecipient.Child(childWithEndedPlacement.id)),
                )
            assertEquals(0, preflightResponse.numberOfRecipientAccounts)

            val messageContentId =
                postNewThread(
                    title = "Sensitive Title",
                    message = "Sensitive content",
                    messageType = MessageType.MESSAGE,
                    sender = employee1Account,
                    recipients = listOf(MessageRecipient.Child(childWithEndedPlacement.id)),
                    user = employee1,
                    sensitive = true,
                )
            assertNull(messageContentId)
        }

        @Test
        fun `citizen with strong auth can reply to sensitive message`() {
            postNewThread(
                title = "Sensitive Title",
                message = "Sensitive content",
                messageType = MessageType.MESSAGE,
                sender = employee1Account,
                recipients = listOf(MessageRecipient.Child(testChild_1.id)),
                user = employee1,
                sensitive = true,
            )

            val thread = getRegularMessageThreads(person1).first()

            replyToThread(
                person1,
                thread.id,
                setOf(employee1Account),
                "Reply to sensitive message",
                now = sendTime.plusSeconds(1),
            )

            val threadsAfterReply = getRegularMessageThreads(person1)
            assertEquals(1, threadsAfterReply.size)
            val updatedThread = threadsAfterReply.first()
            assertEquals(2, updatedThread.messages.size)
            assertEquals("Reply to sensitive message", updatedThread.messages.last().content)
            assertEquals(person1Account, updatedThread.messages.last().sender.id)

            val employeeThreads = getEmployeeMessageThreads(employee1Account, employee1)
            val employeeThread = employeeThreads.first { it.id == thread.id }
            assertEquals(2, employeeThread.messages.size)
            assertEquals("Reply to sensitive message", employeeThread.messages.last().content)
        }

        @Test
        fun `weak auth citizen cannot reply to sensitive message`() {
            postNewThread(
                title = "Sensitive Title",
                message = "Sensitive content",
                messageType = MessageType.MESSAGE,
                sender = employee1Account,
                recipients = listOf(MessageRecipient.Child(testChild_1.id)),
                user = employee1,
                sensitive = true,
            )

            val thread = getRegularMessageThreads(person1).first()

            val weakAuthUser = AuthenticatedUser.Citizen(id = person1.id, CitizenAuthLevel.WEAK)
            val threads = getAllCitizenMessageThreads(weakAuthUser)
            assertEquals(1, threads.size)
            assertTrue(threads[0] is CitizenMessageThread.Redacted)

            assertThrows<Forbidden> {
                messageControllerCitizen.replyToThread(
                    dbInstance(),
                    weakAuthUser,
                    MockEvakaClock(readTime),
                    thread.id,
                    ReplyToMessageBody(
                        content = "Attempted reply",
                        recipientAccountIds = setOf(employee1Account),
                    ),
                )
            }
        }

        @Test
        fun `reply to sensitive message maintains thread sensitivity flag`() {
            postNewThread(
                title = "Sensitive Title",
                message = "Sensitive content",
                messageType = MessageType.MESSAGE,
                sender = employee1Account,
                recipients = listOf(MessageRecipient.Child(testChild_1.id)),
                user = employee1,
                sensitive = true,
            )

            val thread = getRegularMessageThreads(person1).first()
            assertTrue(thread.sensitive)

            replyToThread(
                person1,
                thread.id,
                setOf(employee1Account),
                "Citizen reply",
                now = sendTime.plusSeconds(1),
            )

            val threadsAfterCitizenReply = getRegularMessageThreads(person1)
            val threadAfterCitizenReply = threadsAfterCitizenReply.first()
            assertTrue(threadAfterCitizenReply.sensitive)

            replyToThread(
                employee1,
                employee1Account,
                threadAfterCitizenReply.id,
                setOf(person1Account),
                "Employee reply back",
                now = sendTime.plusSeconds(2),
            )

            val threadsAfterEmployeeReply = getRegularMessageThreads(person1)
            val threadAfterEmployeeReply = threadsAfterEmployeeReply.first()
            assertTrue(threadAfterEmployeeReply.sensitive)

            val weakAuthUser = AuthenticatedUser.Citizen(id = person1.id, CitizenAuthLevel.WEAK)
            val weakAuthThreads = getAllCitizenMessageThreads(weakAuthUser)
            assertEquals(1, weakAuthThreads.size)
            assertTrue(weakAuthThreads[0] is CitizenMessageThread.Redacted)
        }

        @Test
        fun `cannot add new recipients when replying to sensitive message thread`() {
            postNewThread(
                title = "Sensitive Title",
                message = "Sensitive content",
                messageType = MessageType.MESSAGE,
                sender = employee1Account,
                recipients = listOf(MessageRecipient.Child(testChild_1.id)),
                user = employee1,
                sensitive = true,
            )

            val thread = getRegularMessageThreads(person1).first()

            assertThrows<Forbidden> {
                replyToThread(
                    person1,
                    thread.id,
                    setOf(employee1Account, person3Account),
                    "Attempted reply with new recipient",
                )
            }
        }

        @Test
        fun `group account cannot send sensitive messages`() {
            val exception =
                assertThrows<BadRequest> {
                    postNewThread(
                        title = "Sensitive Title",
                        message = "Sensitive content",
                        messageType = MessageType.MESSAGE,
                        sender = group1Account,
                        recipients = listOf(MessageRecipient.Child(testChild_1.id)),
                        user = employee1,
                        sensitive = true,
                    )
                }
            assertEquals(
                "Sensitive messages are only allowed to be sent from personal accounts to a single child recipient or from financial account to a single citizen",
                exception.message,
            )
        }

        @Test
        fun `finance account cannot send sensitive messages to child`() {
            val exception =
                assertThrows<BadRequest> {
                    postNewThread(
                        title = "Sensitive Title",
                        message = "Sensitive content",
                        messageType = MessageType.MESSAGE,
                        sender = financeAccount,
                        recipients = listOf(MessageRecipient.Child(testChild_1.id)),
                        user = financeAdmin.user,
                        sensitive = true,
                    )
                }
            assertEquals(
                "Sensitive messages are only allowed to be sent from personal accounts to a single child recipient or from financial account to a single citizen",
                exception.message,
            )
        }

        @Test
        fun `municipal account cannot send sensitive messages`() {
            val exception =
                assertThrows<BadRequest> {
                    postNewThread(
                        title = "Sensitive Title",
                        message = "Sensitive content",
                        messageType = MessageType.MESSAGE,
                        sender = municipalAccount,
                        recipients = listOf(MessageRecipient.Child(testChild_1.id)),
                        user = messager.user,
                        sensitive = true,
                    )
                }
            assertEquals(
                "Sensitive messages are only allowed to be sent from personal accounts to a single child recipient or from financial account to a single citizen",
                exception.message,
            )
        }

        @Test
        fun `service worker account cannot send sensitive messages`() {
            val exception =
                assertThrows<BadRequest> {
                    postNewThread(
                        title = "Sensitive Title",
                        message = "Sensitive content",
                        messageType = MessageType.MESSAGE,
                        sender = serviceWorkerAccount,
                        recipients = listOf(MessageRecipient.Child(testChild_1.id)),
                        user = serviceWorker.user,
                        sensitive = true,
                    )
                }
            assertEquals(
                "Sensitive messages are only allowed to be sent from personal accounts to a single child recipient or from financial account to a single citizen",
                exception.message,
            )
        }
    }

    @Nested
    inner class CitizenSending {

        @Test
        fun `citizen can send a new messages to multiple accounts if they are related to all selected children`() {
            val groupId3 = GroupId(UUID.randomUUID())
            val group3Account =
                db.transaction { tx ->
                    testChild_2.let {
                        insertChild(tx, it, groupId2)
                        tx.insertGuardian(person2.id, it.id)
                    }

                    tx.insert(
                        DevDaycareGroup(
                            id = groupId3,
                            daycareId = testDaycare2.id,
                            startDate = placementStart,
                        )
                    )
                    testChild_7.let {
                        insertChild(tx, it, groupId3, testDaycare2.id)
                        tx.insertGuardian(person2.id, it.id)
                    }

                    tx.createDaycareGroupMessageAccount(groupId3)
                }

            // Both children in group 1 -> ok
            postNewThread(
                user = person2,
                title = "title",
                message = "content",
                children = listOf(testChild_1.id, testChild_3.id),
                recipients = listOf(group1Account),
            )

            // Child 1 in group 1, child 2 in group 2 -> ok
            postNewThread(
                user = person2,
                title = "title",
                message = "content",
                children = listOf(testChild_1.id, testChild_2.id),
                recipients = listOf(group1Account, group2Account),
            )

            // None of the children in group 2 -> fail
            assertThrows<BadRequest> {
                postNewThread(
                    user = person2,
                    title = "title",
                    message = "content",
                    children = listOf(testChild_1.id, testChild_3.id),
                    recipients = listOf(group1Account, group2Account),
                )
            }

            // Child 1 in group 1, child 7 in group 3, but groups in different unit -> not ok
            assertThrows<BadRequest> {
                postNewThread(
                    user = person2,
                    title = "title",
                    message = "content",
                    children = listOf(testChild_1.id, testChild_7.id),
                    recipients = listOf(group1Account, group3Account),
                )
            }
        }

        @Test
        fun `citizen cannot send message to group when NONE of selected children are in that group`() {
            val groupId3 = GroupId(UUID.randomUUID())
            val group3Account =
                db.transaction { tx ->
                    tx.insert(
                        DevDaycareGroup(
                            id = groupId3,
                            daycareId = testDaycare.id,
                            startDate = placementStart,
                            name = "Group 3",
                        )
                    )
                    tx.createDaycareGroupMessageAccount(groupId3)
                }

            db.transaction { tx ->
                testChild_2.let {
                    insertChild(tx, it, groupId2)
                    tx.insertGuardian(person2.id, it.id)
                }
            }

            assertThrows<BadRequest> {
                postNewThread(
                    user = person2,
                    title = "title",
                    message = "content",
                    children = listOf(testChild_1.id, testChild_2.id),
                    recipients = listOf(group3Account),
                )
            }
        }

        @Test
        fun `citizen can send message to employee when ALL selected children are related to that employee`() {
            db.transaction { tx ->
                testChild_2.let {
                    insertChild(tx, it, groupId2)
                    tx.insertGuardian(person2.id, it.id)
                }
            }

            postNewThread(
                user = person2,
                title = "title",
                message = "content",
                children = listOf(testChild_1.id, testChild_2.id),
                recipients = listOf(employee1Account),
            )

            val threads = getRegularMessageThreads(person2)
            assertEquals(1, threads.size)
            assertEquals("title", threads[0].title)
            assertEquals("content", threads[0].messages[0].content)
        }

        @Test
        fun `citizen cannot send message to employee when only SOME selected children are related to that employee`() {
            db.transaction { tx ->
                testChild_2.let {
                    insertChild(tx, it, groupId2)
                    tx.insertGuardian(person2.id, it.id)
                }
                tx.insertGuardian(person2.id, testChild_8.id)
            }

            assertThrows<BadRequest> {
                postNewThread(
                    user = person2,
                    title = "title",
                    message = "content",
                    children = listOf(testChild_1.id, testChild_8.id),
                    recipients = listOf(employee1Account),
                )
            }
        }

        @Test
        fun `citizen can send message with multiple children when all recipients are valid for ALL children`() {
            db.transaction { tx ->
                testChild_2.let {
                    insertChild(tx, it, groupId2)
                    tx.insertGuardian(person2.id, it.id)
                }
            }

            postNewThread(
                user = person2,
                title = "title",
                message = "content",
                children = listOf(testChild_1.id, testChild_2.id),
                recipients = listOf(employee1Account, group1Account),
            )

            val threads = getRegularMessageThreads(person2)
            assertEquals(1, threads.size)
            assertEquals("title", threads[0].title)
            assertEquals("content", threads[0].messages[0].content)
        }

        @Test
        fun `citizen can send message to groups when all selected children are in same unit`() {
            db.transaction { tx ->
                testChild_2.let {
                    insertChild(tx, it, groupId2)
                    tx.insertGuardian(person2.id, it.id)
                }
            }

            postNewThread(
                user = person2,
                title = "title",
                message = "content",
                children = listOf(testChild_1.id, testChild_2.id),
                recipients = listOf(group1Account, group2Account),
            )

            val threads = getRegularMessageThreads(person2)
            assertEquals(1, threads.size)
            assertEquals("title", threads[0].title)
            assertEquals("content", threads[0].messages[0].content)
        }

        @Test
        fun `citizen can reply to thread with multiple children`() {
            db.transaction { tx ->
                testChild_2.let {
                    insertChild(tx, it, groupId2)
                    tx.insertGuardian(person2.id, it.id)
                }
            }

            val threadId =
                postNewThread(
                    user = person2,
                    title = "Message about both children",
                    message = "content",
                    children = listOf(testChild_1.id, testChild_2.id),
                    recipients = listOf(employee1Account),
                )

            val threads = getRegularMessageThreads(person2)
            assertEquals(1, threads.size)
            assertEquals(threadId, threads[0].id)
            assertEquals(
                setOf(testChild_1.id, testChild_2.id),
                threads[0].children.map { it.childId }.toSet(),
            )

            replyToThread(
                user = person2,
                threadId = threads[0].id,
                recipientAccountIds = setOf(employee1Account),
                content = "reply content",
                now = sendTime.plusSeconds(1),
            )

            val threadsAfterReply = getRegularMessageThreads(person2)
            assertEquals(1, threadsAfterReply.size)
            assertEquals(2, threadsAfterReply[0].messages.size)
            assertEquals("reply content", threadsAfterReply[0].messages[1].content)
            // Verify children association is preserved after reply
            assertEquals(
                setOf(testChild_1.id, testChild_2.id),
                threadsAfterReply[0].children.map { it.childId }.toSet(),
            )
        }

        @Test
        fun `thread with multiple children remains accessible when only some children have active placements`() {
            // Create child 2 with placement ending early
            val child2PlacementEnd = placementStart.plusDays(10)
            db.transaction { tx ->
                tx.insert(testChild_2, DevPersonType.CHILD)
                tx.insertGuardian(person2.id, testChild_2.id)
                val placementId =
                    tx.insert(
                        DevPlacement(
                            childId = testChild_2.id,
                            unitId = testDaycare.id,
                            startDate = placementStart,
                            endDate = child2PlacementEnd,
                        )
                    )
                tx.insert(
                    DevDaycareGroupPlacement(
                        daycarePlacementId = placementId,
                        daycareGroupId = groupId2,
                        startDate = placementStart,
                        endDate = child2PlacementEnd,
                    )
                )
                tx.insert(
                    DevServiceNeed(
                        confirmedBy = employee1.evakaUserId,
                        placementId = placementId,
                        startDate = placementStart,
                        endDate = child2PlacementEnd,
                        optionId = snDefaultDaycare.id,
                        shiftCare = ShiftCareType.NONE,
                    )
                )
            }

            // child 1 already exists with normal placement period (setup in @BeforeEach)
            // person2 is already a guardian of child 1

            // Send message while both placements are active
            val threadId =
                postNewThread(
                    user = person2,
                    title = "Message about both children",
                    message = "content",
                    children = listOf(testChild_1.id, testChild_2.id),
                    recipients = listOf(employee1Account),
                    now = HelsinkiDateTime.of(placementStart.plusDays(5), LocalTime.NOON),
                )

            // After child 2's placement ends but child 1's is still active
            val afterChild2PlacementEnds =
                HelsinkiDateTime.of(child2PlacementEnd.plusDays(1), LocalTime.NOON)

            // Thread should still be accessible for viewing
            val threads = getRegularMessageThreads(person2, now = afterChild2PlacementEnds)
            assertEquals(1, threads.size)
            assertEquals(threadId, threads[0].id)
            // Verify both children are still associated with the thread
            assertEquals(
                setOf(testChild_1.id, testChild_2.id),
                threads[0].children.map { it.childId }.toSet(),
            )
            // Verify the message content is accessible
            assertEquals(1, threads[0].messages.size)
            assertEquals("content", threads[0].messages[0].content)
        }

        @Test
        fun `citizen with multiple children can send message selecting subset of their children`() {
            val child1 =
                DevPerson(
                    dateOfBirth = LocalDate.of(2018, 5, 1),
                    ssn = "010518A9995",
                    firstName = "Child",
                    lastName = "Five",
                )
            val child2 =
                DevPerson(
                    dateOfBirth = LocalDate.of(2018, 6, 1),
                    ssn = "010618A9996",
                    firstName = "Child",
                    lastName = "Six",
                )
            val child3 =
                DevPerson(
                    dateOfBirth = LocalDate.of(2018, 7, 1),
                    ssn = "010718A9997",
                    firstName = "Child",
                    lastName = "Seven",
                )

            db.transaction { tx ->
                insertChild(tx, child1, groupId1)
                tx.insertGuardian(person1.id, child1.id)

                insertChild(tx, child2, groupId1)
                tx.insertGuardian(person1.id, child2.id)

                insertChild(tx, child3, groupId1)
                tx.insertGuardian(person1.id, child3.id)
            }

            val title = "Message with subset of children"
            val content = "This message is sent with only children 1 and 2 selected"

            val threadId =
                postNewThread(
                    user = person1,
                    title = title,
                    message = content,
                    recipients = listOf(group1Account),
                    children = listOf(child1.id, child2.id),
                )

            val employeeThreads = getEmployeeMessageThreads(group1Account, employee1)
            val citizenThreads = getRegularMessageThreads(person1)

            assertEquals(1, employeeThreads.size)
            assertEquals(1, citizenThreads.size)
            assertEquals(threadId, employeeThreads[0].id)
            assertEquals(threadId, citizenThreads[0].id)

            assertEquals(title, employeeThreads[0].title)
            assertEquals(content, employeeThreads[0].messages[0].content)

            assertEquals(title, citizenThreads[0].title)
            assertEquals(content, citizenThreads[0].messages[0].content)

            db.read { tx ->
                val threadChildren =
                    tx.createQuery {
                            sql(
                                "SELECT child_id FROM message_thread_children WHERE thread_id = ${bind(threadId)}"
                            )
                        }
                        .toList<ChildId>()
                assertEquals(2, threadChildren.size)
                assertTrue(threadChildren.contains(child1.id))
                assertTrue(threadChildren.contains(child2.id))
                assertTrue(!threadChildren.contains(child3.id))
            }
        }

        @Test
        fun `citizen cannot reply to a message if the related placement has ended`() {
            db.transaction { tx ->
                testChild_2.let {
                    insertChild(tx, it, groupId2)
                    tx.insertGuardian(person2.id, it.id)
                }
            }
            postNewThread(
                title = "Juhannus",
                message = "Juhannus tulee pian",
                messageType = MessageType.MESSAGE,
                sender = employee1Account,
                recipients = listOf(MessageRecipient.Child(testChild_2.id)),
                user = employee1,
                now = HelsinkiDateTime.of(placementEnd, LocalTime.NOON),
            )
            val threads = getRegularMessageThreads(person2)
            replyToThread(
                user = person2,
                threadId = threads.first().id,
                recipientAccountIds = setOf(employee1Account),
                content = "Viimeisen päivän vastaus",
                now = HelsinkiDateTime.of(placementEnd, LocalTime.NOON).plusMinutes(10),
            )
            assertThrows<Forbidden> {
                replyToThread(
                    user = person2,
                    threadId = threads.first().id,
                    recipientAccountIds = setOf(employee1Account),
                    content = "Se on ohi",
                    now = HelsinkiDateTime.of(placementEnd.plusDays(1), LocalTime.NOON),
                )
            }
        }
    }

    @Nested
    inner class ServiceWorker {

        @Test
        fun `sending a message with a relatedApplicationId creates a note on the application with the contents and a link to the thread`() {
            val applicationId =
                db.transaction { tx ->
                    tx.insertTestApplication(
                        childId = testChild_1.id,
                        guardianId = testAdult_1.id,
                        type = ApplicationType.DAYCARE,
                        document = DaycareFormV0.fromApplication2(validDaycareApplication),
                    )
                }

            val messageContent = "Hakemuksesta puuttuu tietoja, täydennäthän ne"
            // when a message thread related to an application is created
            val messageContentId =
                postNewThread(
                    title = "Hakemuksenne",
                    message = messageContent,
                    messageType = MessageType.MESSAGE,
                    sender = serviceWorkerAccount,
                    recipients = listOf(MessageRecipient.Citizen(testAdult_1.id)),
                    user = serviceWorker.user,
                    relatedApplicationId = applicationId,
                )

            db.transaction { tx ->
                // then a note is created on the application
                val applicationNotes = tx.getApplicationNotes(applicationId)
                assertEquals(1, applicationNotes.size)
                val note = applicationNotes.first()
                assertEquals(messageContent, note.content)

                // and the threadId points to the correct thread
                val messageThreadId =
                    tx.createQuery {
                            sql(
                                "SELECT thread_id FROM message WHERE content_id = ${bind(messageContentId)}"
                            )
                        }
                        .exactlyOne<MessageThreadId>()
                assertEquals(messageThreadId, note.messageThreadId)
            }
        }

        @Test
        fun `the citizen recipient can reply to a message regarding their application, sent by a service worker`() {
            val applicationId =
                db.transaction { tx ->
                    tx.insertTestApplication(
                        childId = testChild_1.id,
                        guardianId = testAdult_1.id,
                        type = ApplicationType.DAYCARE,
                        document = DaycareFormV0.fromApplication2(validDaycareApplication),
                    )
                }

            val messageContent = "Tähän viestiin pitäisi pystyä vastaamaan"
            // when a message thread related to an application is created
            postNewThread(
                title = "Vastaa heti",
                message = messageContent,
                messageType = MessageType.MESSAGE,
                sender = serviceWorkerAccount,
                recipients = listOf(MessageRecipient.Citizen(testAdult_1.id)),
                user = serviceWorker.user,
                relatedApplicationId = applicationId,
            )
            val thread = getRegularMessageThreads(person1)[0]
            replyToThread(
                threadId = thread.id,
                content = "Vastaus",
                recipientAccountIds = setOf(serviceWorkerAccount),
                user = person1,
                now = clock.now(),
            )
            assertEquals(1, unreadMessagesCount(serviceWorkerAccount, serviceWorker.user))
        }

        @Test
        fun `service worker can move a thread to a folder`() {
            val folder1 = DevMessageThreadFolder(owner = serviceWorkerAccount, name = "Eteläinen")
            val folder2 = DevMessageThreadFolder(owner = serviceWorkerAccount, name = "Pohjoinen")
            val folderOther =
                DevMessageThreadFolder(owner = municipalAccount, name = "Toisen käyttäjän kansio")
            db.transaction { tx ->
                tx.insert(folder1)
                tx.insert(folder2)
                tx.insert(folderOther)
            }

            assertEquals(
                setOf(
                    MessageController.MessageThreadFolder(
                        id = folder1.id,
                        ownerId = folder1.owner,
                        name = folder1.name,
                    ),
                    MessageController.MessageThreadFolder(
                        id = folder2.id,
                        ownerId = folder2.owner,
                        name = folder2.name,
                    ),
                ),
                getFolders(serviceWorker.user).toSet(),
            )

            val applicationId =
                db.transaction { tx ->
                    tx.insertTestApplication(
                        childId = testChild_1.id,
                        guardianId = testAdult_1.id,
                        type = ApplicationType.DAYCARE,
                        document = DaycareFormV0.fromApplication2(validDaycareApplication),
                    )
                }

            postNewThread(
                title = "Vastaa heti",
                message = "Tähän viestiin pitäisi pystyä vastaamaan",
                messageType = MessageType.MESSAGE,
                sender = serviceWorkerAccount,
                recipients = listOf(MessageRecipient.Citizen(testAdult_1.id)),
                user = serviceWorker.user,
                relatedApplicationId = applicationId,
                initialFolder = folder1.id,
            )

            assertEquals(
                1,
                getMessagesInFolder(serviceWorkerAccount, folder1.id, serviceWorker.user).data.size,
            )
            val threadId =
                getMessagesInFolder(serviceWorkerAccount, folder1.id, serviceWorker.user)
                    .data
                    .first()
                    .id
            assertEquals(
                0,
                getMessagesInFolder(serviceWorkerAccount, folder2.id, serviceWorker.user).data.size,
            )

            val thread = getRegularMessageThreads(person1)[0]
            replyToThread(
                threadId = thread.id,
                content = "Vastaus",
                recipientAccountIds = setOf(serviceWorkerAccount),
                user = person1,
                now = clock.now(),
            )

            assertEquals(
                UnreadCountByAccount(
                    accountId = serviceWorkerAccount,
                    unreadCount = 0,
                    unreadCopyCount = 0,
                    unreadCountByFolder = mapOf(folder1.id to 1),
                ),
                unreadMessagesCounts(serviceWorkerAccount, serviceWorker.user),
            )
            assertEquals(
                1,
                getMessagesInFolder(serviceWorkerAccount, folder1.id, serviceWorker.user).data.size,
            )
            assertEquals(
                2,
                getMessagesInFolder(serviceWorkerAccount, folder1.id, serviceWorker.user)
                    .data
                    .first()
                    .messages
                    .size,
            )

            moveThreadToFolder(serviceWorkerAccount, threadId, folder2.id, serviceWorker.user)

            assertEquals(
                UnreadCountByAccount(
                    accountId = serviceWorkerAccount,
                    unreadCount = 0,
                    unreadCopyCount = 0,
                    unreadCountByFolder = mapOf(folder2.id to 1),
                ),
                unreadMessagesCounts(serviceWorkerAccount, serviceWorker.user),
            )
            assertEquals(
                0,
                getMessagesInFolder(serviceWorkerAccount, folder1.id, serviceWorker.user).data.size,
            )
            assertEquals(
                1,
                getMessagesInFolder(serviceWorkerAccount, folder2.id, serviceWorker.user).data.size,
            )

            assertThrows<NotFound> {
                moveThreadToFolder(
                    serviceWorkerAccount,
                    threadId,
                    folderOther.id,
                    serviceWorker.user,
                )
            }
        }

        @Test
        fun `service workers cannot send messages without a related application ID`() {
            // when a message thread related to an application is created
            assertThrows<BadRequest> {
                postNewThread(
                    title = "title",
                    message = "content",
                    messageType = MessageType.MESSAGE,
                    sender = serviceWorkerAccount,
                    recipients = listOf(MessageRecipient.Citizen(testAdult_1.id)),
                    user = serviceWorker.user,
                    relatedApplicationId = null,
                )
            }
        }

        @Test
        fun `service workers cannot send messages to children`() {
            assertThrows<BadRequest> {
                postNewThread(
                    title = "title",
                    message = "content",
                    messageType = MessageType.MESSAGE,
                    sender = serviceWorkerAccount,
                    recipients = listOf(MessageRecipient.Child(testChild_1.id)),
                    user = serviceWorker.user,
                    relatedApplicationId = ApplicationId(UUID.randomUUID()),
                )
            }
        }

        @Test
        fun `service workers cannot send messages to areas`() {
            assertThrows<BadRequest> {
                postNewThread(
                    title = "title",
                    message = "content",
                    messageType = MessageType.MESSAGE,
                    sender = serviceWorkerAccount,
                    recipients = listOf(MessageRecipient.Area(testArea.id)),
                    user = serviceWorker.user,
                    relatedApplicationId = ApplicationId(UUID.randomUUID()),
                )
            }
        }

        @Test
        fun `service workers cannot send messages to units`() {
            assertThrows<BadRequest> {
                postNewThread(
                    title = "title",
                    message = "content",
                    messageType = MessageType.MESSAGE,
                    sender = serviceWorkerAccount,
                    recipients = listOf(MessageRecipient.Unit(testDaycare.id)),
                    user = serviceWorker.user,
                    relatedApplicationId = ApplicationId(UUID.randomUUID()),
                )
            }
        }

        @Test
        fun `service workers cannot send messages to groups`() {
            assertThrows<BadRequest> {
                postNewThread(
                    title = "title",
                    message = "content",
                    messageType = MessageType.MESSAGE,
                    sender = serviceWorkerAccount,
                    recipients = listOf(MessageRecipient.Group(groupId1)),
                    user = serviceWorker.user,
                    relatedApplicationId = ApplicationId(UUID.randomUUID()),
                )
            }
        }

        @Test
        fun `non-service worker accounts cannot have a related application`() {
            val applicationId =
                db.transaction { tx ->
                    tx.insertTestApplication(
                        childId = testChild_1.id,
                        guardianId = testAdult_1.id,
                        type = ApplicationType.DAYCARE,
                        document = DaycareFormV0.fromApplication2(validDaycareApplication),
                    )
                }

            assertThrows<BadRequest> {
                postNewThread(
                    title = "title",
                    message = "content",
                    messageType = MessageType.MESSAGE,
                    sender = employee1Account,
                    recipients = listOf(MessageRecipient.Child(testChild_1.id)),
                    user = employee1,
                    relatedApplicationId = applicationId,
                )
            }
        }

        @Test
        fun `service workers cannot send messages to a citizen who has not sent the application identified by related application id`() {
            val applicationId =
                db.transaction { tx ->
                    tx.insertTestApplication(
                        childId = testChild_1.id,
                        guardianId = testAdult_1.id,
                        type = ApplicationType.DAYCARE,
                        document = DaycareFormV0.fromApplication2(validDaycareApplication),
                    )
                }

            // when a message thread related to an application is created
            assertThrows<BadRequest> {
                postNewThread(
                    title = "title",
                    message = "content",
                    messageType = MessageType.MESSAGE,
                    sender = serviceWorkerAccount,
                    recipients = listOf(MessageRecipient.Citizen(testAdult_2.id)),
                    user = serviceWorker.user,
                    relatedApplicationId = applicationId,
                )
            }
        }
    }

    @Nested
    inner class ThreadLogic {

        @Test
        fun `a thread is created, accessed and replied to by participants who are guardian of the same child`() {
            // when a message thread is created
            postNewThread(
                title = "Juhannus",
                message = "Juhannus tulee pian",
                messageType = MessageType.MESSAGE,
                sender = employee1Account,
                recipients = listOf(MessageRecipient.Child(testChild_1.id, false)),
                user = employee1,
                now = sendTime,
            )

            // then sender does not see it in received messages
            assertEquals(listOf(), getEmployeeMessageThreads(employee1Account, employee1))

            // then recipient can see it in received messages
            val threadWithOneReply = getRegularMessageThreads(person1)[0]
            assertEquals("Juhannus", threadWithOneReply.title)
            assertEquals(MessageType.MESSAGE, threadWithOneReply.messageType)
            assertEquals(
                listOf(Pair(employee1Account, "Juhannus tulee pian")),
                threadWithOneReply.toSenderContentPairs(),
            )

            // when
            val replyTime = sendTime.plusSeconds(1)
            replyToThread(
                person1,
                threadWithOneReply.id,
                setOf(employee1Account, person2Account),
                "No niinpä näyttää tulevan",
                now = replyTime,
            )

            // then recipients see the same data
            val person1Threads = getRegularMessageThreads(person1)
            assertEquals(
                listOf(replyTime, null),
                person1Threads.flatMap { thread ->
                    thread.messages
                        .sortedBy { message -> message.sentAt }
                        .map { message -> message.readAt }
                },
            )
            val person1ThreadsWithoutReadAt =
                person1Threads.map { thread ->
                    thread.copy(
                        messages = thread.messages.map { message -> message.copy(readAt = null) }
                    )
                }
            val person2Threads = getRegularMessageThreads(person2)
            assertEquals(person1ThreadsWithoutReadAt, person2Threads)
            assertEquals(
                getEmployeeMessageThreads(employee1Account, employee1).map { messageThread ->
                    CitizenMessageThread.Regular(
                        messageThread.id,
                        messageThread.urgent,
                        messageThread.children,
                        messageThread.type,
                        messageThread.title,
                        messageThread.sensitive,
                        messageThread.isCopy,
                        messageThread.applicationStatus,
                        messageThread.messages,
                    )
                },
                person2Threads,
            )

            // then thread has both messages in correct order
            assertEquals(1, person2Threads.size)
            val person2Thread = person2Threads[0]
            assertEquals("Juhannus", person2Thread.title)
            assertEquals(
                listOf(
                    Pair(employee1Account, "Juhannus tulee pian"),
                    Pair(person1Account, "No niinpä näyttää tulevan"),
                ),
                person2Thread.toSenderContentPairs(),
            )

            // when person one replies to the employee only
            replyToThread(
                person1,
                person2Thread.id,
                setOf(employee1Account),
                "person 2 does not see this",
                now = sendTime.plusSeconds(2),
            )

            // then person one and employee see the new message
            val threadContentWithTwoReplies =
                listOf(
                    Pair(employee1Account, "Juhannus tulee pian"),
                    Pair(person1Account, "No niinpä näyttää tulevan"),
                    Pair(person1Account, "person 2 does not see this"),
                )
            assertEquals(
                threadContentWithTwoReplies,
                getRegularMessageThreads(person1)[0].toSenderContentPairs(),
            )
            assertEquals(
                threadContentWithTwoReplies,
                getEmployeeMessageThreads(employee1Account, employee1)[0].toSenderContentPairs(),
            )

            // then person two does not see the message
            assertEquals(
                listOf(
                    Pair(employee1Account, "Juhannus tulee pian"),
                    Pair(person1Account, "No niinpä näyttää tulevan"),
                ),
                getRegularMessageThreads(person2)[0].toSenderContentPairs(),
            )

            // when author replies to person two
            replyToThread(
                user = employee1,
                sender = employee1Account,
                threadId = threadWithOneReply.id,
                recipientAccountIds = setOf(person2Account),
                content = "person 1 does not see this",
                now = sendTime.plusSeconds(3),
            )

            // then person two sees that
            assertEquals(
                listOf(
                    Pair(employee1Account, "Juhannus tulee pian"),
                    Pair(person1Account, "No niinpä näyttää tulevan"),
                    Pair(employee1Account, "person 1 does not see this"),
                ),
                getRegularMessageThreads(person2)[0].toSenderContentPairs(),
            )

            // then person one does not see that
            assertEquals(
                threadContentWithTwoReplies,
                getRegularMessageThreads(person1)[0].toSenderContentPairs(),
            )

            // then employee sees all the messages
            assertEquals(
                listOf(
                    Pair(employee1Account, "Juhannus tulee pian"),
                    Pair(person1Account, "No niinpä näyttää tulevan"),
                    Pair(person1Account, "person 2 does not see this"),
                    Pair(employee1Account, "person 1 does not see this"),
                ),
                getEmployeeMessageThreads(employee1Account, employee1)[0].toSenderContentPairs(),
            )

            // then employee can see all sent messages
            assertEquals(
                listOf("person 1 does not see this", "Juhannus tulee pian"),
                getSentMessages(employee1Account, employee1).map { it.content },
            )
        }

        @Test
        fun `a message is split to several threads by guardianship`() {
            // when a new thread is created to several recipients who do not all have common
            // children
            val title = "Thread splitting"
            val content = "This message is sent to several participants and split to threads"
            val recipients =
                listOf(
                    MessageRecipient.Child(testChild_1.id),
                    MessageRecipient.Child(testChild_4.id),
                    MessageRecipient.Child(testChild_6.id),
                )
            val recipientNames = listOf("Hippiäiset", "Jani")
            postNewThread(
                title = title,
                message = content,
                messageType = MessageType.MESSAGE,
                sender = employee1Account,
                recipients = recipients,
                recipientNames = recipientNames,
                user = employee1,
            )

            // then three threads should be created
            db.read {
                assertEquals(
                    1,
                    it.createQuery { sql("SELECT COUNT(id) FROM message_content") }
                        .exactlyOne<Int>(),
                )
                assertEquals(
                    3,
                    it.createQuery { sql("SELECT COUNT(id) FROM message_thread") }.exactlyOne<Int>(),
                )
                assertEquals(
                    3,
                    it.createQuery { sql("SELECT COUNT(id) FROM message") }.exactlyOne<Int>(),
                )
                assertEquals(
                    5,
                    it.createQuery { sql("SELECT COUNT(id) FROM message_recipients") }
                        .exactlyOne<Int>(),
                )
            }

            // then sent message is shown as one
            val sentMessages = db.read { it.getMessagesSentByAccount(employee1Account, 10, 1) }
            assertEquals(1, sentMessages.total)
            assertEquals(1, sentMessages.data.size)
            assertEquals(recipientNames, sentMessages.data.flatMap { it.recipientNames })
            assertEquals(title, sentMessages.data[0].threadTitle)
            assertEquals(MessageType.MESSAGE, sentMessages.data[0].type)
            assertEquals(content, sentMessages.data[0].content)

            // then threads are grouped properly
            // person 1 and 2: common child
            // person 2 and 3: common child
            // person 4: no child
            val person1Threads = getRegularMessageThreads(person1)
            val person2Threads = getRegularMessageThreads(person2)
            val person3Threads = getRegularMessageThreads(person3)
            val person4Threads = getRegularMessageThreads(person4)
            val person5Threads = getRegularMessageThreads(person5)

            assertEquals(1, person1Threads.size)
            assertEquals(1, person2Threads.size)
            assertEquals(1, person3Threads.size)
            assertEquals(1, person4Threads.size)
            assertEquals(1, person5Threads.size)
            assertEquals(person1Threads, person2Threads)
            assertEquals(person3Threads, person5Threads)
            assertNotEquals(person1Threads, person3Threads)
            assertNotEquals(person1Threads, person4Threads)
            assertNotEquals(person3Threads, person4Threads)

            val allThreads =
                listOf(
                        person1Threads,
                        person2Threads,
                        person3Threads,
                        person4Threads,
                        person5Threads,
                    )
                    .flatten()
            assertEquals(5, allThreads.size)
            allThreads.forEach {
                assertEquals(title, it.title)
                assertEquals(content, it.messages[0].content)
            }

            // when person 1 replies to thread
            val replyTime = sendTime.plusMinutes(5)
            replyToThread(
                person1,
                person1Threads.first().id,
                setOf(employee1Account, person2Account),
                "Hello",
                replyTime,
            )

            // then only the participants should get the message
            val employeeThreads = getEmployeeMessageThreads(employee1Account, employee1)
            assertEquals(
                listOf(Pair(employee1Account, content), Pair(person1Account, "Hello")),
                employeeThreads.map { it.toSenderContentPairs() }.flatten(),
            )
            val person1ThreadsAfterReply = getRegularMessageThreads(person1)
            assertEquals(
                listOf(replyTime, null),
                person1ThreadsAfterReply.flatMap { thread ->
                    thread.messages
                        .sortedBy { message -> message.sentAt }
                        .map { message -> message.readAt }
                },
            )
            val person1ThreadsWithoutReadAt =
                person1ThreadsAfterReply.map { thread ->
                    thread.copy(
                        messages = thread.messages.map { message -> message.copy(readAt = null) }
                    )
                }
            assertEquals(
                employeeThreads.map { messageThread ->
                    CitizenMessageThread.Regular(
                        messageThread.id,
                        messageThread.urgent,
                        messageThread.children,
                        messageThread.type,
                        messageThread.title,
                        messageThread.sensitive,
                        messageThread.isCopy,
                        messageThread.applicationStatus,
                        messageThread.messages,
                    )
                },
                person1ThreadsWithoutReadAt,
            )
            assertEquals(
                listOf(Pair(employee1Account, content), Pair(person1Account, "Hello")),
                getRegularMessageThreads(person2).map { it.toSenderContentPairs() }.flatten(),
            )

            assertEquals(person3Threads, getRegularMessageThreads(person3))
            assertEquals(person4Threads, getRegularMessageThreads(person4))
        }

        @Test
        fun `message to multiple children with different guardians creates separate threads per guardian group`() {
            val newChild1 =
                DevPerson(
                    dateOfBirth = LocalDate.of(2018, 1, 1),
                    ssn = "010118A9991",
                    firstName = "Child",
                    lastName = "One",
                )
            val newChild2 =
                DevPerson(
                    dateOfBirth = LocalDate.of(2018, 2, 1),
                    ssn = "010218A9992",
                    firstName = "Child",
                    lastName = "Two",
                )

            db.transaction { tx ->
                insertChild(tx, newChild1, groupId1)
                tx.insertGuardian(person1.id, newChild1.id)

                insertChild(tx, newChild2, groupId1)
                tx.insertGuardian(person2.id, newChild2.id)
            }

            val title = "Message to children with different guardians"
            val content = "This message should create separate threads"
            postNewThread(
                title = title,
                message = content,
                messageType = MessageType.MESSAGE,
                sender = employee1Account,
                recipients =
                    listOf(
                        MessageRecipient.Child(newChild1.id),
                        MessageRecipient.Child(newChild2.id),
                    ),
                recipientNames = listOf(testAdult_1.firstName, testAdult_2.firstName),
                user = employee1,
            )

            db.read {
                assertEquals(
                    2,
                    it.createQuery {
                            sql(
                                "SELECT COUNT(DISTINCT id) FROM message_thread WHERE title = ${bind(title)}"
                            )
                        }
                        .exactlyOne<Int>(),
                )
            }

            val guardian1Threads = getRegularMessageThreads(person1)
            val guardian2Threads = getRegularMessageThreads(person2)

            assertEquals(1, guardian1Threads.size)
            assertEquals(1, guardian2Threads.size)
            assertNotEquals(guardian1Threads[0].id, guardian2Threads[0].id)

            assertEquals(title, guardian1Threads[0].title)
            assertEquals(content, guardian1Threads[0].messages[0].content)

            assertEquals(title, guardian2Threads[0].title)
            assertEquals(content, guardian2Threads[0].messages[0].content)
        }

        @Test
        fun `message to multiple children with overlapping guardians groups them correctly`() {
            val child1 =
                DevPerson(
                    dateOfBirth = LocalDate.of(2018, 3, 1),
                    ssn = "010318A9993",
                    firstName = "Child",
                    lastName = "Three",
                )
            val child2 =
                DevPerson(
                    dateOfBirth = LocalDate.of(2018, 4, 1),
                    ssn = "010418A9994",
                    firstName = "Child",
                    lastName = "Four",
                )

            db.transaction { tx ->
                insertChild(tx, child1, groupId1)
                tx.insertGuardian(person1.id, child1.id)
                tx.insertGuardian(person2.id, child1.id)

                insertChild(tx, child2, groupId1)
                tx.insertGuardian(person2.id, child2.id)
                tx.insertGuardian(person3.id, child2.id)
            }

            val title = "Message to children with overlapping guardians"
            val content = "This message tests guardian grouping"
            postNewThread(
                title = title,
                message = content,
                messageType = MessageType.MESSAGE,
                sender = employee1Account,
                recipients =
                    listOf(MessageRecipient.Child(child1.id), MessageRecipient.Child(child2.id)),
                recipientNames =
                    listOf(testAdult_1.firstName, testAdult_2.firstName, testAdult_3.firstName),
                user = employee1,
            )

            db.read {
                assertEquals(
                    2,
                    it.createQuery {
                            sql(
                                "SELECT COUNT(DISTINCT id) FROM message_thread WHERE title = ${bind(title)}"
                            )
                        }
                        .exactlyOne<Int>(),
                )
            }

            val guardianAThreads = getRegularMessageThreads(person1)
            val guardianBThreads = getRegularMessageThreads(person2)
            val guardianCThreads = getRegularMessageThreads(person3)

            assertEquals(1, guardianAThreads.size)
            assertEquals(2, guardianBThreads.size)
            assertEquals(1, guardianCThreads.size)

            assertEquals(title, guardianAThreads[0].title)
            assertEquals(content, guardianAThreads[0].messages[0].content)

            assertEquals(title, guardianBThreads[0].title)
            assertEquals(content, guardianBThreads[0].messages[0].content)
            assertEquals(title, guardianBThreads[1].title)
            assertEquals(content, guardianBThreads[1].messages[0].content)
            assertNotEquals(guardianBThreads[0].id, guardianBThreads[1].id)

            assertEquals(title, guardianCThreads[0].title)
            assertEquals(content, guardianCThreads[0].messages[0].content)

            val guardianBThreadIds = setOf(guardianBThreads[0].id, guardianBThreads[1].id)
            assertTrue(guardianBThreadIds.contains(guardianAThreads[0].id))
            assertTrue(guardianBThreadIds.contains(guardianCThreads[0].id))
            assertNotEquals(guardianAThreads[0].id, guardianCThreads[0].id)
        }
    }

    @Nested
    inner class RecipientFiltering {

        @Test
        fun `recipient list can be filtered by yearsOfBirth`() {
            postNewThread(
                title = "Vappu",
                message = "Vappuna paistaa aurinko",
                messageType = MessageType.BULLETIN,
                sender = municipalAccount,
                recipients =
                    listOf(
                        MessageRecipient.Child(testChild_1.id),
                        MessageRecipient.Child(testChild_3.id),
                        MessageRecipient.Child(testChild_4.id),
                        MessageRecipient.Child(testChild_6.id),
                        MessageRecipient.Child(testChild_8.id),
                    ),
                filters = MessageController.PostMessageFilters(yearsOfBirth = listOf(2017)),
                user = messager.user,
                now = sendTime,
            )

            db.read {
                assertEquals(
                    2,
                    it.createQuery { sql("SELECT COUNT(id) FROM message_recipients") }
                        .exactlyOne<Int>(),
                )
            }
        }

        @Test
        fun `recipient list can be filtered by shiftCare`() {
            postNewThread(
                title = "Vappu",
                message = "Vappuna paistaa aurinko",
                messageType = MessageType.BULLETIN,
                sender = municipalAccount,
                recipients =
                    listOf(
                        MessageRecipient.Child(testChild_1.id),
                        MessageRecipient.Child(testChild_3.id),
                        MessageRecipient.Child(testChild_4.id),
                        MessageRecipient.Child(testChild_6.id),
                        MessageRecipient.Child(testChild_8.id),
                    ),
                filters =
                    MessageController.PostMessageFilters(
                        shiftCare = true,
                        intermittentShiftCare = true,
                    ),
                user = messager.user,
                now = sendTime,
            )

            db.read {
                assertEquals(
                    3,
                    it.createQuery { sql("SELECT COUNT(id) FROM message_recipients") }
                        .exactlyOne<Int>(),
                )
            }
        }

        @Test
        fun `recipient list can be filtered by familyDaycare`() {
            postNewThread(
                title = "Vappu",
                message = "Vappuna paistaa aurinko",
                messageType = MessageType.BULLETIN,
                sender = municipalAccount,
                recipients =
                    listOf(
                        MessageRecipient.Child(testChild_1.id),
                        MessageRecipient.Child(testChild_3.id),
                        MessageRecipient.Child(testChild_4.id),
                        MessageRecipient.Child(testChild_6.id),
                        MessageRecipient.Child(testChild_8.id),
                    ),
                filters = MessageController.PostMessageFilters(familyDaycare = true),
                user = messager.user,
                now = sendTime,
            )

            db.read {
                assertEquals(
                    2,
                    it.createQuery { sql("SELECT COUNT(id) FROM message_recipients") }
                        .exactlyOne<Int>(),
                )
            }
        }

        @Test
        fun `preflight check takes into account recipient filtering`() {
            val response =
                postNewThreadPreflightCheck(
                    user = messager.user,
                    sender = municipalAccount,
                    recipients =
                        listOf(
                            MessageRecipient.Child(testChild_1.id),
                            MessageRecipient.Child(testChild_3.id),
                        ),
                    filters = MessageController.PostMessageFilters(yearsOfBirth = listOf(2018)),
                )
            assertEquals(PostMessagePreflightResponse(numberOfRecipientAccounts = 2), response)
        }

        @Test
        fun `create messages preflight check returns recipient count`() {
            val response =
                postNewThreadPreflightCheck(
                    user = employee1,
                    sender = employee1Account,
                    recipients =
                        listOf(
                            MessageRecipient.Child(testChild_1.id),
                            MessageRecipient.Child(testChild_3.id),
                        ),
                )
            assertEquals(PostMessagePreflightResponse(numberOfRecipientAccounts = 3), response)
        }

        @Test
        fun `getMessageCopiesByAccount filters recipient names by group names`() {
            // Test with STAFF employee assigned to group1 but not group2
            val staffEmployee = DevEmployee(firstName = "Staff", lastName = "Member")
            val staffUser =
                db.transaction { tx ->
                    tx.insert(staffEmployee)
                    tx.insertDaycareAclRow(testDaycare.id, staffEmployee.id, UserRole.STAFF)
                    tx.syncDaycareGroupAcl(
                        testDaycare.id,
                        staffEmployee.id,
                        listOf(groupId1),
                        readTime.minusMonths(1),
                    )
                    staffEmployee.user
                }

            val allRecipientNames =
                listOf(
                    "Testiläiset", // Group 1 name
                    "Group 2", // Group 2 name
                )

            // Send bulletin to both groups
            postNewThread(
                title = "Bulletin to Both Groups",
                message = "This bulletin is sent to both groups",
                messageType = MessageType.BULLETIN,
                sender = employee1Account,
                recipients =
                    listOf(MessageRecipient.Group(groupId1), MessageRecipient.Group(groupId2)),
                recipientNames = allRecipientNames,
                user = employee1,
                now = sendTime,
            )

            val staffCopies = getMessageCopies(staffUser, group1Account, readTime)

            assertEquals(1, staffCopies.size)
            val staffCopy = staffCopies.first()

            // Staff should only see group1 name in recipientNames
            assertEquals(listOf("Testiläiset"), staffCopy.recipientNames)
        }

        @Test
        fun `getMessageCopiesByAccount prefers unit name over group names in recipient names`() {
            // Test with STAFF employee assigned to group1
            val staffEmployee = DevEmployee(firstName = "Staff", lastName = "Member")
            val staffUser =
                db.transaction { tx ->
                    tx.insert(staffEmployee)
                    tx.insertDaycareAclRow(testDaycare.id, staffEmployee.id, UserRole.STAFF)
                    tx.syncDaycareGroupAcl(
                        testDaycare.id,
                        staffEmployee.id,
                        listOf(groupId1),
                        readTime.minusMonths(1),
                    )
                    staffEmployee.user
                }

            val allRecipientNames =
                listOf(
                    "Test Daycare" // Unit name
                )

            // Send bulletin to unit
            postNewThread(
                title = "Bulletin to whole unit",
                message = "This bulletin is sent to whole unit",
                messageType = MessageType.BULLETIN,
                sender = employee1Account,
                recipients = listOf(MessageRecipient.Unit(testDaycare.id)),
                recipientNames = allRecipientNames,
                user = employee1,
                now = sendTime,
            )

            val staffCopies = getMessageCopies(staffUser, group1Account, readTime)

            assertEquals(1, staffCopies.size)
            val staffCopy = staffCopies.first()

            // Staff should only see unit name in recipientNames
            assertEquals(listOf("Test Daycare"), staffCopy.recipientNames)
        }

        @Test
        fun `getMessageCopiesByAccount prefers area name over other names in recipient names`() {
            // Test with STAFF employee assigned to group1
            val staffEmployee = DevEmployee(firstName = "Staff", lastName = "Member")
            val staffUser =
                db.transaction { tx ->
                    tx.insert(staffEmployee)
                    tx.insertDaycareAclRow(testDaycare.id, staffEmployee.id, UserRole.STAFF)
                    tx.syncDaycareGroupAcl(
                        testDaycare.id,
                        staffEmployee.id,
                        listOf(groupId1),
                        readTime.minusMonths(1),
                    )
                    staffEmployee.user
                }

            val allRecipientNames =
                listOf(
                    "Test Area" // Area name
                )

            // Send bulletin to area
            postNewThread(
                title = "Bulletin to whole area",
                message = "This bulletin is sent to whole area",
                messageType = MessageType.BULLETIN,
                sender = municipalAccount,
                recipients = listOf(MessageRecipient.Area(testArea.id)),
                recipientNames = allRecipientNames,
                user = messager.user,
                now = sendTime,
            )

            val staffCopies = getMessageCopies(staffUser, group1Account, readTime)

            assertEquals(1, staffCopies.size)
            val staffCopy = staffCopies.first()

            // Staff should only see area name in recipientNames
            assertEquals(listOf("Test Area"), staffCopy.recipientNames)
        }

        @ParameterizedTest(name = "replyDaysAgo={0}, userRole={1}")
        @MethodSource("fi.espoo.evaka.messaging.MessageIntegrationTest#groupAccountAccessParams")
        fun `group account inbox visibility respects group assignment date`(
            replyDaysAgo: Int,
            userRole: UserRole,
            isVisible: Boolean,
        ) {
            val newEmployee = prepareGroupAccountAccessTest(userRole, clock.now())
            postNewThread(
                person2,
                "title",
                "content",
                listOf(group1Account, employee1Account),
                listOf(testChild_2.id),
                now = clock.now().minusWeeks(3),
            )
            val thread2 = getRegularMessageThreads(person2).first()
            replyToThread(
                employee1,
                employee1Account,
                thread2.id,
                setOf(person2Account, group1Account),
                "reply",
                now = clock.now().minusDays(replyDaysAgo.toLong()),
            )

            val newEmployeeThreads =
                getEmployeeMessageThreads(group1Account, newEmployee, now = clock.now())
            assertEquals(if (isVisible) 1 else 0, newEmployeeThreads.size)
        }

        @ParameterizedTest(name = "sentDaysAgo={0}, userRole={1}")
        @MethodSource("fi.espoo.evaka.messaging.MessageIntegrationTest#groupAccountAccessParams")
        fun `sent message visibility respects group assignment date`(
            sentDaysAgo: Int,
            userRole: UserRole,
            isVisible: Boolean,
        ) {
            val newEmployee = prepareGroupAccountAccessTest(userRole, clock.now())
            postNewThread(
                "title",
                "content",
                MessageType.MESSAGE,
                group1Account,
                listOf(MessageRecipient.Child(testChild_2.id)),
                user = employee1,
                now = clock.now().minusDays(sentDaysAgo.toLong()),
            )

            val sentMessages = getSentMessages(group1Account, newEmployee)
            assertEquals(if (isVisible) 1 else 0, sentMessages.size)
        }

        @ParameterizedTest(name = "updatedDaysAgo={0}, userRole={1}")
        @MethodSource("fi.espoo.evaka.messaging.MessageIntegrationTest#groupAccountAccessParams")
        fun `draft message visibility respects group assignment date`(
            updatedDaysAgo: Int,
            userRole: UserRole,
            isVisible: Boolean,
        ) {
            val newEmployee = prepareGroupAccountAccessTest(userRole, clock.now())

            createDraft(
                employee1,
                group1Account,
                "title",
                "content",
                setOf(DraftRecipient(person2Account, false)),
                emptyList(),
                clock.now().minusDays(updatedDaysAgo.toLong()),
            )

            val drafts = getDrafts(newEmployee, group1Account, clock.now())
            assertEquals(if (isVisible) 1 else 0, drafts.size)
        }

        @ParameterizedTest(name = "copyDaysAgo={0}, userRole={1}")
        @MethodSource("fi.espoo.evaka.messaging.MessageIntegrationTest#groupAccountAccessParams")
        fun `message copy visibility respects group assignment date`(
            copyDaysAgo: Int,
            userRole: UserRole,
            isVisible: Boolean,
        ) {
            val newEmployee = prepareGroupAccountAccessTest(userRole, clock.now())

            postNewThread(
                "title",
                "content",
                MessageType.BULLETIN,
                employee1Account,
                listOf(MessageRecipient.Child(testChild_2.id), MessageRecipient.Group(groupId1)),
                user = employee1,
                now = clock.now().minusDays(copyDaysAgo.toLong()),
            )

            val copiedThreads = getMessageCopies(newEmployee, group1Account, clock.now())
            assertEquals(if (isVisible) 1 else 0, copiedThreads.size)
        }

        @ParameterizedTest(name = "messagesDaysAgo={0}, userRole={1}")
        @MethodSource("fi.espoo.evaka.messaging.MessageIntegrationTest#groupAccountAccessParams")
        fun `archived message visibility respects group assignment date`(
            messagesDaysAgo: Int,
            userRole: UserRole,
            isVisible: Boolean,
        ) {
            val newEmployee = prepareGroupAccountAccessTest(userRole, clock.now())
            val threadId =
                postNewThread(
                    person2,
                    "title",
                    "content",
                    listOf(group1Account, employee1Account),
                    listOf(testChild_2.id),
                    now = clock.now().minusDays(messagesDaysAgo.toLong()),
                )

            archiveThread(employee1, group1Account, threadId, now = clock.now().minusDays(1))

            val archivedThreads =
                getEmployeeArchivedThreads(group1Account, newEmployee, now = clock.now())
            assertEquals(if (isVisible) 1 else 0, archivedThreads.size)
        }
    }

    @Nested
    inner class StatusAndAccess {

        @Test
        fun `messages can be marked read`() {
            // when a message thread is created
            postNewThread(
                title = "t1",
                message = "m1",
                messageType = MessageType.MESSAGE,
                sender = employee1Account,
                recipients = listOf(MessageRecipient.Child(testChild_1.id)),
                user = employee1,
            )

            // then
            val person1UnreadMessages = getUnreadReceivedMessages(person1Account, person1)
            assertEquals(1, person1UnreadMessages.size)
            assertEquals(1, getUnreadReceivedMessages(person2Account, person2).size)
            assertEquals(0, getUnreadReceivedMessages(employee1Account, employee1).size)

            // when a person replies to the thread
            replyToThread(
                person1,
                person1UnreadMessages.first().threadId,
                setOf(employee1Account, person2Account),
                "reply",
            )

            // then
            assertEquals(1, getUnreadReceivedMessages(employee1Account, employee1).size)
            assertEquals(0, getUnreadReceivedMessages(person1Account, person1).size)
            assertEquals(2, getUnreadReceivedMessages(person2Account, person2).size)

            // when a thread is marked read
            markThreadRead(person2, getRegularMessageThreads(person2).first().id)

            // then the thread is marked read
            assertEquals(1, getUnreadReceivedMessages(employee1Account, employee1).size)
            assertEquals(0, getUnreadReceivedMessages(person1Account, person1).size)
            assertEquals(0, getUnreadReceivedMessages(person2Account, person2).size)
        }

        @Test
        fun `unread message counts and marking messages read`() {
            postNewThread(
                title = "Juhannus",
                message = "Juhannus tulee pian",
                messageType = MessageType.MESSAGE,
                sender = employee1Account,
                recipients = listOf(MessageRecipient.Child(testChild_1.id)),
                user = employee1,
            )
            assertEquals(0, unreadMessagesCount(employee1Account, employee1))
            assertEquals(1, unreadMessagesCount(person1))
            assertEquals(1, unreadMessagesCount(person2))

            // citizen reads the message
            markThreadRead(person1, getRegularMessageThreads(person1).first().id)
            assertEquals(0, unreadMessagesCount(employee1Account, employee1))
            assertEquals(0, unreadMessagesCount(person1))
            assertEquals(1, unreadMessagesCount(person2))

            // thread is replied
            replyToThread(
                user = person1,
                threadId = getRegularMessageThreads(person1).first().id,
                recipientAccountIds = setOf(person2Account, employee1Account),
                content = "Juhannus on jo ohi",
            )
            assertEquals(1, unreadMessagesCount(employee1Account, employee1))
            assertEquals(0, unreadMessagesCount(person1))
            assertEquals(2, unreadMessagesCount(person2))
        }

        @Test
        fun `last message in a group thread can be marked as unread by an employee`() {
            val messageThreadId =
                postNewThread(
                    user = person1,
                    title = "t1",
                    message = "m1",
                    recipients = listOf(group1Account),
                    children = listOf(testChild_1.id),
                )

            val unreadMessagesBefore = getUnreadReceivedMessages(group1Account, employee1)
            assertEquals(1, unreadMessagesBefore.size)

            markThreadRead(employee1, group1Account, messageThreadId, clock.now())
            assertEquals(0, getUnreadReceivedMessages(group1Account, employee1).size)

            markLastMessageInThreadUnread(employee1, group1Account, messageThreadId, clock.now())
            assertEquals(1, getUnreadReceivedMessages(group1Account, employee1).size)
        }

        @Test
        fun `citizen can mark last message unread`() {
            postNewThread(
                title = "t2",
                message = "m2",
                messageType = MessageType.MESSAGE,
                sender = employee1Account,
                recipients = listOf(MessageRecipient.Child(testChild_1.id)),
                user = employee1,
            )
            val person1UnreadMessages = getUnreadReceivedMessages(person1Account, person1)
            assertEquals(1, person1UnreadMessages.size)

            markThreadRead(person1, getRegularMessageThreads(person1).first().id)
            assertEquals(0, getUnreadReceivedMessages(person1Account, person1).size)

            markLastMessageUnread(person1, person1UnreadMessages.first().threadId, clock.now())
            assertEquals(1, getUnreadReceivedMessages(person1Account, person1).size)
        }

        @Test
        fun `unread message count excludes messages beyond employee access limit`() {
            val aclCreationDate = placementStart
            val area = DevCareArea(shortName = "testArea")
            val unit =
                DevDaycare(areaId = area.id, enabledPilotFeatures = setOf(PilotFeature.MESSAGING))
            val group =
                DevDaycareGroup(daycareId = unit.id, startDate = placementStart.minusYears(1))
            val employee = DevEmployee()
            val employeeUser =
                AuthenticatedUser.Employee(id = employee.id, roles = setOf(UserRole.STAFF))
            val admin = DevEmployee(roles = setOf(UserRole.ADMIN))
            val adminUser = AuthenticatedUser.Employee(id = admin.id, roles = setOf(UserRole.ADMIN))
            val daycareGroupAcl =
                DevDaycareGroupAcl(
                    groupId = group.id,
                    employeeId = employee.id,
                    created = HelsinkiDateTime.of(aclCreationDate, LocalTime.of(12, 0)),
                )
            val adult = DevPerson()
            val adultUser = AuthenticatedUser.Citizen(adult.id, CitizenAuthLevel.STRONG)
            val child = DevPerson()
            db.transaction { tx ->
                tx.insert(area)
                tx.insert(unit)
                tx.insert(group)
                tx.insert(employee)
                tx.insert(admin)
                tx.insert(daycareGroupAcl)
                tx.insert(adult, DevPersonType.ADULT)
                tx.insert(child, DevPersonType.CHILD)
                tx.insert(DevGuardian(adult.id, child.id))
                val placementId =
                    tx.insert(
                        DevPlacement(
                            childId = child.id,
                            unitId = unit.id,
                            startDate = placementStart.minusMonths(5),
                            endDate = placementEnd,
                        )
                    )
                tx.insert(
                    DevDaycareGroupPlacement(
                        daycarePlacementId = placementId,
                        daycareGroupId = group.id,
                        startDate = placementStart.minusMonths(5),
                        endDate = placementEnd,
                    )
                )
            }

            val groupAccount =
                db.transaction { tx -> tx.createDaycareGroupMessageAccount(group.id) }

            // Message thread beyond employee access limit (1 week before daycare group acl
            // creation)
            postNewThread(
                adultUser,
                "Juhannus",
                "Juhannus tulee pian",
                listOf(groupAccount),
                listOf(child.id),
                now = HelsinkiDateTime.of(aclCreationDate.minusDays(8), LocalTime.of(12, 11)),
            )

            // Bulletin thread beyond employee access limit
            postNewThread(
                title = "Juhannus",
                message = "Juhannus tulee pian",
                messageType = MessageType.BULLETIN,
                sender = municipalAccount,
                recipients = listOf(MessageRecipient.Area(area.id)),
                user = adminUser,
                now = HelsinkiDateTime.of(aclCreationDate.minusDays(8), LocalTime.of(12, 11)),
            )

            // Bulletin thread within employee access limit
            postNewThread(
                title = "Juhannus",
                message = "Juhannus tulee pian",
                messageType = MessageType.BULLETIN,
                sender = municipalAccount,
                recipients = listOf(MessageRecipient.Area(area.id)),
                user = adminUser,
                now = HelsinkiDateTime.of(aclCreationDate.minusDays(7), LocalTime.of(12, 11)),
            )
            assertEquals(EmployeeUnreadCounts(0, 1, 0), allUnreadMessagesCount(employeeUser))

            // Citizen replies to the message thread which makes both of its messages visible to the
            // employee since the newest message is in the employee's access limit
            replyToThread(
                user = adultUser,
                threadId =
                    getRegularMessageThreads(adultUser)
                        .first { it.messageType == MessageType.MESSAGE }
                        .id,
                recipientAccountIds = setOf(groupAccount),
                content = "Kiitos tiedosta",
            )

            assertEquals(EmployeeUnreadCounts(2, 1, 0), allUnreadMessagesCount(employeeUser))
        }

        @Test
        fun `message sent by employee should not be visible to citizen before the asyncjob has been executed`() {
            disableAsyncJobRunning {
                postNewThread(
                    title = "Juhannus",
                    message = "Juhannus tulee pian",
                    messageType = MessageType.MESSAGE,
                    sender = employee1Account,
                    recipients = listOf(MessageRecipient.Child(testChild_1.id)),
                    user = employee1,
                    now = sendTime,
                )
            }

            // Message is still not visible to citizen if the async job hasn't run
            assertEquals(0, unreadMessagesCount(person1, now = readTime))
            assertTrue(getUnreadReceivedMessages(person1Account, person1, now = readTime).isEmpty())

            // Messages are visible to citizen after the async job has run
            asyncJobRunner.runPendingJobsSync(MockEvakaClock(readTime))
            assertEquals(1, unreadMessagesCount(person1, now = readTime))
            val personUnreadMessages =
                getUnreadReceivedMessages(person1Account, person1, now = readTime)
            assertEquals(1, personUnreadMessages.size)

            // Citizen replies
            replyToThread(
                threadId = personUnreadMessages.first().threadId,
                content = "Ihanko totta?",
                recipientAccountIds = setOf(employee1Account),
                user = person1,
                now = readTime,
            )

            // Employee replies
            val sendReplyTime = readTime.plusMinutes(5)
            disableAsyncJobRunning {
                replyToThread(
                    threadId =
                        getUnreadReceivedMessages(employee1Account, employee1, now = sendReplyTime)
                            .first()
                            .threadId,
                    content = "Ei sittenkään, väärä hälytys",
                    recipientAccountIds = setOf(person1Account),
                    sender = employee1Account,
                    user = employee1,
                    now = sendReplyTime,
                )
            }

            // Reply is still not visible to citizen if the async job hasn't run
            val readReplyTime = sendReplyTime.plusSeconds(30)
            assertEquals(0, unreadMessagesCount(person1, now = readReplyTime))
            assertEquals(
                0,
                getUnreadReceivedMessages(person1Account, person1, now = readReplyTime).size,
            )

            // Reply is visible to citizen after the async job has run
            asyncJobRunner.runPendingJobsSync(MockEvakaClock(readReplyTime))
            assertEquals(1, unreadMessagesCount(person1, now = readReplyTime))
            assertEquals(
                1,
                getUnreadReceivedMessages(person1Account, person1, now = readReplyTime).size,
            )
        }

        @Test
        fun `employee with access to two groups cannot send messages as group1 to group2`() {
            postNewThread(
                title = "Juhannus",
                message = "Juhannus tulee pian",
                messageType = MessageType.MESSAGE,
                sender = group1Account,
                recipients = listOf(MessageRecipient.Group(groupId2)),
                user = employee1,
            )
            assertEquals(0, getRegularMessageThreads(person4).size)

            postNewThread(
                title = "Juhannus",
                message = "Juhannus tulee pian",
                messageType = MessageType.MESSAGE,
                sender = group2Account,
                recipients = listOf(MessageRecipient.Group(groupId2)),
                user = employee1,
            )
            assertEquals(1, getRegularMessageThreads(person4).size)
        }

        @Test
        fun `employee with access to two groups cannot send messages as group1 to child in group2`() {
            postNewThread(
                title = "Juhannus",
                message = "Juhannus tulee pian",
                messageType = MessageType.MESSAGE,
                sender = group1Account,
                recipients = listOf(MessageRecipient.Child(testChild_4.id)),
                user = employee1,
            )
            assertEquals(0, getRegularMessageThreads(person4).size)

            postNewThread(
                title = "Juhannus",
                message = "Juhannus tulee pian",
                messageType = MessageType.MESSAGE,
                sender = group2Account,
                recipients = listOf(MessageRecipient.Child(testChild_4.id)),
                user = employee1,
            )
            assertEquals(1, getRegularMessageThreads(person4).size)
        }

        @Test
        fun `guardian can send a message only to group and other guardian, not group staff`() {
            fun getRecipients() =
                getCitizenRecipients(person1).messageAccounts.map { it.account.id }.toSet()

            assertEquals(setOf(group1Account, person2Account, employee1Account), getRecipients())

            // When a supervisor works as staff, her account is deactivated
            db.transaction { it.deactivateEmployeeMessageAccount(employee1.id) }
            assertEquals(setOf(group1Account, person2Account), getRecipients())
        }
    }

    @Nested
    inner class Finance {

        @Test
        fun `finance can send a message only to a single adult at a time`() {
            postNewThread(
                title = "title",
                message = "content",
                messageType = MessageType.MESSAGE,
                sender = financeAccount,
                recipients = listOf(MessageRecipient.Citizen(testAdult_1.id)),
                user = financeAdmin.user,
            )

            assertThrows<BadRequest> {
                postNewThread(
                    title = "title",
                    message = "content",
                    messageType = MessageType.MESSAGE,
                    sender = financeAccount,
                    recipients =
                        listOf(
                            MessageRecipient.Citizen(testAdult_1.id),
                            MessageRecipient.Citizen(testAdult_2.id),
                        ),
                    user = financeAdmin.user,
                )
            }

            assertEquals(1, getSentMessages(financeAccount, financeAdmin.user).size)
        }

        @Test
        fun `citizen can reply to message from finance`() {
            postNewThread(
                title = "Vastaa heti",
                message = "Viestin sisältö",
                messageType = MessageType.MESSAGE,
                sender = financeAccount,
                recipients = listOf(MessageRecipient.Citizen(testAdult_1.id)),
                user = financeAdmin.user,
            )
            val thread = getRegularMessageThreads(person1)[0]
            replyToThread(
                threadId = thread.id,
                content = "Vastaus",
                recipientAccountIds = setOf(financeAccount),
                user = person1,
                now = clock.now(),
            )
            assertEquals(1, unreadMessagesCount(financeAccount, financeAdmin.user))
        }

        @Test
        fun `finance cannot send messages to children`() {
            assertThrows<BadRequest> {
                postNewThread(
                    title = "title",
                    message = "content",
                    messageType = MessageType.MESSAGE,
                    sender = financeAccount,
                    recipients = listOf(MessageRecipient.Child(testChild_1.id)),
                    user = financeAdmin.user,
                )
            }
        }

        @Test
        fun `finance cannot send messages to areas`() {
            assertThrows<BadRequest> {
                postNewThread(
                    title = "title",
                    message = "content",
                    messageType = MessageType.MESSAGE,
                    sender = financeAccount,
                    recipients = listOf(MessageRecipient.Area(testArea.id)),
                    user = financeAdmin.user,
                )
            }
        }

        @Test
        fun `finance cannot send messages to units`() {
            assertThrows<BadRequest> {
                postNewThread(
                    title = "title",
                    message = "content",
                    messageType = MessageType.MESSAGE,
                    sender = financeAccount,
                    recipients = listOf(MessageRecipient.Unit(testDaycare.id)),
                    user = financeAdmin.user,
                )
            }
        }

        @Test
        fun `finance cannot send messages to groups`() {
            assertThrows<BadRequest> {
                postNewThread(
                    title = "title",
                    message = "content",
                    messageType = MessageType.MESSAGE,
                    sender = financeAccount,
                    recipients = listOf(MessageRecipient.Group(groupId1)),
                    user = financeAdmin.user,
                )
            }
        }

        @Test
        fun `finance can move a thread to a folder`() {
            val folder1 = DevMessageThreadFolder(owner = financeAccount, name = "Eteläinen")
            val folder2 = DevMessageThreadFolder(owner = financeAccount, name = "Pohjoinen")
            val folderOther =
                DevMessageThreadFolder(owner = municipalAccount, name = "Toisen käyttäjän kansio")
            db.transaction { tx ->
                tx.insert(folder1)
                tx.insert(folder2)
                tx.insert(folderOther)
            }

            assertEquals(
                setOf(
                    MessageController.MessageThreadFolder(
                        id = folder1.id,
                        ownerId = folder1.owner,
                        name = folder1.name,
                    ),
                    MessageController.MessageThreadFolder(
                        id = folder2.id,
                        ownerId = folder2.owner,
                        name = folder2.name,
                    ),
                ),
                getFolders(financeAdmin.user).toSet(),
            )

            postNewThread(
                title = "Vastaa heti",
                message = "Tähän viestiin pitäisi pystyä vastaamaan",
                messageType = MessageType.MESSAGE,
                sender = financeAccount,
                recipients = listOf(MessageRecipient.Citizen(testAdult_1.id)),
                user = financeAdmin.user,
                initialFolder = folder1.id,
            )

            assertEquals(
                1,
                getMessagesInFolder(financeAccount, folder1.id, financeAdmin.user).data.size,
            )
            val threadId =
                getMessagesInFolder(financeAccount, folder1.id, financeAdmin.user).data.first().id
            assertEquals(
                0,
                getMessagesInFolder(financeAccount, folder2.id, financeAdmin.user).data.size,
            )

            val thread = getRegularMessageThreads(person1)[0]
            replyToThread(
                threadId = thread.id,
                content = "Vastaus",
                recipientAccountIds = setOf(financeAccount),
                user = person1,
                now = clock.now(),
            )

            assertEquals(
                UnreadCountByAccount(
                    accountId = financeAccount,
                    unreadCount = 0,
                    unreadCopyCount = 0,
                    unreadCountByFolder = mapOf(folder1.id to 1),
                ),
                unreadMessagesCounts(financeAccount, financeAdmin.user),
            )
            assertEquals(
                1,
                getMessagesInFolder(financeAccount, folder1.id, financeAdmin.user).data.size,
            )
            assertEquals(
                2,
                getMessagesInFolder(financeAccount, folder1.id, financeAdmin.user)
                    .data
                    .first()
                    .messages
                    .size,
            )

            moveThreadToFolder(financeAccount, threadId, folder2.id, financeAdmin.user)

            assertEquals(
                UnreadCountByAccount(
                    accountId = financeAccount,
                    unreadCount = 0,
                    unreadCopyCount = 0,
                    unreadCountByFolder = mapOf(folder2.id to 1),
                ),
                unreadMessagesCounts(financeAccount, financeAdmin.user),
            )
            assertEquals(
                0,
                getMessagesInFolder(financeAccount, folder1.id, financeAdmin.user).data.size,
            )
            assertEquals(
                1,
                getMessagesInFolder(financeAccount, folder2.id, financeAdmin.user).data.size,
            )

            assertThrows<NotFound> {
                moveThreadToFolder(financeAccount, threadId, folderOther.id, financeAdmin.user)
            }
        }
    }

    @Nested
    inner class Bulletins {

        @Test
        fun `a bulletin cannot be replied to by the recipients`() {
            // when a bulletin thread is created
            postNewThread(
                title = "Tiedote",
                message = "Juhannus tulee pian",
                messageType = MessageType.BULLETIN,
                sender = employee1Account,
                recipients = listOf(MessageRecipient.Child(testChild_1.id)),
                user = employee1,
            )

            // then the recipient can see it
            val thread = getRegularMessageThreads(person1).first()
            assertEquals("Tiedote", thread.title)
            assertEquals(MessageType.BULLETIN, thread.messageType)
            assertEquals(
                listOf(Pair(employee1Account, "Juhannus tulee pian")),
                thread.toSenderContentPairs(),
            )

            // when the recipient tries to reply to the bulletin, it is denied
            assertThrows<Forbidden> {
                replyToThread(
                    user = person1,
                    threadId = thread.id,
                    recipientAccountIds = setOf(thread.messages.first().sender.id),
                    content = "Kiitos tiedosta",
                )
            }

            // when the author himself replies to the bulletin, it succeeds
            replyToThread(
                sender = employee1Account,
                user = employee1,
                threadId = thread.id,
                recipientAccountIds = setOf(person1Account),
                content = "Nauttikaa siitä",
                now = sendTime.plusMinutes(5),
            )

            // then the recipient can see it
            assertEquals(
                listOf(
                    Pair(employee1Account, "Juhannus tulee pian"),
                    Pair(employee1Account, "Nauttikaa siitä"),
                ),
                getRegularMessageThreads(person1).first().toSenderContentPairs(),
            )
        }

        @Test
        fun `bulletin copy is created for others supervisors, veos and groups`() {
            // given
            val child = DevPerson()
            val secondSupervisor = DevEmployee()
            var secondSupervisorAccount: MessageAccountId? = null
            val veo = DevEmployee()
            var veoAccount: MessageAccountId? = null
            db.transaction { tx ->
                insertChild(tx, child, groupId1)
                tx.insert(
                    secondSupervisor,
                    unitRoles = mapOf(testDaycare.id to UserRole.UNIT_SUPERVISOR),
                )
                secondSupervisorAccount = tx.upsertEmployeeMessageAccount(secondSupervisor.id)
                tx.insert(
                    veo,
                    unitRoles = mapOf(testDaycare.id to UserRole.SPECIAL_EDUCATION_TEACHER),
                )
                veoAccount = tx.upsertEmployeeMessageAccount(veo.id)
            }

            // when
            postNewThread(
                "title",
                "content",
                MessageType.BULLETIN,
                employee1Account,
                listOf(MessageRecipient.Group(groupId1)),
                user = employee1,
                now = sendTime,
            )

            // then

            // no copy for the sender
            assertEquals(0, getMessageCopies(employee1, employee1Account, readTime).size)

            // copy for the second supervisor
            assertEquals(
                1,
                getMessageCopies(secondSupervisor.user, secondSupervisorAccount!!, readTime).size,
            )

            // no copy for supervisor of another unit
            assertEquals(0, getMessageCopies(employee2, employee2Account, readTime).size)

            // copy for veo
            assertEquals(1, getMessageCopies(veo.user, veoAccount!!, readTime).size)

            // copy for group
            assertEquals(1, getMessageCopies(employee1, group1Account, readTime).size)

            // no copy for the other group
            assertEquals(0, getMessageCopies(employee1, group2Account, readTime).size)

            // no copy for service worker
            assertEquals(
                0,
                getMessageCopies(serviceWorker.user, serviceWorkerAccount, readTime).size,
            )
        }

        @Test
        fun `message copy is not created for non-bulletins`() {
            // given
            db.transaction { tx -> insertChild(tx, DevPerson(), groupId1) }

            // when
            postNewThread(
                "title",
                "content",
                MessageType.MESSAGE,
                employee1Account,
                listOf(MessageRecipient.Group(groupId1)),
                user = employee1,
                now = sendTime,
            )

            // then
            assertEquals(0, getMessageCopies(employee1, group1Account, readTime).size)
        }

        @Test
        fun `municipal bulletin can be sent to 100 recipients and all can see it`() {
            val guardianAccounts = mutableListOf<MessageAccountId>()

            db.transaction { tx ->
                repeat(100) { i ->
                    val child = DevPerson()
                    val guardian = DevPerson(firstName = "Guardian$i", lastName = "Test")
                    tx.insert(guardian, DevPersonType.ADULT)
                    val guardianAccount = tx.getCitizenMessageAccount(guardian.id)
                    guardianAccounts.add(guardianAccount)

                    insertChild(tx, child, groupId1)
                    tx.insertGuardian(guardian.id, child.id)
                }
            }

            postNewThread(
                title = "Municipal Bulletin",
                message = "Important announcement to 100 families",
                messageType = MessageType.BULLETIN,
                sender = municipalAccount,
                recipients = listOf(MessageRecipient.Area(testArea.id)),
                user = messager.user,
                now = sendTime,
            )

            guardianAccounts.forEach { guardianAccount ->
                val threads =
                    db.read {
                        it.getThreads(
                            accountId = guardianAccount,
                            folderId = null,
                            pageSize = 20,
                            page = 1,
                            municipalAccountName = "Municipal Account",
                            serviceWorkerAccountName = "Service Worker",
                            financeAccountName = "Finance",
                        )
                    }
                assertEquals(1, threads.data.size)
                val thread = threads.data.first()
                assertEquals("Municipal Bulletin", thread.title)
                assertEquals(MessageType.BULLETIN, thread.type)
                assertEquals(1, thread.messages.size)
                assertEquals(
                    "Important announcement to 100 families",
                    thread.messages.first().content,
                )
            }

            val sentMessages =
                db.read {
                    it.getMessagesSentByAccount(
                        accountId = municipalAccount,
                        pageSize = 20,
                        page = 1,
                    )
                }
            assertEquals(1, sentMessages.data.size)
            assertEquals("Municipal Bulletin", sentMessages.data.first().threadTitle)
            assertEquals(MessageType.BULLETIN, sentMessages.data.first().type)
        }
    }

    @Nested
    inner class RecipientGrouping {

        @Test
        fun `municipal bulletin hides recipients from other recipients`() {
            postNewThread(
                title = "Municipal Bulletin",
                message = "Important announcement",
                messageType = MessageType.BULLETIN,
                sender = municipalAccount,
                recipients = listOf(MessageRecipient.Group(groupId1)),
                user = messager.user,
                now = sendTime,
            )

            val threads = getRegularMessageThreads(person1)
            assertEquals(1, threads.size)
            val message = threads.first().messages.first()
            assertTrue(message.recipients.isEmpty())
        }

        @Test
        fun `non-municipal message shows only co-guardians as recipients`() {
            // Setup: testChild_1 has guardians person1 and person2
            //        testChild_3 has guardians person2 and person3
            // Send message to both children
            postNewThread(
                title = "Group Message",
                message = "Message from group",
                messageType = MessageType.MESSAGE,
                sender = employee1Account,
                recipients =
                    listOf(
                        MessageRecipient.Child(testChild_1.id),
                        MessageRecipient.Child(testChild_3.id),
                    ),
                user = employee1,
                now = sendTime,
            )

            // person1 should see themselves and person2 (co-guardian of testChild_1)
            val person1Threads = getRegularMessageThreads(person1)
            assertEquals(1, person1Threads.size)
            val person1Recipients = person1Threads.first().messages.first().recipients
            assertEquals(
                setOf(person1Account, person2Account),
                person1Recipients.map { it.id }.toSet(),
            )

            // person2 is guardian of both children, so gets 2 threads (one per child's guardian
            // group)
            val person2Threads = getRegularMessageThreads(person2)
            assertEquals(2, person2Threads.size)
            // One thread with person1 (for testChild_1)
            val person2ThreadWithPerson1 =
                person2Threads.find { thread ->
                    thread.messages.first().recipients.any { it.id == person1Account }
                }!!
            assertEquals(
                setOf(person1Account, person2Account),
                person2ThreadWithPerson1.messages.first().recipients.map { it.id }.toSet(),
            )
            // One thread with person3 (for testChild_3)
            val person2ThreadWithPerson3 =
                person2Threads.find { thread ->
                    thread.messages.first().recipients.any { it.id == person3Account }
                }!!
            assertEquals(
                setOf(person2Account, person3Account),
                person2ThreadWithPerson3.messages.first().recipients.map { it.id }.toSet(),
            )

            // person3 should see themselves and person2 (co-guardian of testChild_3)
            val person3Threads = getRegularMessageThreads(person3)
            assertEquals(1, person3Threads.size)
            val person3Recipients = person3Threads.first().messages.first().recipients
            assertEquals(
                setOf(person2Account, person3Account),
                person3Recipients.map { it.id }.toSet(),
            )
        }
    }

    @Nested
    inner class Attachments {

        @Test
        fun `citizen can not add attachments by default`() {
            val child = DevPerson()
            db.transaction { tx ->
                insertChild(tx, child, groupId1, shiftCare = ShiftCareType.NONE)
                tx.insertGuardian(person1.id, child.id)
            }
            assertEquals(
                MessageControllerCitizen.MyAccountResponse(
                    accountId = person1Account,
                    messageAttachmentsAllowed = false,
                ),
                getMyAccount(person1),
            )

            assertThrows<Forbidden> { uploadMessageAttachmentCitizen(person1) }

            assertThrows<Forbidden> {
                postNewThread(
                    user = person1,
                    title = "title",
                    message = "content",
                    children = listOf(child.id),
                    recipients = listOf(employee1Account),
                    attachmentIds = listOf(AttachmentId(UUID.randomUUID())),
                )
            }
        }

        @Test
        fun `citizen with child in shift care can add attachment`() {
            val child = DevPerson()
            db.transaction { tx ->
                insertChild(tx, child, groupId1, shiftCare = ShiftCareType.FULL)
                tx.insertGuardian(person1.id, child.id)
            }
            assertEquals(
                MessageControllerCitizen.MyAccountResponse(
                    accountId = person1Account,
                    messageAttachmentsAllowed = true,
                ),
                getMyAccount(person1),
            )

            val attachmentId = uploadMessageAttachmentCitizen(person1)
            postNewThread(
                user = person1,
                title = "title",
                message = "content",
                children = listOf(child.id),
                recipients = listOf(employee1Account),
                attachmentIds = listOf(attachmentId),
            )

            attachmentsController.getAttachment(
                dbInstance(),
                employee1,
                clock,
                attachmentId,
                "evaka-logo.png",
            )

            assertEquals(
                1,
                db.read {
                    it.createQuery {
                            sql(
                                "SELECT COUNT(*) FROM attachment WHERE message_content_id IS NOT NULL"
                            )
                        }
                        .exactlyOne<Int>()
                },
            )
        }

        @Test
        fun `messages can have attachments`() {
            val draftId = db.transaction { it.initDraft(employee1Account) }

            assertEquals(1, db.read { it.getDrafts(employee1Account) }.size)

            // when an attachment it uploaded
            val attachmentId = uploadMessageAttachment(employee1, draftId)

            // then another employee cannot read or delete the attachment
            assertThrows<Forbidden> {
                attachmentsController.getAttachment(
                    dbInstance(),
                    employee2,
                    clock,
                    attachmentId,
                    "evaka-logo.png",
                )
            }
            assertThrows<Forbidden> {
                attachmentsController.deleteAttachment(dbInstance(), employee2, clock, attachmentId)
            }

            // then the author can read and delete the attachment
            attachmentsController.getAttachment(
                dbInstance(),
                employee1,
                clock,
                attachmentId,
                "evaka-logo.png",
            )
            attachmentsController.deleteAttachment(dbInstance(), employee1, clock, attachmentId)

            // a user cannot upload attachments to another user's draft
            assertThrows<Forbidden> { uploadMessageAttachment(employee2, draftId) }

            val attachmentIds =
                setOf(
                    uploadMessageAttachment(employee1, draftId),
                    uploadMessageAttachment(employee1, draftId),
                )

            // when a message thread with attachment is created
            postNewThread(
                title = "t1",
                message = "m1",
                messageType = MessageType.MESSAGE,
                sender = employee1Account,
                recipients = listOf(MessageRecipient.Child(testChild_1.id)),
                user = employee1,
                attachmentIds = attachmentIds,
                draftId = draftId,
            )

            // then
            // the draft is deleted
            assertEquals(0, db.read { it.getDrafts(employee1Account) }.size)

            // the attachments are associated to a message
            assertEquals(
                2,
                db.read {
                    it.createQuery {
                            sql(
                                "SELECT COUNT(*) FROM attachment WHERE message_content_id IS NOT NULL"
                            )
                        }
                        .exactlyOne<Int>()
                },
            )

            // the author can read the attachment
            attachmentsController.getAttachment(
                dbInstance(),
                employee1,
                clock,
                attachmentIds.first(),
                "evaka-logo.png",
            )
            // another employee cannot read the attachment
            assertThrows<Forbidden> {
                attachmentsController.getAttachment(
                    dbInstance(),
                    employee2,
                    clock,
                    attachmentIds.first(),
                    "evaka-logo.png",
                )
            }

            // the recipient can read the attachment
            val threads = getRegularMessageThreads(person1)
            assertEquals(1, threads.size)
            val messages = threads.first().messages
            assertEquals(1, messages.size)
            val receivedAttachments = messages.first().attachments
            assertEquals(attachmentIds, receivedAttachments.map { it.id }.toSet())
            attachmentsController.getAttachment(
                dbInstance(),
                person1,
                clock,
                attachmentIds.first(),
                "evaka-logo.png",
            )

            // another citizen cannot read the attachment
            assertThrows<Forbidden> {
                attachmentsController.getAttachment(
                    dbInstance(),
                    person3,
                    clock,
                    attachmentIds.first(),
                    "evaka-logo.png",
                )
            }
        }
    }

    companion object {
        @JvmStatic
        fun groupAccountAccessParams(): Stream<Array<Any>> =
            Stream.of(
                arrayOf(10, UserRole.STAFF, false),
                arrayOf(1, UserRole.STAFF, true),
                arrayOf(10, UserRole.UNIT_SUPERVISOR, true),
            )
    }

    private fun insertChild(
        tx: Database.Transaction,
        child: DevPerson,
        groupId: GroupId,
        daycareId: DaycareId = testDaycare.id,
        optionId: ServiceNeedOptionId = snDefaultDaycare.id,
        shiftCare: ShiftCareType = ShiftCareType.NONE,
    ) {
        tx.insert(child, DevPersonType.CHILD)

        val placementId =
            tx.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycareId,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
        tx.insert(
            DevDaycareGroupPlacement(
                daycarePlacementId = placementId,
                daycareGroupId = groupId,
                startDate = placementStart,
                endDate = placementEnd,
            )
        )
        tx.insert(
            DevServiceNeed(
                confirmedBy = employee1.evakaUserId,
                placementId = placementId,
                startDate = placementStart,
                endDate = placementEnd,
                optionId = optionId,
                shiftCare = shiftCare,
            )
        )
    }

    private fun prepareGroupAccountAccessTest(
        userRole: UserRole,
        now: HelsinkiDateTime,
    ): AuthenticatedUser.Employee {
        return db.transaction { tx ->
            testChild_2.let {
                insertChild(tx, it, groupId1)
                tx.insertGuardian(person2.id, it.id)
            }
            val placementId =
                tx.insert(
                    DevPlacement(
                        childId = testChild_2.id,
                        unitId = testDaycare.id,
                        startDate = now.toLocalDate().minusMonths(6),
                        endDate = now.toLocalDate().plusMonths(6),
                    )
                )
            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placementId,
                    daycareGroupId = groupId1,
                    startDate = now.toLocalDate().minusMonths(6),
                    endDate = now.toLocalDate().plusMonths(6),
                )
            )
            val employee = DevEmployee(firstName = "New", lastName = "Staff")
            tx.insert(employee)
            tx.insertDaycareAclRow(testDaycare.id, employee.id, userRole)
            tx.syncDaycareGroupAcl(testDaycare.id, employee.id, listOf(groupId1), now)
            employee.user
        }
    }

    private fun getUnreadReceivedMessages(
        accountId: MessageAccountId,
        user: AuthenticatedUser.Citizen,
        now: HelsinkiDateTime = readTime,
    ) =
        getRegularMessageThreads(user, now).flatMap {
            it.messages.filter { m -> m.sender.id != accountId && m.readAt == null }
        }

    private fun getUnreadReceivedMessages(
        accountId: MessageAccountId,
        user: AuthenticatedUser.Employee,
        now: HelsinkiDateTime = readTime,
    ) =
        getEmployeeMessageThreads(accountId, user, now).flatMap {
            it.messages.filter { m -> m.sender.id != accountId && m.readAt == null }
        }

    private fun uploadMessageAttachment(
        user: AuthenticatedUser.Employee,
        draftId: MessageDraftId,
    ): AttachmentId =
        attachmentsController.uploadMessageAttachment(
            dbInstance(),
            user,
            clock,
            draftId,
            MockMultipartFile("evaka-logo.png", "evaka-logo.png", null, pngFile.readBytes()),
        )

    private fun uploadMessageAttachmentCitizen(user: AuthenticatedUser.Citizen): AttachmentId =
        attachmentsController.uploadMessageAttachmentCitizen(
            dbInstance(),
            user,
            MockEvakaClock(readTime),
            MockMultipartFile("evaka-logo.png", "evaka-logo.png", null, pngFile.readBytes()),
        )

    private fun postNewThreadPreflightCheck(
        sender: MessageAccountId,
        recipients: List<MessageRecipient>,
        filters: MessageController.PostMessageFilters? = null,
        user: AuthenticatedUser.Employee,
        now: HelsinkiDateTime = sendTime,
    ): PostMessagePreflightResponse {
        return messageController.createMessagePreflightCheck(
            dbInstance(),
            user,
            MockEvakaClock(now),
            sender,
            MessageController.PostMessagePreflightBody(
                recipients = recipients.toSet(),
                filters = filters,
            ),
        )
    }

    private fun postNewThread(
        title: String,
        message: String,
        messageType: MessageType,
        sender: MessageAccountId,
        recipients: List<MessageRecipient>,
        recipientNames: List<String> = listOf(),
        user: AuthenticatedUser.Employee,
        attachmentIds: Set<AttachmentId> = setOf(),
        draftId: MessageDraftId? = null,
        now: HelsinkiDateTime = sendTime,
        relatedApplicationId: ApplicationId? = null,
        sensitive: Boolean = false,
        filters: MessageController.PostMessageFilters? = null,
        initialFolder: MessageThreadFolderId? = null,
    ): MessageContentId? {
        val messageContentId =
            messageController
                .createMessage(
                    dbInstance(),
                    user,
                    MockEvakaClock(now),
                    sender,
                    initialFolder,
                    MessageController.PostMessageBody(
                        title = title,
                        content = message,
                        type = messageType,
                        recipients = recipients.toSet(),
                        recipientNames = recipientNames,
                        attachmentIds = attachmentIds,
                        draftId = draftId,
                        urgent = false,
                        sensitive = sensitive,
                        relatedApplicationId = relatedApplicationId,
                        filters = filters,
                    ),
                )
                .createdId
        if (asyncJobRunningEnabled) {
            asyncJobRunner.runPendingJobsSync(MockEvakaClock(now.plusSeconds(30)))
        }
        return messageContentId
    }

    private fun postNewThread(
        user: AuthenticatedUser.Citizen,
        title: String,
        message: String,
        recipients: List<MessageAccountId>,
        children: List<ChildId>,
        attachmentIds: List<AttachmentId> = emptyList(),
        now: HelsinkiDateTime = sendTime,
    ): MessageThreadId {
        val messageThreadId =
            messageControllerCitizen.newMessage(
                dbInstance(),
                user,
                MockEvakaClock(now),
                CitizenMessageBody(
                    recipients = recipients.toSet(),
                    children = children.toSet(),
                    title = title,
                    content = message,
                    attachmentIds = attachmentIds,
                ),
            )
        if (asyncJobRunningEnabled) {
            asyncJobRunner.runPendingJobsSync(MockEvakaClock(now.plusSeconds(30)))
        }
        return messageThreadId
    }

    private fun replyToThread(
        user: AuthenticatedUser.Citizen,
        threadId: MessageThreadId,
        recipientAccountIds: Set<MessageAccountId>,
        content: String,
        now: HelsinkiDateTime = sendTime,
    ) {
        messageControllerCitizen.replyToThread(
            dbInstance(),
            user,
            MockEvakaClock(now),
            threadId,
            ReplyToMessageBody(content = content, recipientAccountIds = recipientAccountIds),
        )
        if (asyncJobRunningEnabled) {
            asyncJobRunner.runPendingJobsSync(MockEvakaClock(now.plusSeconds(30)))
        }
    }

    private fun replyToThread(
        user: AuthenticatedUser.Employee,
        sender: MessageAccountId,
        threadId: MessageThreadId,
        recipientAccountIds: Set<MessageAccountId>,
        content: String,
        now: HelsinkiDateTime = sendTime,
    ) {
        messageController.replyToThread(
            dbInstance(),
            user,
            MockEvakaClock(now),
            sender,
            threadId,
            ReplyToMessageBody(content = content, recipientAccountIds = recipientAccountIds),
        )
        if (asyncJobRunningEnabled) {
            asyncJobRunner.runPendingJobsSync(MockEvakaClock(now.plusSeconds(30)))
        }
    }

    private fun markThreadRead(user: AuthenticatedUser.Citizen, threadId: MessageThreadId) {
        messageControllerCitizen.markThreadRead(
            dbInstance(),
            user,
            MockEvakaClock(readTime),
            threadId,
        )
    }

    private fun markThreadRead(
        user: AuthenticatedUser.Employee,
        accountId: MessageAccountId,
        threadId: MessageThreadId,
        now: HelsinkiDateTime,
    ) {
        messageController.markThreadRead(
            dbInstance(),
            user,
            MockEvakaClock(now),
            accountId,
            threadId,
        )
    }

    private fun markLastMessageInThreadUnread(
        user: AuthenticatedUser.Employee,
        accountId: MessageAccountId,
        threadId: MessageThreadId,
        now: HelsinkiDateTime,
    ) {
        messageController.markLastReceivedMessageInThreadUnread(
            dbInstance(),
            user,
            MockEvakaClock(now),
            accountId,
            threadId,
        )
    }

    private fun markLastMessageUnread(
        person1: AuthenticatedUser.Citizen,
        threadId: MessageThreadId,
        now: HelsinkiDateTime,
    ) {
        messageControllerCitizen.markLastReceivedMessageInThreadUnread(
            dbInstance(),
            person1,
            MockEvakaClock(now),
            threadId,
        )
    }

    private fun getRegularMessageThreads(
        user: AuthenticatedUser.Citizen,
        now: HelsinkiDateTime = readTime,
    ): List<CitizenMessageThread.Regular> {
        return messageControllerCitizen
            .getReceivedMessages(dbInstance(), user, MockEvakaClock(now), page = 1)
            .data
            .filterIsInstance<CitizenMessageThread.Regular>()
    }

    private fun getAllCitizenMessageThreads(
        user: AuthenticatedUser.Citizen,
        now: HelsinkiDateTime = readTime,
    ): List<CitizenMessageThread> {
        return messageControllerCitizen
            .getReceivedMessages(dbInstance(), user, MockEvakaClock(now), page = 1)
            .data
    }

    private fun getEmployeeMessageThreads(
        accountId: MessageAccountId,
        user: AuthenticatedUser.Employee,
        now: HelsinkiDateTime = readTime,
    ): List<MessageThread> {
        return messageController
            .getReceivedMessages(dbInstance(), user, MockEvakaClock(now), accountId, page = 1)
            .data
    }

    private fun getSentMessages(
        accountId: MessageAccountId,
        user: AuthenticatedUser.Employee,
    ): List<SentMessage> {
        return messageController
            .getSentMessages(dbInstance(), user, MockEvakaClock(readTime), accountId, page = 1)
            .data
    }

    private fun createDraft(
        user: AuthenticatedUser.Employee,
        accountId: MessageAccountId,
        title: String,
        content: String,
        recipients: Set<DraftRecipient>,
        recipientNames: List<String>,
        now: HelsinkiDateTime,
    ) {
        val draftId =
            messageController.initDraftMessage(
                dbInstance(),
                user,
                MockEvakaClock(now.minusMinutes(1)),
                accountId,
            )
        messageController.updateDraftMessage(
            dbInstance(),
            user,
            MockEvakaClock(now),
            accountId,
            draftId,
            UpdatableDraftContent(
                MessageType.MESSAGE,
                title,
                content,
                urgent = false,
                sensitive = false,
                recipients = recipients,
                recipientNames = recipientNames,
            ),
        )
    }

    private fun getDrafts(
        user: AuthenticatedUser.Employee,
        accountId: MessageAccountId,
        now: HelsinkiDateTime,
    ): List<DraftContent> {
        return messageController.getDraftMessages(
            dbInstance(),
            user,
            MockEvakaClock(now),
            accountId,
        )
    }

    private fun getMessageCopies(
        user: AuthenticatedUser.Employee,
        accountId: MessageAccountId,
        now: HelsinkiDateTime,
    ): List<MessageCopy> {
        return messageController
            .getMessageCopies(dbInstance(), user, MockEvakaClock(now), accountId, page = 1)
            .data
    }

    private fun archiveThread(
        user: AuthenticatedUser.Employee,
        accountId: MessageAccountId,
        threadId: MessageThreadId,
        now: HelsinkiDateTime,
    ) {
        messageController.archiveThread(
            dbInstance(),
            user,
            MockEvakaClock(now),
            accountId,
            threadId,
        )
    }

    private fun getEmployeeArchivedThreads(
        accountId: MessageAccountId,
        user: AuthenticatedUser.Employee,
        now: HelsinkiDateTime = readTime,
    ): List<MessageThread> {
        return messageController
            .getArchivedMessages(dbInstance(), user, MockEvakaClock(now), accountId, page = 1)
            .data
    }

    private fun getMyAccount(
        user: AuthenticatedUser.Citizen
    ): MessageControllerCitizen.MyAccountResponse {
        return messageControllerCitizen.getMyAccount(dbInstance(), user, MockEvakaClock(readTime))
    }

    private fun getCitizenRecipients(
        user: AuthenticatedUser.Citizen
    ): MessageControllerCitizen.GetRecipientsResponse {
        return messageControllerCitizen.getRecipients(dbInstance(), user, MockEvakaClock(readTime))
    }

    private fun unreadMessagesCount(
        user: AuthenticatedUser.Citizen,
        now: HelsinkiDateTime = readTime,
    ): Int {
        return messageControllerCitizen.getUnreadMessages(dbInstance(), user, MockEvakaClock(now))
    }

    private fun unreadMessagesCount(
        accountId: MessageAccountId,
        user: AuthenticatedUser.Employee,
        now: HelsinkiDateTime = readTime,
    ): Int {
        return messageController
            .getUnreadMessages(dbInstance(), user, MockEvakaClock(now))
            .find { it.accountId == accountId }
            ?.unreadCount ?: 0
    }

    private fun unreadMessagesCounts(
        accountId: MessageAccountId,
        user: AuthenticatedUser.Employee,
        now: HelsinkiDateTime = readTime,
    ): UnreadCountByAccount {
        return messageController.getUnreadMessages(dbInstance(), user, MockEvakaClock(now)).find {
            it.accountId == accountId
        } ?: throw Exception("No unread counts for account $accountId")
    }

    data class EmployeeUnreadCounts(
        val unreadCount: Int,
        val unreadCopyCount: Int,
        val unreadCountInFolders: Int,
    )

    private fun allUnreadMessagesCount(
        user: AuthenticatedUser.Employee,
        now: HelsinkiDateTime = readTime,
    ): EmployeeUnreadCounts {
        val unreadMessages =
            messageController.getUnreadMessages(dbInstance(), user, MockEvakaClock(now))
        return EmployeeUnreadCounts(
            unreadCount = unreadMessages.sumOf { it.unreadCount },
            unreadCopyCount = unreadMessages.sumOf { it.unreadCopyCount },
            unreadCountInFolders = unreadMessages.sumOf { it.unreadCountByFolder.values.sum() },
        )
    }

    private fun getFolders(user: AuthenticatedUser.Employee, now: HelsinkiDateTime = readTime) =
        messageController.getFolders(dbInstance(), user, MockEvakaClock(now))

    private fun getMessagesInFolder(
        accountId: MessageAccountId,
        folderId: MessageThreadFolderId,
        user: AuthenticatedUser.Employee,
        now: HelsinkiDateTime = readTime,
    ) =
        messageController.getMessagesInFolder(
            dbInstance(),
            user,
            MockEvakaClock(now),
            accountId,
            folderId,
            page = 1,
        )

    private fun moveThreadToFolder(
        accountId: MessageAccountId,
        threadId: MessageThreadId,
        folderId: MessageThreadFolderId,
        user: AuthenticatedUser.Employee,
        now: HelsinkiDateTime = readTime,
    ) =
        messageController.moveThreadToFolder(
            dbInstance(),
            user,
            MockEvakaClock(now),
            accountId,
            threadId,
            folderId,
        )

    private fun disableAsyncJobRunning(f: () -> Unit) {
        asyncJobRunningEnabled = false
        try {
            f()
        } finally {
            asyncJobRunningEnabled = true
        }
    }
}

fun CitizenMessageThread.Regular.toSenderContentPairs(): List<Pair<MessageAccountId, String>> =
    this.messages.map { Pair(it.sender.id, it.content) }

fun MessageThread.toSenderContentPairs(): List<Pair<MessageAccountId, String>> =
    this.messages.map { Pair(it.sender.id, it.content) }
