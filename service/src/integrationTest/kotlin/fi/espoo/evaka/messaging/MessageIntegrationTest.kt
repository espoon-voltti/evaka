// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging

import fi.espoo.evaka.*
import fi.espoo.evaka.application.ApplicationType
import fi.espoo.evaka.application.notes.getApplicationNotes
import fi.espoo.evaka.attachment.AttachmentsController
import fi.espoo.evaka.daycare.CareType
import fi.espoo.evaka.messaging.MessageController.PostMessagePreflightResponse
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.serviceneed.ShiftCareType
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.AttachmentId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.MessageAccountId
import fi.espoo.evaka.shared.MessageContentId
import fi.espoo.evaka.shared.MessageDraftId
import fi.espoo.evaka.shared.MessageId
import fi.espoo.evaka.shared.MessageThreadId
import fi.espoo.evaka.shared.ServiceNeedOptionId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.insertDaycareAclRow
import fi.espoo.evaka.shared.auth.insertDaycareGroupAcl
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertTestApplication
import fi.espoo.evaka.shared.dev.insertTestDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.insertTestParentship
import fi.espoo.evaka.shared.dev.insertTestServiceNeed
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.domain.RealEvakaClock
import fi.espoo.evaka.shared.security.PilotFeature
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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
            roles = setOf(UserRole.UNIT_SUPERVISOR)
        )
    private val employee2 =
        AuthenticatedUser.Employee(
            id = EmployeeId(UUID.randomUUID()),
            roles = setOf(UserRole.UNIT_SUPERVISOR)
        )
    private val serviceWorker =
        AuthenticatedUser.Employee(
            id = EmployeeId(UUID.randomUUID()),
            roles = setOf(UserRole.SERVICE_WORKER)
        )
    private val messager =
        AuthenticatedUser.Employee(
            id = EmployeeId(UUID.randomUUID()),
            roles = setOf(UserRole.MESSAGING)
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
    private lateinit var messagerAccount: MessageAccountId

    private fun insertChild(
        tx: Database.Transaction,
        child: DevPerson,
        groupId: GroupId,
        daycareId: DaycareId = testDaycare.id,
        optionId: ServiceNeedOptionId = snDefaultDaycare.id,
        shiftCare: ShiftCareType = ShiftCareType.NONE
    ) {
        tx.insert(child, DevPersonType.CHILD)

        val placementId =
            tx.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycareId,
                    startDate = placementStart,
                    endDate = placementEnd
                )
            )
        tx.insertTestDaycareGroupPlacement(
            placementId,
            groupId,
            startDate = placementStart,
            endDate = placementEnd
        )
        tx.insertTestServiceNeed(
            confirmedBy = employee1.evakaUserId,
            placementId = placementId,
            period = FiniteDateRange(placementStart, placementEnd),
            optionId = optionId,
            shiftCare = shiftCare
        )
    }

    @BeforeEach
    fun setUp() {
        db.transaction { tx ->
            tx.insert(testArea)
            tx.insert(
                DevDaycare(
                    areaId = testArea.id,
                    id = testDaycare.id,
                    name = testDaycare.name,
                    enabledPilotFeatures = setOf(PilotFeature.MESSAGING)
                )
            )
            tx.insert(
                DevDaycare(
                    areaId = testArea.id,
                    id = testDaycare2.id,
                    name = testDaycare2.name,
                    type = setOf(CareType.FAMILY),
                    enabledPilotFeatures = setOf(PilotFeature.MESSAGING)
                )
            )

            fun insertGroup(id: GroupId): MessageAccountId {
                tx.insert(
                    DevDaycareGroup(id = id, daycareId = testDaycare.id, startDate = placementStart)
                )
                return tx.createDaycareGroupMessageAccount(id)
            }
            group1Account = insertGroup(groupId1)
            group2Account = insertGroup(groupId2)

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
                tx.insertTestParentship(
                    fridgeHeadId,
                    it.id
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
                    shiftCare = ShiftCareType.FULL
                )
                tx.insertGuardian(person4.id, it.id)
            }

            testChild_5.let {
                insertChild(tx, it, groupId1)
                tx.insertTestParentship(fridgeHeadId, it.id) // no guardian, no messages
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
                    shiftCare = ShiftCareType.INTERMITTENT
                )
                tx.insertGuardian(person6.id, it.id)
                tx.insertGuardian(person7.id, it.id)
            }

            employee1Account = tx.upsertEmployeeMessageAccount(employee1.id)
            tx.insertDaycareAclRow(testDaycare.id, employee1.id, UserRole.UNIT_SUPERVISOR)
            tx.insertDaycareGroupAcl(testDaycare.id, employee1.id, listOf(groupId1, groupId2))

            tx.insert(DevEmployee(id = employee2.id, firstName = "Foo", lastName = "Supervisor"))
            employee2Account = tx.upsertEmployeeMessageAccount(employee2.id)
            tx.insertDaycareAclRow(testDaycare2.id, employee2.id, UserRole.UNIT_SUPERVISOR)

            tx.insert(
                DevEmployee(id = serviceWorker.id, firstName = "Service", lastName = "Worker")
            )
            serviceWorkerAccount =
                tx.upsertEmployeeMessageAccount(serviceWorker.id, AccountType.SERVICE_WORKER)
            tx.insert(DevEmployee(id = messager.id, firstName = "Municipal", lastName = "Messager"))
            messagerAccount = tx.upsertEmployeeMessageAccount(messager.id, AccountType.MUNICIPAL)
        }
    }

    @Test
    fun `a thread is created, accessed and replied to by participants who are guardian of the same child`() {
        // when a message thread is created
        postNewThread(
            title = "Juhannus",
            message = "Juhannus tulee pian",
            messageType = MessageType.MESSAGE,
            sender = employee1Account,
            recipients = listOf(MessageRecipient(MessageRecipientType.CHILD, testChild_1.id)),
            user = employee1,
            now = sendTime
        )

        // then sender does not see it in received messages
        assertEquals(listOf(), getEmployeeMessageThreads(employee1Account, employee1))

        // then recipient can see it in received messages
        val threadWithOneReply = getRegularMessageThreads(person1)[0]
        assertEquals("Juhannus", threadWithOneReply.title)
        assertEquals(MessageType.MESSAGE, threadWithOneReply.messageType)
        assertEquals(
            listOf(Pair(employee1Account, "Juhannus tulee pian")),
            threadWithOneReply.toSenderContentPairs()
        )

        // when
        replyToMessage(
            person1,
            threadWithOneReply.messages[0].id,
            setOf(employee1Account, person2Account),
            "No niinpä näyttää tulevan",
            now = sendTime.plusSeconds(1)
        )

        // then recipients see the same data
        val person2Threads = getRegularMessageThreads(person2)
        assertEquals(getRegularMessageThreads(person1), person2Threads)
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
                    messageThread.messages
                )
            },
            person2Threads
        )

        // then thread has both messages in correct order
        assertEquals(1, person2Threads.size)
        val person2Thread = person2Threads[0]
        assertEquals("Juhannus", person2Thread.title)
        assertEquals(
            listOf(
                Pair(employee1Account, "Juhannus tulee pian"),
                Pair(person1Account, "No niinpä näyttää tulevan")
            ),
            person2Thread.toSenderContentPairs()
        )

        // when person one replies to the employee only
        replyToMessage(
            person1,
            person2Thread.messages.last().id,
            setOf(employee1Account),
            "person 2 does not see this",
            now = sendTime.plusSeconds(2)
        )

        // then person one and employee see the new message
        val threadContentWithTwoReplies =
            listOf(
                Pair(employee1Account, "Juhannus tulee pian"),
                Pair(person1Account, "No niinpä näyttää tulevan"),
                Pair(person1Account, "person 2 does not see this")
            )
        assertEquals(
            threadContentWithTwoReplies,
            getRegularMessageThreads(person1)[0].toSenderContentPairs()
        )
        assertEquals(
            threadContentWithTwoReplies,
            getEmployeeMessageThreads(employee1Account, employee1)[0].toSenderContentPairs()
        )

        // then person two does not see the message
        assertEquals(
            listOf(
                Pair(employee1Account, "Juhannus tulee pian"),
                Pair(person1Account, "No niinpä näyttää tulevan")
            ),
            getRegularMessageThreads(person2)[0].toSenderContentPairs()
        )

        // when author replies to person two
        replyToMessage(
            user = employee1,
            sender = employee1Account,
            messageId = threadWithOneReply.messages.last().id,
            recipientAccountIds = setOf(person2Account),
            content = "person 1 does not see this",
            now = sendTime.plusSeconds(3)
        )

        // then person two sees that
        assertEquals(
            listOf(
                Pair(employee1Account, "Juhannus tulee pian"),
                Pair(person1Account, "No niinpä näyttää tulevan"),
                Pair(employee1Account, "person 1 does not see this")
            ),
            getRegularMessageThreads(person2)[0].toSenderContentPairs()
        )

        // then person one does not see that
        assertEquals(
            threadContentWithTwoReplies,
            getRegularMessageThreads(person1)[0].toSenderContentPairs()
        )

        // then employee sees all the messages
        assertEquals(
            listOf(
                Pair(employee1Account, "Juhannus tulee pian"),
                Pair(person1Account, "No niinpä näyttää tulevan"),
                Pair(person1Account, "person 2 does not see this"),
                Pair(employee1Account, "person 1 does not see this")
            ),
            getEmployeeMessageThreads(employee1Account, employee1)[0].toSenderContentPairs()
        )

        // then employee can see all sent messages
        assertEquals(
            listOf("person 1 does not see this", "Juhannus tulee pian"),
            getSentMessages(employee1Account, employee1).map { it.content }
        )
    }

    @Test
    fun `guardian can send a message only to group and other guardian, not group staff`() {
        fun getRecipients() = getCitizenReceivers(person1).messageAccounts.map { it.id }.toSet()

        assertEquals(setOf(group1Account, person2Account, employee1Account), getRecipients())

        // When a supervisor works as staff, her account is deactivated
        db.transaction { it.deactivateEmployeeMessageAccount(employee1.id) }
        assertEquals(setOf(group1Account, person2Account), getRecipients())
    }

    @Test
    fun `a message is split to several threads by guardianship`() {
        // when a new thread is created to several recipients who do not all have common children
        val title = "Thread splitting"
        val content = "This message is sent to several participants and split to threads"
        val recipients =
            listOf(
                MessageRecipient(MessageRecipientType.CHILD, testChild_1.id),
                MessageRecipient(MessageRecipientType.CHILD, testChild_4.id),
                MessageRecipient(MessageRecipientType.CHILD, testChild_6.id)
            )
        val recipientNames = listOf("Hippiäiset", "Jani")
        postNewThread(
            title = title,
            message = content,
            messageType = MessageType.MESSAGE,
            sender = employee1Account,
            recipients = recipients,
            recipientNames = recipientNames,
            user = employee1
        )

        // then three threads should be created
        db.read {
            assertEquals(
                1,
                @Suppress("DEPRECATION")
                it.createQuery("SELECT COUNT(id) FROM message_content").exactlyOne<Int>()
            )
            assertEquals(
                3,
                @Suppress("DEPRECATION")
                it.createQuery("SELECT COUNT(id) FROM message_thread").exactlyOne<Int>()
            )
            @Suppress("DEPRECATION")
            assertEquals(3, it.createQuery("SELECT COUNT(id) FROM message").exactlyOne<Int>())
            assertEquals(
                5,
                @Suppress("DEPRECATION")
                it.createQuery("SELECT COUNT(id) FROM message_recipients").exactlyOne<Int>()
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
            listOf(person1Threads, person2Threads, person3Threads, person4Threads, person5Threads)
                .flatten()
        assertEquals(5, allThreads.size)
        allThreads.forEach {
            assertEquals(title, it.title)
            assertEquals(content, it.messages[0].content)
        }

        // when person 1 replies to thread
        replyToMessage(
            person1,
            person1Threads.first().messages.first().id,
            setOf(employee1Account, person2Account),
            "Hello"
        )

        // then only the participants should get the message
        val employeeThreads = getEmployeeMessageThreads(employee1Account, employee1)
        assertEquals(
            listOf(Pair(employee1Account, content), Pair(person1Account, "Hello")),
            employeeThreads.map { it.toSenderContentPairs() }.flatten()
        )
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
                    messageThread.messages
                )
            },
            getRegularMessageThreads(person1)
        )
        assertEquals(
            listOf(Pair(employee1Account, content), Pair(person1Account, "Hello")),
            getRegularMessageThreads(person2).map { it.toSenderContentPairs() }.flatten()
        )

        assertEquals(person3Threads, getRegularMessageThreads(person3))
        assertEquals(person4Threads, getRegularMessageThreads(person4))
    }

    @Test
    fun `a bulletin cannot be replied to by the recipients`() {
        // when a bulletin thread is created
        postNewThread(
            title = "Tiedote",
            message = "Juhannus tulee pian",
            messageType = MessageType.BULLETIN,
            sender = employee1Account,
            recipients = listOf(MessageRecipient(MessageRecipientType.CHILD, testChild_1.id)),
            user = employee1
        )

        // then the recipient can see it
        val thread = getRegularMessageThreads(person1).first()
        assertEquals("Tiedote", thread.title)
        assertEquals(MessageType.BULLETIN, thread.messageType)
        assertEquals(
            listOf(Pair(employee1Account, "Juhannus tulee pian")),
            thread.toSenderContentPairs()
        )

        // when the recipient tries to reply to the bulletin, it is denied
        assertThrows<Forbidden> {
            replyToMessage(
                user = person1,
                messageId = thread.messages.first().id,
                recipientAccountIds = setOf(thread.messages.first().sender.id),
                content = "Kiitos tiedosta"
            )
        }

        // when the author himself replies to the bulletin, it succeeds
        //
        // NOTE: This will not be implemented for now, because author
        //       replying to their own message (without other replies)
        //       lacks spec. It would be bad UX to only allow replies
        //       to own bulletin only. (Date 25.11.2021)
        replyToMessage(
            sender = employee1Account,
            user = employee1,
            messageId = thread.messages.last().id,
            recipientAccountIds = setOf(person1Account),
            content = "Nauttikaa siitä"
        )

        // then the recipient can see it
        assertEquals(
            listOf(
                Pair(employee1Account, "Juhannus tulee pian"),
                Pair(employee1Account, "Nauttikaa siitä")
            ),
            getRegularMessageThreads(person1).first().toSenderContentPairs()
        )
    }

    @Test
    fun `messages can be marked read`() {
        // when a message thread is created
        postNewThread(
            title = "t1",
            message = "m1",
            messageType = MessageType.MESSAGE,
            sender = employee1Account,
            recipients = listOf(MessageRecipient(MessageRecipientType.CHILD, testChild_1.id)),
            user = employee1
        )

        // then
        val person1UnreadMessages = getUnreadReceivedMessages(person1Account, person1)
        assertEquals(1, person1UnreadMessages.size)
        assertEquals(1, getUnreadReceivedMessages(person2Account, person2).size)
        assertEquals(0, getUnreadReceivedMessages(employee1Account, employee1).size)

        // when a person replies to the thread
        replyToMessage(
            person1,
            person1UnreadMessages.first().id,
            setOf(employee1Account, person2Account),
            "reply"
        )

        // then
        assertEquals(1, getUnreadReceivedMessages(employee1Account, employee1).size)
        assertEquals(1, getUnreadReceivedMessages(person1Account, person1).size)
        assertEquals(2, getUnreadReceivedMessages(person2Account, person2).size)

        // when a thread is marked read
        markThreadRead(person2, getRegularMessageThreads(person2).first().id)

        // then the thread is marked read
        assertEquals(1, getUnreadReceivedMessages(employee1Account, employee1).size)
        assertEquals(1, getUnreadReceivedMessages(person1Account, person1).size)
        assertEquals(0, getUnreadReceivedMessages(person2Account, person2).size)
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
                "evaka-logo.png"
            )
        }
        assertThrows<Forbidden> {
            attachmentsController.deleteAttachmentHandler(
                dbInstance(),
                employee2,
                clock,
                attachmentId
            )
        }

        // then the author can read and delete the attachment
        attachmentsController.getAttachment(
            dbInstance(),
            employee1,
            clock,
            attachmentId,
            "evaka-logo.png"
        )
        attachmentsController.deleteAttachmentHandler(dbInstance(), employee1, clock, attachmentId)

        // a user cannot upload attachments to another user's draft
        assertThrows<Forbidden> { uploadMessageAttachment(employee2, draftId) }

        val attachmentIds =
            setOf(
                uploadMessageAttachment(employee1, draftId),
                uploadMessageAttachment(employee1, draftId)
            )

        // when a message thread with attachment is created
        postNewThread(
            title = "t1",
            message = "m1",
            messageType = MessageType.MESSAGE,
            sender = employee1Account,
            recipients = listOf(MessageRecipient(MessageRecipientType.CHILD, testChild_1.id)),
            user = employee1,
            attachmentIds = attachmentIds,
            draftId = draftId
        )

        // then
        // the draft is deleted
        assertEquals(0, db.read { it.getDrafts(employee1Account) }.size)

        // the attachments are associated to a message
        assertEquals(
            2,
            db.read {
                @Suppress("DEPRECATION")
                it.createQuery(
                        "SELECT COUNT(*) FROM attachment WHERE message_content_id IS NOT NULL"
                    )
                    .exactlyOne<Int>()
            }
        )

        // the author can read the attachment
        attachmentsController.getAttachment(
            dbInstance(),
            employee1,
            clock,
            attachmentIds.first(),
            "evaka-logo.png"
        )
        // another employee cannot read the attachment
        assertThrows<Forbidden> {
            attachmentsController.getAttachment(
                dbInstance(),
                employee2,
                clock,
                attachmentIds.first(),
                "evaka-logo.png"
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
            "evaka-logo.png"
        )

        // another citizen cannot read the attachment
        assertThrows<Forbidden> {
            attachmentsController.getAttachment(
                dbInstance(),
                person3,
                clock,
                attachmentIds.first(),
                "evaka-logo.png"
            )
        }
    }

    @Test
    fun `employee with access to two groups cannot send messages as group1 to group2`() {
        postNewThread(
            title = "Juhannus",
            message = "Juhannus tulee pian",
            messageType = MessageType.MESSAGE,
            sender = group1Account,
            recipients = listOf(MessageRecipient(MessageRecipientType.GROUP, groupId2)),
            user = employee1,
        )
        assertEquals(0, getRegularMessageThreads(person4).size)

        postNewThread(
            title = "Juhannus",
            message = "Juhannus tulee pian",
            messageType = MessageType.MESSAGE,
            sender = group2Account,
            recipients = listOf(MessageRecipient(MessageRecipientType.GROUP, groupId2)),
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
            recipients = listOf(MessageRecipient(MessageRecipientType.CHILD, testChild_4.id)),
            user = employee1,
        )
        assertEquals(0, getRegularMessageThreads(person4).size)

        postNewThread(
            title = "Juhannus",
            message = "Juhannus tulee pian",
            messageType = MessageType.MESSAGE,
            sender = group2Account,
            recipients = listOf(MessageRecipient(MessageRecipientType.CHILD, testChild_4.id)),
            user = employee1,
        )
        assertEquals(1, getRegularMessageThreads(person4).size)
    }

    @Test
    fun `unread message counts and marking messages read`() {
        postNewThread(
            title = "Juhannus",
            message = "Juhannus tulee pian",
            messageType = MessageType.MESSAGE,
            sender = employee1Account,
            recipients = listOf(MessageRecipient(MessageRecipientType.CHILD, testChild_1.id)),
            user = employee1
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
        replyToMessage(
            user = person1,
            messageId = getRegularMessageThreads(person1).first().messages.last().id,
            recipientAccountIds = setOf(person2Account, employee1Account),
            content = "Juhannus on jo ohi"
        )
        assertEquals(1, unreadMessagesCount(employee1Account, employee1))
        assertEquals(0, unreadMessagesCount(person1))
        assertEquals(2, unreadMessagesCount(person2))
    }

    @Test
    fun `message sent by employee should not be visible to citizen before the undo time has passed (and asyncjob run)`() {
        disableAsyncJobRunning {
            postNewThread(
                title = "Juhannus",
                message = "Juhannus tulee pian",
                messageType = MessageType.MESSAGE,
                sender = employee1Account,
                recipients = listOf(MessageRecipient(MessageRecipientType.CHILD, testChild_1.id)),
                user = employee1,
                now = sendTime,
            )
        }

        // Message is not visible to citizen before undo time has passed
        val beforeUndo = sendTime.plusSeconds(5)
        assertEquals(0, unreadMessagesCount(person1, now = beforeUndo))
        assertTrue(getUnreadReceivedMessages(person1Account, person1, now = beforeUndo).isEmpty())

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
        replyToMessage(
            messageId = personUnreadMessages.first().id,
            content = "Ihanko totta?",
            recipientAccountIds = setOf(employee1Account),
            user = person1,
            now = readTime,
        )

        // Employee replies
        val sendReplyTime = readTime.plusMinutes(5)
        disableAsyncJobRunning {
            replyToMessage(
                messageId =
                    getUnreadReceivedMessages(employee1Account, employee1, now = sendReplyTime)
                        .first()
                        .id,
                content = "Ei sittenkään, väärä hälytys",
                recipientAccountIds = setOf(person1Account),
                sender = employee1Account,
                user = employee1,
                now = sendReplyTime,
            )
        }

        // Reply is not visible to citizen before undo time has passed
        val beforeReplyUndo = sendReplyTime.plusSeconds(5)
        assertEquals(1, unreadMessagesCount(person1, now = beforeReplyUndo))
        assertEquals(
            1,
            getUnreadReceivedMessages(person1Account, person1, now = beforeReplyUndo).size
        )

        // Reply is still not visible to citizen if the async job hasn't run
        val readReplyTime = sendReplyTime.plusSeconds(30)
        assertEquals(1, unreadMessagesCount(person1, now = readReplyTime))
        assertEquals(
            1,
            getUnreadReceivedMessages(person1Account, person1, now = readReplyTime).size
        )

        // Reply is visible to citizen after the async job has run
        asyncJobRunner.runPendingJobsSync(MockEvakaClock(readReplyTime))
        assertEquals(2, unreadMessagesCount(person1, now = readReplyTime))
        assertEquals(
            2,
            getUnreadReceivedMessages(person1Account, person1, now = readReplyTime).size
        )
    }

    @Test
    fun `sending a message with a relatedApplicationId creates a note on the application with the contents and a link to the thread`() {
        val applicationId =
            db.transaction { tx ->
                tx.insertTestApplication(
                    childId = testChild_1.id,
                    guardianId = testAdult_1.id,
                    type = ApplicationType.DAYCARE
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
                recipients = listOf(MessageRecipient(MessageRecipientType.CITIZEN, testAdult_1.id)),
                user = serviceWorker,
                relatedApplicationId = applicationId
            )

        db.transaction { tx ->
            // then a note is created on the application
            val applicationNotes = tx.getApplicationNotes(applicationId)
            assertEquals(1, applicationNotes.size)
            val note = applicationNotes.first()
            assertEquals(messageContent, note.content)

            // and the threadId points to the correct thread
            val messageThreadId =
                @Suppress("DEPRECATION")
                tx.createQuery("""SELECT thread_id FROM message WHERE content_id = :contentId""")
                    .bind("contentId", messageContentId)
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
                    type = ApplicationType.DAYCARE
                )
            }

        val messageContent = "Tähän viestiin pitäisi pystyä vastaamaan"
        // when a message thread related to an application is created
        postNewThread(
            title = "Vastaa heti",
            message = messageContent,
            messageType = MessageType.MESSAGE,
            sender = serviceWorkerAccount,
            recipients = listOf(MessageRecipient(MessageRecipientType.CITIZEN, testAdult_1.id)),
            user = serviceWorker,
            relatedApplicationId = applicationId
        )
        val thread = getRegularMessageThreads(person1)[0]
        replyToMessage(
            messageId = thread.messages.first().id,
            content = "Vastaus",
            recipientAccountIds = setOf(serviceWorkerAccount),
            user = person1,
            now = clock.now()
        )
        assertEquals(1, unreadMessagesCount(serviceWorkerAccount, serviceWorker))
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
                recipients = listOf(MessageRecipient(MessageRecipientType.CITIZEN, testAdult_1.id)),
                user = serviceWorker,
                relatedApplicationId = null
            )
        }
    }

    @Test
    fun `service workers cannot send messages to children`() {
        // when a message thread related to an application is created
        assertThrows<BadRequest> {
            postNewThread(
                title = "title",
                message = "content",
                messageType = MessageType.MESSAGE,
                sender = serviceWorkerAccount,
                recipients = listOf(MessageRecipient(MessageRecipientType.CHILD, testChild_1.id)),
                user = serviceWorker,
                relatedApplicationId = ApplicationId(UUID.randomUUID())
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
                    type = ApplicationType.DAYCARE
                )
            }

        // when a message thread related to an application is created
        assertThrows<BadRequest> {
            postNewThread(
                title = "title",
                message = "content",
                messageType = MessageType.MESSAGE,
                sender = serviceWorkerAccount,
                recipients = listOf(MessageRecipient(MessageRecipientType.CITIZEN, testAdult_2.id)),
                user = serviceWorker,
                relatedApplicationId = applicationId
            )
        }
    }

    @Test
    fun `sensitive messages can be sent from personal accounts for single child recipients`() {
        postNewThread(
            title = "title",
            message = "Juhannus tulee pian",
            messageType = MessageType.MESSAGE,
            sender = employee1Account,
            recipients = listOf(MessageRecipient(MessageRecipientType.CHILD, testChild_1.id)),
            user = employee1,
            sensitive = true
        )

        // then the recipient can see it
        assertEquals(
            listOf(Pair(employee1Account, "Juhannus tulee pian")),
            getRegularMessageThreads(person1).first().toSenderContentPairs()
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
                        MessageRecipient(MessageRecipientType.CHILD, testChild_1.id),
                        MessageRecipient(MessageRecipientType.CHILD, testChild_2.id)
                    ),
                user = employee1,
                sensitive = true
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
                recipients = listOf(MessageRecipient(MessageRecipientType.CITIZEN, testAdult_1.id)),
                user = employee1,
                sensitive = true
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
                recipients = listOf(MessageRecipient(MessageRecipientType.CHILD, testAdult_1.id)),
                user = employee1,
                sensitive = true
            )
        }
    }

    @Test
    fun `create messages preflight check returns recipient count`() {
        val response =
            postNewThreadPreflightCheck(
                user = employee1,
                sender = employee1Account,
                recipients =
                    listOf(
                        MessageRecipient(MessageRecipientType.CHILD, testChild_1.id),
                        MessageRecipient(MessageRecipientType.CHILD, testChild_3.id)
                    )
            )
        assertEquals(PostMessagePreflightResponse(numberOfRecipientAccounts = 3), response)
    }

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
                        startDate = placementStart
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
            recipients = listOf(group1Account)
        )

        // Child 1 in group 1, child 2 in group 2 -> ok
        postNewThread(
            user = person2,
            title = "title",
            message = "content",
            children = listOf(testChild_1.id, testChild_2.id),
            recipients = listOf(group1Account, group2Account)
        )

        // None of the children in group 2 -> fail
        assertThrows<BadRequest> {
            postNewThread(
                user = person2,
                title = "title",
                message = "content",
                children = listOf(testChild_1.id, testChild_3.id),
                recipients = listOf(group1Account, group2Account)
            )
        }

        // Child 1 in group 1, child 7 in group 3, but groups in different unit -> not ok
        assertThrows<BadRequest> {
            postNewThread(
                user = person2,
                title = "title",
                message = "content",
                children = listOf(testChild_1.id, testChild_7.id),
                recipients = listOf(group1Account, group3Account)
            )
        }
    }

    @Test
    fun `recipient list can be filtered`() {
        postNewThread(
            title = "Vappu",
            message = "Vappuna paistaa aurinko",
            messageType = MessageType.BULLETIN,
            sender = messagerAccount,
            recipients =
                listOf(
                    MessageRecipient(MessageRecipientType.CHILD, testChild_1.id),
                    MessageRecipient(MessageRecipientType.CHILD, testChild_3.id),
                    MessageRecipient(MessageRecipientType.CHILD, testChild_4.id),
                    MessageRecipient(MessageRecipientType.CHILD, testChild_6.id),
                    MessageRecipient(MessageRecipientType.CHILD, testChild_8.id),
                ),
            filters = MessageController.PostMessageFilters(yearsOfBirth = listOf(2017)),
            user = messager,
            now = sendTime
        )

        db.read {
            assertEquals(
                2,
                it.createQuery { sql("SELECT COUNT(id) FROM message_recipients") }.exactlyOne<Int>()
            )
        }
    }

    @Test
    fun `recipient list can be filtered 2`() {
        postNewThread(
            title = "Vappu",
            message = "Vappuna paistaa aurinko",
            messageType = MessageType.BULLETIN,
            sender = messagerAccount,
            recipients =
                listOf(
                    MessageRecipient(MessageRecipientType.CHILD, testChild_1.id),
                    MessageRecipient(MessageRecipientType.CHILD, testChild_3.id),
                    MessageRecipient(MessageRecipientType.CHILD, testChild_4.id),
                    MessageRecipient(MessageRecipientType.CHILD, testChild_6.id),
                    MessageRecipient(MessageRecipientType.CHILD, testChild_8.id),
                ),
            filters =
                MessageController.PostMessageFilters(
                    serviceNeedOptionIds =
                        listOf(snDefaultPartDayDaycare.id, snDefaultFiveYearOldsPartDayDaycare.id)
                ),
            user = messager,
            now = sendTime
        )

        db.read {
            assertEquals(
                3,
                it.createQuery { sql("SELECT COUNT(id) FROM message_recipients") }.exactlyOne<Int>()
            )
        }
    }

    @Test
    fun `recipient list can be filtered 3`() {
        postNewThread(
            title = "Vappu",
            message = "Vappuna paistaa aurinko",
            messageType = MessageType.BULLETIN,
            sender = messagerAccount,
            recipients =
                listOf(
                    MessageRecipient(MessageRecipientType.CHILD, testChild_1.id),
                    MessageRecipient(MessageRecipientType.CHILD, testChild_3.id),
                    MessageRecipient(MessageRecipientType.CHILD, testChild_4.id),
                    MessageRecipient(MessageRecipientType.CHILD, testChild_6.id),
                    MessageRecipient(MessageRecipientType.CHILD, testChild_8.id),
                ),
            filters =
                MessageController.PostMessageFilters(
                    shiftCare = true,
                    intermittentShiftCare = true
                ),
            user = messager,
            now = sendTime
        )

        db.read {
            assertEquals(
                3,
                it.createQuery { sql("SELECT COUNT(id) FROM message_recipients") }.exactlyOne<Int>()
            )
        }
    }

    @Test
    fun `recipient list can be filtered 4`() {
        postNewThread(
            title = "Vappu",
            message = "Vappuna paistaa aurinko",
            messageType = MessageType.BULLETIN,
            sender = messagerAccount,
            recipients =
                listOf(
                    MessageRecipient(MessageRecipientType.CHILD, testChild_1.id),
                    MessageRecipient(MessageRecipientType.CHILD, testChild_3.id),
                    MessageRecipient(MessageRecipientType.CHILD, testChild_4.id),
                    MessageRecipient(MessageRecipientType.CHILD, testChild_6.id),
                    MessageRecipient(MessageRecipientType.CHILD, testChild_8.id),
                ),
            filters = MessageController.PostMessageFilters(familyDaycare = true),
            user = messager,
            now = sendTime
        )

        db.read {
            assertEquals(
                2,
                it.createQuery { sql("SELECT COUNT(id) FROM message_recipients") }.exactlyOne<Int>()
            )
        }
    }

    @Test
    fun `preflight check takes into account recipient filtering`() {
        val response =
            postNewThreadPreflightCheck(
                user = messager,
                sender = messagerAccount,
                recipients =
                    listOf(
                        MessageRecipient(MessageRecipientType.CHILD, testChild_1.id),
                        MessageRecipient(MessageRecipientType.CHILD, testChild_3.id),
                    ),
                filters =
                    MessageController.PostMessageFilters(
                        yearsOfBirth = listOf(2018),
                        serviceNeedOptionIds = listOf()
                    )
            )
        assertEquals(PostMessagePreflightResponse(numberOfRecipientAccounts = 2), response)
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
        draftId: MessageDraftId
    ): AttachmentId =
        attachmentsController.uploadMessageAttachment(
            dbInstance(),
            user,
            clock,
            draftId,
            MockMultipartFile("evaka-logo.png", "evaka-logo.png", null, pngFile.readBytes())
        )

    private fun postNewThreadPreflightCheck(
        sender: MessageAccountId,
        recipients: List<MessageRecipient>,
        filters: MessageController.PostMessageFilters? = null,
        user: AuthenticatedUser.Employee,
        now: HelsinkiDateTime = sendTime
    ): PostMessagePreflightResponse {
        return messageController.createMessagePreflightCheck(
            dbInstance(),
            user,
            MockEvakaClock(now),
            sender,
            MessageController.PostMessagePreflightBody(
                recipients = recipients.toSet(),
                filters = filters
            )
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
    ): MessageContentId? {
        val messageContentId =
            messageController.createMessage(
                dbInstance(),
                user,
                MockEvakaClock(now),
                sender,
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
                    filters = filters
                )
            )
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
                )
            )
        if (asyncJobRunningEnabled) {
            asyncJobRunner.runPendingJobsSync(MockEvakaClock(now.plusSeconds(30)))
        }
        return messageThreadId
    }

    private fun replyToMessage(
        user: AuthenticatedUser.Citizen,
        messageId: MessageId,
        recipientAccountIds: Set<MessageAccountId>,
        content: String,
        now: HelsinkiDateTime = sendTime,
    ) {
        messageControllerCitizen.replyToThread(
            dbInstance(),
            user,
            MockEvakaClock(now),
            messageId,
            ReplyToMessageBody(content = content, recipientAccountIds = recipientAccountIds)
        )
        if (asyncJobRunningEnabled) {
            asyncJobRunner.runPendingJobsSync(MockEvakaClock(now.plusSeconds(30)))
        }
    }

    private fun replyToMessage(
        user: AuthenticatedUser.Employee,
        sender: MessageAccountId,
        messageId: MessageId,
        recipientAccountIds: Set<MessageAccountId>,
        content: String,
        now: HelsinkiDateTime = sendTime,
    ) {
        messageController.replyToThread(
            dbInstance(),
            user,
            MockEvakaClock(now),
            sender,
            messageId,
            ReplyToMessageBody(content = content, recipientAccountIds = recipientAccountIds)
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
            threadId
        )
    }

    private fun getRegularMessageThreads(
        user: AuthenticatedUser.Citizen,
        now: HelsinkiDateTime = readTime,
    ): List<CitizenMessageThread.Regular> {
        return messageControllerCitizen
            .getReceivedMessages(dbInstance(), user, MockEvakaClock(now), page = 1, pageSize = 100)
            .data
            .filterIsInstance<CitizenMessageThread.Regular>()
    }

    private fun getEmployeeMessageThreads(
        accountId: MessageAccountId,
        user: AuthenticatedUser.Employee,
        now: HelsinkiDateTime = readTime,
    ): List<MessageThread> {
        return messageController
            .getReceivedMessages(
                dbInstance(),
                user,
                MockEvakaClock(now),
                accountId,
                page = 1,
                pageSize = 100
            )
            .data
    }

    private fun getSentMessages(
        accountId: MessageAccountId,
        user: AuthenticatedUser.Employee
    ): List<SentMessage> {
        return messageController
            .getSentMessages(
                dbInstance(),
                user,
                MockEvakaClock(readTime),
                accountId,
                page = 1,
                pageSize = 100
            )
            .data
    }

    private fun getCitizenReceivers(
        user: AuthenticatedUser.Citizen
    ): MessageControllerCitizen.GetReceiversResponse {
        return messageControllerCitizen.getReceivers(dbInstance(), user, MockEvakaClock(readTime))
    }

    private fun unreadMessagesCount(
        user: AuthenticatedUser.Citizen,
        now: HelsinkiDateTime = readTime
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
            ?.unreadCount ?: throw Exception("No unread count for account $accountId")
    }

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
