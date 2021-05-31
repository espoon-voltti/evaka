// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.data

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.invoicing.controller.FeeDecisionSortParam
import fi.espoo.evaka.invoicing.controller.SortDirection
import fi.espoo.evaka.invoicing.createFeeDecisionChildFixture
import fi.espoo.evaka.invoicing.createFeeDecisionFixture
import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
import fi.espoo.evaka.invoicing.domain.FeeDecisionType
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.config.defaultObjectMapper
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.snDaycareFullDay35
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testAdult_2
import fi.espoo.evaka.testAdult_3
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testChild_5
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDecisionMaker_1
import fi.espoo.evaka.toFeeDecisionServiceNeed
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class FeeDecisionQueriesTest : PureJdbiTest() {
    val objectMapper = defaultObjectMapper()

    private val testPeriod = DateRange(LocalDate.of(2019, 5, 1), LocalDate.of(2019, 5, 31))
    private val testDecisions = listOf(
        createFeeDecisionFixture(
            status = FeeDecisionStatus.DRAFT,
            decisionType = FeeDecisionType.NORMAL,
            headOfFamilyId = testAdult_1.id,
            period = testPeriod,
            children = listOf(
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
            children = listOf(
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
            children = listOf(
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
        }
    }

    @AfterEach
    fun afterEach() {
        db.transaction { tx ->
            tx.resetDatabase()
        }
    }

    @Test
    fun `activateDrafts with one decision`() {
        db.transaction { tx ->
            val draft = testDecisions.find { it.status == FeeDecisionStatus.DRAFT }!!
            tx.upsertFeeDecisions(listOf(draft))
            tx.approveFeeDecisionDraftsForSending(listOf(draft.id), testDecisionMaker_1.id, approvedAt = Instant.now())

            val result = tx.getFeeDecision(testDecisions[0].id)!!
            assertEquals(FeeDecisionStatus.WAITING_FOR_SENDING, result.status)
            assertEquals(1L, result.decisionNumber)
            assertEquals(testDecisionMaker_1, result.approvedBy)
            assertNotNull(result.approvedAt)
        }
    }

    @Test
    fun `approveDraftsForSending with multiple decisions`() {
        db.transaction { tx ->
            val decisions = listOf(
                testDecisions[0],
                testDecisions[0].copy(
                    id = UUID.randomUUID(),
                    validDuring = DateRange(testPeriod.start.minusYears(1), testPeriod.end!!.minusYears(1))
                )
            )
            tx.upsertFeeDecisions(decisions)

            tx.approveFeeDecisionDraftsForSending(decisions.map { it.id }, testDecisionMaker_1.id, approvedAt = Instant.now())

            val result = tx.getFeeDecisionsByIds(decisions.map { it.id }).sortedBy { it.decisionNumber }
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
    fun `searchIgnoresAccent`() {
        db.transaction { tx ->
            tx.upsertFeeDecisions(testDecisions)
            tx.upsertFeeDecisions(
                listOf(
                    createFeeDecisionFixture(
                        status = FeeDecisionStatus.DRAFT,
                        decisionType = FeeDecisionType.NORMAL,
                        headOfFamilyId = testAdult_3.id,
                        period = testPeriod,
                        children = listOf(
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

    private fun searchAndAssert(searchTerms: String, expectedChildLastName: String) {
        val result = db.read { tx ->
            tx.searchFeeDecisions(
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
                financeDecisionHandlerId = null
            )
        }
        assertEquals(1, result.data.size)
        assertEquals(expectedChildLastName, result.data.get(0).children.get(0).lastName)
    }
}
