// SPDX-FileCopyrightText: 2017-2022 City of Espoo
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
import fi.espoo.evaka.invoicing.domain.FeeDecision
import fi.espoo.evaka.invoicing.domain.FeeDecisionChild
import fi.espoo.evaka.invoicing.domain.FeeDecisionDifference
import fi.espoo.evaka.invoicing.domain.FeeDecisionServiceNeed
import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
import fi.espoo.evaka.invoicing.domain.FeeDecisionSummary
import fi.espoo.evaka.invoicing.domain.FeeDecisionType
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.serviceneed.ServiceNeedOption
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.FeeDecisionId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.snDaycareFullDay35
import fi.espoo.evaka.testAdult_3
import fi.espoo.evaka.testAdult_4
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testArea2
import fi.espoo.evaka.testAreaSvebi
import fi.espoo.evaka.testChild_3
import fi.espoo.evaka.testChild_4
import fi.espoo.evaka.testChild_5
import fi.espoo.evaka.testChild_6
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
import fi.espoo.evaka.testDecisionMaker_1
import fi.espoo.evaka.testDecisionMaker_2
import fi.espoo.evaka.testSvebiDaycare
import fi.espoo.evaka.toFeeDecisionServiceNeed
import fi.espoo.evaka.toPersonBasic
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FeeDecisionSearchTest : PureJdbiTest(resetDbBeforeEach = true) {
    private val clock = MockEvakaClock(HelsinkiDateTime.of(LocalDateTime.of(2022, 1, 1, 12, 0)))

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insertGeneralTestFixtures()
            listOf(testChild_3, testChild_4, testChild_5, testChild_6).forEach {
                tx.insert(it, DevPersonType.CHILD)
            }
            tx.insert(snDaycareFullDay35)
        }
    }

    @Test
    fun `text and number search`() {
        db.transaction { tx ->
            tx.upsertFeeDecisions(
                listOf(
                    decisionFixture(
                            headOfFamily = testAdult_3.id,
                            children = listOf(childFixture(testChild_5))
                        )
                        .copy(decisionNumber = 99_999_999L),
                    decisionFixture(
                            headOfFamily = testAdult_4.id,
                            children = listOf(childFixture(testChild_6))
                        )
                        .copy(decisionNumber = 11_111_111L)
                )
            )
        }
        val testCases =
            listOf(
                "Vir" to testChild_5,
                "Nope" to null,
                99_999_999L to testChild_5,
                9_999_999L to null
            )
        for ((term, child) in testCases) {
            val result = search(term.toString())
            assertResultsByChild(result, child)
        }
    }

    @Test
    fun `textual search ignores accents`() {
        val child = testChild_5
        db.transaction { tx ->
            tx.upsertFeeDecisions(
                listOf(
                    decisionFixture(
                        headOfFamily = testAdult_3.id,
                        children = listOf(childFixture(child))
                    )
                )
            )
        }

        for (searchTerm in listOf("Viren", "Virén", "Visa Viren", "Visa Virén")) {
            val result = search(searchTerm = searchTerm)
            assertResultsByChild(result, child)
        }
    }

    @Test
    fun `status search`() {
        db.transaction { tx ->
            tx.upsertFeeDecisions(
                listOf(
                    decisionFixture(
                        headOfFamily = testAdult_3.id,
                        children = listOf(childFixture(testChild_5)),
                        status = FeeDecisionStatus.DRAFT
                    ),
                    decisionFixture(
                        headOfFamily = testAdult_4.id,
                        children = listOf(childFixture(testChild_6)),
                        status = FeeDecisionStatus.WAITING_FOR_MANUAL_SENDING
                    )
                )
            )
        }
        val testCases =
            listOf(
                FeeDecisionStatus.DRAFT to true,
                FeeDecisionStatus.WAITING_FOR_MANUAL_SENDING to true,
                FeeDecisionStatus.ANNULLED to false
            )
        for ((status, expected) in testCases) {
            val result = search(status = status)
            if (expected) {
                assertEquals(1, result.size)
                assertEquals(status, result[0].status)
            } else {
                assertTrue(result.isEmpty())
            }
        }
    }

    @Test
    fun `area and unit search`() {
        db.transaction { tx ->
            tx.upsertFeeDecisions(
                listOf(
                    decisionFixture(
                        headOfFamily = testAdult_3.id,
                        children = listOf(childFixture(testChild_5, testDaycare.id))
                    ),
                    decisionFixture(
                        headOfFamily = testAdult_4.id,
                        children = listOf(childFixture(testChild_6, testDaycare2.id))
                    )
                )
            )
        }
        val areaTestCases =
            listOf(testArea to testChild_5, testArea2 to testChild_6, testAreaSvebi to null)
        for ((area, child) in areaTestCases) {
            val result = search(areas = listOf(area.shortName))
            assertResultsByChild(result, child)
        }
        val unitTestCases =
            listOf(
                testDaycare to testChild_5,
                testDaycare2 to testChild_6,
                testSvebiDaycare to null
            )
        for ((unit, child) in unitTestCases) {
            val result = search(unit = unit.id)
            assertResultsByChild(result, child)
        }
    }

    @Test
    fun `distinct parameters search`() {
        db.transaction { tx ->
            tx.upsertFeeDecisions(
                listOf(
                    decisionFixture(
                        headOfFamily = testAdult_3.id,
                        children =
                            listOf(childFixture(testChild_3, testDaycare.id, serviceNeed = null))
                    ),
                    decisionFixture(
                        headOfFamily = testAdult_4.id,
                        children = listOf(childFixture(testChild_4, testDaycare2.id)),
                        period = DateRange(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 8, 1))
                    )
                )
            )
        }
        val testCases =
            listOf(
                DistinctiveParams.UNCONFIRMED_HOURS to testChild_3,
                DistinctiveParams.EXTERNAL_CHILD to testChild_4,
                DistinctiveParams.RETROACTIVE to testChild_3
            )
        for ((params, child) in testCases) {
            val result = search(distinctiveParams = listOf(params))
            assertResultsByChild(result, child)
        }
    }

    @Test
    fun `starting children filter search`() {
        val now = MockEvakaClock(HelsinkiDateTime.of(LocalDateTime.of(2022, 1, 1, 12, 0)))

        db.transaction { tx ->
            tx.upsertFeeDecisions(
                listOf(
                    decisionFixture(
                        headOfFamily = testAdult_3.id,
                        children =
                            listOf(childFixture(testChild_3, testDaycare.id, serviceNeed = null)),
                        period = DateRange(now.today(), now.today())
                    )
                )
            )
        }
        assertEquals(
            1,
            search(distinctiveParams = listOf(DistinctiveParams.NO_STARTING_PLACEMENTS)).size
        )

        assertEquals(
            1,
            search(
                    areas = listOf(testArea.shortName),
                    distinctiveParams = listOf(DistinctiveParams.NO_STARTING_PLACEMENTS)
                )
                .size
        )

        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    childId = testChild_3.id,
                    unitId = testDaycare.id,
                    startDate = now.today(),
                    endDate = now.today()
                )
            )
        }

        assertEquals(
            0,
            search(distinctiveParams = listOf(DistinctiveParams.NO_STARTING_PLACEMENTS)).size
        )
    }

    @Test
    fun `date search`() {
        db.transaction { tx ->
            tx.upsertFeeDecisions(
                listOf(
                    decisionFixture(
                        headOfFamily = testAdult_3.id,
                        children = listOf(childFixture(testChild_3)),
                        period = DateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 5, 1))
                    ),
                    decisionFixture(
                        headOfFamily = testAdult_4.id,
                        children = listOf(childFixture(testChild_4)),
                        period = DateRange(LocalDate.of(2022, 6, 1), LocalDate.of(2023, 1, 1))
                    )
                )
            )
        }
        val overlapTestCases =
            listOf(
                (LocalDate.of(2021, 1, 1) to LocalDate.of(2021, 12, 31)) to null,
                (LocalDate.of(2021, 1, 1) to LocalDate.of(2022, 1, 1)) to testChild_3,
                (LocalDate.of(2022, 4, 30) to LocalDate.of(2022, 5, 31)) to testChild_3,
                (LocalDate.of(2022, 12, 31) to LocalDate.of(2023, 6, 1)) to testChild_4
            )
        for ((range, child) in overlapTestCases) {
            val result = search(startDate = range.first, endDate = range.second)
            assertResultsByChild(result, child)
        }
        val startTestCases =
            listOf(
                (LocalDate.of(2021, 1, 1) to LocalDate.of(2021, 12, 31)) to null,
                (LocalDate.of(2021, 1, 1) to LocalDate.of(2022, 1, 1)) to testChild_3,
                (LocalDate.of(2022, 4, 30) to LocalDate.of(2022, 5, 31)) to null,
                (LocalDate.of(2022, 12, 31) to LocalDate.of(2023, 6, 1)) to null
            )
        for ((range, child) in startTestCases) {
            val result =
                search(startDate = range.first, endDate = range.second, searchByStartDate = true)
            assertResultsByChild(result, child)
        }
    }

    @Test
    fun `finance decision handler search`() {
        db.transaction { tx ->
            tx.upsertFeeDecisions(
                listOf(
                    decisionFixture(
                        headOfFamily = testAdult_3.id,
                        children = listOf(childFixture(testChild_3, testDaycare2.id))
                    ),
                    decisionFixture(
                        headOfFamily = testAdult_4.id,
                        children = listOf(childFixture(testChild_4, testDaycare.id))
                    )
                )
            )
        }
        val testCases = listOf((testDecisionMaker_2 to testChild_3), (testDecisionMaker_1 to null))
        for ((financeHandler, child) in testCases) {
            val result = search(financeDecisionHandlerId = financeHandler.id)
            assertResultsByChild(result, child)
        }
    }

    @Test
    fun `sort orders`() {
        fun Database.Transaction.updateTimestamps(
            id: FeeDecisionId,
            created: HelsinkiDateTime,
            sentAt: HelsinkiDateTime
        ) =
            @Suppress("DEPRECATION")
            createUpdate(
                    """
                UPDATE fee_decision SET created = :created, sent_at = :sentAt WHERE id = :id
            """
                        .trimIndent()
                )
                .bind("created", created)
                .bind("sentAt", sentAt)
                .bind("id", id)
                .execute()
        lateinit var decisions: List<FeeDecision>
        db.transaction { tx ->
            decisions =
                listOf(
                    decisionFixture(
                            headOfFamily = testAdult_4.id,
                            children = listOf(childFixture(testChild_3, fee = 28900)),
                            period = DateRange(LocalDate.of(2021, 1, 1), LocalDate.of(2022, 1, 1)),
                            status = FeeDecisionStatus.DRAFT
                        )
                        .copy(decisionNumber = 1_111_111L),
                    decisionFixture(
                            headOfFamily = testAdult_3.id,
                            children = listOf(childFixture(testChild_4, fee = 38900)),
                            period = DateRange(LocalDate.of(2021, 6, 1), LocalDate.of(2022, 6, 1)),
                            status = FeeDecisionStatus.WAITING_FOR_MANUAL_SENDING
                        )
                        .copy(decisionNumber = 2_222_222L)
                )
            tx.upsertFeeDecisions(decisions)
            val oldTimestamp = HelsinkiDateTime.of(LocalDateTime.of(2020, 1, 1, 12, 0))
            val newTimestamp = HelsinkiDateTime.of(LocalDateTime.of(2022, 1, 1, 12, 0))
            tx.updateTimestamps(decisions[0].id, created = oldTimestamp, sentAt = oldTimestamp)
            tx.updateTimestamps(decisions[1].id, created = newTimestamp, sentAt = newTimestamp)
        }
        val testCases =
            FeeDecisionSortParam.values().flatMap {
                listOf(it to SortDirection.ASC, it to SortDirection.DESC)
            }
        for ((sortBy, direction) in testCases) {
            val result = search(sortBy = sortBy, sortDirection = direction)
            assertEquals(2, result.size)
            when (direction) {
                SortDirection.ASC -> assertEquals(decisions.map { it.id }, result.map { it.id })
                SortDirection.DESC ->
                    assertEquals(decisions.reversed().map { it.id }, result.map { it.id })
            }
        }
    }

    private fun decisionFixture(
        headOfFamily: PersonId,
        children: List<FeeDecisionChild>,
        status: FeeDecisionStatus = FeeDecisionStatus.DRAFT,
        period: DateRange = DateRange(LocalDate.of(2019, 5, 1), LocalDate.of(2019, 5, 31))
    ) =
        createFeeDecisionFixture(
            status = status,
            decisionType = FeeDecisionType.NORMAL,
            headOfFamilyId = headOfFamily,
            period = period,
            children = children
        )

    private fun childFixture(
        child: DevPerson,
        placementUnit: DaycareId = testDaycare.id,
        serviceNeed: ServiceNeedOption? = snDaycareFullDay35,
        fee: Int = 28900
    ) =
        createFeeDecisionChildFixture(
            childId = child.id,
            dateOfBirth = child.dateOfBirth,
            placementUnitId = placementUnit,
            placementType = PlacementType.DAYCARE,
            serviceNeed =
                serviceNeed?.toFeeDecisionServiceNeed()
                    ?: FeeDecisionServiceNeed(
                        optionId = serviceNeed?.id,
                        feeCoefficient = BigDecimal.ONE,
                        contractDaysPerMonth = null,
                        descriptionFi = "",
                        descriptionSv = "",
                        missing = true
                    ),
            fee = fee
        )

    private fun assertResultsByChild(result: List<FeeDecisionSummary>, child: DevPerson?) {
        if (child != null) {
            assertEquals(1, result.size)
            val decision = result[0]
            assertEquals(listOf(child.toPersonBasic()), decision.children)
        } else {
            assertTrue(result.isEmpty())
        }
    }

    private fun search(
        searchTerm: String = "",
        status: FeeDecisionStatus? = null,
        areas: List<String> = emptyList(),
        sortBy: FeeDecisionSortParam = FeeDecisionSortParam.CREATED,
        sortDirection: SortDirection = SortDirection.ASC,
        distinctiveParams: List<DistinctiveParams> = emptyList(),
        unit: DaycareId? = null,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null,
        searchByStartDate: Boolean = false,
        financeDecisionHandlerId: EmployeeId? = null,
        difference: Set<FeeDecisionDifference> = emptySet()
    ) =
        db.read { tx ->
            tx.searchFeeDecisions(
                    clock = clock,
                    postOffice = "ESPOO",
                    searchTerms = searchTerm,
                    page = 0,
                    pageSize = 100,
                    statuses = listOfNotNull(status),
                    areas = areas,
                    sortBy = sortBy,
                    sortDirection = sortDirection,
                    distinctiveParams = distinctiveParams,
                    unit = unit,
                    startDate = startDate,
                    endDate = endDate,
                    searchByStartDate = searchByStartDate,
                    financeDecisionHandlerId = financeDecisionHandlerId,
                    difference = difference
                )
                .let { result ->
                    assertEquals(1, result.pages)
                    assertEquals(result.total, result.data.size)
                    result.data
                }
        }
}
