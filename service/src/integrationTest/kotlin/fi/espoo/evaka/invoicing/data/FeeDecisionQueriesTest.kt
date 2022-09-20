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
import fi.espoo.evaka.invoicing.domain.FeeDecisionDetailed
import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
import fi.espoo.evaka.invoicing.domain.FeeDecisionType
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.FeeDecisionId
import fi.espoo.evaka.shared.config.defaultJsonMapper
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.RealEvakaClock
import fi.espoo.evaka.snDaycareFullDay35
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testAdult_2
import fi.espoo.evaka.testAdult_3
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testChild_5
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
import fi.espoo.evaka.testDecisionMaker_1
import fi.espoo.evaka.testDecisionMaker_2
import fi.espoo.evaka.toFeeDecisionServiceNeed
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FeeDecisionQueriesTest : PureJdbiTest(resetDbBeforeEach = true) {
    val jsonMapper = defaultJsonMapper()

    private val testPeriod = DateRange(LocalDate.of(2019, 5, 1), LocalDate.of(2019, 5, 31))
    private val testDecisions =
        listOf(
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
        db.transaction { tx -> tx.insertGeneralTestFixtures() }
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

    private fun searchAndAssert(searchTerms: String, expectedChildLastName: String) {
        val result =
            db.read { tx ->
                tx.searchFeeDecisions(
                    clock = RealEvakaClock(),
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
