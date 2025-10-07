// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing

import fi.espoo.evaka.EmailEnv
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.caseprocess.CaseProcessState
import fi.espoo.evaka.caseprocess.ProcessMetadataController
import fi.espoo.evaka.caseprocess.getCaseProcessByFeeDecisionId
import fi.espoo.evaka.emailclient.Email
import fi.espoo.evaka.emailclient.IEmailMessageProvider
import fi.espoo.evaka.emailclient.MockEmailClient
import fi.espoo.evaka.incomestatement.IncomeStatementBody
import fi.espoo.evaka.incomestatement.IncomeStatementStatus
import fi.espoo.evaka.invoicing.controller.DistinctiveParams
import fi.espoo.evaka.invoicing.controller.FeeDecisionController
import fi.espoo.evaka.invoicing.controller.FeeDecisionSortParam
import fi.espoo.evaka.invoicing.controller.FeeDecisionTypeRequest
import fi.espoo.evaka.invoicing.controller.SearchFeeDecisionRequest
import fi.espoo.evaka.invoicing.controller.SortDirection
import fi.espoo.evaka.invoicing.data.PagedFeeDecisionSummaries
import fi.espoo.evaka.invoicing.data.upsertFeeDecisions
import fi.espoo.evaka.invoicing.domain.FeeDecision
import fi.espoo.evaka.invoicing.domain.FeeDecisionChildDetailed
import fi.espoo.evaka.invoicing.domain.FeeDecisionDetailed
import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
import fi.espoo.evaka.invoicing.domain.FeeDecisionSummary
import fi.espoo.evaka.invoicing.domain.FeeDecisionType
import fi.espoo.evaka.invoicing.domain.FinanceDecisionType
import fi.espoo.evaka.invoicing.domain.UnitData
import fi.espoo.evaka.pis.EmailMessageType
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.sficlient.MockSfiMessagesClient
import fi.espoo.evaka.sficlient.SfiAsyncJobs
import fi.espoo.evaka.sficlient.getSfiGetEventsContinuationTokens
import fi.espoo.evaka.sficlient.getSfiMessageEventsByMessageId
import fi.espoo.evaka.sficlient.rest.EventType
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.FeeDecisionId
import fi.espoo.evaka.shared.ParentshipId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevIncomeStatement
import fi.espoo.evaka.shared.dev.DevParentship
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertTestPartnership
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.domain.RealEvakaClock
import fi.espoo.evaka.snDaycareFullDay35
import fi.espoo.evaka.snDaycarePartDay25
import fi.espoo.evaka.snDefaultDaycare
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testAdult_2
import fi.espoo.evaka.testAdult_3
import fi.espoo.evaka.testAdult_4
import fi.espoo.evaka.testAdult_5
import fi.espoo.evaka.testAdult_6
import fi.espoo.evaka.testAdult_7
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testChild_3
import fi.espoo.evaka.testChild_4
import fi.espoo.evaka.testDecisionMaker_1
import fi.espoo.evaka.testDecisionMaker_2
import fi.espoo.evaka.toEmployeeWithName
import fi.espoo.evaka.toFeeDecisionServiceNeed
import fi.espoo.evaka.toPersonBasic
import fi.espoo.evaka.toPersonDetailed
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class FeeDecisionIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var feeDecisionController: FeeDecisionController
    @Autowired private lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>
    @Autowired private lateinit var emailMessageProvider: IEmailMessageProvider
    @Autowired private lateinit var emailEnv: EmailEnv
    @Autowired private lateinit var processMetadataController: ProcessMetadataController
    @Autowired private lateinit var sfiAsyncJobs: SfiAsyncJobs

    private val area1 = DevCareArea(name = "Area 1", shortName = "area1")
    private val area2 = DevCareArea(name = "Area 2", shortName = "area2")
    private val daycare1 = DevDaycare(areaId = area1.id)
    private val daycare2 = DevDaycare(areaId = area2.id)

    private val user =
        AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.FINANCE_ADMIN))
    private val adminUser =
        AuthenticatedUser.Employee(testDecisionMaker_2.id, setOf(UserRole.ADMIN))

    private val testDecisions =
        listOf(
            createFeeDecisionFixture(
                status = FeeDecisionStatus.DRAFT,
                decisionType = FeeDecisionType.NORMAL,
                headOfFamilyId = testAdult_1.id,
                period = FiniteDateRange(LocalDate.of(2018, 5, 1), LocalDate.of(2018, 5, 31)),
                children =
                    listOf(
                        createFeeDecisionChildFixture(
                            childId = testChild_1.id,
                            dateOfBirth = testChild_1.dateOfBirth,
                            placementUnitId = daycare1.id,
                            placementType = PlacementType.DAYCARE,
                            serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                        ),
                        createFeeDecisionChildFixture(
                            childId = testChild_2.id,
                            dateOfBirth = testChild_2.dateOfBirth,
                            placementUnitId = daycare1.id,
                            placementType = PlacementType.DAYCARE,
                            serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                            siblingDiscount = 50,
                            fee = 14500,
                        ),
                    ),
            ),
            createFeeDecisionFixture(
                status = FeeDecisionStatus.SENT,
                decisionType = FeeDecisionType.NORMAL,
                headOfFamilyId = testAdult_1.id,
                period = FiniteDateRange(LocalDate.of(2019, 5, 1), LocalDate.of(2019, 5, 31)),
                children =
                    listOf(
                        createFeeDecisionChildFixture(
                            childId = testChild_2.id,
                            dateOfBirth = testChild_2.dateOfBirth,
                            placementUnitId = daycare1.id,
                            placementType = PlacementType.DAYCARE,
                            serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                        )
                    ),
            ),
            createFeeDecisionFixture(
                status = FeeDecisionStatus.SENT,
                decisionType = FeeDecisionType.NORMAL,
                headOfFamilyId = testAdult_2.id,
                period = FiniteDateRange(LocalDate.of(2019, 5, 1), LocalDate.of(2019, 5, 31)),
                children =
                    listOf(
                        createFeeDecisionChildFixture(
                            childId = testChild_1.id,
                            dateOfBirth = testChild_1.dateOfBirth,
                            placementUnitId = daycare1.id,
                            placementType = PlacementType.DAYCARE,
                            serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                        )
                    ),
            ),
            createFeeDecisionFixture(
                status = FeeDecisionStatus.DRAFT,
                decisionType = FeeDecisionType.NORMAL,
                headOfFamilyId = testAdult_3.id,
                period = FiniteDateRange(LocalDate.of(2015, 5, 1), LocalDate.of(2015, 5, 31)),
                children =
                    listOf(
                        createFeeDecisionChildFixture(
                            childId = testChild_3.id,
                            dateOfBirth = testChild_3.dateOfBirth,
                            placementUnitId = daycare2.id,
                            placementType = PlacementType.DAYCARE,
                            serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                        )
                    ),
            ),
            createFeeDecisionFixture(
                status = FeeDecisionStatus.DRAFT,
                decisionType = FeeDecisionType.NORMAL,
                headOfFamilyId = testAdult_3.id,
                period = FiniteDateRange(LocalDate.of(2016, 5, 1), LocalDate.of(2016, 5, 31)),
                children =
                    listOf(
                        createFeeDecisionChildFixture(
                            childId = testChild_4.id,
                            dateOfBirth = testChild_4.dateOfBirth,
                            placementUnitId = daycare2.id,
                            placementType = PlacementType.DAYCARE,
                            serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                        )
                    ),
            ),
            createFeeDecisionFixture(
                status = FeeDecisionStatus.DRAFT,
                decisionType = FeeDecisionType.RELIEF_ACCEPTED,
                headOfFamilyId = testAdult_3.id,
                period = FiniteDateRange(LocalDate.of(2017, 5, 1), LocalDate.of(2017, 5, 31)),
                children =
                    listOf(
                        createFeeDecisionChildFixture(
                            childId = testChild_4.id,
                            dateOfBirth = testChild_4.dateOfBirth,
                            placementUnitId = daycare2.id,
                            placementType = PlacementType.DAYCARE,
                            serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                        )
                    ),
            ),
            createFeeDecisionFixture(
                status = FeeDecisionStatus.DRAFT,
                decisionType = FeeDecisionType.RELIEF_PARTLY_ACCEPTED,
                headOfFamilyId = testAdult_3.id,
                period = FiniteDateRange(LocalDate.of(2018, 5, 1), LocalDate.of(2018, 5, 31)),
                children =
                    listOf(
                        createFeeDecisionChildFixture(
                            childId = testChild_4.id,
                            dateOfBirth = testChild_4.dateOfBirth,
                            placementUnitId = daycare2.id,
                            placementType = PlacementType.DAYCARE,
                            serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                        )
                    ),
            ),
            createFeeDecisionFixture(
                status = FeeDecisionStatus.DRAFT,
                decisionType = FeeDecisionType.RELIEF_REJECTED,
                headOfFamilyId = testAdult_3.id,
                period = FiniteDateRange(LocalDate.of(2019, 5, 1), LocalDate.of(2019, 5, 31)),
                children =
                    listOf(
                        createFeeDecisionChildFixture(
                            childId = testChild_4.id,
                            dateOfBirth = testChild_4.dateOfBirth,
                            placementUnitId = daycare2.id,
                            placementType = PlacementType.DAYCARE,
                            serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                        )
                    ),
            ),
        )

    val preschoolClubDecisions =
        listOf(
            createFeeDecisionFixture(
                status = FeeDecisionStatus.DRAFT,
                decisionType = FeeDecisionType.NORMAL,
                headOfFamilyId = testAdult_1.id,
                period = FiniteDateRange(LocalDate.of(2018, 8, 1), LocalDate.of(2018, 8, 31)),
                children =
                    listOf(
                        createFeeDecisionChildFixture(
                            childId = testChild_1.id,
                            dateOfBirth = testChild_1.dateOfBirth,
                            placementUnitId = daycare1.id,
                            placementType = PlacementType.PRESCHOOL_CLUB,
                            serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                        ),
                        createFeeDecisionChildFixture(
                            childId = testChild_2.id,
                            dateOfBirth = testChild_2.dateOfBirth,
                            placementUnitId = daycare1.id,
                            placementType = PlacementType.DAYCARE,
                            serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                            siblingDiscount = 50,
                            fee = 14500,
                        ),
                    ),
            ),
            createFeeDecisionFixture(
                status = FeeDecisionStatus.DRAFT,
                decisionType = FeeDecisionType.NORMAL,
                headOfFamilyId = testAdult_1.id,
                period = FiniteDateRange(LocalDate.of(2018, 9, 1), LocalDate.of(2018, 9, 30)),
                children =
                    listOf(
                        createFeeDecisionChildFixture(
                            childId = testChild_1.id,
                            dateOfBirth = testChild_1.dateOfBirth,
                            placementUnitId = daycare1.id,
                            placementType = PlacementType.DAYCARE,
                            serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                        ),
                        createFeeDecisionChildFixture(
                            childId = testChild_2.id,
                            dateOfBirth = testChild_2.dateOfBirth,
                            placementUnitId = daycare1.id,
                            placementType = PlacementType.PRESCHOOL_CLUB,
                            serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                            siblingDiscount = 50,
                            fee = 14500,
                        ),
                    ),
            ),
        )

    private fun assertEqualEnough(
        expected: List<FeeDecisionSummary>,
        actual: List<FeeDecisionSummary>,
    ) {
        val created = HelsinkiDateTime.now()
        assertEquals(
            expected.map { it.copy(created = created, approvedAt = null, sentAt = null) }.toSet(),
            actual.map { it.copy(created = created, approvedAt = null, sentAt = null) }.toSet(),
        )
    }

    private fun assertEqualEnough(expected: FeeDecisionDetailed, actual: FeeDecisionDetailed) {
        val created = HelsinkiDateTime.now()
        assertEquals(
            expected.copy(
                created = created,
                approvedAt = null,
                sentAt = null,
                headOfFamily = expected.headOfFamily.copy(email = null),
            ),
            actual.copy(created = created, approvedAt = null, sentAt = null),
        )
    }

    private val allAreas = listOf(area1, area2)
    private val allDaycares = listOf(daycare1, daycare2)
    private val allWorkers = listOf(testDecisionMaker_1, testDecisionMaker_2)
    private val allAdults =
        listOf(
            testAdult_1,
            testAdult_2,
            testAdult_3,
            testAdult_4,
            testAdult_5,
            testAdult_6,
            testAdult_7,
        )
    private val allChildren = listOf(testChild_1, testChild_2, testChild_3, testChild_4)

    @BeforeEach
    fun beforeEach() {
        MockSfiMessagesClient.reset()
        MockEmailClient.clear()
        db.transaction { tx ->
            allWorkers.forEach { tx.insert(it) }
            allAreas.forEach { tx.insert(it) }
            allDaycares.forEach { tx.insert(it) }
            allAdults.forEach { tx.insert(it, DevPersonType.ADULT) }
            allChildren.forEach { tx.insert(it, DevPersonType.CHILD) }
            listOf(snDaycareFullDay35, snDaycarePartDay25, snDefaultDaycare).forEach {
                tx.insert(it)
            }
        }
    }

    @Test
    fun `search works with no data in DB`() {
        val result = searchDecisions(SearchFeeDecisionRequest(page = 0))

        assertEqualEnough(listOf(), result.data)
    }

    @Test
    fun `search works with test data in DB`() {
        db.transaction { tx -> tx.upsertFeeDecisions(testDecisions) }

        val result = searchDecisions(SearchFeeDecisionRequest(page = 0))

        assertEqualEnough(testDecisions.map(::toSummary), result.data)
    }

    @Test
    fun `search works with draft status parameter`() {
        db.transaction { tx -> tx.upsertFeeDecisions(testDecisions) }
        val drafts = testDecisions.filter { it.status == FeeDecisionStatus.DRAFT }

        val result =
            searchDecisions(
                SearchFeeDecisionRequest(page = 0, statuses = listOf(FeeDecisionStatus.DRAFT))
            )

        assertEqualEnough(drafts.map(::toSummary), result.data)
    }

    @Test
    fun `search works with sent status parameter`() {
        db.transaction { tx -> tx.upsertFeeDecisions(testDecisions) }
        val sent = testDecisions.filter { it.status == FeeDecisionStatus.SENT }

        val result =
            searchDecisions(
                SearchFeeDecisionRequest(page = 0, statuses = listOf(FeeDecisionStatus.SENT))
            )

        assertEqualEnough(sent.map(::toSummary), result.data)
    }

    @Test
    fun `search works with multiple status parameters`() {
        db.transaction { tx -> tx.upsertFeeDecisions(testDecisions) }

        val result =
            searchDecisions(
                SearchFeeDecisionRequest(
                    page = 0,
                    statuses = listOf(FeeDecisionStatus.DRAFT, FeeDecisionStatus.SENT),
                )
            )

        assertEqualEnough(testDecisions.map(::toSummary), result.data)
    }

    @Test
    fun `search works with distinctions param UNCONFIRMED_HOURS`() {
        val testDecision = testDecisions[0]
        val testDecisionMissingServiceNeed =
            testDecisions[1].copy(
                id = FeeDecisionId(UUID.randomUUID()),
                children =
                    testDecisions[1].children[0].let { part ->
                        listOf(part.copy(serviceNeed = part.serviceNeed.copy(missing = true)))
                    },
            )

        db.transaction { tx ->
            tx.upsertFeeDecisions(listOf(testDecision, testDecisionMissingServiceNeed))
        }

        val result =
            searchDecisions(
                SearchFeeDecisionRequest(
                    page = 0,
                    distinctions = listOf(DistinctiveParams.UNCONFIRMED_HOURS),
                )
            )

        assertEqualEnough(listOf(toSummary(testDecisionMissingServiceNeed)), result.data)
    }

    @Test
    fun `search works with distinctions param EXTERNAL_CHILD`() {
        val testDecisionWithExtChild = testDecisions[4]

        db.transaction { tx ->
            tx.upsertFeeDecisions(
                listOf(testDecisions[0], testDecisions[3], testDecisionWithExtChild)
            )
        }

        val result =
            searchDecisions(
                SearchFeeDecisionRequest(
                    page = 0,
                    distinctions = listOf(DistinctiveParams.EXTERNAL_CHILD),
                )
            )

        assertEqualEnough(listOf(toSummary(testDecisionWithExtChild)), result.data)
    }

    @Test
    fun `search works with distinctions param RETROACTIVE`() {
        val now = LocalDate.now()
        val oldDecision =
            testDecisions[0].copy(
                validDuring = FiniteDateRange(now.minusMonths(2), now.minusMonths(1))
            )
        val futureDecision =
            testDecisions[1].copy(
                validDuring = FiniteDateRange(now.plusMonths(1), now.plusMonths(2))
            )

        db.transaction { tx -> tx.upsertFeeDecisions(listOf(oldDecision, futureDecision)) }

        val result =
            searchDecisions(
                SearchFeeDecisionRequest(
                    page = 0,
                    distinctions = listOf(DistinctiveParams.RETROACTIVE),
                )
            )

        assertEqualEnough(listOf(toSummary(oldDecision)), result.data)
    }

    @Test
    fun `search works with distinctions param NO_STARTING_PLACEMENTS`() {
        val testDecisionWithOneStartingChild = testDecisions[0]
        val firstPlacementStartingThisMonthChild = testDecisionWithOneStartingChild.children[0]
        db.transaction {
            it.insert(
                DevPlacement(
                    childId = firstPlacementStartingThisMonthChild.child.id,
                    unitId = firstPlacementStartingThisMonthChild.placement.unitId,
                    startDate = LocalDate.now(),
                    endDate = LocalDate.now(),
                )
            )
        }

        val testDecisionWithNoStartingChild = testDecisions[3]
        val previousPlacementChild = testDecisionWithNoStartingChild.children[0]
        db.transaction {
            it.insert(
                DevPlacement(
                    childId = previousPlacementChild.child.id,
                    unitId = previousPlacementChild.placement.unitId,
                    startDate = LocalDate.now().minusMonths(1),
                    endDate = LocalDate.now(),
                )
            )
        }

        db.transaction { tx ->
            tx.upsertFeeDecisions(
                listOf(testDecisionWithOneStartingChild, testDecisionWithNoStartingChild)
            )
        }

        val result =
            searchDecisions(
                SearchFeeDecisionRequest(
                    page = 0,
                    distinctions = listOf(DistinctiveParams.NO_STARTING_PLACEMENTS),
                )
            )

        assertEqualEnough(listOf(toSummary(testDecisionWithNoStartingChild)), result.data)
    }

    @Test
    fun `search works with distinctions param PRESCHOOL_CLUB`() {
        db.transaction { tx -> tx.upsertFeeDecisions(testDecisions + preschoolClubDecisions) }
        val result =
            searchDecisions(
                SearchFeeDecisionRequest(
                    page = 0,
                    distinctions = listOf(DistinctiveParams.PRESCHOOL_CLUB),
                )
            )

        assertEquals(2, result.data.size)
        assertEqualEnough(preschoolClubDecisions.map { toSummary(it) }, result.data)
    }

    @Test
    fun `search works with distinctions param NO_OPEN_INCOME_STATEMENTS`() {
        val clock = RealEvakaClock()
        val decisionWithHandledStatement =
            createFeeDecisionsForFamily(testAdult_1, testAdult_2, listOf(testChild_1))
        val decisionWithOpenAdultStatement =
            createFeeDecisionsForFamily(testAdult_3, testAdult_4, listOf(testChild_2))
        val decisionWithFarAwayAndFutureOpenStatements =
            createFeeDecisionsForFamily(testAdult_5, testAdult_6, listOf(testChild_3))
        val decisionWithOpenChildStatement =
            createFeeDecisionsForFamily(testAdult_7, partner = null, listOf(testChild_4))

        db.transaction { tx ->
            tx.upsertFeeDecisions(
                listOf(
                    decisionWithHandledStatement,
                    decisionWithOpenAdultStatement,
                    decisionWithFarAwayAndFutureOpenStatements,
                    decisionWithOpenChildStatement,
                )
            )

            tx.insert(
                DevIncomeStatement(
                    personId = testAdult_1.id,
                    data =
                        IncomeStatementBody.HighestFee(
                            clock.today().minusMonths(2),
                            clock.today().minusMonths(1),
                        ),
                    status = IncomeStatementStatus.HANDLED,
                    handledAt = clock.now(),
                    handlerId = testDecisionMaker_1.id,
                )
            )

            // testAdult_2 statement not submitted
            tx.insert(
                DevIncomeStatement(
                    personId = testAdult_2.id,
                    data =
                        IncomeStatementBody.HighestFee(
                            clock.today().minusMonths(2),
                            clock.today().minusMonths(1),
                        ),
                    status = IncomeStatementStatus.DRAFT,
                    sentAt = null,
                )
            )

            tx.insert(
                DevIncomeStatement(
                    personId = testAdult_3.id,
                    data =
                        IncomeStatementBody.HighestFee(
                            clock.today().minusMonths(2),
                            clock.today().minusMonths(1),
                        ),
                    status = IncomeStatementStatus.HANDLED,
                    handledAt = clock.now(),
                    handlerId = testDecisionMaker_1.id,
                )
            )

            // testAdult_4 statement not handled
            tx.insert(
                DevIncomeStatement(
                    personId = testAdult_4.id,
                    data =
                        IncomeStatementBody.HighestFee(
                            clock.today().minusMonths(2),
                            clock.today().minusMonths(1),
                        ),
                    status = IncomeStatementStatus.SENT,
                    handledAt = null,
                    handlerId = null,
                )
            )

            // testAdult_5 statement not handled
            tx.insert(
                DevIncomeStatement(
                    personId = testAdult_5.id,
                    data =
                        IncomeStatementBody.HighestFee(
                            clock.today().minusMonths(20),
                            clock.today().minusMonths(14).minusDays(1),
                        ),
                    status = IncomeStatementStatus.SENT,
                    handledAt = null,
                    handlerId = null,
                )
            )

            // testAdult_6 statement not handled
            tx.insert(
                DevIncomeStatement(
                    personId = testAdult_6.id,
                    data =
                        IncomeStatementBody.HighestFee(
                            clock.today().plusDays(1),
                            clock.today().plusMonths(12),
                        ),
                    status = IncomeStatementStatus.SENT,
                    handledAt = null,
                    handlerId = null,
                )
            )

            tx.insert(
                DevIncomeStatement(
                    personId = testAdult_7.id,
                    data =
                        IncomeStatementBody.HighestFee(
                            clock.today().minusMonths(2),
                            clock.today().minusMonths(1),
                        ),
                    status = IncomeStatementStatus.HANDLED,
                    handledAt = clock.now(),
                    handlerId = testDecisionMaker_1.id,
                )
            )

            // testChild_4 statement not handled
            tx.insert(
                DevIncomeStatement(
                    personId = testChild_4.id,
                    data =
                        IncomeStatementBody.HighestFee(
                            clock.today().minusMonths(2),
                            clock.today().minusMonths(1),
                        ),
                    status = IncomeStatementStatus.SENT,
                    handledAt = null,
                    handlerId = null,
                )
            )
        }

        val result =
            searchDecisions(
                SearchFeeDecisionRequest(
                    page = 0,
                    distinctions = listOf(DistinctiveParams.NO_OPEN_INCOME_STATEMENTS),
                )
            )

        assertEqualEnough(
            listOf(
                toSummary(decisionWithHandledStatement),
                toSummary(decisionWithFarAwayAndFutureOpenStatements),
            ),
            result.data,
        )
    }

    @Test
    fun `search works with distinctions param NO_OPEN_INCOME_STATEMENTS for childless decisions`() {
        val clock = RealEvakaClock()
        val decisionWithHandledStatements =
            createFeeDecisionsForFamily(testAdult_1, testAdult_2, listOf())
        val decisionWithOpenStatements =
            createFeeDecisionsForFamily(testAdult_3, testAdult_4, listOf())

        db.transaction { tx ->
            tx.upsertFeeDecisions(listOf(decisionWithHandledStatements, decisionWithOpenStatements))
            tx.insert(
                DevIncomeStatement(
                    personId = testAdult_1.id,
                    data =
                        IncomeStatementBody.HighestFee(
                            clock.today().minusMonths(2),
                            clock.today().minusMonths(1),
                        ),
                    status = IncomeStatementStatus.HANDLED,
                    handledAt = clock.now(),
                    handlerId = testDecisionMaker_1.id,
                )
            )
            // testAdult_2 statement not submitted
            tx.insert(
                DevIncomeStatement(
                    personId = testAdult_2.id,
                    data =
                        IncomeStatementBody.HighestFee(
                            clock.today().minusMonths(2),
                            clock.today().minusMonths(1),
                        ),
                    status = IncomeStatementStatus.DRAFT,
                    sentAt = null,
                )
            )
            tx.insert(
                DevIncomeStatement(
                    personId = testAdult_3.id,
                    data =
                        IncomeStatementBody.HighestFee(
                            clock.today().minusMonths(2),
                            clock.today().minusMonths(1),
                        ),
                    status = IncomeStatementStatus.HANDLED,
                    handledAt = clock.now(),
                    handlerId = testDecisionMaker_1.id,
                )
            )
            // testAdult_4 statement not handled
            tx.insert(
                DevIncomeStatement(
                    personId = testAdult_4.id,
                    data =
                        IncomeStatementBody.HighestFee(
                            clock.today().minusMonths(2),
                            clock.today().minusMonths(1),
                        ),
                    status = IncomeStatementStatus.SENT,
                    handledAt = null,
                    handlerId = null,
                )
            )
        }

        val result =
            searchDecisions(
                SearchFeeDecisionRequest(
                    page = 0,
                    distinctions = listOf(DistinctiveParams.NO_OPEN_INCOME_STATEMENTS),
                )
            )

        assertEqualEnough(listOf(toSummary(decisionWithHandledStatements)), result.data)
    }

    @Test
    fun `search works as expected with existing area param`() {
        db.transaction { tx -> tx.upsertFeeDecisions(testDecisions) }

        val result =
            searchDecisions(SearchFeeDecisionRequest(page = 0, area = listOf(area1.shortName)))

        assertEqualEnough(testDecisions.map(::toSummary).take(3), result.data)
    }

    @Test
    fun `search works as expected with area and status params`() {
        db.transaction { tx -> tx.upsertFeeDecisions(testDecisions) }

        val result =
            searchDecisions(
                SearchFeeDecisionRequest(
                    page = 0,
                    area = listOf(area1.shortName),
                    statuses = listOf(FeeDecisionStatus.DRAFT),
                )
            )

        assertEqualEnough(testDecisions.take(1).map(::toSummary), result.data)
    }

    @Test
    fun `search works as expected with non-existent area param`() {
        db.transaction { tx -> tx.upsertFeeDecisions(testDecisions) }

        val result =
            searchDecisions(SearchFeeDecisionRequest(page = 0, area = listOf("non_existent")))
        assertEqualEnough(listOf(), result.data)
    }

    @Test
    fun `search works as expected with a unit param`() {
        db.transaction { tx -> tx.upsertFeeDecisions(testDecisions) }

        val result = searchDecisions(SearchFeeDecisionRequest(page = 0, unit = daycare1.id))

        val expected =
            testDecisions.filter { decision ->
                decision.children.any { it.placement.unitId == daycare1.id }
            }
        assertEqualEnough(expected.map(::toSummary), result.data)
    }

    @Test
    fun `search works as expected with a non-existent unit id param`() {
        db.transaction { tx -> tx.upsertFeeDecisions(testDecisions) }

        val result =
            searchDecisions(SearchFeeDecisionRequest(page = 0, unit = DaycareId(UUID.randomUUID())))

        assertEqualEnough(listOf(), result.data)
    }

    @Test
    fun `search works as expected with multiple partial search terms`() {
        db.transaction { tx -> tx.upsertFeeDecisions(testDecisions) }

        val result =
            searchDecisions(
                SearchFeeDecisionRequest(
                    page = 0,
                    searchTerms =
                        "${testAdult_1.streetAddress} ${testAdult_1.firstName.substring(0, 2)}",
                )
            )

        val expectedDecisions = testDecisions.filter { it.headOfFamilyId != testAdult_3.id }

        assertEqualEnough(expectedDecisions.map(::toSummary), result.data)
    }

    @Test
    fun `search works as expected with multiple more specific search terms`() {
        db.transaction { tx -> tx.upsertFeeDecisions(testDecisions) }

        val result =
            searchDecisions(
                SearchFeeDecisionRequest(
                    page = 0,
                    searchTerms = "${testAdult_1.lastName.substring(0, 2)} ${testAdult_1.firstName}",
                )
            )

        assertEqualEnough(testDecisions.take(2).map(::toSummary), result.data)
    }

    @Test
    fun `search works as expected with multiple search terms where one does not match anything`() {
        db.transaction { tx -> tx.upsertFeeDecisions(testDecisions) }

        val result =
            searchDecisions(
                SearchFeeDecisionRequest(
                    page = 0,
                    searchTerms = "${testAdult_1.lastName} ${testAdult_1.streetAddress} nomatch",
                )
            )

        assertEqualEnough(listOf(), result.data)
    }

    @Test
    fun `search works as expected with child name as search term`() {
        db.transaction { tx -> tx.upsertFeeDecisions(testDecisions) }

        val result =
            searchDecisions(
                SearchFeeDecisionRequest(
                    page = 0,
                    searchTerms = testChild_2.firstName,
                    sortBy = FeeDecisionSortParam.STATUS,
                    sortDirection = SortDirection.ASC,
                )
            )

        assertEqualEnough(
            testDecisions.take(2).map(::toSummary).sortedBy { it.status.name },
            result.data,
        )
    }

    @Test
    fun `search works as expected with decision number as search term`() {
        val sentDecision =
            testDecisions
                .find { it.status == FeeDecisionStatus.SENT }!!
                .copy(decisionNumber = 123123123L)
        val otherDecision = testDecisions.find { it.status == FeeDecisionStatus.DRAFT }!!
        db.transaction { tx -> tx.upsertFeeDecisions(listOf(sentDecision, otherDecision)) }

        val result = searchDecisions(SearchFeeDecisionRequest(page = 0, searchTerms = "123123123"))

        assertEqualEnough(listOf(sentDecision).map(::toSummary), result.data)
    }

    @Test
    fun `search works as expected with ssn as search term`() {
        db.transaction { tx -> tx.upsertFeeDecisions(testDecisions) }

        val result =
            searchDecisions(SearchFeeDecisionRequest(page = 0, searchTerms = testAdult_1.ssn))

        assertEqualEnough(testDecisions.take(2).map(::toSummary), result.data)
    }

    @Test
    fun `search works as expected with date of birth as search term`() {
        db.transaction { tx -> tx.upsertFeeDecisions(testDecisions) }

        val result =
            searchDecisions(
                SearchFeeDecisionRequest(page = 0, searchTerms = testAdult_1.ssn!!.substring(0, 6))
            )

        assertEqualEnough(testDecisions.take(2).map(::toSummary), result.data)
    }

    @Test
    fun `getDecision works with existing decision`() {
        db.transaction { tx -> tx.upsertFeeDecisions(testDecisions) }
        val decision = testDecisions[0]

        val result = getDecision(decision.id)
        assertEqualEnough(toDetailed(decision), result)
    }

    @Test
    fun `getDecision returns not found with non-existent decision`() {
        db.transaction { tx -> tx.upsertFeeDecisions(testDecisions) }

        assertThrows<NotFound> {
            getDecision(FeeDecisionId(UUID.fromString("00000000-0000-0000-0000-000000000000")))
        }
    }

    @Test
    fun `getting head of family's fee decisions works`() {
        db.transaction { tx -> tx.upsertFeeDecisions(testDecisions) }

        val feeDecisions = getHeadOfFamilyDecisions(testAdult_1.id)

        assertEquals(2, feeDecisions.size)
        feeDecisions
            .find { it.status == FeeDecisionStatus.DRAFT }
            .let { draft ->
                assertNotNull(draft)
                assertEquals(testAdult_1.id, draft.headOfFamilyId)
                assertEquals(28900 + 14500, draft.totalFee)
            }
        feeDecisions
            .find { it.status == FeeDecisionStatus.SENT }
            .let { sent ->
                assertNotNull(sent)
                assertEquals(testAdult_1.id, sent.headOfFamilyId)
                assertEquals(28900, sent.totalFee)
            }
    }

    @Test
    fun `confirmDrafts works with draft decision`() {
        db.transaction { tx -> tx.upsertFeeDecisions(testDecisions) }
        val draft = testDecisions.find { it.status == FeeDecisionStatus.DRAFT }!!

        confirmDrafts(listOf(draft.id))
    }

    @Test
    fun `confirmDrafts updates status, decision number, approver and process id on draft`() {
        db.transaction { tx -> tx.upsertFeeDecisions(testDecisions) }
        val draft = testDecisions.find { it.status == FeeDecisionStatus.DRAFT }!!

        confirmDrafts(listOf(draft.id))

        asyncJobRunner.runPendingJobsSync(RealEvakaClock(), 2)

        val activated =
            draft.copy(
                decisionNumber = 1,
                status = FeeDecisionStatus.SENT,
                approvedById = testDecisionMaker_1.id,
                decisionHandlerId = testDecisionMaker_1.id,
                documentKey = "feedecision_${draft.id}_fi.pdf",
            )

        val result = getDecision(draft.id)
        assertEqualEnough(toDetailed(activated), result)

        val process = db.read { tx -> tx.getCaseProcessByFeeDecisionId(draft.id) }
        assertNotNull(process)
        assertEquals("1/123.789.a/${LocalDate.now().year}", process.caseIdentifier)
        assertEquals(
            listOf(CaseProcessState.INITIAL, CaseProcessState.DECIDING, CaseProcessState.COMPLETED),
            process.history.map { it.state },
        )
        assertEquals(
            listOf(
                AuthenticatedUser.SystemInternalUser.evakaUserId,
                user.evakaUserId,
                AuthenticatedUser.SystemInternalUser.evakaUserId,
            ),
            process.history.map { it.enteredBy.id },
        )

        val metadata =
            processMetadataController.getFeeDecisionMetadata(
                dbInstance(),
                adminUser,
                RealEvakaClock(),
                draft.id,
            )
        assertEquals(process.caseIdentifier, metadata.data?.process?.caseIdentifier)
    }

    @Test
    fun `confirmDrafts updates status, decision number, approver on relief decision`() {
        db.transaction { tx -> tx.upsertFeeDecisions(testDecisions) }
        val draft = testDecisions.find { it.decisionType == FeeDecisionType.RELIEF_ACCEPTED }!!

        confirmDrafts(listOf(draft.id))

        asyncJobRunner.runPendingJobsSync(RealEvakaClock(), 2)

        val activated =
            draft.copy(
                decisionNumber = 1,
                status = FeeDecisionStatus.WAITING_FOR_MANUAL_SENDING,
                approvedById = testDecisionMaker_1.id,
                decisionHandlerId = testDecisionMaker_1.id,
                documentKey = "feedecision_${draft.id}_fi.pdf",
            )

        val result = getDecision(draft.id)
        assertEqualEnough(toDetailed(activated), result)
    }

    @Test
    fun `confirmDrafts updates status, decision number, approver on relief partly accepted`() {
        db.transaction { tx -> tx.upsertFeeDecisions(testDecisions) }
        val draft =
            testDecisions.find { it.decisionType == FeeDecisionType.RELIEF_PARTLY_ACCEPTED }!!

        confirmDrafts(listOf(draft.id))

        asyncJobRunner.runPendingJobsSync(RealEvakaClock(), 2)

        val activated =
            draft.copy(
                decisionNumber = 1,
                status = FeeDecisionStatus.WAITING_FOR_MANUAL_SENDING,
                approvedById = testDecisionMaker_1.id,
                decisionHandlerId = testDecisionMaker_1.id,
                documentKey = "feedecision_${draft.id}_fi.pdf",
            )

        val result = getDecision(draft.id)
        assertEqualEnough(toDetailed(activated), result)
    }

    @Test
    fun `confirmDrafts updates status, decision number, approver on relief rejected`() {
        db.transaction { tx -> tx.upsertFeeDecisions(testDecisions) }
        val draft = testDecisions.find { it.decisionType == FeeDecisionType.RELIEF_REJECTED }!!

        confirmDrafts(listOf(draft.id))

        asyncJobRunner.runPendingJobsSync(RealEvakaClock(), 2)

        val activated =
            draft.copy(
                decisionNumber = 1,
                status = FeeDecisionStatus.WAITING_FOR_MANUAL_SENDING,
                approvedById = testDecisionMaker_1.id,
                decisionHandlerId = testDecisionMaker_1.id,
                documentKey = "feedecision_${draft.id}_fi.pdf",
            )

        val result = getDecision(draft.id)
        assertEqualEnough(toDetailed(activated), result)
    }

    @Test
    fun `confirmDrafts picks decision handler from unit when decision is not a relief decision nor retroactive`() {
        val draft =
            testDecisions
                .find { it.status == FeeDecisionStatus.DRAFT }!!
                .copy(
                    validDuring =
                        FiniteDateRange(
                            LocalDate.now().withDayOfMonth(1),
                            LocalDate.now().plusMonths(1),
                        )
                )
        db.transaction { tx -> tx.upsertFeeDecisions(listOf(draft)) }
        db.transaction {
            it.execute {
                sql(
                    """
                    UPDATE daycare
                    SET finance_decision_handler = ${bind(testDecisionMaker_2.id)}
                    WHERE id = ${bind(draft.children.maxByOrNull { p -> p.child.dateOfBirth }!!.placement.unitId)}
                """
                )
            }
        }

        confirmDrafts(listOf(draft.id))

        asyncJobRunner.runPendingJobsSync(RealEvakaClock(), 2)

        val activated =
            draft.copy(
                decisionNumber = 1,
                status = FeeDecisionStatus.SENT,
                approvedById = testDecisionMaker_1.id,
                decisionHandlerId = testDecisionMaker_2.id,
                documentKey = "feedecision_${draft.id}_fi.pdf",
            )

        val result = getDecision(draft.id)
        assertEqualEnough(toDetailed(activated), result)
    }

    @Test
    fun `confirmDrafts uses approver as decision handler when decision is a relief decision`() {
        db.transaction { tx -> tx.upsertFeeDecisions(testDecisions) }
        val draft = testDecisions.find { it.decisionType == FeeDecisionType.RELIEF_ACCEPTED }!!
        db.transaction {
            it.execute {
                sql(
                    """
                    UPDATE daycare
                    SET finance_decision_handler = ${bind(testDecisionMaker_2.id)}
                    WHERE id = ${bind(draft.children.maxByOrNull { p -> p.child.dateOfBirth }!!.placement.unitId)}
                    """
                )
            }
        }

        confirmDrafts(listOf(draft.id))

        asyncJobRunner.runPendingJobsSync(RealEvakaClock(), 2)

        val activated =
            draft.copy(
                decisionNumber = 1,
                status = FeeDecisionStatus.WAITING_FOR_MANUAL_SENDING,
                approvedById = testDecisionMaker_1.id,
                decisionHandlerId = testDecisionMaker_1.id,
                documentKey = "feedecision_${draft.id}_fi.pdf",
            )

        val result = getDecision(draft.id)
        assertEqualEnough(toDetailed(activated), result)
    }

    @Test
    fun `confirmDrafts uses approver as decision handler when decision is retroactive`() {
        val now = LocalDate.now()
        val oldDecision =
            testDecisions
                .first()
                .copy(validDuring = FiniteDateRange(now.minusMonths(2), now.minusMonths(1)))

        db.transaction { tx -> tx.upsertFeeDecisions(listOf(oldDecision)) }

        db.transaction {
            it.execute {
                sql(
                    """
                    UPDATE daycare
                    SET finance_decision_handler = ${bind(testDecisionMaker_2.id)}
                    WHERE id = ${bind(oldDecision.children.maxByOrNull { p -> p.child.dateOfBirth }!!.placement.unitId)}
                """
                )
            }
        }

        confirmDrafts(listOf(oldDecision.id))

        asyncJobRunner.runPendingJobsSync(RealEvakaClock(), 2)

        val activated =
            oldDecision.copy(
                decisionNumber = 1,
                status = FeeDecisionStatus.SENT,
                approvedById = testDecisionMaker_1.id,
                decisionHandlerId = testDecisionMaker_1.id,
                documentKey = "feedecision_${oldDecision.id}_fi.pdf",
            )

        val result = getDecision(oldDecision.id)
        assertEqualEnough(toDetailed(activated), result)
    }

    @Test
    fun `sendDecisions does not send if ssn missing`() {
        val draft =
            testDecisions.find {
                it.status == FeeDecisionStatus.DRAFT && it.headOfFamilyId == testAdult_3.id
            }!!

        db.transaction { tx -> tx.upsertFeeDecisions(testDecisions) }

        confirmDrafts(listOf(draft.id))

        asyncJobRunner.runPendingJobsSync(RealEvakaClock(), 2)

        val result = getDecision(draft.id)

        val activated =
            draft.copy(
                decisionNumber = 1,
                status = FeeDecisionStatus.WAITING_FOR_MANUAL_SENDING,
                approvedById = testDecisionMaker_1.id,
                decisionHandlerId = testDecisionMaker_1.id,
                documentKey = "feedecision_${draft.id}_fi.pdf",
            )

        assertEqualEnough(toDetailed(activated), result)
    }

    @Test
    fun `confirmDrafts returns bad request when some decisions are not drafts`() {
        db.transaction { tx -> tx.upsertFeeDecisions(testDecisions) }

        assertThrows<BadRequest> { confirmDrafts(testDecisions.map { it.id }) }
    }

    @Test
    fun `confirmDrafts returns bad request when some decisions being in the future`() {
        val draftWithFutureDates =
            testDecisions
                .find { it.status == FeeDecisionStatus.DRAFT }!!
                .copy(
                    validDuring =
                        FiniteDateRange(LocalDate.now().plusDays(2), LocalDate.now().plusYears(1))
                )
        db.transaction { tx -> tx.upsertFeeDecisions(listOf(draftWithFutureDates)) }

        val error = assertThrows<BadRequest> { confirmDrafts(listOf(draftWithFutureDates.id)) }
        assertEquals("feeDecisions.confirmation.tooFarInFuture", error.errorCode)
    }

    @Test
    fun `confirmDrafts when decision at last possible confirmation date exists`() {
        val now = HelsinkiDateTime.now()
        val draftWithFutureDates =
            testDecisions
                .find { it.status == FeeDecisionStatus.DRAFT }!!
                .copy(
                    validDuring =
                        FiniteDateRange(
                            now.toLocalDate().plusDays(1),
                            now.toLocalDate().plusYears(1),
                        )
                )
        db.transaction { tx -> tx.upsertFeeDecisions(listOf(draftWithFutureDates)) }

        confirmDrafts(listOf(draftWithFutureDates.id), MockEvakaClock(now))

        asyncJobRunner.runPendingJobsSync(RealEvakaClock(), 2)

        val actual = getDecision(draftWithFutureDates.id)

        val expected =
            draftWithFutureDates.copy(
                decisionNumber = 1,
                status = FeeDecisionStatus.SENT,
                approvedById = testDecisionMaker_1.id,
                decisionHandlerId = testDecisionMaker_1.id,
                documentKey = "feedecision_${draftWithFutureDates.id}_fi.pdf",
            )
        assertEqualEnough(toDetailed(expected), actual)
        assertEquals(now, actual.approvedAt)
    }

    @Test
    fun `confirmDrafts updates conflicting sent decision end date on confirm`() {
        val draft = testDecisions.find { it.status == FeeDecisionStatus.DRAFT }!!
        val conflict =
            draft.copy(
                id = FeeDecisionId(UUID.randomUUID()),
                status = FeeDecisionStatus.SENT,
                validDuring = draft.validDuring.copy(start = draft.validFrom.minusDays(30)),
            )
        db.transaction { tx -> tx.upsertFeeDecisions(listOf(draft, conflict)) }

        confirmDrafts(listOf(draft.id))

        val result = getDecision(conflict.id)

        val updatedConflict =
            conflict.copy(
                validDuring = conflict.validDuring.copy(end = draft.validFrom.minusDays(1))
            )
        assertEqualEnough(toDetailed(updatedConflict), result)
    }

    @Test
    fun `confirmDrafts updates conflicting decisions with exactly same validity dates to annulled state`() {
        val draft = testDecisions.find { it.status == FeeDecisionStatus.DRAFT }!!
        val conflict =
            draft.copy(id = FeeDecisionId(UUID.randomUUID()), status = FeeDecisionStatus.SENT)
        db.transaction { tx -> tx.upsertFeeDecisions(listOf(draft, conflict)) }

        confirmDrafts(listOf(draft.id))

        val result = getDecision(conflict.id)

        val updatedConflict = conflict.copy(status = FeeDecisionStatus.ANNULLED)
        assertEqualEnough(toDetailed(updatedConflict), result)
    }

    @Test
    fun `confirmDrafts updates completely overlapped conflicting decisions to annulled state`() {
        val draft = testDecisions.find { it.status == FeeDecisionStatus.DRAFT }!!
        val conflict1 =
            draft.copy(
                id = FeeDecisionId(UUID.randomUUID()),
                status = FeeDecisionStatus.SENT,
                validDuring =
                    FiniteDateRange(draft.validFrom.plusDays(5), draft.validFrom.plusDays(10)),
            )
        val conflict2 =
            draft.copy(
                id = FeeDecisionId(UUID.randomUUID()),
                status = FeeDecisionStatus.SENT,
                validDuring =
                    FiniteDateRange(draft.validFrom.plusDays(11), draft.validFrom.plusDays(20)),
            )
        db.transaction { tx -> tx.upsertFeeDecisions(listOf(draft, conflict1, conflict2)) }

        confirmDrafts(listOf(draft.id))

        val result1 = getDecision(conflict1.id)

        val updatedConflict1 = conflict1.copy(status = FeeDecisionStatus.ANNULLED)
        assertEqualEnough(toDetailed(updatedConflict1), result1)

        val result2 = getDecision(conflict2.id)

        val updatedConflict2 = conflict2.copy(status = FeeDecisionStatus.ANNULLED)
        assertEqualEnough(toDetailed(updatedConflict2), result2)
    }

    @Test
    fun `confirmDrafts updates conflicting sent decision to annulled even when the sent decision would end later`() {
        val draft = testDecisions.find { it.status == FeeDecisionStatus.DRAFT }!!
        val conflict =
            draft.copy(
                id = FeeDecisionId(UUID.randomUUID()),
                status = FeeDecisionStatus.SENT,
                validDuring = draft.validDuring.copy(end = draft.validTo.plusDays(10)),
            )
        db.transaction { tx -> tx.upsertFeeDecisions(listOf(draft, conflict)) }

        confirmDrafts(listOf(draft.id))

        val result = getDecision(conflict.id)

        val updatedConflict = conflict.copy(status = FeeDecisionStatus.ANNULLED)
        assertEqualEnough(toDetailed(updatedConflict), result)
    }

    @Test
    fun `confirmDrafts updates conflicting sent decision to annulled even when the new decisions cover it exactly`() {
        val originalDraft = testDecisions.find { it.status == FeeDecisionStatus.DRAFT }!!
        val splitDrafts =
            listOf(
                originalDraft.copy(
                    id = FeeDecisionId(UUID.randomUUID()),
                    validDuring =
                        originalDraft.validDuring.copy(end = originalDraft.validFrom.plusDays(7)),
                ),
                originalDraft.copy(
                    id = FeeDecisionId(UUID.randomUUID()),
                    validDuring =
                        originalDraft.validDuring.copy(start = originalDraft.validFrom.plusDays(8)),
                ),
            )
        val conflict =
            originalDraft.copy(
                id = FeeDecisionId(UUID.randomUUID()),
                status = FeeDecisionStatus.SENT,
            )
        db.transaction { tx -> tx.upsertFeeDecisions(splitDrafts + conflict) }

        confirmDrafts(splitDrafts.map { it.id })

        val result = getDecision(conflict.id)

        val updatedConflict = conflict.copy(status = FeeDecisionStatus.ANNULLED)
        assertEqualEnough(toDetailed(updatedConflict), result)
    }

    @Test
    fun `confirmDrafts updates conflicting sent decision to annulled even when the new decisions cover it`() {
        val originalDraft = testDecisions.find { it.status == FeeDecisionStatus.DRAFT }!!
        val splitDrafts =
            listOf(
                originalDraft.copy(
                    id = FeeDecisionId(UUID.randomUUID()),
                    validDuring =
                        FiniteDateRange(
                            originalDraft.validFrom.minusDays(1),
                            originalDraft.validFrom.plusDays(7),
                        ),
                ),
                originalDraft.copy(
                    id = FeeDecisionId(UUID.randomUUID()),
                    validDuring =
                        FiniteDateRange(
                            originalDraft.validFrom.plusDays(8),
                            originalDraft.validTo.plusDays(1),
                        ),
                ),
            )
        val conflict =
            originalDraft.copy(
                id = FeeDecisionId(UUID.randomUUID()),
                status = FeeDecisionStatus.SENT,
            )
        db.transaction { tx -> tx.upsertFeeDecisions(splitDrafts + conflict) }

        confirmDrafts(splitDrafts.map { it.id })

        val result = getDecision(conflict.id)

        val updatedConflict = conflict.copy(status = FeeDecisionStatus.ANNULLED)
        assertEqualEnough(toDetailed(updatedConflict), result)
    }

    @Test
    fun `confirmDrafts ignores decisions that have related manual sending decisions`() {
        val toBeCreatedDecisions =
            testDecisions.filter {
                it.status == FeeDecisionStatus.DRAFT && it.decisionType == FeeDecisionType.NORMAL
            }
        db.transaction { tx -> tx.upsertFeeDecisions(toBeCreatedDecisions) }
        val notToBeCreated = toBeCreatedDecisions.first()
        val requiresManualSending =
            notToBeCreated.copy(
                id = FeeDecisionId(UUID.randomUUID()),
                status = FeeDecisionStatus.WAITING_FOR_MANUAL_SENDING,
            )
        db.transaction { tx -> tx.upsertFeeDecisions(listOf(requiresManualSending)) }

        confirmDrafts(toBeCreatedDecisions.map { it.id })

        val draftDecisions =
            searchDecisions(
                    SearchFeeDecisionRequest(page = 0, statuses = listOf(FeeDecisionStatus.DRAFT))
                )
                .data

        assertEquals(1, draftDecisions.size)
        assertEquals(notToBeCreated.id, draftDecisions.first().id)
    }

    @Test
    fun `confirmDrafts ignores decisions that have related decision waiting for sending`() {
        val toBeCreatedDecisions =
            testDecisions.filter {
                it.status == FeeDecisionStatus.DRAFT && it.decisionType == FeeDecisionType.NORMAL
            }
        db.transaction { tx -> tx.upsertFeeDecisions(toBeCreatedDecisions) }
        val notToBeCreated = toBeCreatedDecisions.first()
        val requiresSending =
            notToBeCreated.copy(
                id = FeeDecisionId(UUID.randomUUID()),
                status = FeeDecisionStatus.WAITING_FOR_SENDING,
            )
        db.transaction { tx -> tx.upsertFeeDecisions(listOf(requiresSending)) }

        confirmDrafts(toBeCreatedDecisions.map { it.id })

        val draftDecisions =
            searchDecisions(
                    SearchFeeDecisionRequest(page = 0, statuses = listOf(FeeDecisionStatus.DRAFT))
                )
                .data

        assertEquals(1, draftDecisions.size)
        assertEquals(notToBeCreated.id, draftDecisions.first().id)
    }

    @Test
    fun `confirmDrafts updates end dates of conflicting decisions correctly with different heads of families`() {
        val draft = testDecisions.find { it.status == FeeDecisionStatus.DRAFT }!!
        val conflict =
            draft.copy(
                id = FeeDecisionId(UUID.randomUUID()),
                status = FeeDecisionStatus.SENT,
                validDuring = draft.validDuring.copy(start = draft.validFrom.minusDays(30)),
            )
        val secondDraft =
            draft.copy(
                id = FeeDecisionId(UUID.randomUUID()),
                headOfFamilyId = testAdult_5.id,
                validDuring = draft.validDuring.copy(start = draft.validFrom.plusDays(15)),
            )
        val secondConflict =
            secondDraft.copy(
                id = FeeDecisionId(UUID.randomUUID()),
                headOfFamilyId = testAdult_5.id,
                status = FeeDecisionStatus.SENT,
                validDuring = draft.validDuring.copy(start = secondDraft.validFrom.minusDays(15)),
            )
        db.transaction { tx ->
            tx.upsertFeeDecisions(listOf(draft, conflict, secondDraft, secondConflict))
        }

        confirmDrafts(listOf(draft.id, secondDraft.id))

        getDecision(conflict.id).let { result ->
            val updatedConflict =
                conflict.copy(
                    validDuring = conflict.validDuring.copy(end = draft.validFrom.minusDays(1))
                )
            assertEqualEnough(toDetailed(updatedConflict), result)
        }

        getDecision(secondConflict.id).let { result ->
            val updatedConflict =
                secondConflict.copy(
                    validDuring =
                        secondConflict.validDuring.copy(end = secondDraft.validFrom.minusDays(1))
                )
            assertEqualEnough(toDetailed(updatedConflict), result)
        }
    }

    @Test
    fun `confirmDrafts replaces both parents decisions with a new combined decision`() {
        val decisionPeriod = FiniteDateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31))
        val sentDecisions =
            listOf(
                createFeeDecisionFixture(
                    status = FeeDecisionStatus.SENT,
                    decisionType = FeeDecisionType.NORMAL,
                    headOfFamilyId = testAdult_1.id,
                    partnerId = testAdult_2.id,
                    period = decisionPeriod,
                    feeThresholds = testFeeThresholds.getFeeDecisionThresholds(4),
                    familySize = 4,
                    children =
                        listOf(
                            createFeeDecisionChildFixture(
                                childId = testChild_1.id,
                                dateOfBirth = testChild_1.dateOfBirth,
                                placementUnitId = daycare1.id,
                                placementType = PlacementType.DAYCARE,
                                serviceNeed = snDefaultDaycare.toFeeDecisionServiceNeed(),
                                baseFee = 28900,
                                siblingDiscount = 0,
                                fee = 28900,
                            )
                        ),
                ),
                createFeeDecisionFixture(
                    status = FeeDecisionStatus.SENT,
                    decisionType = FeeDecisionType.NORMAL,
                    headOfFamilyId = testAdult_2.id,
                    partnerId = testAdult_1.id,
                    period = decisionPeriod,
                    feeThresholds = testFeeThresholds.getFeeDecisionThresholds(4),
                    familySize = 4,
                    children =
                        listOf(
                            createFeeDecisionChildFixture(
                                childId = testChild_2.id,
                                dateOfBirth = testChild_2.dateOfBirth,
                                placementUnitId = daycare1.id,
                                placementType = PlacementType.DAYCARE,
                                serviceNeed = snDefaultDaycare.toFeeDecisionServiceNeed(),
                                baseFee = 28900,
                                siblingDiscount = 50,
                                fee = 14500,
                            )
                        ),
                ),
            )
        db.transaction { tx -> tx.upsertFeeDecisions(sentDecisions) }
        val newDraft =
            createFeeDecisionFixture(
                status = FeeDecisionStatus.DRAFT,
                decisionType = FeeDecisionType.NORMAL,
                headOfFamilyId = testAdult_1.id,
                partnerId = testAdult_2.id,
                period = decisionPeriod,
                feeThresholds = testFeeThresholds.getFeeDecisionThresholds(4),
                familySize = 4,
                children =
                    listOf(
                        createFeeDecisionChildFixture(
                            childId = testChild_1.id,
                            dateOfBirth = testChild_1.dateOfBirth,
                            placementUnitId = daycare1.id,
                            placementType = PlacementType.DAYCARE,
                            serviceNeed = snDefaultDaycare.toFeeDecisionServiceNeed(),
                            baseFee = 28900,
                            siblingDiscount = 0,
                            fee = 28900,
                        ),
                        createFeeDecisionChildFixture(
                            childId = testChild_2.id,
                            dateOfBirth = testChild_2.dateOfBirth,
                            placementUnitId = daycare1.id,
                            placementType = PlacementType.DAYCARE,
                            serviceNeed = snDaycarePartDay25.toFeeDecisionServiceNeed(),
                            baseFee = 28900,
                            siblingDiscount = 50,
                            fee = 8700,
                        ),
                    ),
            )
        db.transaction { tx -> tx.upsertFeeDecisions(listOf(newDraft)) }

        confirmDrafts(listOf(newDraft.id))
        asyncJobRunner.runPendingJobsSync(RealEvakaClock(), 2)

        val newSentDecisions =
            searchDecisions(
                    SearchFeeDecisionRequest(page = 0, statuses = listOf(FeeDecisionStatus.SENT))
                )
                .data

        assertEquals(1, newSentDecisions.size)
        assertEquals(newDraft.id, newSentDecisions.first().id)

        val annulledDecisions =
            searchDecisions(
                    SearchFeeDecisionRequest(
                        page = 0,
                        statuses = listOf(FeeDecisionStatus.ANNULLED),
                    )
                )
                .data

        assertEquals(2, annulledDecisions.size)
        assertEquals(sentDecisions.map { it.id }.toSet(), annulledDecisions.map { it.id }.toSet())
    }

    @Test
    fun `confirmDrafts updates both parents decisions end dates when a new combined decision is sent`() {
        val decisionPeriod = FiniteDateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31))
        val sentDecisions =
            listOf(
                createFeeDecisionFixture(
                    status = FeeDecisionStatus.SENT,
                    decisionType = FeeDecisionType.NORMAL,
                    headOfFamilyId = testAdult_1.id,
                    partnerId = testAdult_2.id,
                    period = decisionPeriod,
                    feeThresholds = testFeeThresholds.getFeeDecisionThresholds(4),
                    familySize = 4,
                    children =
                        listOf(
                            createFeeDecisionChildFixture(
                                childId = testChild_1.id,
                                dateOfBirth = testChild_1.dateOfBirth,
                                placementUnitId = daycare1.id,
                                placementType = PlacementType.DAYCARE,
                                serviceNeed = snDefaultDaycare.toFeeDecisionServiceNeed(),
                                baseFee = 28900,
                                siblingDiscount = 0,
                                fee = 28900,
                            )
                        ),
                ),
                createFeeDecisionFixture(
                    status = FeeDecisionStatus.SENT,
                    decisionType = FeeDecisionType.NORMAL,
                    headOfFamilyId = testAdult_2.id,
                    partnerId = testAdult_1.id,
                    period = decisionPeriod,
                    feeThresholds = testFeeThresholds.getFeeDecisionThresholds(4),
                    familySize = 4,
                    children =
                        listOf(
                            createFeeDecisionChildFixture(
                                childId = testChild_2.id,
                                dateOfBirth = testChild_2.dateOfBirth,
                                placementUnitId = daycare1.id,
                                placementType = PlacementType.DAYCARE,
                                serviceNeed = snDefaultDaycare.toFeeDecisionServiceNeed(),
                                baseFee = 28900,
                                siblingDiscount = 50,
                                fee = 14500,
                            )
                        ),
                ),
            )
        db.transaction { tx -> tx.upsertFeeDecisions(sentDecisions) }
        val newDraft =
            createFeeDecisionFixture(
                status = FeeDecisionStatus.DRAFT,
                decisionType = FeeDecisionType.NORMAL,
                headOfFamilyId = testAdult_1.id,
                partnerId = testAdult_2.id,
                period = decisionPeriod.copy(start = decisionPeriod.start.plusDays(31)),
                feeThresholds = testFeeThresholds.getFeeDecisionThresholds(4),
                familySize = 4,
                children =
                    listOf(
                        createFeeDecisionChildFixture(
                            childId = testChild_1.id,
                            dateOfBirth = testChild_1.dateOfBirth,
                            placementUnitId = daycare1.id,
                            placementType = PlacementType.DAYCARE,
                            serviceNeed = snDefaultDaycare.toFeeDecisionServiceNeed(),
                            baseFee = 28900,
                            siblingDiscount = 0,
                            fee = 28900,
                        ),
                        createFeeDecisionChildFixture(
                            childId = testChild_2.id,
                            dateOfBirth = testChild_2.dateOfBirth,
                            placementUnitId = daycare1.id,
                            placementType = PlacementType.DAYCARE,
                            serviceNeed = snDaycarePartDay25.toFeeDecisionServiceNeed(),
                            baseFee = 28900,
                            siblingDiscount = 50,
                            fee = 8700,
                        ),
                    ),
            )
        db.transaction { tx -> tx.upsertFeeDecisions(listOf(newDraft)) }

        confirmDrafts(listOf(newDraft.id))
        asyncJobRunner.runPendingJobsSync(RealEvakaClock(), 2)

        val decisions =
            searchDecisions(
                    SearchFeeDecisionRequest(page = 0, statuses = listOf(FeeDecisionStatus.SENT))
                )
                .data

        assertEquals(3, decisions.size)
        decisions
            .find { it.id == sentDecisions.first().id }
            .let {
                assertNotNull(it)
                assertEquals(newDraft.validFrom.minusDays(1), it.validDuring.end)
            }
        decisions
            .find { it.id == sentDecisions.last().id }
            .let {
                assertNotNull(it)
                assertEquals(newDraft.validFrom.minusDays(1), it.validDuring.end)
            }
        assertNotNull(decisions.find { it.id == newDraft.id })
    }

    @Test
    fun `date range picker does not return anything with range before first decision`() {
        val now = LocalDate.now()
        val oldDecision =
            testDecisions[0].copy(
                validDuring = FiniteDateRange(now.minusMonths(2), now.minusMonths(1))
            )
        val futureDecision =
            testDecisions[1].copy(
                validDuring = FiniteDateRange(now.plusMonths(1), now.plusMonths(2))
            )

        db.transaction { tx -> tx.upsertFeeDecisions(listOf(oldDecision, futureDecision)) }

        val result =
            searchDecisions(
                SearchFeeDecisionRequest(
                    page = 0,
                    startDate = LocalDate.of(1900, 1, 1),
                    endDate = LocalDate.of(1901, 1, 1),
                )
            )

        assertEqualEnough(listOf(), result.data)
    }

    @Test
    fun `date range picker finds only one decision in the past`() {
        val now = LocalDate.now()
        val oldDecision =
            testDecisions[0].copy(
                validDuring = FiniteDateRange(now.minusMonths(2), now.minusMonths(1))
            )
        val futureDecision =
            testDecisions[1].copy(
                validDuring = FiniteDateRange(now.plusMonths(1), now.plusMonths(2))
            )

        db.transaction { tx -> tx.upsertFeeDecisions(listOf(oldDecision, futureDecision)) }

        val result =
            searchDecisions(
                SearchFeeDecisionRequest(
                    page = 0,
                    startDate = now.minusMonths(3),
                    endDate = now.minusDays(1),
                )
            )
        assertEqualEnough(listOf(toSummary(oldDecision)), result.data)
    }

    @Test
    fun `date range picker finds only one decision in the future`() {
        val now = LocalDate.now()
        val oldDecision =
            testDecisions[0].copy(
                validDuring = FiniteDateRange(now.minusMonths(2), now.minusMonths(1))
            )
        val futureDecision =
            testDecisions[1].copy(
                validDuring = FiniteDateRange(now.plusMonths(1), now.plusMonths(2))
            )

        db.transaction { tx -> tx.upsertFeeDecisions(listOf(oldDecision, futureDecision)) }

        val result =
            searchDecisions(
                SearchFeeDecisionRequest(
                    page = 0,
                    startDate = now.plusDays(1),
                    endDate = now.plusMonths(8),
                )
            )

        assertEqualEnough(listOf(toSummary(futureDecision)), result.data)
    }

    @Test
    fun `date range picker finds a decisions with exact start date or end date`() {
        val now = LocalDate.now()
        val oldDecision =
            testDecisions[0].copy(
                validDuring = FiniteDateRange(now.minusMonths(2), now.minusMonths(1))
            )
        val futureDecision =
            testDecisions[1].copy(
                validDuring = FiniteDateRange(now.plusMonths(1), now.plusMonths(2))
            )

        db.transaction { tx -> tx.upsertFeeDecisions(listOf(oldDecision, futureDecision)) }

        val resultPast =
            searchDecisions(
                SearchFeeDecisionRequest(
                    page = 0,
                    startDate = now.minusMonths(6),
                    endDate = now.minusMonths(1),
                )
            )

        val resultFuture =
            searchDecisions(
                SearchFeeDecisionRequest(
                    page = 0,
                    startDate = now.plusMonths(2),
                    endDate = now.plusMonths(8),
                )
            )

        assertEqualEnough(listOf(toSummary(oldDecision)), resultPast.data)

        assertEqualEnough(listOf(toSummary(futureDecision)), resultFuture.data)
    }

    @Test
    fun `date range picker finds all decisions without specifying a start date and having end date in the future`() {
        val now = LocalDate.now()
        val oldDecision =
            testDecisions[0].copy(
                validDuring = FiniteDateRange(now.minusMonths(2), now.minusMonths(1))
            )
        val futureDecision =
            testDecisions[1].copy(
                validDuring = FiniteDateRange(now.plusMonths(1), now.plusMonths(2))
            )

        db.transaction { tx -> tx.upsertFeeDecisions(listOf(oldDecision, futureDecision)) }

        val result = searchDecisions(SearchFeeDecisionRequest(page = 0, endDate = now.plusYears(8)))

        assertEqualEnough(listOf(toSummary(oldDecision), toSummary(futureDecision)), result.data)
    }

    @Test
    fun `date range picker finds all decisions without specifying a end date and having start date in the past`() {
        val now = LocalDate.now()
        val oldDecision =
            testDecisions[0].copy(
                validDuring = FiniteDateRange(now.minusMonths(2), now.minusMonths(1))
            )
        val futureDecision =
            testDecisions[1].copy(
                validDuring = FiniteDateRange(now.plusMonths(1), now.plusMonths(2))
            )

        db.transaction { tx -> tx.upsertFeeDecisions(listOf(oldDecision, futureDecision)) }

        val result =
            searchDecisions(SearchFeeDecisionRequest(page = 0, startDate = now.minusYears(8)))

        assertEqualEnough(listOf(toSummary(oldDecision), toSummary(futureDecision)), result.data)
    }

    @Test
    fun `set type updates decision type`() {
        db.transaction { tx -> tx.upsertFeeDecisions(testDecisions) }
        val draft = testDecisions.find { it.decisionType == FeeDecisionType.NORMAL }!!

        setDecisionType(draft.id, FeeDecisionTypeRequest(type = FeeDecisionType.RELIEF_ACCEPTED))

        val modified = draft.copy(decisionType = FeeDecisionType.RELIEF_ACCEPTED)

        val result = getDecision(draft.id)
        assertEqualEnough(toDetailed(modified), result)
    }

    @Test
    fun `sorting works with different params`() {
        db.transaction { tx -> tx.upsertFeeDecisions(testDecisions) }

        val statusAsc =
            searchDecisions(
                SearchFeeDecisionRequest(
                    page = 0,
                    sortBy = FeeDecisionSortParam.STATUS,
                    sortDirection = SortDirection.ASC,
                )
            )

        assertEqualEnough(
            testDecisions
                .map(::toSummary)
                .sortedBy { it.id.toString() }
                .sortedBy { it.status.name },
            statusAsc.data,
        )

        val statusDesc =
            searchDecisions(
                SearchFeeDecisionRequest(
                    page = 0,
                    sortBy = FeeDecisionSortParam.STATUS,
                    sortDirection = SortDirection.DESC,
                )
            )

        assertEqualEnough(
            testDecisions
                .map(::toSummary)
                .sortedBy { it.id.toString() }
                .sortedByDescending { it.status.name },
            statusDesc.data,
        )

        val headAsc =
            searchDecisions(
                SearchFeeDecisionRequest(
                    page = 0,
                    sortBy = FeeDecisionSortParam.HEAD_OF_FAMILY,
                    sortDirection = SortDirection.ASC,
                )
            )

        assertEqualEnough(
            testDecisions
                .map(::toSummary)
                .sortedBy { it.id.toString() }
                .sortedBy { it.headOfFamily.lastName },
            headAsc.data,
        )

        val headDesc =
            searchDecisions(
                SearchFeeDecisionRequest(
                    page = 0,
                    sortBy = FeeDecisionSortParam.HEAD_OF_FAMILY,
                    sortDirection = SortDirection.DESC,
                )
            )

        assertEqualEnough(
            testDecisions
                .map(::toSummary)
                .sortedBy { it.id.toString() }
                .sortedByDescending { it.headOfFamily.lastName },
            headDesc.data,
        )

        val validityAsc =
            searchDecisions(
                SearchFeeDecisionRequest(
                    page = 0,
                    sortBy = FeeDecisionSortParam.VALIDITY,
                    sortDirection = SortDirection.ASC,
                )
            )

        assertEqualEnough(
            testDecisions
                .map(::toSummary)
                .sortedBy { it.id.toString() }
                .sortedBy { it.validDuring.start },
            validityAsc.data,
        )

        val validityDesc =
            searchDecisions(
                SearchFeeDecisionRequest(
                    page = 0,
                    sortBy = FeeDecisionSortParam.VALIDITY,
                    sortDirection = SortDirection.DESC,
                )
            )

        assertEqualEnough(
            testDecisions
                .map(::toSummary)
                .sortedBy { it.id.toString() }
                .sortedByDescending { it.validDuring.start },
            validityDesc.data,
        )
    }

    @Test
    fun `fee decisions can be forced to be sent manually`() {
        val decision = testDecisions.first()

        db.transaction { tx -> tx.upsertFeeDecisions(listOf(decision)) }

        val beforeForcing = getDecision(decision.id)
        assertEquals(false, beforeForcing.requiresManualSending)

        db.transaction { tx ->
            tx.execute {
                sql(
                    "UPDATE person SET force_manual_fee_decisions = true WHERE id = ${bind(decision.headOfFamilyId)}"
                )
            }
        }

        confirmDrafts(listOf(decision.id))

        asyncJobRunner.runPendingJobsSync(RealEvakaClock(), 2)

        val confirmed = getDecision(decision.id)
        assertEquals(true, confirmed.requiresManualSending)
        assertEquals(FeeDecisionStatus.WAITING_FOR_MANUAL_SENDING, confirmed.status)
    }

    @Test
    fun `Fee decision indicates elementary family for a family with two partners and 2 common children`() {
        db.transaction {
            it.insertGuardian(testAdult_1.id, testChild_1.id)
            it.insertGuardian(testAdult_1.id, testChild_2.id)
            it.insertGuardian(testAdult_2.id, testChild_1.id)
            it.insertGuardian(testAdult_2.id, testChild_2.id)
        }

        val decision =
            createFeeDecisionsForFamily(testAdult_1, testAdult_2, listOf(testChild_1, testChild_2))

        db.transaction { tx -> tx.upsertFeeDecisions(listOf(decision)) }

        val decisionForFamily = getDecision(decision.id)
        assertEquals(true, decisionForFamily.partnerIsCodebtor)
    }

    @Test
    fun `Fee decision indicates elementary family for a family with two partners and one common child and one child not in fee decision`() {
        db.transaction {
            it.insertGuardian(testAdult_1.id, testChild_1.id)
            it.insertGuardian(testAdult_2.id, testChild_1.id)

            // Adult 2 has a custodian in another family
            it.insertGuardian(testAdult_2.id, testChild_2.id)
        }

        val decision = createFeeDecisionsForFamily(testAdult_1, testAdult_2, listOf(testChild_1))

        db.transaction { tx -> tx.upsertFeeDecisions(listOf(decision)) }

        val decisionForFamily = getDecision(decision.id)
        assertEquals(true, decisionForFamily.partnerIsCodebtor)
    }

    @Test
    fun `Fee decision indicates elementary family for a family with two partners and one common child and one not common child`() {
        db.transaction {
            it.insertGuardian(testAdult_1.id, testChild_1.id)
            it.insertGuardian(testAdult_1.id, testChild_2.id)

            // Adult 2 is the parent of child 2 -> an elementary family
            it.insertGuardian(testAdult_2.id, testChild_2.id)
        }

        val decision =
            createFeeDecisionsForFamily(testAdult_1, testAdult_2, listOf(testChild_1, testChild_2))

        db.transaction { tx -> tx.upsertFeeDecisions(listOf(decision)) }

        val decisionForFamily = getDecision(decision.id)
        assertEquals(true, decisionForFamily.partnerIsCodebtor)
    }

    @Test
    fun `Fee decision indicates elementary family for a family with two partners and partner has no dependants child`() {
        db.transaction {
            it.insertGuardian(testAdult_1.id, testChild_1.id)
            it.insertGuardian(testAdult_1.id, testChild_2.id)
            // Adult 2 is not a guardian of either child -> not an elementary family
        }

        val decision =
            createFeeDecisionsForFamily(testAdult_1, testAdult_2, listOf(testChild_1, testChild_2))

        db.transaction { tx -> tx.upsertFeeDecisions(listOf(decision)) }

        val decisionForFamily = getDecision(decision.id)
        assertEquals(false, decisionForFamily.partnerIsCodebtor)
    }

    @Test
    fun `PDF can be downloaded`() {
        db.transaction {
            it.insertGuardian(testAdult_1.id, testChild_1.id)
            it.insertGuardian(testAdult_1.id, testChild_2.id)
            it.insertGuardian(testAdult_2.id, testChild_1.id)
            it.insertGuardian(testAdult_2.id, testChild_2.id)
        }
        val decision =
            createAndConfirmFeeDecisionsForFamily(
                testAdult_1,
                testAdult_2,
                listOf(testChild_1, testChild_2),
            )

        getPdf(decision.id, user)
    }

    @Test
    fun `Legacy PDF can be downloaded`() {
        db.transaction {
            it.insertGuardian(testAdult_1.id, testChild_1.id)
            it.insertGuardian(testAdult_1.id, testChild_2.id)
            it.insertGuardian(testAdult_2.id, testChild_1.id)
            it.insertGuardian(testAdult_2.id, testChild_2.id)
        }
        val decision =
            createAndConfirmFeeDecisionsForFamily(
                testAdult_1,
                testAdult_2,
                listOf(testChild_1, testChild_2),
                legacyPdfWithContactInfo = true,
            )

        getPdf(decision.id, user)
    }

    @Test
    fun `Legacy PDF can not be downloaded if head of family has restricted details`() {
        // testAdult_7 has restricted details on
        db.transaction {
            it.insertGuardian(testAdult_7.id, testChild_1.id)
            it.insertGuardian(testAdult_7.id, testChild_2.id)
            it.insertGuardian(testAdult_2.id, testChild_1.id)
            it.insertGuardian(testAdult_2.id, testChild_2.id)
        }
        val decision =
            createAndConfirmFeeDecisionsForFamily(
                testAdult_7,
                testAdult_2,
                listOf(testChild_1, testChild_2),
                legacyPdfWithContactInfo = true,
            )

        assertThrows<Forbidden> { getPdf(decision.id, user) }

        // Check that message is still sent via sfi
        asyncJobRunner.runPendingJobsSync(MockEvakaClock(HelsinkiDateTime.now()))
        assertEquals(1, MockSfiMessagesClient.getMessages().size)
    }

    @Test
    fun `Legacy PDF can not be downloaded if a child has restricted details`() {
        val testChildRestricted =
            testChild_1.copy(
                id = PersonId(UUID.randomUUID()),
                ssn = "010617A125W",
                restrictedDetailsEnabled = true,
            )
        db.transaction {
            it.insert(testChildRestricted, DevPersonType.RAW_ROW)
            it.insertGuardian(testAdult_1.id, testChildRestricted.id)
            it.insertGuardian(testAdult_1.id, testChild_2.id)
            it.insertGuardian(testAdult_2.id, testChildRestricted.id)
            it.insertGuardian(testAdult_2.id, testChild_2.id)
        }
        val decision =
            createAndConfirmFeeDecisionsForFamily(
                testAdult_1,
                testAdult_2,
                listOf(testChildRestricted, testChild_2),
                legacyPdfWithContactInfo = true,
            )

        assertThrows<Forbidden> { getPdf(decision.id, user) }

        asyncJobRunner.runPendingJobsSync(MockEvakaClock(HelsinkiDateTime.now()))
        assertEquals(1, MockSfiMessagesClient.getMessages().size)
    }

    @Test
    fun `PDF without contact info can be downloaded even if head of family has restricted details`() {
        // testAdult_7 has restricted details on
        db.transaction {
            it.insertGuardian(testAdult_7.id, testChild_1.id)
            it.insertGuardian(testAdult_7.id, testChild_2.id)
            it.insertGuardian(testAdult_2.id, testChild_1.id)
            it.insertGuardian(testAdult_2.id, testChild_2.id)
        }
        val decision =
            createAndConfirmFeeDecisionsForFamily(
                testAdult_7,
                testAdult_2,
                listOf(testChild_1, testChild_2),
                legacyPdfWithContactInfo = false,
            )

        getPdf(decision.id, user)
    }

    @Test
    fun `PDF without contact info can be downloaded even if a child has restricted details`() {
        val testChildRestricted =
            testChild_1.copy(
                id = PersonId(UUID.randomUUID()),
                ssn = "010617A125W",
                restrictedDetailsEnabled = true,
            )
        db.transaction {
            it.insert(testChildRestricted, DevPersonType.RAW_ROW)
            it.insertGuardian(testAdult_1.id, testChildRestricted.id)
            it.insertGuardian(testAdult_1.id, testChild_2.id)
            it.insertGuardian(testAdult_2.id, testChildRestricted.id)
            it.insertGuardian(testAdult_2.id, testChild_2.id)
        }
        val decision =
            createAndConfirmFeeDecisionsForFamily(
                testAdult_1,
                testAdult_2,
                listOf(testChildRestricted, testChild_2),
                legacyPdfWithContactInfo = false,
            )

        getPdf(decision.id, user)
    }

    @Test
    fun `Legacy PDF can be downloaded by admin even if someone in the family has restricted details`() {
        // testAdult_7 has restricted details on
        db.transaction {
            it.insertGuardian(testAdult_7.id, testChild_1.id)
            it.insertGuardian(testAdult_7.id, testChild_2.id)
            it.insertGuardian(testAdult_2.id, testChild_1.id)
            it.insertGuardian(testAdult_2.id, testChild_2.id)
        }
        val decision =
            createAndConfirmFeeDecisionsForFamily(
                testAdult_7,
                testAdult_2,
                listOf(testChild_1, testChild_2),
                legacyPdfWithContactInfo = true,
            )

        getPdf(decision.id, adminUser)
    }

    @Test
    fun `Email notification is sent to hof when decision in WAITING_FOR_SENDING is set to SENT`() {
        // optInAdult has an email address, and does not require manual sending of PDF decision
        val optInAdult =
            testAdult_6.copy(
                id = PersonId(UUID.randomUUID()),
                email = "optin@test.com",
                forceManualFeeDecisions = false,
                ssn = "291090-9986",
            )
        db.transaction {
            it.insert(optInAdult, DevPersonType.RAW_ROW)
            it.insert(
                DevParentship(
                    ParentshipId(UUID.randomUUID()),
                    testChild_2.id,
                    optInAdult.id,
                    testChild_2.dateOfBirth,
                    testChild_2.dateOfBirth.plusYears(18).minusDays(1),
                    HelsinkiDateTime.now(),
                )
            )
            it.insertTestPartnership(adult1 = optInAdult.id, adult2 = testAdult_7.id)
        }
        createAndConfirmFeeDecisionsForFamily(optInAdult, testAdult_7, listOf(testChild_2))

        asyncJobRunner.runPendingJobsSync(RealEvakaClock())

        val emailContent =
            emailMessageProvider.financeDecisionNotification(FinanceDecisionType.FEE_DECISION)

        // assert that only hof has mail
        assertEquals(
            setOfNotNull(optInAdult.email),
            MockEmailClient.emails.map { it.toAddress }.toSet(),
        )
        assertEquals(emailContent.subject, getEmailFor(optInAdult).content.subject)
        assertEquals(
            "${emailEnv.senderNameFi} <${emailEnv.senderAddress}>",
            getEmailFor(optInAdult).fromAddress.address,
        )
    }

    @Test
    fun `Email notification is sent to hof when decision in WAITING_FOR_MANUAL_SENDING is set to SENT`() {
        db.transaction {
            // testAdult_3 has an email address, but no mail address -> marked for manual sending
            it.insert(
                DevParentship(
                    ParentshipId(UUID.randomUUID()),
                    testChild_2.id,
                    testAdult_3.id,
                    testChild_2.dateOfBirth,
                    testChild_2.dateOfBirth.plusYears(18).minusDays(1),
                    HelsinkiDateTime.now(),
                )
            )
            it.insertTestPartnership(adult1 = testAdult_3.id, adult2 = testAdult_4.id)
        }
        val feeDecision =
            createAndConfirmFeeDecisionsForFamily(testAdult_3, testAdult_4, listOf(testChild_2))

        asyncJobRunner.runPendingJobsSync(RealEvakaClock())

        // assert that nothing sent yet
        assertEquals(emptySet(), MockEmailClient.emails.map { it.toAddress }.toSet())

        // mark waiting as sent
        feeDecisionController.setFeeDecisionSent(
            dbInstance(),
            user,
            RealEvakaClock(),
            listOf(feeDecision.id),
        )
        asyncJobRunner.runPendingJobsSync(RealEvakaClock())

        val emailContent =
            emailMessageProvider.financeDecisionNotification(FinanceDecisionType.FEE_DECISION)

        // assert that only hof has mail
        assertEquals(
            setOfNotNull(testAdult_3.email),
            MockEmailClient.emails.map { it.toAddress }.toSet(),
        )
        assertEquals(emailContent.subject, getEmailFor(testAdult_3).content.subject)
        assertEquals(
            "${emailEnv.senderNameFi} <${emailEnv.senderAddress}>",
            getEmailFor(testAdult_3).fromAddress.address,
        )
    }

    @Test
    fun `Email notification is not sent to hof when opted out of decision emails`() {
        // optOutAdult is eligible, but has elected to not receive decision emails
        val optOutAdult =
            testAdult_6.copy(
                id = PersonId(UUID.randomUUID()),
                ssn = "291090-9986",
                email = "optout@test.com",
                forceManualFeeDecisions = false,
                disabledEmailTypes = setOf(EmailMessageType.DECISION_NOTIFICATION),
            )
        db.transaction {
            it.insert(optOutAdult, DevPersonType.RAW_ROW)
            it.insert(
                DevParentship(
                    ParentshipId(UUID.randomUUID()),
                    testChild_2.id,
                    optOutAdult.id,
                    testChild_2.dateOfBirth,
                    testChild_2.dateOfBirth.plusYears(18).minusDays(1),
                    HelsinkiDateTime.now(),
                )
            )
            it.insertTestPartnership(adult1 = optOutAdult.id, adult2 = testAdult_7.id)
        }
        createAndConfirmFeeDecisionsForFamily(optOutAdult, testAdult_7, listOf(testChild_2))

        asyncJobRunner.runPendingJobsSync(RealEvakaClock())

        // assert that no mail for anyone
        assertEquals(emptySet(), MockEmailClient.emails.map { it.toAddress }.toSet())
    }

    @Test
    fun `Sfi message event is stored correctly`() {

        // testAdult_7 has restricted details on
        db.transaction {
            it.insertGuardian(testAdult_7.id, testChild_1.id)
            it.insertGuardian(testAdult_7.id, testChild_2.id)
            it.insertGuardian(testAdult_2.id, testChild_1.id)
            it.insertGuardian(testAdult_2.id, testChild_2.id)
        }
        val decision =
            createAndConfirmFeeDecisionsForFamily(
                testAdult_7,
                testAdult_2,
                listOf(testChild_1, testChild_2),
                legacyPdfWithContactInfo = true,
            )

        assertThrows<Forbidden> { getPdf(decision.id, user) }

        // Check that message is still sent via sfi
        asyncJobRunner.runPendingJobsSync(MockEvakaClock(HelsinkiDateTime.now()))

        val messageId = MockSfiMessagesClient.getMessages().first().messageId

        db.read { assertEquals(0, it.getSfiGetEventsContinuationTokens().size) }

        sfiAsyncJobs.getEvents(db, MockEvakaClock(HelsinkiDateTime.now()))

        db.read {
            val processedEvents = it.getSfiMessageEventsByMessageId(messageId)
            assertEquals(1, processedEvents.size)
            assertEquals(EventType.ELECTRONIC_MESSAGE_CREATED, processedEvents[0].eventType)

            assertEquals(1, it.getSfiGetEventsContinuationTokens().size)
        }
    }

    private fun getPdf(id: FeeDecisionId, user: AuthenticatedUser.Employee) {
        feeDecisionController.getFeeDecisionPdf(dbInstance(), user, RealEvakaClock(), id)
    }

    private fun createAndConfirmFeeDecisionsForFamily(
        headOfFamily: DevPerson,
        partner: DevPerson?,
        familyChildren: List<DevPerson>,
        legacyPdfWithContactInfo: Boolean = false,
    ): FeeDecision {
        val decision = createFeeDecisionsForFamily(headOfFamily, partner, familyChildren)
        db.transaction { tx ->
            tx.upsertFeeDecisions(listOf(decision))
            if (legacyPdfWithContactInfo) {
                tx.execute {
                    sql(
                        """
                    UPDATE fee_decision SET document_contains_contact_info = TRUE
                    WHERE id = ${bind(decision.id)}
                """
                    )
                }
            }
        }

        confirmDrafts(listOf(decision.id))

        asyncJobRunner.runPendingJobsSync(RealEvakaClock())
        return decision
    }

    private fun createFeeDecisionsForFamily(
        headOfFamily: DevPerson,
        partner: DevPerson?,
        familyChildren: List<DevPerson>,
    ): FeeDecision {
        return createFeeDecisionFixture(
            status = FeeDecisionStatus.DRAFT,
            decisionType = FeeDecisionType.NORMAL,
            headOfFamilyId = headOfFamily.id,
            period = FiniteDateRange(LocalDate.of(2018, 5, 1), LocalDate.of(2018, 5, 31)),
            children =
                familyChildren.map {
                    createFeeDecisionChildFixture(
                        childId = it.id,
                        dateOfBirth = it.dateOfBirth,
                        placementUnitId = daycare1.id,
                        placementType = PlacementType.DAYCARE,
                        serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                        siblingDiscount = 50,
                        fee = 14500,
                    )
                },
            partnerId = partner?.id,
        )
    }

    private fun searchDecisions(body: SearchFeeDecisionRequest): PagedFeeDecisionSummaries {
        return feeDecisionController.searchFeeDecisions(dbInstance(), user, RealEvakaClock(), body)
    }

    private fun getDecision(id: FeeDecisionId): FeeDecisionDetailed {
        return feeDecisionController.getFeeDecision(dbInstance(), user, RealEvakaClock(), id).data
    }

    private fun getHeadOfFamilyDecisions(id: PersonId): List<FeeDecision> {
        return feeDecisionController.getHeadOfFamilyFeeDecisions(
            dbInstance(),
            user,
            RealEvakaClock(),
            id,
        )
    }

    private fun confirmDrafts(
        decisionIds: List<FeeDecisionId>,
        now: EvakaClock = RealEvakaClock(),
    ) {
        feeDecisionController.confirmFeeDecisionDrafts(dbInstance(), user, now, decisionIds, null)
    }

    private fun setDecisionType(id: FeeDecisionId, body: FeeDecisionTypeRequest) {
        feeDecisionController.setFeeDecisionType(dbInstance(), user, RealEvakaClock(), id, body)
    }

    private fun getEmailFor(person: DevPerson): Email {
        val address = person.email ?: throw Error("$person has no email")
        return MockEmailClient.getEmail(address) ?: throw Error("No emails sent to $address")
    }

    private fun toDetailed(feeDecision: FeeDecision): FeeDecisionDetailed =
        FeeDecisionDetailed(
            id = feeDecision.id,
            status = feeDecision.status,
            decisionNumber = feeDecision.decisionNumber,
            decisionType = feeDecision.decisionType,
            validDuring = feeDecision.validDuring,
            headOfFamily =
                allAdults.find { it.id == feeDecision.headOfFamilyId }!!.toPersonDetailed(),
            partner = allAdults.find { it.id == feeDecision.partnerId }?.toPersonDetailed(),
            headOfFamilyIncome = feeDecision.headOfFamilyIncome,
            partnerIncome = feeDecision.partnerIncome,
            familySize = feeDecision.familySize,
            feeThresholds = feeDecision.feeThresholds,
            children =
                feeDecision.children.map { child ->
                    FeeDecisionChildDetailed(
                        child = allChildren.find { it.id == child.child.id }!!.toPersonDetailed(),
                        placementType = child.placement.type,
                        placementUnit =
                            allDaycares.find { it.id == child.placement.unitId }!!.toUnitData(),
                        serviceNeedOptionId = child.serviceNeed.optionId,
                        serviceNeedFeeCoefficient = child.serviceNeed.feeCoefficient,
                        serviceNeedDescriptionFi = child.serviceNeed.descriptionFi,
                        serviceNeedDescriptionSv = child.serviceNeed.descriptionSv,
                        serviceNeedMissing = child.serviceNeed.missing,
                        baseFee = child.baseFee,
                        siblingDiscount = child.siblingDiscount,
                        fee = child.fee,
                        feeAlterations = child.feeAlterations,
                        finalFee = child.finalFee,
                        childIncome = child.childIncome,
                    )
                },
            documentKey = feeDecision.documentKey,
            approvedBy =
                allWorkers.find { it.id == feeDecision.approvedById }?.toEmployeeWithName(),
            approvedAt = feeDecision.approvedAt,
            financeDecisionHandlerFirstName =
                allWorkers.find { it.id == feeDecision.decisionHandlerId }?.firstName,
            financeDecisionHandlerLastName =
                allWorkers.find { it.id == feeDecision.decisionHandlerId }?.lastName,
            created = feeDecision.created,
            documentContainsContactInfo = false,
            archivedAt = null,
        )

    private fun toSummary(feeDecision: FeeDecision): FeeDecisionSummary =
        FeeDecisionSummary(
            id = feeDecision.id,
            status = feeDecision.status,
            decisionNumber = feeDecision.decisionNumber,
            validDuring = feeDecision.validDuring,
            headOfFamily = allAdults.find { it.id == feeDecision.headOfFamilyId }!!.toPersonBasic(),
            children =
                feeDecision.children.map { child ->
                    allChildren.find { it.id == child.child.id }!!.toPersonBasic()
                },
            approvedAt = feeDecision.approvedAt,
            finalPrice = feeDecision.children.fold(0) { sum, child -> sum + child.finalFee },
            created = feeDecision.created,
            difference = feeDecision.difference,
        )

    private fun DevDaycare.toUnitData() =
        UnitData(
            id = this.id,
            name = this.name,
            areaId = this.areaId,
            areaName = allAreas.find { it.id == this.areaId }?.name ?: "",
            language = this.language.name,
        )
}
