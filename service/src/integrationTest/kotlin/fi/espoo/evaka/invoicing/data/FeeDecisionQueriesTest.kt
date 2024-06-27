// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.data

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.invoicing.controller.DistinctiveParams
import fi.espoo.evaka.invoicing.controller.FeeDecisionSortParam
import fi.espoo.evaka.invoicing.controller.SortDirection
import fi.espoo.evaka.invoicing.createFeeDecisionChildFixture
import fi.espoo.evaka.invoicing.createFeeDecisionFixture
import fi.espoo.evaka.invoicing.domain.DecisionIncome
import fi.espoo.evaka.invoicing.domain.FeeDecisionDetailed
import fi.espoo.evaka.invoicing.domain.FeeDecisionDifference
import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
import fi.espoo.evaka.invoicing.domain.FeeDecisionSummary
import fi.espoo.evaka.invoicing.domain.FeeDecisionType
import fi.espoo.evaka.invoicing.domain.IncomeEffect
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.FeeDecisionId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.config.defaultJsonMapperBuilder
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.domain.RealEvakaClock
import fi.espoo.evaka.snDaycareFullDay35
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testAdult_2
import fi.espoo.evaka.testAdult_3
import fi.espoo.evaka.testAdult_4
import fi.espoo.evaka.testAdult_5
import fi.espoo.evaka.testAdult_6
import fi.espoo.evaka.testAdult_7
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testArea2
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testChild_3
import fi.espoo.evaka.testChild_4
import fi.espoo.evaka.testChild_5
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
import fi.espoo.evaka.testDecisionMaker_1
import fi.espoo.evaka.testDecisionMaker_2
import fi.espoo.evaka.toFeeDecisionServiceNeed
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FeeDecisionQueriesTest : PureJdbiTest(resetDbBeforeEach = true) {
    val jsonMapper = defaultJsonMapperBuilder().build()

    private val testPeriod = DateRange(LocalDate.of(2019, 5, 1), LocalDate.of(2019, 5, 31))
    private val testPeriod2 = DateRange(LocalDate.of(2019, 5, 15), LocalDate.of(2019, 5, 31))
    private val testDecisions =
        listOf(
            createFeeDecisionFixture(
                status = FeeDecisionStatus.DRAFT,
                decisionType = FeeDecisionType.NORMAL,
                headOfFamilyId = testAdult_1.id,
                period = testPeriod2,
                children =
                    listOf(
                        createFeeDecisionChildFixture(
                            childId = testChild_1.id,
                            dateOfBirth = testChild_1.dateOfBirth,
                            placementUnitId = testDaycare.id,
                            placementType = PlacementType.DAYCARE,
                            serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed()
                        ),
                        createFeeDecisionChildFixture(
                            childId = testChild_2.id,
                            dateOfBirth = testChild_2.dateOfBirth,
                            placementUnitId = testDaycare.id,
                            placementType = PlacementType.DAYCARE,
                            serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                            siblingDiscount = 50,
                            fee = 145
                        )
                    )
            ),
            createFeeDecisionFixture(
                status = FeeDecisionStatus.SENT,
                decisionType = FeeDecisionType.NORMAL,
                headOfFamilyId = testAdult_1.id,
                period = testPeriod,
                children =
                    listOf(
                        createFeeDecisionChildFixture(
                            childId = testChild_2.id,
                            dateOfBirth = testChild_2.dateOfBirth,
                            placementUnitId = testDaycare.id,
                            placementType = PlacementType.DAYCARE,
                            serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed()
                        )
                    )
            ),
            createFeeDecisionFixture(
                status = FeeDecisionStatus.SENT,
                decisionType = FeeDecisionType.NORMAL,
                headOfFamilyId = testAdult_2.id,
                period = testPeriod,
                children =
                    listOf(
                        createFeeDecisionChildFixture(
                            childId = testChild_1.id,
                            dateOfBirth = testChild_1.dateOfBirth,
                            placementUnitId = testDaycare.id,
                            placementType = PlacementType.DAYCARE,
                            serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed()
                        )
                    )
            )
        )

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insertGeneralTestFixtures()
            tx.insert(testDecisionMaker_1)
            tx.insert(testDecisionMaker_2)
            tx.insert(testArea)
            tx.insert(testDaycare)
            tx.insert(testArea2)
            tx.insert(testDaycare2.copy(financeDecisionHandler = testDecisionMaker_2.id))
            listOf(
                    testAdult_1,
                    testAdult_2,
                    testAdult_3,
                    testAdult_4,
                    testAdult_5,
                    testAdult_6,
                    testAdult_7
                )
                .forEach { tx.insert(it, DevPersonType.ADULT) }
            listOf(testChild_1, testChild_2, testChild_3, testChild_4, testChild_5).forEach {
                tx.insert(it, DevPersonType.CHILD)
            }
            tx.insert(snDaycareFullDay35)
        }
    }

    @Test
    fun `activateDrafts with one decision`() {
        db.transaction { tx ->
            val draft = testDecisions.find { it.status == FeeDecisionStatus.DRAFT }!!
            tx.upsertFeeDecisions(listOf(draft))
            tx.approveFeeDecisionDraftsForSending(
                listOf(draft.id),
                testDecisionMaker_1.id,
                approvedAt = HelsinkiDateTime.now(),
                null,
                false,
                false
            )

            val result = tx.getFeeDecision(testDecisions[0].id)!!
            assertEquals(FeeDecisionStatus.WAITING_FOR_SENDING, result.status)
            assertEquals(1L, result.decisionNumber)
            assertEquals(testDecisionMaker_1.id, result.approvedBy?.id)
            assertNotNull(result.approvedAt)
        }
    }

    @Test
    fun `activateDrafts sets current user as approver for retroactive decision`() {
        val result = createAndApproveFeeDecisionForSending(false, testDaycare)
        assertEquals(FeeDecisionStatus.WAITING_FOR_SENDING, result.status)
        assertEquals(1L, result.decisionNumber)
        assertEquals(testDecisionMaker_1.id, result.approvedBy?.id)
        assertNotNull(result.approvedAt)
        assertEquals(
            "${testDecisionMaker_1.lastName} ${testDecisionMaker_1.firstName}",
            "${result.financeDecisionHandlerLastName} ${result.financeDecisionHandlerFirstName}"
        )
    }

    @Test
    fun `activateDrafts sets daycare handler as approver for retroactive decision if forced`() {
        val result = createAndApproveFeeDecisionForSending(true, testDaycare2)
        assertEquals(FeeDecisionStatus.WAITING_FOR_SENDING, result.status)
        assertEquals(1L, result.decisionNumber)
        assertEquals(testDecisionMaker_1.id, result.approvedBy?.id)
        assertNotNull(result.approvedAt)
        assertEquals(
            "${testDecisionMaker_2.lastName} ${testDecisionMaker_2.firstName}",
            "${result.financeDecisionHandlerLastName} ${result.financeDecisionHandlerFirstName}"
        )
    }

    private fun createAndApproveFeeDecisionForSending(
        forceUseDaycareHandler: Boolean,
        childDaycare: DevDaycare
    ): FeeDecisionDetailed {
        return db.transaction { tx ->
            val draft =
                createFeeDecisionFixture(
                    status = FeeDecisionStatus.DRAFT,
                    decisionType = FeeDecisionType.NORMAL,
                    headOfFamilyId = testAdult_1.id,
                    period = testPeriod,
                    children =
                        listOf(
                            createFeeDecisionChildFixture(
                                childId = testChild_1.id,
                                dateOfBirth = testChild_1.dateOfBirth,
                                placementUnitId = childDaycare.id,
                                placementType = PlacementType.DAYCARE,
                                serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed()
                            )
                        )
                )

            tx.upsertFeeDecisions(listOf(draft))
            tx.approveFeeDecisionDraftsForSending(
                listOf(draft.id),
                testDecisionMaker_1.id,
                approvedAt = HelsinkiDateTime.now(),
                null,
                true,
                forceUseDaycareHandler
            )

            tx.getFeeDecision(draft.id)!!
        }
    }

    @Test
    fun `approveDraftsForSending with multiple decisions`() {
        db.transaction { tx ->
            val decisions =
                listOf(
                    testDecisions[0],
                    testDecisions[0].copy(
                        id = FeeDecisionId(UUID.randomUUID()),
                        validDuring =
                            DateRange(
                                testPeriod.start.minusYears(1),
                                testPeriod.end!!.minusYears(1)
                            )
                    )
                )
            tx.upsertFeeDecisions(decisions)

            tx.approveFeeDecisionDraftsForSending(
                decisions.map { it.id },
                testDecisionMaker_1.id,
                approvedAt = HelsinkiDateTime.now(),
                null,
                false,
                false
            )

            val result =
                tx.getFeeDecisionsByIds(decisions.map { it.id }).sortedBy { it.decisionNumber }
            with(result[0]) {
                assertEquals(FeeDecisionStatus.WAITING_FOR_SENDING, status)
                assertEquals(1L, decisionNumber)
            }
            with(result[1]) {
                assertEquals(FeeDecisionStatus.WAITING_FOR_SENDING, status)
                assertEquals(2L, decisionNumber)
            }
        }
    }

    @Test
    fun `findFeeDecisionsForHeadOfFamily works`() {
        db.transaction { tx -> tx.upsertFeeDecisions(testDecisions) }

        val both = setOf(testDecisions[0].id, testDecisions[1].id)
        val sent = setOf(testDecisions[1].id)

        fun find(headOfFamilyId: PersonId, period: DateRange?, status: List<FeeDecisionStatus>?) =
            db.read { tx -> tx.findFeeDecisionsForHeadOfFamily(headOfFamilyId, period, status) }
                .map { it.id }
                .toSet()

        assertEquals(both, find(testAdult_1.id, null, null))

        // Filter by period
        assertEquals(
            both,
            find(
                testAdult_1.id,
                DateRange(LocalDate.of(2019, 5, 15), LocalDate.of(2019, 5, 15)),
                null
            )
        )

        assertEquals(
            sent,
            find(
                testAdult_1.id,
                DateRange(LocalDate.of(2019, 5, 1), LocalDate.of(2019, 5, 1)),
                null
            )
        )

        assertEquals(
            emptySet(),
            find(
                testAdult_1.id,
                DateRange(LocalDate.of(2015, 1, 1), LocalDate.of(2015, 1, 1)),
                null
            )
        )

        // Filter by status
        assertEquals(
            both,
            find(testAdult_1.id, null, listOf(FeeDecisionStatus.DRAFT, FeeDecisionStatus.SENT))
        )

        assertEquals(sent, find(testAdult_1.id, null, listOf(FeeDecisionStatus.SENT)))

        assertEquals(
            emptySet(),
            find(testAdult_1.id, null, listOf(FeeDecisionStatus.WAITING_FOR_MANUAL_SENDING))
        )

        // Filter by both period and status
        assertEquals(
            sent,
            find(
                testAdult_1.id,
                DateRange(LocalDate.of(2019, 5, 10), LocalDate.of(2019, 5, 15)),
                listOf(
                    FeeDecisionStatus.WAITING_FOR_SENDING,
                    FeeDecisionStatus.WAITING_FOR_MANUAL_SENDING,
                    FeeDecisionStatus.SENT
                )
            )
        )

        assertEquals(
            emptySet(),
            find(
                testAdult_1.id,
                DateRange(LocalDate.of(2018, 5, 10), LocalDate.of(2019, 5, 15)),
                listOf(FeeDecisionStatus.ANNULLED)
            )
        )
    }

    @Test
    fun `search ignores accent`() {
        db.transaction { tx ->
            tx.upsertFeeDecisions(
                listOf(
                    createFeeDecisionFixture(
                        status = FeeDecisionStatus.DRAFT,
                        decisionType = FeeDecisionType.NORMAL,
                        headOfFamilyId = testAdult_3.id,
                        period = testPeriod,
                        children =
                            listOf(
                                createFeeDecisionChildFixture(
                                    childId = testChild_5.id,
                                    dateOfBirth = testChild_5.dateOfBirth,
                                    placementUnitId = testDaycare.id,
                                    placementType = PlacementType.DAYCARE,
                                    serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed()
                                )
                            )
                    )
                )
            )
        }

        searchAndAssert("Viren", "Virén")
        searchAndAssert("Virén", "Virén")
        searchAndAssert("Visa Viren", "Virén")
        searchAndAssert("Visa Virén", "Virén")
    }

    @Test
    fun `search with max fee accepted`() {
        val decisions =
            db.transaction { tx ->
                val baseDecision = { child: DevPerson ->
                    createFeeDecisionFixture(
                        status = FeeDecisionStatus.DRAFT,
                        decisionType = FeeDecisionType.NORMAL,
                        period = testPeriod,
                        headOfFamilyId = PersonId(UUID.randomUUID()),
                        children =
                            listOf(
                                createFeeDecisionChildFixture(
                                    childId = child.id,
                                    dateOfBirth = child.dateOfBirth,
                                    placementUnitId = testDaycare.id,
                                    placementType = PlacementType.DAYCARE,
                                    serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed()
                                )
                            )
                    )
                }
                tx.upsertFeeDecisions(
                    listOf(
                        baseDecision(testChild_1)
                            .copy(headOfFamilyId = testAdult_1.id, headOfFamilyIncome = null),
                        baseDecision(testChild_2)
                            .copy(
                                headOfFamilyId = testAdult_2.id,
                                headOfFamilyIncome =
                                    DecisionIncome(
                                        effect = IncomeEffect.MAX_FEE_ACCEPTED,
                                        emptyMap(),
                                        totalIncome = 0,
                                        totalExpenses = 0,
                                        total = 0,
                                        worksAtECHA = false
                                    )
                            ),
                        baseDecision(testChild_3)
                            .copy(
                                headOfFamilyId = testAdult_3.id,
                                headOfFamilyIncome = null,
                                partnerId = testAdult_4.id,
                                partnerIncome =
                                    DecisionIncome(
                                        effect = IncomeEffect.MAX_FEE_ACCEPTED,
                                        emptyMap(),
                                        totalIncome = 0,
                                        totalExpenses = 0,
                                        total = 0,
                                        worksAtECHA = false
                                    )
                            ),
                        baseDecision(testChild_4)
                            .copy(
                                headOfFamilyId = testAdult_5.id,
                                headOfFamilyIncome =
                                    DecisionIncome(
                                        effect = IncomeEffect.INCOME,
                                        emptyMap(),
                                        totalIncome = 200000,
                                        totalExpenses = 0,
                                        total = 200000,
                                        worksAtECHA = false
                                    )
                            ),
                        baseDecision(testChild_5)
                            .copy(
                                headOfFamilyId = testAdult_6.id,
                                headOfFamilyIncome =
                                    DecisionIncome(
                                        effect = IncomeEffect.INCOME,
                                        emptyMap(),
                                        totalIncome = 200000,
                                        totalExpenses = 0,
                                        total = 200000,
                                        worksAtECHA = false
                                    ),
                                partnerId = testAdult_7.id,
                                partnerIncome =
                                    DecisionIncome(
                                        effect = IncomeEffect.MAX_FEE_ACCEPTED,
                                        emptyMap(),
                                        totalIncome = 0,
                                        totalExpenses = 0,
                                        total = 0,
                                        worksAtECHA = false
                                    )
                            )
                    )
                )
                val ids =
                    tx.createQuery { sql("SELECT id FROM fee_decision") }.toList<FeeDecisionId>()
                tx.getDetailedFeeDecisionsByIds(ids)
            }
        assertThat(decisions)
            .extracting(
                { it.headOfFamily.lastName },
                { it.headOfFamily.firstName },
                { it.incomeEffect }
            )
            .containsExactlyInAnyOrder(
                Tuple(testAdult_1.lastName, testAdult_1.firstName, IncomeEffect.NOT_AVAILABLE),
                Tuple(testAdult_2.lastName, testAdult_2.firstName, IncomeEffect.MAX_FEE_ACCEPTED),
                Tuple(testAdult_3.lastName, testAdult_3.firstName, IncomeEffect.MAX_FEE_ACCEPTED),
                Tuple(testAdult_5.lastName, testAdult_5.firstName, IncomeEffect.INCOME),
                Tuple(testAdult_6.lastName, testAdult_6.firstName, IncomeEffect.MAX_FEE_ACCEPTED)
            )

        val result =
            db.read { tx ->
                tx.searchFeeDecisions(
                    clock =
                        MockEvakaClock(HelsinkiDateTime.of(testPeriod.start, LocalTime.of(13, 37))),
                    postOffice = "ESPOO",
                    page = 0,
                    pageSize = 100,
                    sortBy = FeeDecisionSortParam.HEAD_OF_FAMILY,
                    sortDirection = SortDirection.ASC,
                    statuses = emptyList(),
                    areas = emptyList(),
                    unit = null,
                    distinctiveParams = listOf(DistinctiveParams.MAX_FEE_ACCEPTED),
                    startDate = null,
                    endDate = null,
                    financeDecisionHandlerId = null,
                    difference = emptySet()
                )
            }

        assertThat(result.data)
            .extracting({ it.headOfFamily.lastName }, { it.headOfFamily.firstName })
            .containsExactly(
                Tuple(testAdult_2.lastName, testAdult_2.firstName),
                Tuple(testAdult_3.lastName, testAdult_3.firstName),
                Tuple(testAdult_6.lastName, testAdult_6.firstName)
            )
    }

    @Test
    fun `search with difference`() {
        db.transaction { tx ->
            val baseDecision = { child: DevPerson ->
                createFeeDecisionFixture(
                    status = FeeDecisionStatus.DRAFT,
                    decisionType = FeeDecisionType.NORMAL,
                    period = testPeriod,
                    headOfFamilyId = PersonId(UUID.randomUUID()),
                    children =
                        listOf(
                            createFeeDecisionChildFixture(
                                childId = child.id,
                                dateOfBirth = child.dateOfBirth,
                                placementUnitId = testDaycare.id,
                                placementType = PlacementType.DAYCARE,
                                serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed()
                            )
                        )
                )
            }
            tx.upsertFeeDecisions(
                listOf(
                    baseDecision(testChild_1)
                        .copy(headOfFamilyId = testAdult_1.id, difference = emptySet()),
                    baseDecision(testChild_2)
                        .copy(
                            headOfFamilyId = testAdult_2.id,
                            difference = setOf(FeeDecisionDifference.INCOME)
                        ),
                    baseDecision(testChild_3)
                        .copy(
                            headOfFamilyId = testAdult_3.id,
                            difference =
                                setOf(
                                    FeeDecisionDifference.INCOME,
                                    FeeDecisionDifference.FAMILY_SIZE
                                )
                        ),
                    baseDecision(testChild_4)
                        .copy(
                            headOfFamilyId = testAdult_4.id,
                            difference = setOf(FeeDecisionDifference.FEE_ALTERATIONS)
                        )
                )
            )
        }

        val result =
            db.read { tx ->
                tx.searchFeeDecisions(
                    clock =
                        MockEvakaClock(HelsinkiDateTime.of(testPeriod.start, LocalTime.of(17, 16))),
                    postOffice = "ESPOO",
                    page = 0,
                    pageSize = 100,
                    sortBy = FeeDecisionSortParam.HEAD_OF_FAMILY,
                    sortDirection = SortDirection.ASC,
                    statuses = emptyList(),
                    areas = emptyList(),
                    unit = null,
                    distinctiveParams = emptyList(),
                    startDate = null,
                    endDate = null,
                    financeDecisionHandlerId = null,
                    difference = setOf(FeeDecisionDifference.INCOME)
                )
            }

        assertThat(result.data)
            .extracting({ it.headOfFamily.lastName }, { it.headOfFamily.firstName })
            .containsExactly(
                Tuple(testAdult_2.lastName, testAdult_2.firstName),
                Tuple(testAdult_3.lastName, testAdult_3.firstName)
            )
    }

    @Test
    fun `search with multiple statuses`() {
        db.transaction { tx ->
            val baseDecision = { child: DevPerson ->
                createFeeDecisionFixture(
                    status = FeeDecisionStatus.DRAFT,
                    decisionType = FeeDecisionType.NORMAL,
                    period = testPeriod,
                    headOfFamilyId = PersonId(UUID.randomUUID()),
                    children =
                        listOf(
                            createFeeDecisionChildFixture(
                                childId = child.id,
                                dateOfBirth = child.dateOfBirth,
                                placementUnitId = testDaycare.id,
                                placementType = PlacementType.DAYCARE,
                                serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed()
                            )
                        )
                )
            }
            tx.upsertFeeDecisions(
                listOf(
                    baseDecision(testChild_1)
                        .copy(
                            status = FeeDecisionStatus.DRAFT,
                            headOfFamilyId = testAdult_1.id,
                            headOfFamilyIncome = null
                        ),
                    baseDecision(testChild_2)
                        .copy(
                            status = FeeDecisionStatus.SENT,
                            headOfFamilyId = testAdult_2.id,
                            headOfFamilyIncome = null
                        ),
                    baseDecision(testChild_3)
                        .copy(
                            status = FeeDecisionStatus.WAITING_FOR_MANUAL_SENDING,
                            headOfFamilyId = testAdult_3.id,
                            headOfFamilyIncome = null
                        ),
                )
            )
        }

        assertEquals(3, searchByStatuses(emptyList()).size)
        assertEquals(
            2,
            searchByStatuses(listOf(FeeDecisionStatus.DRAFT, FeeDecisionStatus.SENT)).size
        )
        assertEquals(1, searchByStatuses(listOf(FeeDecisionStatus.DRAFT)).size)
    }

    private fun searchByStatuses(statuses: List<FeeDecisionStatus>): List<FeeDecisionSummary> {
        return db.read { tx ->
                tx.searchFeeDecisions(
                    clock =
                        MockEvakaClock(HelsinkiDateTime.of(testPeriod.start, LocalTime.of(13, 37))),
                    postOffice = "ESPOO",
                    page = 0,
                    pageSize = 100,
                    sortBy = FeeDecisionSortParam.HEAD_OF_FAMILY,
                    sortDirection = SortDirection.ASC,
                    statuses = statuses,
                    areas = emptyList(),
                    unit = null,
                    distinctiveParams = emptyList(),
                    startDate = null,
                    endDate = null,
                    financeDecisionHandlerId = null,
                    difference = emptySet()
                )
            }
            .data
    }

    private fun searchAndAssert(searchTerms: String, expectedChildLastName: String) {
        val result =
            db.read { tx ->
                tx.searchFeeDecisions(
                    clock = RealEvakaClock(),
                    postOffice = "ESPOO",
                    searchTerms = searchTerms,
                    page = 0,
                    pageSize = 100,
                    statuses = emptyList(),
                    areas = emptyList(),
                    sortBy = FeeDecisionSortParam.CREATED,
                    sortDirection = SortDirection.ASC,
                    distinctiveParams = emptyList(),
                    unit = null,
                    startDate = null,
                    endDate = null,
                    financeDecisionHandlerId = null,
                    difference = emptySet()
                )
            }
        assertEquals(1, result.data.size)
        assertEquals(expectedChildLastName, result.data.get(0).children.get(0).lastName)
    }
}
