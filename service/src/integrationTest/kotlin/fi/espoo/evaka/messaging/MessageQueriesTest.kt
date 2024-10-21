// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.*
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.insertDaycareAclRow
import fi.espoo.evaka.shared.config.testFeatureConfig
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.*
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import fi.espoo.evaka.shared.security.PilotFeature
import fi.espoo.evaka.shared.security.actionrule.DefaultActionRuleMapping
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/*
 * TODO: These tests should be moved to MessageIntegrationTest because inserting directly to the database doesn't
 * reflect reality: it's controllers' and MessageService's job but these tests don't invoke them
 */
class MessageQueriesTest : PureJdbiTest(resetDbBeforeEach = true) {
    private val accessControl = AccessControl(DefaultActionRuleMapping(), noopTracer)

    private val person1 = DevPerson(firstName = "Firstname", lastName = "Person")
    private val person2 = DevPerson(firstName = "Firstname", lastName = "Person Two")
    private val employee1 = DevEmployee(firstName = "Firstname", lastName = "Employee")
    private val employee2 = DevEmployee(firstName = "Firstname", lastName = "Employee Two")

    private lateinit var clock: EvakaClock
    private val sendTime = HelsinkiDateTime.of(LocalDate.of(2022, 5, 14), LocalTime.of(12, 11))

    private data class TestAccounts(
        val person1: MessageAccount,
        val person2: MessageAccount,
        val employee1: MessageAccount,
        val employee2: MessageAccount,
    )

    private lateinit var accounts: TestAccounts

    @BeforeEach
    fun setUp() {
        clock = MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2022, 11, 8), LocalTime.of(13, 1)))
        db.transaction { tx ->
            tx.insert(person1, DevPersonType.ADULT)
            tx.insert(person2, DevPersonType.ADULT)
            tx.insert(employee1)
            tx.insert(employee2)
            accounts =
                TestAccounts(
                    person1 = tx.getAccount(person1),
                    person2 = tx.getAccount(person2),
                    employee1 = tx.createAccount(employee1),
                    employee2 = tx.createAccount(employee2),
                )
        }
    }

    @Test
    fun `a thread can be created`() {
        val content = "Content"
        val title = "Hello"
        createThread(title, content, accounts.employee1, listOf(accounts.person1, accounts.person2))

        assertEquals(
            setOf(accounts.person1.id, accounts.person2.id),
            db.read {
                it.createQuery { sql("SELECT recipient_id FROM message_recipients") }
                    .toSet<MessageAccountId>()
            },
        )
        assertEquals(
            content,
            db.read {
                it.createQuery { sql("SELECT content FROM message_content") }.exactlyOne<String>()
            },
        )
        assertEquals(
            title,
            db.read {
                it.createQuery { sql("SELECT title FROM message_thread") }.exactlyOne<String>()
            },
        )
        assertEquals(
            "Employee Firstname",
            db.read {
                it.createQuery { sql("SELECT sender_name FROM message") }.exactlyOne<String>()
            },
        )
        assertEquals(
            setOf("Person Firstname", "Person Two Firstname"),
            db.read {
                    it.createQuery { sql("SELECT recipient_names FROM message") }
                        .exactlyOne<Array<String>>()
                }
                .toSet(),
        )
    }

    @Test
    fun `messages received by account are grouped properly`() {
        val thread1Id =
            createThread(
                "Hello",
                "Content",
                accounts.employee1,
                listOf(accounts.person1, accounts.person2),
                sendTime,
            )
        val thread2Id =
            createThread(
                "Newest thread",
                "Content 2",
                accounts.employee1,
                listOf(accounts.person1),
                sendTime.plusSeconds(1),
            )
        createThread(
            "Lone Thread",
            "Alone",
            accounts.employee2,
            listOf(accounts.employee2),
            sendTime.plusSeconds(2),
        )

        // employee is not a recipient in any threads
        assertEquals(
            0,
            db.read {
                    it.getReceivedThreads(
                        accounts.employee1.id,
                        10,
                        1,
                        "Espoo",
                        "Espoon palveluohjaus",
                    )
                }
                .data
                .size,
        )
        val personResult =
            db.read { it.getThreads(accounts.employee1.id, 10, 1, "Espoo", "Espoon palveluohjaus") }
        assertEquals(2, personResult.data.size)

        val thread = personResult.data.first()
        assertEquals(thread2Id, thread.id)
        assertEquals("Newest thread", thread.title)

        // when the thread is marked read for person 1
        db.transaction { it.markThreadRead(clock, accounts.person1.id, thread1Id) }

        // then the message has correct readAt
        val person1Threads =
            db.read { it.getThreads(accounts.person1.id, 10, 1, "Espoo", "Espoon palveluohjaus") }
        assertEquals(2, person1Threads.data.size)
        val readMessages = person1Threads.data.flatMap { it.messages.mapNotNull { m -> m.readAt } }
        assertEquals(1, readMessages.size)
        assertEquals(clock.now(), readMessages[0])

        // then person 2 threads are not affected
        assertEquals(
            0,
            db.read { it.getThreads(accounts.person2.id, 10, 1, "Espoo", "Espoon palveluohjaus") }
                .data
                .flatMap { it.messages.mapNotNull { m -> m.readAt } }
                .size,
        )

        // when employee gets a reply
        replyToThread(
            thread2Id,
            accounts.person1,
            setOf(accounts.employee1),
            "Just replying here",
            thread.messages.last().id,
            now = sendTime.plusSeconds(3),
        )

        // then employee sees the thread
        val employeeResult =
            db.read {
                it.getReceivedThreads(accounts.employee1.id, 10, 1, "Espoo", "Espoon palveluohjaus")
            }
        assertEquals(1, employeeResult.data.size)
        assertEquals("Newest thread", employeeResult.data[0].title)
        assertEquals(2, employeeResult.data[0].messages.size)

        // person 1 is recipient in both threads
        val person1Result =
            db.read { it.getThreads(accounts.person1.id, 10, 1, "Espoo", "Espoon palveluohjaus") }
        assertEquals(2, person1Result.data.size)

        val newestThread = person1Result.data[0]
        assertEquals(thread2Id, newestThread.id)
        assertEquals("Newest thread", newestThread.title)
        assertEquals(
            listOf(
                Pair(accounts.employee1, "Content 2"),
                Pair(accounts.person1, "Just replying here"),
            ),
            newestThread.messages.map { Pair(it.sender, it.content) },
        )
        assertEquals(employeeResult.data[0], newestThread)

        val oldestThread = person1Result.data[1]
        assertEquals(thread1Id, oldestThread.id)
        assertNotNull(oldestThread.messages.find { it.content == "Content" }?.readAt)
        assertNull(oldestThread.messages.find { it.content == "Just replying here" }?.readAt)

        // person 2 is recipient in the oldest thread only
        val person2Result =
            db.read { it.getThreads(accounts.person2.id, 10, 1, "Espoo", "Espoon palveluohjaus") }
        assertEquals(1, person2Result.data.size)
        assertEquals(oldestThread.id, person2Result.data[0].id)
        assertEquals(0, person2Result.data.flatMap { it.messages }.mapNotNull { it.readAt }.size)

        // employee 2 is participating with himself
        val employee2Result =
            db.read {
                it.getReceivedThreads(accounts.employee2.id, 10, 1, "Espoo", "Espoon palveluohjaus")
            }
        assertEquals(1, employee2Result.data.size)
        assertEquals(1, employee2Result.data[0].messages.size)
        assertEquals(accounts.employee2, employee2Result.data[0].messages[0].sender)
        assertEquals("Alone", employee2Result.data[0].messages[0].content)
    }

    @Test
    fun `received messages can be paged`() {
        createThread("t1", "c1", accounts.employee1, listOf(accounts.person1))
        createThread("t2", "c2", accounts.employee1, listOf(accounts.person1))

        val messages =
            db.read { it.getThreads(accounts.person1.id, 10, 1, "Espoo", "Espoon palveluohjaus") }
        assertEquals(2, messages.total)
        assertEquals(2, messages.data.size)
        assertEquals(setOf("t1", "t2"), messages.data.map { it.title }.toSet())

        val (page1, page2) =
            db.read {
                listOf(
                    it.getThreads(accounts.person1.id, 1, 1, "Espoo", "Espoon palveluohjaus"),
                    it.getThreads(accounts.person1.id, 1, 2, "Espoo", "Espoon palveluohjaus"),
                )
            }
        assertEquals(2, page1.total)
        assertEquals(2, page1.pages)
        assertEquals(1, page1.data.size)
        assertEquals(messages.data[0], page1.data[0])

        assertEquals(2, page2.total)
        assertEquals(2, page2.pages)
        assertEquals(1, page2.data.size)
        assertEquals(messages.data[1], page2.data[0])
    }

    @Test
    fun `sent messages`() {
        // when two threads are created
        createThread(
            "thread 1",
            "content 1",
            accounts.employee1,
            listOf(accounts.person1, accounts.person2),
            sendTime,
        )
        createThread(
            "thread 2",
            "content 2",
            accounts.employee1,
            listOf(accounts.person1),
            sendTime.plusSeconds(1),
        )

        // then sent messages are returned for sender id
        val firstPage = db.read { it.getMessagesSentByAccount(accounts.employee1.id, 1, 1) }
        assertEquals(2, firstPage.total)
        assertEquals(2, firstPage.pages)
        assertEquals(1, firstPage.data.size)

        val newestMessage = firstPage.data[0]
        assertEquals("content 2", newestMessage.content)
        assertEquals("thread 2", newestMessage.threadTitle)
        assertEquals(listOf(accounts.person1.name), newestMessage.recipientNames)

        val secondPage = db.read { it.getMessagesSentByAccount(accounts.employee1.id, 1, 2) }
        assertEquals(2, secondPage.total)
        assertEquals(2, secondPage.pages)
        assertEquals(1, secondPage.data.size)

        val oldestMessage = secondPage.data[0]
        assertEquals("content 1", oldestMessage.content)
        assertEquals("thread 1", oldestMessage.threadTitle)
        assertEquals(
            listOf(accounts.person1.name, accounts.person2.name),
            oldestMessage.recipientNames,
        )

        // then fetching sent messages by recipient ids does not return the messages
        assertEquals(0, db.read { it.getMessagesSentByAccount(accounts.person1.id, 1, 1) }.total)
    }

    @Test
    fun `message participants by messageId`() {
        val threadId =
            createThread(
                "Hello",
                "Content",
                accounts.employee1,
                listOf(accounts.person1, accounts.person2),
            )

        val participants =
            db.read {
                val messageId =
                    it.createQuery {
                            sql("SELECT id FROM message WHERE thread_id = ${bind(threadId)}")
                        }
                        .exactlyOne<MessageId>()
                it.getThreadByMessageId(messageId)
            }
        assertEquals(
            ThreadWithParticipants(
                threadId = threadId,
                type = MessageType.MESSAGE,
                isCopy = false,
                senders = setOf(accounts.employee1.id),
                recipients = setOf(accounts.person1.id, accounts.person2.id),
                applicationId = null,
            ),
            participants,
        )

        val now = HelsinkiDateTime.now()
        val participants2 =
            db.transaction { tx ->
                val contentId = tx.insertMessageContent("foo", accounts.person2.id)
                val messageId =
                    tx.insertMessage(
                        now = now,
                        contentId = contentId,
                        threadId = threadId,
                        sender = accounts.person2.id,
                        sentAt = now,
                        recipientNames =
                            tx.getAccountNames(
                                setOf(accounts.employee1.id),
                                testFeatureConfig.serviceWorkerMessageAccountName,
                            ),
                        municipalAccountName = "Espoo",
                        serviceWorkerAccountName = "Espoon palveluohjaus",
                    )
                tx.insertRecipients(listOf(messageId to setOf(accounts.employee1.id)))
                tx.getThreadByMessageId(messageId)
            }
        assertEquals(
            ThreadWithParticipants(
                threadId = threadId,
                type = MessageType.MESSAGE,
                isCopy = false,
                senders = setOf(accounts.employee1.id, accounts.person2.id),
                recipients = setOf(accounts.person1.id, accounts.person2.id, accounts.employee1.id),
                applicationId = null,
            ),
            participants2,
        )
    }

    @Test
    fun `query citizen receivers for group change over 2 weeks into the future`() {
        lateinit var group1Account: MessageAccount

        val today = LocalDate.now()
        val startDate = today.minusDays(30)
        val endDateGroup1 = today.plusWeeks(2)
        val startDateGroup2 = endDateGroup1.plusDays(1)
        val endDate = today.plusDays(30)

        db.transaction { tx ->
            val (childId, daycareId, group1Id, tempGroup1Account, group2Id) =
                prepareDataForReceiversTest(tx)
            group1Account = tempGroup1Account

            val placementId =
                tx.insert(
                    DevPlacement(
                        childId = childId,
                        unitId = daycareId,
                        type = PlacementType.DAYCARE,
                        startDate = startDate,
                        endDate = endDate,
                    )
                )
            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placementId,
                    daycareGroupId = group1Id,
                    startDate = startDate,
                    endDate = endDateGroup1,
                )
            )
            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placementId,
                    daycareGroupId = group2Id,
                    startDate = startDateGroup2,
                    endDate = endDate,
                )
            )
        }

        // when we get the receivers for the citizen person1
        val receivers =
            db.read { it.getCitizenReceivers(today, accounts.person1.id).values.flatten().toSet() }

        assertEquals(setOf(group1Account, accounts.employee1), receivers)
    }

    @Test
    fun `query citizen receivers for group change less than 2 weeks into the future`() {
        lateinit var group1Account: MessageAccount
        lateinit var group2Account: MessageAccount

        val today = LocalDate.now()
        val startDate = today.minusDays(30)
        val endDateGroup1 = today.plusWeeks(2).minusDays(1)
        val startDateGroup2 = endDateGroup1.plusDays(1)
        val endDate = today.plusDays(30)

        db.transaction { tx ->
            val (childId, daycareId, group1Id, tempGroup1Account, group2Id, tempGroup2Account) =
                prepareDataForReceiversTest(tx)
            group1Account = tempGroup1Account
            group2Account = tempGroup2Account

            val placementId =
                tx.insert(
                    DevPlacement(
                        childId = childId,
                        unitId = daycareId,
                        type = PlacementType.DAYCARE,
                        startDate = startDate,
                        endDate = endDate,
                    )
                )
            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placementId,
                    daycareGroupId = group1Id,
                    startDate = startDate,
                    endDate = endDateGroup1,
                )
            )
            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placementId,
                    daycareGroupId = group2Id,
                    startDate = startDateGroup2,
                    endDate = endDate,
                )
            )
        }

        // when we get the receivers for the citizen person1
        val receivers =
            db.read { it.getCitizenReceivers(today, accounts.person1.id).values.flatten().toSet() }

        assertEquals(setOf(group1Account, group2Account, accounts.employee1), receivers)
    }

    @Test
    fun `citizen doesn't get any receivers for ended placements`() {
        val today = LocalDate.now()
        val startDate = today.minusMonths(2)
        val endDate = today.minusDays(1)

        db.transaction { tx ->
            val (childId, daycareId, group1Id, _, _) = prepareDataForReceiversTest(tx)
            val placementId =
                tx.insert(
                    DevPlacement(
                        childId = childId,
                        unitId = daycareId,
                        type = PlacementType.DAYCARE,
                        startDate = startDate,
                        endDate = endDate,
                    )
                )
            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placementId,
                    daycareGroupId = group1Id,
                    startDate = startDate,
                    endDate = endDate,
                )
            )
        }
        val receivers =
            db.read { it.getCitizenReceivers(today, accounts.person1.id).values.flatten().toSet() }

        assertEquals(setOf(), receivers)
    }

    @Test
    fun `citizen receivers include backup and standard placements`() {
        val today = LocalDate.now()
        val startDate = today.minusMonths(2)
        val endDate = today.plusMonths(2)
        lateinit var groupAccount: MessageAccount

        db.transaction { tx ->
            val (childId, daycareId, groupId, tempGroupAccount, _, _, areaId) =
                prepareDataForReceiversTest(tx)
            groupAccount = tempGroupAccount

            val placementId =
                tx.insert(
                    DevPlacement(
                        childId = childId,
                        unitId = daycareId,
                        type = PlacementType.DAYCARE,
                        startDate = startDate,
                        endDate = endDate,
                    )
                )
            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placementId,
                    daycareGroupId = groupId,
                    startDate = startDate,
                    endDate = endDate,
                )
            )
            val backupDaycareId =
                tx.insert(
                    DevDaycare(
                        areaId = areaId,
                        language = Language.fi,
                        enabledPilotFeatures = setOf(PilotFeature.MESSAGING),
                    )
                )
            tx.insertDaycareAclRow(
                daycareId = backupDaycareId,
                employeeId = employee2.id,
                role = UserRole.UNIT_SUPERVISOR,
            )
            tx.insert(
                DevBackupCare(
                    childId = childId,
                    unitId = backupDaycareId,
                    period = FiniteDateRange(today.plusDays(1), today.plusDays(2)),
                )
            )
        }
        val receivers =
            db.read { it.getCitizenReceivers(today, accounts.person1.id).values.flatten().toSet() }

        assertEquals(setOf(accounts.employee1, accounts.employee2, groupAccount), receivers)
    }

    @Test
    fun `children with only secondary recipients are not included in receivers`() {
        lateinit var child1Id: PersonId
        lateinit var child2Id: PersonId
        lateinit var groupAccount: MessageAccount
        lateinit var child2Placement: PlacementId
        val now = HelsinkiDateTime.of(LocalDate.of(2022, 1, 1), LocalTime.of(12, 0))
        db.transaction { tx ->
            val areaId = tx.insert(DevCareArea())
            val daycareId =
                tx.insert(
                    DevDaycare(
                        areaId = areaId,
                        enabledPilotFeatures = setOf(PilotFeature.MESSAGING),
                    )
                )
            tx.insertDaycareAclRow(daycareId, employee1.id, UserRole.UNIT_SUPERVISOR)
            val group = DevDaycareGroup(daycareId = daycareId)
            tx.insert(group)
            groupAccount =
                MessageAccount(
                    id = tx.createDaycareGroupMessageAccount(group.id),
                    name = group.name,
                    type = AccountType.GROUP,
                )
            child1Id = tx.insert(DevPerson(), DevPersonType.CHILD)
            child2Id = tx.insert(DevPerson(), DevPersonType.CHILD)
            listOf(child1Id, child2Id).forEach { childId ->
                listOf(person1.id, person2.id).forEach { guardianId ->
                    tx.insert(DevGuardian(guardianId = guardianId, childId = childId))
                }
            }
            val startDate = now.toLocalDate()
            val endDate = startDate.plusDays(30)
            val placementId =
                tx.insert(
                    DevPlacement(
                        childId = child1Id,
                        unitId = daycareId,
                        startDate = startDate,
                        endDate = endDate,
                    )
                )
            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placementId,
                    daycareGroupId = group.id,
                    startDate = startDate,
                    endDate = endDate,
                )
            )
            child2Placement =
                tx.insert(
                    DevPlacement(
                        childId = child2Id,
                        unitId = daycareId,
                        startDate = startDate,
                        endDate = endDate,
                    )
                )
        }
        fun receiversOf(account: MessageAccount) =
            db.read { it.getCitizenReceivers(now.toLocalDate(), account.id) }
                .mapValues { (_, accounts) -> accounts.toSet() }

        assertEquals(
            mapOf(
                child1Id to setOf(accounts.employee1, groupAccount, accounts.person2),
                child2Id to setOf(accounts.employee1, accounts.person2),
            ),
            receiversOf(accounts.person1),
        )
        deletePlacement(child2Placement)
        assertEquals(
            mapOf(child1Id to setOf(accounts.employee1, groupAccount, accounts.person2)),
            receiversOf(accounts.person1),
        )
    }

    @Test
    fun `a thread can be archived`() {
        val content = "Content"
        val title = "Hello"
        val threadId = createThread(title, content, accounts.employee1, listOf(accounts.person1))

        assertEquals(1, unreadMessagesCount(person1.id))

        db.transaction { tx -> tx.archiveThread(accounts.person1.id, threadId) }

        assertEquals(0, unreadMessagesCount(person1.id))

        assertEquals(
            1,
            db.read {
                val archiveFolderId = it.getArchiveFolderId(accounts.person1.id)
                it.getReceivedThreads(
                        accounts.person1.id,
                        50,
                        1,
                        "Espoo",
                        "Espoon palveluohjaus",
                        archiveFolderId,
                    )
                    .total
            },
        )
    }

    @Test
    fun `an archived threads returns to inbox when it receives messages`() {
        val content = "Content"
        val title = "Hello"
        val threadId = createThread(title, content, accounts.employee1, listOf(accounts.person1))
        db.transaction { tx -> tx.archiveThread(accounts.person1.id, threadId) }
        assertEquals(
            1,
            db.read {
                val archiveFolderId = it.getArchiveFolderId(accounts.person1.id)
                it.getReceivedThreads(
                        accounts.person1.id,
                        50,
                        1,
                        "Espoo",
                        "Espoon palveluohjaus",
                        archiveFolderId,
                    )
                    .total
            },
        )

        replyToThread(threadId, accounts.employee1, setOf(accounts.person1), "Reply")

        assertEquals(
            1,
            db.read {
                it.getReceivedThreads(
                        accounts.person1.id,
                        50,
                        1,
                        "Espoo",
                        "Espoon palveluohjaus",
                        null,
                    )
                    .total
            },
        )
        assertEquals(
            0,
            db.read {
                val archiveFolderId = it.getArchiveFolderId(accounts.person1.id)
                it.getReceivedThreads(
                        accounts.person1.id,
                        50,
                        1,
                        "Espoo",
                        "Espoon palveluohjaus",
                        archiveFolderId,
                    )
                    .total
            },
        )
    }

    @Test
    fun `staff copies are sent to active groups`() {
        val today = LocalDate.now()
        val area = DevCareArea()
        val daycare =
            DevDaycare(areaId = area.id, enabledPilotFeatures = setOf(PilotFeature.MESSAGING))

        val ongoingGroup = DevDaycareGroup(daycareId = daycare.id, name = "A")
        val untilYesterdayGroup =
            DevDaycareGroup(daycareId = daycare.id, name = "B", endDate = today.minusDays(1))
        val untilTodayGroup = DevDaycareGroup(daycareId = daycare.id, name = "C", endDate = today)
        val untilTomorrowGroup =
            DevDaycareGroup(daycareId = daycare.id, name = "D", endDate = today.plusDays(1))

        val (
            groupMessageAccountOngoing,
            groupMessageAccountUntilToday,
            groupMessageAccountUntilTomorrow) =
            db.transaction { tx ->
                tx.insert(area)
                tx.insert(daycare)

                tx.insert(ongoingGroup)
                tx.insert(untilYesterdayGroup)
                tx.insert(untilTodayGroup)
                tx.insert(untilTomorrowGroup)

                val groupMessageAccountOngoing =
                    tx.createDaycareGroupMessageAccount(ongoingGroup.id)
                val groupMessageAccountUntilYesterday =
                    tx.createDaycareGroupMessageAccount(untilYesterdayGroup.id)
                val groupMessageAccountUntilToday =
                    tx.createDaycareGroupMessageAccount(untilTodayGroup.id)
                val groupMessageAccountUntilTomorrow =
                    tx.createDaycareGroupMessageAccount(untilTomorrowGroup.id)

                tx.insertDaycareAclRow(daycare.id, employee1.id, UserRole.UNIT_SUPERVISOR)

                Triple(
                    groupMessageAccountOngoing,
                    groupMessageAccountUntilToday,
                    groupMessageAccountUntilTomorrow,
                )
            }

        val recipients =
            db.transaction { tx ->
                tx.getStaffCopyRecipients(
                    accounts.employee1.id,
                    setOf(MessageRecipient(MessageRecipientType.AREA, area.id)),
                    today,
                )
            }

        assertEquals(
            setOf(
                groupMessageAccountOngoing,
                groupMessageAccountUntilToday,
                groupMessageAccountUntilTomorrow,
            ),
            recipients,
        )
    }

    /*
     * TODO: Tests in this file should be moved to MessageIntegrationTest because creating a thread like this
     * doesn't reflect reality
     */
    private fun createThread(
        title: String,
        content: String,
        sender: MessageAccount,
        recipientAccounts: List<MessageAccount>,
        now: HelsinkiDateTime = sendTime,
    ): MessageThreadId =
        db.transaction { tx ->
            val recipientIds = recipientAccounts.map { it.id }.toSet()
            val contentId = tx.insertMessageContent(content, sender.id)
            val threadId =
                tx.insertThread(
                    MessageType.MESSAGE,
                    title,
                    urgent = false,
                    sensitive = false,
                    isCopy = false,
                )
            val messageId =
                tx.insertMessage(
                    now = now,
                    contentId = contentId,
                    threadId = threadId,
                    sender = sender.id,
                    sentAt = now,
                    recipientNames =
                        tx.getAccountNames(
                            recipientIds,
                            testFeatureConfig.serviceWorkerMessageAccountName,
                        ),
                    municipalAccountName = "Espoo",
                    serviceWorkerAccountName = "Espoon palveluohjaus",
                )
            tx.insertRecipients(listOf(messageId to recipientAccounts.map { it.id }.toSet()))
            tx.upsertSenderThreadParticipants(sender.id, listOf(threadId), now)
            tx.upsertReceiverThreadParticipants(contentId, now)
            threadId
        }

    /*
     * TODO: Tests in this file should be moved to MessageIntegrationTest because replying to a thread like this
     * doesn't reflect reality
     */
    private fun replyToThread(
        threadId: MessageThreadId,
        sender: MessageAccount,
        recipients: Set<MessageAccount>,
        content: String,
        repliesToMessageId: MessageId? = null,
        now: HelsinkiDateTime = sendTime,
    ) =
        db.transaction { tx ->
            val recipientIds = recipients.map { it.id }.toSet()
            val contentId = tx.insertMessageContent(content = content, sender = sender.id)
            val messageId =
                tx.insertMessage(
                    now = now,
                    contentId = contentId,
                    threadId = threadId,
                    sender = sender.id,
                    sentAt = now,
                    repliesToMessageId = repliesToMessageId,
                    recipientNames = listOf(),
                    municipalAccountName = "Espoo",
                    serviceWorkerAccountName = "Espoon palveluohjaus",
                )
            tx.insertRecipients(listOf(messageId to recipientIds))
            tx.upsertSenderThreadParticipants(sender.id, listOf(threadId), now)
            tx.upsertReceiverThreadParticipants(contentId, now)
        }

    private fun unreadMessagesCount(personId: PersonId) =
        db.read { tx ->
            tx.getUnreadMessagesCounts(
                    accessControl.requireAuthorizationFilter(
                        tx,
                        AuthenticatedUser.Citizen(personId, CitizenAuthLevel.WEAK),
                        clock,
                        Action.MessageAccount.ACCESS,
                    )
                )
                .first()
                .unreadCount
        }

    private fun Database.Transaction.getAccount(person: DevPerson) =
        MessageAccount(
            id = getCitizenMessageAccount(person.id),
            name = "${person.lastName} ${person.firstName}",
            type = AccountType.CITIZEN,
        )

    private fun Database.Transaction.createAccount(group: DevDaycareGroup) =
        MessageAccount(
            id = createDaycareGroupMessageAccount(group.id),
            name = group.name,
            type = AccountType.GROUP,
        )

    private fun Database.Transaction.createAccount(employee: DevEmployee) =
        MessageAccount(
            id = upsertEmployeeMessageAccount(employee.id),
            name = "${employee.lastName} ${employee.firstName}",
            type = AccountType.PERSONAL,
        )

    private fun deletePlacement(placement: PlacementId) =
        db.transaction {
            it.createUpdate { sql("DELETE FROM placement WHERE id = ${bind(placement)}") }.execute()
        }

    private data class ReceiverTestData(
        val personId: PersonId,
        val daycareId: DaycareId,
        val group1Id: GroupId,
        val group1Account: MessageAccount,
        val group2Id: GroupId,
        val group2Account: MessageAccount,
        val areaId: AreaId,
    )

    private fun prepareDataForReceiversTest(tx: Database.Transaction): ReceiverTestData {
        val areaId = tx.insert(DevCareArea())
        val daycareId =
            tx.insert(
                DevDaycare(
                    areaId = areaId,
                    language = Language.fi,
                    enabledPilotFeatures = setOf(PilotFeature.MESSAGING),
                )
            )
        tx.insertDaycareAclRow(
            daycareId = daycareId,
            employeeId = employee1.id,
            role = UserRole.UNIT_SUPERVISOR,
        )
        val group1 = DevDaycareGroup(daycareId = daycareId, name = "Testiläiset")
        val group2 = DevDaycareGroup(daycareId = daycareId, name = "Testiläiset 2")
        listOf(group1, group2).forEach { tx.insert(it) }
        val group1Account = tx.createAccount(group1)
        val group2Account = tx.createAccount(group2)

        val childId =
            tx.insert(
                DevPerson(firstName = "Firstname", lastName = "Test Child"),
                DevPersonType.CHILD,
            )

        tx.insertGuardian(guardianId = person1.id, childId = childId)
        return ReceiverTestData(
            childId,
            daycareId,
            group1.id,
            group1Account,
            group2.id,
            group2Account,
            areaId,
        )
    }
}
