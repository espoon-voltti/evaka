// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.invoicing.data

import evaka.core.PureJdbiTest
import evaka.core.daycare.domain.Language
import evaka.core.invoicing.controller.DistinctiveParams
import evaka.core.invoicing.controller.FeeDecisionSortParam
import evaka.core.invoicing.controller.SortDirection
import evaka.core.invoicing.createFeeDecisionChildFixture
import evaka.core.invoicing.createFeeDecisionFixture
import evaka.core.invoicing.domain.FeeDecision
import evaka.core.invoicing.domain.FeeDecisionChild
import evaka.core.invoicing.domain.FeeDecisionDifference
import evaka.core.invoicing.domain.FeeDecisionServiceNeed
import evaka.core.invoicing.domain.FeeDecisionStatus
import evaka.core.invoicing.domain.FeeDecisionSummary
import evaka.core.invoicing.domain.FeeDecisionType
import evaka.core.invoicing.domain.PersonBasic
import evaka.core.placement.PlacementType
import evaka.core.serviceneed.ServiceNeedOption
import evaka.core.shared.DaycareId
import evaka.core.shared.EmployeeId
import evaka.core.shared.FeeDecisionId
import evaka.core.shared.PersonId
import evaka.core.shared.db.Database
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.DevPlacement
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import evaka.core.snDaycareFullDay35
import evaka.core.toFeeDecisionServiceNeed
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FeeDecisionSearchTest : PureJdbiTest(resetDbBeforeEach = true) {
    private val clock = MockEvakaClock(HelsinkiDateTime.of(LocalDateTime.of(2022, 1, 1, 12, 0)))

    private val employee1 = DevEmployee()
    private val employee2 = DevEmployee()
    private val adult1 = DevPerson(firstName = "Mark", lastName = "Foo")
    private val adult2 = DevPerson(firstName = "Dork", lastName = "Aman")
    // Names needed for text search tests
    private val child1 = DevPerson(dateOfBirth = LocalDate.of(2018, 9, 1), postOffice = "Espoo")
    private val child2 = DevPerson(dateOfBirth = LocalDate.of(2019, 3, 2), postOffice = "Helsinki")
    private val child3 =
        DevPerson(dateOfBirth = LocalDate.of(2018, 11, 13), firstName = "Visa", lastName = "Virén")
    private val child4 = DevPerson(dateOfBirth = LocalDate.of(2018, 11, 13))

    private val area1 = DevCareArea(name = "Area 1", shortName = "area1")
    private val daycare1 = DevDaycare(areaId = area1.id)
    private val area2 = DevCareArea(name = "Area 2", shortName = "area2")
    private val daycare2 =
        DevDaycare(areaId = area2.id, name = "Daycare 2", financeDecisionHandler = employee2.id)
    private val areaSv = DevCareArea(name = "Area sv", shortName = "area_sv")
    private val daycareSv =
        DevDaycare(areaId = areaSv.id, name = "Daycare sv", language = Language.sv)

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(employee1)
            tx.insert(employee2)
            tx.insert(areaSv)
            tx.insert(daycareSv)
            tx.insert(area1)
            tx.insert(daycare1)
            tx.insert(area2)
            tx.insert(daycare2)
            listOf(adult1, adult2).forEach { tx.insert(it, DevPersonType.ADULT) }
            listOf(child1, child2, child3, child4).forEach { tx.insert(it, DevPersonType.CHILD) }
            tx.insert(snDaycareFullDay35)
        }
    }

    @Test
    fun `text and number search`() {
        db.transaction { tx ->
            tx.upsertFeeDecisions(
                listOf(
                    decisionFixture(
                            headOfFamily = adult1.id,
                            children = listOf(childFixture(child3)),
                        )
                        .copy(decisionNumber = 99_999_999L),
                    decisionFixture(
                            headOfFamily = adult2.id,
                            children = listOf(childFixture(child4)),
                        )
                        .copy(decisionNumber = 11_111_111L),
                )
            )
        }
        val testCases =
            listOf("Vir" to child3, "Nope" to null, 99_999_999L to child3, 9_999_999L to null)
        for ((term, child) in testCases) {
            val result = search(term.toString())
            assertResultsByChild(result, child)
        }
    }

    @Test
    fun `textual search ignores accents`() {
        val child = child3
        db.transaction { tx ->
            tx.upsertFeeDecisions(
                listOf(
                    decisionFixture(
                        headOfFamily = adult1.id,
                        children = listOf(childFixture(child)),
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
                        headOfFamily = adult1.id,
                        children = listOf(childFixture(child3)),
                        status = FeeDecisionStatus.DRAFT,
                    ),
                    decisionFixture(
                        headOfFamily = adult2.id,
                        children = listOf(childFixture(child4)),
                        status = FeeDecisionStatus.WAITING_FOR_MANUAL_SENDING,
                    ),
                )
            )
        }
        val testCases =
            listOf(
                FeeDecisionStatus.DRAFT to true,
                FeeDecisionStatus.WAITING_FOR_MANUAL_SENDING to true,
                FeeDecisionStatus.ANNULLED to false,
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
                        headOfFamily = adult1.id,
                        children = listOf(childFixture(child3, daycare1.id)),
                    ),
                    decisionFixture(
                        headOfFamily = adult2.id,
                        children = listOf(childFixture(child4, daycare2.id)),
                    ),
                )
            )
        }
        val areaTestCases = listOf(area1 to child3, area2 to child4, areaSv to null)
        for ((area, child) in areaTestCases) {
            val result = search(areas = listOf(area.shortName))
            assertResultsByChild(result, child)
        }
        val unitTestCases = listOf(daycare1 to child3, daycare2 to child4, daycareSv to null)
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
                        headOfFamily = adult1.id,
                        children = listOf(childFixture(child1, daycare1.id, serviceNeed = null)),
                    ),
                    decisionFixture(
                        headOfFamily = adult2.id,
                        children = listOf(childFixture(child2, daycare2.id)),
                        period = FiniteDateRange(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 8, 1)),
                    ),
                )
            )
        }
        val testCases =
            listOf(
                DistinctiveParams.UNCONFIRMED_HOURS to child1,
                DistinctiveParams.EXTERNAL_CHILD to child2,
                DistinctiveParams.RETROACTIVE to child1,
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
                        headOfFamily = adult1.id,
                        children = listOf(childFixture(child1, daycare1.id, serviceNeed = null)),
                        period = FiniteDateRange(now.today(), now.today()),
                    )
                )
            )
        }
        assertEquals(
            1,
            search(distinctiveParams = listOf(DistinctiveParams.NO_STARTING_PLACEMENTS)).size,
        )

        assertEquals(
            1,
            search(
                    areas = listOf(area1.shortName),
                    distinctiveParams = listOf(DistinctiveParams.NO_STARTING_PLACEMENTS),
                )
                .size,
        )

        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    childId = child1.id,
                    unitId = daycare1.id,
                    startDate = now.today(),
                    endDate = now.today(),
                )
            )
        }

        assertEquals(
            0,
            search(distinctiveParams = listOf(DistinctiveParams.NO_STARTING_PLACEMENTS)).size,
        )
    }

    @Test
    fun `date search`() {
        db.transaction { tx ->
            tx.upsertFeeDecisions(
                listOf(
                    decisionFixture(
                        headOfFamily = adult1.id,
                        children = listOf(childFixture(child1)),
                        period = FiniteDateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 5, 1)),
                    ),
                    decisionFixture(
                        headOfFamily = adult2.id,
                        children = listOf(childFixture(child2)),
                        period = FiniteDateRange(LocalDate.of(2022, 6, 1), LocalDate.of(2023, 1, 1)),
                    ),
                )
            )
        }
        val overlapTestCases =
            listOf(
                (LocalDate.of(2021, 1, 1) to LocalDate.of(2021, 12, 31)) to null,
                (LocalDate.of(2021, 1, 1) to LocalDate.of(2022, 1, 1)) to child1,
                (LocalDate.of(2022, 4, 30) to LocalDate.of(2022, 5, 31)) to child1,
                (LocalDate.of(2022, 12, 31) to LocalDate.of(2023, 6, 1)) to child2,
            )
        for ((range, child) in overlapTestCases) {
            val result = search(startDate = range.first, endDate = range.second)
            assertResultsByChild(result, child)
        }
        val startTestCases =
            listOf(
                (LocalDate.of(2021, 1, 1) to LocalDate.of(2021, 12, 31)) to null,
                (LocalDate.of(2021, 1, 1) to LocalDate.of(2022, 1, 1)) to child1,
                (LocalDate.of(2022, 4, 30) to LocalDate.of(2022, 5, 31)) to null,
                (LocalDate.of(2022, 12, 31) to LocalDate.of(2023, 6, 1)) to null,
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
                        headOfFamily = adult1.id,
                        children = listOf(childFixture(child1, daycare2.id)),
                    ),
                    decisionFixture(
                        headOfFamily = adult2.id,
                        children = listOf(childFixture(child2, daycare1.id)),
                    ),
                )
            )
        }
        val testCases = listOf((employee2 to child1), (employee1 to null))
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
            sentAt: HelsinkiDateTime,
        ) = execute {
            sql(
                """
                UPDATE fee_decision SET created = ${bind(created)}, sent_at = ${bind(sentAt)} WHERE id = ${bind(id)}
            """
            )
        }
        lateinit var decisions: List<FeeDecision>
        db.transaction { tx ->
            decisions =
                listOf(
                    decisionFixture(
                            headOfFamily = adult2.id,
                            children = listOf(childFixture(child1, fee = 28900)),
                            period =
                                FiniteDateRange(LocalDate.of(2021, 1, 1), LocalDate.of(2022, 1, 1)),
                            status = FeeDecisionStatus.DRAFT,
                        )
                        .copy(decisionNumber = 1_111_111L),
                    decisionFixture(
                            headOfFamily = adult1.id,
                            children = listOf(childFixture(child2, fee = 38900)),
                            period =
                                FiniteDateRange(LocalDate.of(2021, 6, 1), LocalDate.of(2022, 6, 1)),
                            status = FeeDecisionStatus.WAITING_FOR_MANUAL_SENDING,
                        )
                        .copy(decisionNumber = 2_222_222L),
                )
            tx.upsertFeeDecisions(decisions)
            val oldTimestamp = HelsinkiDateTime.of(LocalDateTime.of(2020, 1, 1, 12, 0))
            val newTimestamp = HelsinkiDateTime.of(LocalDateTime.of(2022, 1, 1, 12, 0))
            tx.updateTimestamps(decisions[0].id, created = oldTimestamp, sentAt = oldTimestamp)
            tx.updateTimestamps(decisions[1].id, created = newTimestamp, sentAt = newTimestamp)
        }
        val testCases =
            FeeDecisionSortParam.entries.flatMap {
                listOf(it to SortDirection.ASC, it to SortDirection.DESC)
            }
        for ((sortBy, direction) in testCases) {
            val result = search(sortBy = sortBy, sortDirection = direction)
            assertEquals(2, result.size)
            when (direction) {
                SortDirection.ASC -> {
                    assertEquals(decisions.map { it.id }, result.map { it.id })
                }

                SortDirection.DESC -> {
                    assertEquals(decisions.reversed().map { it.id }, result.map { it.id })
                }
            }
        }
    }

    private fun decisionFixture(
        headOfFamily: PersonId,
        children: List<FeeDecisionChild>,
        status: FeeDecisionStatus = FeeDecisionStatus.DRAFT,
        period: FiniteDateRange =
            FiniteDateRange(LocalDate.of(2019, 5, 1), LocalDate.of(2019, 5, 31)),
    ) =
        createFeeDecisionFixture(
            status = status,
            decisionType = FeeDecisionType.NORMAL,
            headOfFamilyId = headOfFamily,
            period = period,
            children = children,
        )

    private fun childFixture(
        child: DevPerson,
        placementUnit: DaycareId = daycare1.id,
        serviceNeed: ServiceNeedOption? = snDaycareFullDay35,
        fee: Int = 28900,
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
                        missing = true,
                    ),
            fee = fee,
        )

    private fun assertResultsByChild(result: List<FeeDecisionSummary>, child: DevPerson?) {
        if (child != null) {
            assertEquals(1, result.size)
            val decision = result[0]
            assertEquals(
                listOf(
                    PersonBasic(
                        id = child.id,
                        dateOfBirth = child.dateOfBirth,
                        firstName = child.firstName,
                        lastName = child.lastName,
                        ssn = child.ssn,
                    )
                ),
                decision.children,
            )
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
        difference: Set<FeeDecisionDifference> = emptySet(),
    ) = db.read { tx ->
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
                difference = difference,
            )
            .let { result ->
                assertEquals(1, result.pages)
                assertEquals(result.total, result.data.size)
                result.data
            }
    }
}
