// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.data

import fi.espoo.evaka.PureJdbiTest
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
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.domain.RealEvakaClock
import fi.espoo.evaka.snDaycareFullDay35
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

    private val area = DevCareArea()
    private val decisionMaker1 = DevEmployee()
    private val decisionMaker2 = DevEmployee(firstName = "Handler", lastName = "Unit")
    private val daycare = DevDaycare(areaId = area.id)
    private val daycare2 =
        DevDaycare(areaId = area.id, name = "Daycare 2", financeDecisionHandler = decisionMaker2.id)

    // Distinct last names in alphabetical order for sort-dependent search tests
    private val adult1 = DevPerson(lastName = "Aaberg")
    private val adult2 = DevPerson(lastName = "Baker")
    private val adult3 = DevPerson(lastName = "Clark")
    private val adult4 = DevPerson(lastName = "Davis")
    private val adult5 = DevPerson(lastName = "Evans")
    private val adult6 = DevPerson(lastName = "Foster")
    private val adult7 = DevPerson(lastName = "Grant")

    private val child1 = DevPerson(dateOfBirth = LocalDate.of(2017, 6, 1))
    private val child2 = DevPerson(dateOfBirth = LocalDate.of(2016, 3, 1))
    private val child3 = DevPerson(dateOfBirth = LocalDate.of(2018, 9, 1))
    private val child4 = DevPerson(dateOfBirth = LocalDate.of(2019, 3, 2))
    private val child5 =
        DevPerson(firstName = "Visa", lastName = "Virén", dateOfBirth = LocalDate.of(2018, 11, 13))

    private val testPeriod = FiniteDateRange(LocalDate.of(2019, 5, 1), LocalDate.of(2019, 5, 31))
    private val testPeriod2 = FiniteDateRange(LocalDate.of(2019, 5, 15), LocalDate.of(2019, 5, 31))
    private val testDecisions =
        listOf(
            createFeeDecisionFixture(
                status = FeeDecisionStatus.DRAFT,
                decisionType = FeeDecisionType.NORMAL,
                headOfFamilyId = adult1.id,
                period = testPeriod2,
                children =
                    listOf(
                        createFeeDecisionChildFixture(
                            childId = child1.id,
                            dateOfBirth = child1.dateOfBirth,
                            placementUnitId = daycare.id,
                            placementType = PlacementType.DAYCARE,
                            serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                        ),
                        createFeeDecisionChildFixture(
                            childId = child2.id,
                            dateOfBirth = child2.dateOfBirth,
                            placementUnitId = daycare.id,
                            placementType = PlacementType.DAYCARE,
                            serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                            siblingDiscount = 50,
                            fee = 145,
                        ),
                    ),
            ),
            createFeeDecisionFixture(
                status = FeeDecisionStatus.SENT,
                decisionType = FeeDecisionType.NORMAL,
                headOfFamilyId = adult1.id,
                period = testPeriod,
                children =
                    listOf(
                        createFeeDecisionChildFixture(
                            childId = child2.id,
                            dateOfBirth = child2.dateOfBirth,
                            placementUnitId = daycare.id,
                            placementType = PlacementType.DAYCARE,
                            serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                        )
                    ),
            ),
            createFeeDecisionFixture(
                status = FeeDecisionStatus.SENT,
                decisionType = FeeDecisionType.NORMAL,
                headOfFamilyId = adult2.id,
                period = testPeriod,
                children =
                    listOf(
                        createFeeDecisionChildFixture(
                            childId = child1.id,
                            dateOfBirth = child1.dateOfBirth,
                            placementUnitId = daycare.id,
                            placementType = PlacementType.DAYCARE,
                            serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                        )
                    ),
            ),
        )

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(decisionMaker1)
            tx.insert(decisionMaker2)
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(daycare2)
            listOf(adult1, adult2, adult3, adult4, adult5, adult6, adult7).forEach {
                tx.insert(it, DevPersonType.ADULT)
            }
            listOf(child1, child2, child3, child4, child5).forEach {
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
                decisionMaker1.id,
                approvedAt = HelsinkiDateTime.now(),
                null,
                false,
                false,
            )

            val result = tx.getFeeDecision(testDecisions[0].id)!!
            assertEquals(FeeDecisionStatus.WAITING_FOR_SENDING, result.status)
            assertEquals(1L, result.decisionNumber)
            assertEquals(decisionMaker1.id, result.approvedBy?.id)
            assertNotNull(result.approvedAt)
        }
    }

    @Test
    fun `activateDrafts sets current user as approver for retroactive decision`() {
        val result = createAndApproveFeeDecisionForSending(false, daycare)
        assertEquals(FeeDecisionStatus.WAITING_FOR_SENDING, result.status)
        assertEquals(1L, result.decisionNumber)
        assertEquals(decisionMaker1.id, result.approvedBy?.id)
        assertNotNull(result.approvedAt)
        assertEquals(
            "${decisionMaker1.lastName} ${decisionMaker1.firstName}",
            "${result.financeDecisionHandlerLastName} ${result.financeDecisionHandlerFirstName}",
        )
    }

    @Test
    fun `activateDrafts sets daycare handler as approver for retroactive decision if forced`() {
        val result = createAndApproveFeeDecisionForSending(true, daycare2)
        assertEquals(FeeDecisionStatus.WAITING_FOR_SENDING, result.status)
        assertEquals(1L, result.decisionNumber)
        assertEquals(decisionMaker1.id, result.approvedBy?.id)
        assertNotNull(result.approvedAt)
        assertEquals(
            "${decisionMaker2.lastName} ${decisionMaker2.firstName}",
            "${result.financeDecisionHandlerLastName} ${result.financeDecisionHandlerFirstName}",
        )
    }

    private fun createAndApproveFeeDecisionForSending(
        forceUseDaycareHandler: Boolean,
        unit: DevDaycare,
    ): FeeDecisionDetailed {
        return db.transaction { tx ->
            val draft =
                createFeeDecisionFixture(
                    status = FeeDecisionStatus.DRAFT,
                    decisionType = FeeDecisionType.NORMAL,
                    headOfFamilyId = adult1.id,
                    period = testPeriod,
                    children =
                        listOf(
                            createFeeDecisionChildFixture(
                                childId = child1.id,
                                dateOfBirth = child1.dateOfBirth,
                                placementUnitId = unit.id,
                                placementType = PlacementType.DAYCARE,
                                serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                            )
                        ),
                )

            tx.upsertFeeDecisions(listOf(draft))
            tx.approveFeeDecisionDraftsForSending(
                listOf(draft.id),
                decisionMaker1.id,
                approvedAt = HelsinkiDateTime.now(),
                null,
                true,
                forceUseDaycareHandler,
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
                            FiniteDateRange(
                                testPeriod.start.minusYears(1),
                                testPeriod.end.minusYears(1),
                            ),
                    ),
                )
            tx.upsertFeeDecisions(decisions)

            tx.approveFeeDecisionDraftsForSending(
                decisions.map { it.id },
                decisionMaker1.id,
                approvedAt = HelsinkiDateTime.now(),
                null,
                false,
                false,
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

        fun find(
            headOfFamilyId: PersonId,
            period: FiniteDateRange?,
            status: List<FeeDecisionStatus>?,
        ) =
            db.read { tx -> tx.findFeeDecisionsForHeadOfFamily(headOfFamilyId, period, status) }
                .map { it.id }
                .toSet()

        assertEquals(both, find(adult1.id, null, null))

        // Filter by period
        assertEquals(
            both,
            find(
                adult1.id,
                FiniteDateRange(LocalDate.of(2019, 5, 15), LocalDate.of(2019, 5, 15)),
                null,
            ),
        )

        assertEquals(
            sent,
            find(
                adult1.id,
                FiniteDateRange(LocalDate.of(2019, 5, 1), LocalDate.of(2019, 5, 1)),
                null,
            ),
        )

        assertEquals(
            emptySet(),
            find(
                adult1.id,
                FiniteDateRange(LocalDate.of(2015, 1, 1), LocalDate.of(2015, 1, 1)),
                null,
            ),
        )

        // Filter by status
        assertEquals(
            both,
            find(adult1.id, null, listOf(FeeDecisionStatus.DRAFT, FeeDecisionStatus.SENT)),
        )

        assertEquals(sent, find(adult1.id, null, listOf(FeeDecisionStatus.SENT)))

        assertEquals(
            emptySet(),
            find(adult1.id, null, listOf(FeeDecisionStatus.WAITING_FOR_MANUAL_SENDING)),
        )

        // Filter by both period and status
        assertEquals(
            sent,
            find(
                adult1.id,
                FiniteDateRange(LocalDate.of(2019, 5, 10), LocalDate.of(2019, 5, 15)),
                listOf(
                    FeeDecisionStatus.WAITING_FOR_SENDING,
                    FeeDecisionStatus.WAITING_FOR_MANUAL_SENDING,
                    FeeDecisionStatus.SENT,
                ),
            ),
        )

        assertEquals(
            emptySet(),
            find(
                adult1.id,
                FiniteDateRange(LocalDate.of(2018, 5, 10), LocalDate.of(2019, 5, 15)),
                listOf(FeeDecisionStatus.ANNULLED),
            ),
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
                        headOfFamilyId = adult3.id,
                        period = testPeriod,
                        children =
                            listOf(
                                createFeeDecisionChildFixture(
                                    childId = child5.id,
                                    dateOfBirth = child5.dateOfBirth,
                                    placementUnitId = daycare.id,
                                    placementType = PlacementType.DAYCARE,
                                    serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                                )
                            ),
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
                                    placementUnitId = daycare.id,
                                    placementType = PlacementType.DAYCARE,
                                    serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                                )
                            ),
                    )
                }
                tx.upsertFeeDecisions(
                    listOf(
                        baseDecision(child1)
                            .copy(headOfFamilyId = adult1.id, headOfFamilyIncome = null),
                        baseDecision(child2)
                            .copy(
                                headOfFamilyId = adult2.id,
                                headOfFamilyIncome =
                                    DecisionIncome(
                                        effect = IncomeEffect.MAX_FEE_ACCEPTED,
                                        data = emptyMap(),
                                        totalIncome = 0,
                                        totalExpenses = 0,
                                        total = 0,
                                        worksAtECHA = false,
                                    ),
                            ),
                        baseDecision(child3)
                            .copy(
                                headOfFamilyId = adult3.id,
                                headOfFamilyIncome = null,
                                partnerId = adult4.id,
                                partnerIncome =
                                    DecisionIncome(
                                        effect = IncomeEffect.MAX_FEE_ACCEPTED,
                                        data = emptyMap(),
                                        totalIncome = 0,
                                        totalExpenses = 0,
                                        total = 0,
                                        worksAtECHA = false,
                                    ),
                            ),
                        baseDecision(child4)
                            .copy(
                                headOfFamilyId = adult5.id,
                                headOfFamilyIncome =
                                    DecisionIncome(
                                        effect = IncomeEffect.INCOME,
                                        data = emptyMap(),
                                        totalIncome = 200000,
                                        totalExpenses = 0,
                                        total = 200000,
                                        worksAtECHA = false,
                                    ),
                            ),
                        baseDecision(child5)
                            .copy(
                                headOfFamilyId = adult6.id,
                                headOfFamilyIncome =
                                    DecisionIncome(
                                        effect = IncomeEffect.INCOME,
                                        data = emptyMap(),
                                        totalIncome = 200000,
                                        totalExpenses = 0,
                                        total = 200000,
                                        worksAtECHA = false,
                                    ),
                                partnerId = adult7.id,
                                partnerIncome =
                                    DecisionIncome(
                                        effect = IncomeEffect.MAX_FEE_ACCEPTED,
                                        data = emptyMap(),
                                        totalIncome = 0,
                                        totalExpenses = 0,
                                        total = 0,
                                        worksAtECHA = false,
                                    ),
                            ),
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
                { it.incomeEffect },
            )
            .containsExactlyInAnyOrder(
                Tuple(adult1.lastName, adult1.firstName, IncomeEffect.NOT_AVAILABLE),
                Tuple(adult2.lastName, adult2.firstName, IncomeEffect.MAX_FEE_ACCEPTED),
                Tuple(adult3.lastName, adult3.firstName, IncomeEffect.MAX_FEE_ACCEPTED),
                Tuple(adult5.lastName, adult5.firstName, IncomeEffect.INCOME),
                Tuple(adult6.lastName, adult6.firstName, IncomeEffect.MAX_FEE_ACCEPTED),
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
                    difference = emptySet(),
                )
            }

        assertThat(result.data)
            .extracting({ it.headOfFamily.lastName }, { it.headOfFamily.firstName })
            .containsExactly(
                Tuple(adult2.lastName, adult2.firstName),
                Tuple(adult3.lastName, adult3.firstName),
                Tuple(adult6.lastName, adult6.firstName),
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
                                placementUnitId = daycare.id,
                                placementType = PlacementType.DAYCARE,
                                serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                            )
                        ),
                )
            }
            tx.upsertFeeDecisions(
                listOf(
                    baseDecision(child1).copy(headOfFamilyId = adult1.id, difference = emptySet()),
                    baseDecision(child2)
                        .copy(
                            headOfFamilyId = adult2.id,
                            difference = setOf(FeeDecisionDifference.INCOME),
                        ),
                    baseDecision(child3)
                        .copy(
                            headOfFamilyId = adult3.id,
                            difference =
                                setOf(
                                    FeeDecisionDifference.INCOME,
                                    FeeDecisionDifference.FAMILY_SIZE,
                                ),
                        ),
                    baseDecision(child4)
                        .copy(
                            headOfFamilyId = adult4.id,
                            difference = setOf(FeeDecisionDifference.FEE_ALTERATIONS),
                        ),
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
                    difference = setOf(FeeDecisionDifference.INCOME),
                )
            }

        assertThat(result.data)
            .extracting({ it.headOfFamily.lastName }, { it.headOfFamily.firstName })
            .containsExactly(
                Tuple(adult2.lastName, adult2.firstName),
                Tuple(adult3.lastName, adult3.firstName),
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
                                placementUnitId = daycare.id,
                                placementType = PlacementType.DAYCARE,
                                serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                            )
                        ),
                )
            }
            tx.upsertFeeDecisions(
                listOf(
                    baseDecision(child1)
                        .copy(
                            status = FeeDecisionStatus.DRAFT,
                            headOfFamilyId = adult1.id,
                            headOfFamilyIncome = null,
                        ),
                    baseDecision(child2)
                        .copy(
                            status = FeeDecisionStatus.SENT,
                            headOfFamilyId = adult2.id,
                            headOfFamilyIncome = null,
                        ),
                    baseDecision(child3)
                        .copy(
                            status = FeeDecisionStatus.WAITING_FOR_MANUAL_SENDING,
                            headOfFamilyId = adult3.id,
                            headOfFamilyIncome = null,
                        ),
                )
            )
        }

        assertEquals(3, searchByStatuses(emptyList()).size)
        assertEquals(
            2,
            searchByStatuses(listOf(FeeDecisionStatus.DRAFT, FeeDecisionStatus.SENT)).size,
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
                    difference = emptySet(),
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
                    difference = emptySet(),
                )
            }
        assertEquals(1, result.data.size)
        assertEquals(expectedChildLastName, result.data[0].children[0].lastName)
    }
}
