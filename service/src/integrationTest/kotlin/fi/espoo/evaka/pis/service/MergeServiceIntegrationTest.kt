// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.service

import fi.espoo.evaka.BucketEnv
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.childimages.replaceImage
import fi.espoo.evaka.messaging.MessageService
import fi.espoo.evaka.messaging.NewMessageStub
import fi.espoo.evaka.messaging.archiveThread
import fi.espoo.evaka.messaging.getArchiveFolderId
import fi.espoo.evaka.messaging.getCitizenMessageAccount
import fi.espoo.evaka.messaging.getMessagesSentByAccount
import fi.espoo.evaka.messaging.getReceivedThreads
import fi.espoo.evaka.messaging.getThreads
import fi.espoo.evaka.messaging.upsertEmployeeMessageAccount
import fi.espoo.evaka.s3.DocumentService
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.MessageAccountId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevIncome
import fi.espoo.evaka.shared.dev.DevParentship
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.Conflict
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.utils.decodeHex
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockMultipartFile

class MergeServiceIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>
    @Autowired private lateinit var messageService: MessageService
    @Autowired private lateinit var documentClient: DocumentService
    @Autowired private lateinit var bucketEnv: BucketEnv

    private lateinit var mergeService: MergeService
    private lateinit var mergeServiceAsyncJobRunnerMock: AsyncJobRunner<AsyncJob>

    private val clock = MockEvakaClock(2024, 1, 1, 12, 0)

    private val child = DevPerson()
    private val area = DevCareArea()
    private val unit = DevDaycare(areaId = area.id)

    @BeforeEach
    fun setUp() {
        mergeServiceAsyncJobRunnerMock = mock {}
        mergeService = MergeService(mergeServiceAsyncJobRunnerMock, documentClient, bucketEnv)
        db.transaction { tx ->
            tx.insert(area)
            tx.insert(unit)
            tx.insert(child, DevPersonType.CHILD)
        }
    }

    @Test
    fun `empty person can be deleted`() {
        val id = ChildId(UUID.randomUUID())
        db.transaction { it.insert(DevPerson(id = id), DevPersonType.CHILD) }

        val countBefore =
            db.read {
                it.createQuery { sql("SELECT count(*) FROM person WHERE id = ${bind(id)}") }
                    .exactlyOne<Int>()
            }
        assertEquals(1, countBefore)

        db.transaction { mergeService.deleteEmptyPerson(it, id) }

        val countAfter =
            db.read {
                it.createQuery { sql("SELECT count(*) FROM person WHERE id = ${bind(id)}") }
                    .exactlyOne<Int>()
            }
        assertEquals(0, countAfter)
    }

    @Test
    fun `cannot delete person with data - placement`() {
        val id = ChildId(UUID.randomUUID())
        db.transaction {
            it.insert(DevPerson(id = id), DevPersonType.CHILD)
            it.insert(DevPlacement(childId = id, unitId = unit.id))
        }

        assertThrows<Conflict> { db.transaction { mergeService.deleteEmptyPerson(it, id) } }
    }

    @Test
    fun `merging adult moves incomes and sends update event`() {
        val adultId = PersonId(UUID.randomUUID())
        val adultIdDuplicate = PersonId(UUID.randomUUID())
        val childId = ChildId(UUID.randomUUID())
        val validFrom = LocalDate.of(2010, 1, 1)
        val validTo = LocalDate.of(2020, 12, 30)
        db.transaction {
            it.insert(DevPerson(id = childId), DevPersonType.RAW_ROW)
            it.insert(DevPerson(id = adultId), DevPersonType.RAW_ROW)
            it.insert(DevPerson(id = adultIdDuplicate), DevPersonType.RAW_ROW)
            it.insert(
                DevParentship(
                    childId = childId,
                    headOfChildId = adultId,
                    startDate = LocalDate.of(2015, 1, 1),
                    endDate = LocalDate.of(2030, 1, 1),
                )
            )
            it.insert(
                DevIncome(
                    adultIdDuplicate,
                    validFrom = validFrom,
                    validTo = validTo,
                    updatedBy = AuthenticatedUser.SystemInternalUser.evakaUserId,
                )
            )
        }

        val countBefore =
            db.read {
                it.createQuery {
                        sql(
                            "SELECT count(*) FROM income WHERE person_id = ${bind(adultIdDuplicate)}"
                        )
                    }
                    .exactlyOne<Int>()
            }
        assertEquals(1, countBefore)

        db.transaction { mergeService.mergePeople(it, clock, adultId, adultIdDuplicate) }

        val countAfter =
            db.read {
                it.createQuery {
                        sql("SELECT count(*) FROM income WHERE person_id = ${bind(adultId)}")
                    }
                    .exactlyOne<Int>()
            }
        assertEquals(1, countAfter)

        verify(mergeServiceAsyncJobRunnerMock)
            .plan(
                any(),
                eq(
                    listOf(
                        AsyncJob.GenerateFinanceDecisions.forAdult(
                            adultId,
                            DateRange(validFrom, validTo),
                        )
                    )
                ),
                any(),
                any(),
                any(),
            )
    }

    @Test
    fun `merging child moves placements`() {
        val childId = ChildId(UUID.randomUUID())
        val childIdDuplicate = ChildId(UUID.randomUUID())
        db.transaction {
            it.insert(DevPerson(id = childId), DevPersonType.CHILD)
            it.insert(DevPerson(id = childIdDuplicate), DevPersonType.CHILD)
        }
        val from = LocalDate.of(2010, 1, 1)
        val to = LocalDate.of(2020, 12, 30)
        db.transaction {
            it.insert(
                DevPlacement(
                    childId = childIdDuplicate,
                    unitId = unit.id,
                    startDate = from,
                    endDate = to,
                )
            )
        }

        val countBefore =
            db.read {
                it.createQuery {
                        sql(
                            "SELECT count(*) FROM placement WHERE child_id = ${bind(childIdDuplicate)}"
                        )
                    }
                    .exactlyOne<Int>()
            }
        assertEquals(1, countBefore)

        db.transaction { mergeService.mergePeople(it, clock, childId, childIdDuplicate) }

        val countAfter =
            db.read {
                it.createQuery {
                        sql("SELECT count(*) FROM placement WHERE child_id = ${bind(childId)}")
                    }
                    .exactlyOne<Int>()
            }
        assertEquals(1, countAfter)

        verifyNoInteractions(mergeServiceAsyncJobRunnerMock)
    }

    @Test
    fun `merging a person moves sent messages`() {
        val employeeId = EmployeeId(UUID.randomUUID())
        val receiverAccount =
            db.transaction {
                it.insert(DevEmployee(id = employeeId))
                it.upsertEmployeeMessageAccount(employeeId)
            }
        val senderId = PersonId(UUID.randomUUID())
        val senderIdDuplicate = PersonId(UUID.randomUUID())
        val (senderAccount, senderDuplicateAccount) =
            db.transaction { tx ->
                listOf(senderId, senderIdDuplicate).map {
                    tx.insert(DevPerson(id = it), DevPersonType.ADULT)
                    tx.getCitizenMessageAccount(it)
                }
            }

        db.transaction { tx ->
            messageService.sendMessageAsCitizen(
                tx,
                HelsinkiDateTime.now(),
                sender = senderDuplicateAccount,
                dummyMessage,
                recipients = setOf(receiverAccount),
                children = setOf(child.id),
            )
        }
        assertEquals(listOf(0, 1), sentMessageCounts(senderAccount, senderDuplicateAccount))

        db.transaction {
            mergeService.mergePeople(it, clock, senderId, senderIdDuplicate)
            mergeService.deleteEmptyPerson(it, senderIdDuplicate)
        }

        assertEquals(listOf(1, 0), sentMessageCounts(senderAccount, senderDuplicateAccount))
    }

    private fun sentMessageCounts(vararg accountIds: MessageAccountId): List<Int> =
        db.read { tx -> accountIds.map { tx.getMessagesSentByAccount(it, 10, 1).total } }

    @Test
    fun `merging a person moves received messages`() {
        val now = HelsinkiDateTime.of(LocalDate.of(2022, 8, 11), LocalTime.of(13, 45))
        val employeeId = EmployeeId(UUID.randomUUID())
        val senderAccount =
            db.transaction {
                it.insert(DevEmployee(id = employeeId))
                it.upsertEmployeeMessageAccount(employeeId)
            }

        val receiverId = PersonId(UUID.randomUUID())
        val receiverIdDuplicate = PersonId(UUID.randomUUID())
        val (receiverAccount, receiverDuplicateAccount) =
            db.transaction { tx ->
                listOf(receiverId, receiverIdDuplicate)
                    .map {
                        tx.insert(DevPerson(id = it), DevPersonType.ADULT)
                        tx.getCitizenMessageAccount(it)
                    }
                    .also { tx.insertGuardian(receiverIdDuplicate, child.id) }
            }

        db.transaction { tx ->
            messageService.sendMessageAsCitizen(
                tx,
                now.minusMinutes(1),
                sender = senderAccount,
                dummyMessage,
                recipients = setOf(receiverDuplicateAccount),
                children = setOf(child.id),
            )
        }
        asyncJobRunner.runPendingJobsSync(MockEvakaClock(now))

        assertEquals(
            listOf(0, 1),
            receivedThreadCounts(listOf(receiverAccount, receiverDuplicateAccount)),
        )

        db.transaction {
            mergeService.mergePeople(it, clock, receiverId, receiverIdDuplicate)
            mergeService.deleteEmptyPerson(it, receiverIdDuplicate)
        }

        assertEquals(
            listOf(1, 0),
            receivedThreadCounts(listOf(receiverAccount, receiverDuplicateAccount)),
        )
    }

    private fun receivedThreadCounts(accountIds: List<MessageAccountId>): List<Int> =
        db.read { tx ->
            accountIds.map {
                tx.getReceivedThreads(it, 10, 1, "Espoo", "Espoon palveluohjaus").total
            }
        }

    private fun archivedThreadCounts(accountIds: List<MessageAccountId>): List<Int> =
        db.read { tx ->
            accountIds.map {
                val archiveFolderId = tx.getArchiveFolderId(it)
                tx.getReceivedThreads(it, 10, 1, "Espoo", "Espoon palveluohjaus", archiveFolderId)
                    .total
            }
        }

    @Test
    fun `merging a person should move archived messages as well`() {
        val now = HelsinkiDateTime.of(LocalDate.of(2022, 8, 11), LocalTime.of(13, 45))
        val employeeId = EmployeeId(UUID.randomUUID())
        val senderAccount =
            db.transaction {
                it.insert(DevEmployee(id = employeeId))
                it.upsertEmployeeMessageAccount(employeeId)
            }

        val receiverId = PersonId(UUID.randomUUID())
        val receiverIdDuplicate = PersonId(UUID.randomUUID())
        val (receiverAccount, receiverDuplicateAccount) =
            db.transaction { tx ->
                listOf(receiverId, receiverIdDuplicate).map {
                    tx.insert(DevPerson(id = it), DevPersonType.ADULT)
                    tx.getCitizenMessageAccount(it)
                }
            }
        db.transaction { tx ->
            messageService.sendMessageAsCitizen(
                tx,
                now.minusMinutes(1),
                sender = senderAccount,
                dummyMessage,
                recipients = setOf(receiverDuplicateAccount),
                children = setOf(child.id),
            )
        }
        asyncJobRunner.runPendingJobsSync(MockEvakaClock(now))
        db.transaction { tx ->
            val threadId =
                tx.getThreads(senderAccount, 1, 1, "Espoo", "Espoon palveluohjaus").data.first().id
            tx.archiveThread(receiverDuplicateAccount, threadId)
        }

        assertEquals(
            listOf(0, 0),
            receivedThreadCounts(listOf(receiverAccount, receiverDuplicateAccount)),
        )
        assertEquals(
            listOf(0, 1),
            archivedThreadCounts(listOf(receiverAccount, receiverDuplicateAccount)),
        )

        db.transaction {
            mergeService.mergePeople(it, clock, receiverId, receiverIdDuplicate)
            mergeService.deleteEmptyPerson(it, receiverIdDuplicate)
        }

        assertEquals(
            listOf(1, 0),
            archivedThreadCounts(listOf(receiverAccount, receiverDuplicateAccount)),
        )
    }

    @Test
    fun `merging child sends update event to head of child`() {
        val adultId = PersonId(UUID.randomUUID())
        val childId = ChildId(UUID.randomUUID())
        val childIdDuplicate = ChildId(UUID.randomUUID())
        db.transaction {
            it.insert(DevPerson(id = adultId), DevPersonType.RAW_ROW)
            it.insert(DevPerson(id = childId), DevPersonType.CHILD)
            it.insert(DevPerson(id = childIdDuplicate), DevPersonType.CHILD)
            it.insert(
                DevParentship(
                    childId = childId,
                    headOfChildId = adultId,
                    startDate = LocalDate.of(2015, 1, 1),
                    endDate = LocalDate.of(2030, 1, 1),
                )
            )
        }
        val placementStart = LocalDate.of(2017, 1, 1)
        val placementEnd = LocalDate.of(2020, 12, 30)
        db.transaction {
            it.insert(
                DevPlacement(
                    childId = childIdDuplicate,
                    unitId = unit.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
        }

        db.transaction { mergeService.mergePeople(it, clock, childId, childIdDuplicate) }

        verify(mergeServiceAsyncJobRunnerMock)
            .plan(
                any(),
                eq(
                    listOf(
                        AsyncJob.GenerateFinanceDecisions.forAdult(
                            adultId,
                            DateRange(placementStart, placementEnd),
                        )
                    )
                ),
                any(),
                any(),
                any(),
            )
    }

    @Test
    fun `evaka_user reference to duplicate person is removed but the data is left intact`() {
        val person = DevPerson()
        val duplicate = DevPerson()
        db.transaction {
            it.insert(person, DevPersonType.ADULT)
            it.insert(duplicate, DevPersonType.ADULT)
        }
        db.read {
            val (citizenId, name) =
                it.createQuery {
                        sql(
                            "SELECT citizen_id, name FROM evaka_user WHERE id = ${bind(duplicate.id)}"
                        )
                    }
                    .exactlyOne { column<PersonId?>("citizen_id") to column<String>("name") }

            assertEquals(duplicate.id, citizenId)
            assertEquals("${duplicate.lastName} ${duplicate.firstName}", name)
        }

        db.transaction {
            mergeService.mergePeople(it, clock, person.id, duplicate.id)
            mergeService.deleteEmptyPerson(it, duplicate.id)
        }
        db.read {
            val (citizenId, name) =
                it.createQuery {
                        sql(
                            "SELECT citizen_id, name FROM evaka_user WHERE id = ${bind(duplicate.id)}"
                        )
                    }
                    .exactlyOne { column<PersonId?>("citizen_id") to column<String>("name") }

            assertEquals(null, citizenId)
            assertEquals("${duplicate.lastName} ${duplicate.firstName}", name)
        }
    }

    @Test
    fun `merging a child moves the image`() {
        val childId = ChildId(UUID.randomUUID())
        val childIdDuplicate = ChildId(UUID.randomUUID())
        db.transaction {
            it.insert(DevPerson(id = childId), DevPersonType.CHILD)
            it.insert(DevPerson(id = childIdDuplicate), DevPersonType.CHILD)
            it.insert(
                DevPlacement(
                    unitId = unit.id,
                    childId = childIdDuplicate,
                    startDate = clock.today(),
                    endDate = clock.today().plusMonths(1),
                )
            )
        }

        setChildImage(childIdDuplicate)
        assertEquals(0, childImageCount(childId))
        assertEquals(1, childImageCount(childIdDuplicate))

        db.transaction { mergeService.mergePeople(it, clock, childId, childIdDuplicate) }

        assertEquals(1, childImageCount(childId))
        assertEquals(0, childImageCount(childIdDuplicate))
    }

    @Test
    fun `merging a child works if both master and duplicate have images`() {
        val childId = ChildId(UUID.randomUUID())
        val childIdDuplicate = ChildId(UUID.randomUUID())
        db.transaction {
            it.insert(DevPerson(id = childId), DevPersonType.CHILD)
            it.insert(DevPerson(id = childIdDuplicate), DevPersonType.CHILD)
        }

        setChildImage(childId)
        setChildImage(childIdDuplicate)
        assertEquals(1, childImageCount(childId))
        assertEquals(1, childImageCount(childIdDuplicate))

        db.transaction { mergeService.mergePeople(it, clock, childId, childIdDuplicate) }

        assertEquals(1, childImageCount(childId))
        assertEquals(1, childImageCount(childIdDuplicate))

        db.transaction { mergeService.deleteEmptyPerson(it, childIdDuplicate) }
        assertEquals(0, childImageCount(childIdDuplicate))
    }

    private val dummyMessage =
        NewMessageStub(
            title = "Juhannus",
            content = "Juhannus tulee kohta",
            urgent = false,
            sensitive = false,
        )

    private fun setChildImage(childId: ChildId) =
        replaceImage(
            db,
            documentClient,
            bucketEnv.data,
            childId,
            MockMultipartFile("file", imageName, "image/jpeg", imageData),
            "image/jpeg",
        )

    private fun childImageCount(childId: ChildId): Int =
        db.read {
            it.createQuery {
                    sql("SELECT COUNT(*) FROM child_images WHERE child_id = ${bind(childId)}")
                }
                .exactlyOne()
        }

    private val imageName = "test1.jpg"
    private val imageData =
        """
FF D8 FF E0 00 10 4A 46 49 46 00 01 01 01 00 48 00 48 00 00
FF DB 00 43 00 FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF
FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF
FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF
FF FF FF FF FF FF FF FF FF FF C2 00 0B 08 00 01 00 01 01 01
11 00 FF C4 00 14 10 01 00 00 00 00 00 00 00 00 00 00 00 00
00 00 00 00 FF DA 00 08 01 01 00 01 3F 10
"""
            .decodeHex()
}
