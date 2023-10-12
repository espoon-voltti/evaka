// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.invoicing.controller.VoucherValueDecisionController
import fi.espoo.evaka.invoicing.data.findValueDecisionsForChild
import fi.espoo.evaka.invoicing.domain.IncomeEffect
import fi.espoo.evaka.invoicing.domain.VoucherValueDecision
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionStatus
import fi.espoo.evaka.placement.cancelPlacement
import fi.espoo.evaka.placement.updatePlacementStartAndEndDate
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevIncome
import fi.espoo.evaka.shared.dev.insertTestIncome
import fi.espoo.evaka.shared.dev.insertTestParentship
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDecisionMaker_2
import fi.espoo.evaka.testVoucherDaycare
import java.time.LocalDate
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class VoucherValueDecisionGenerationForDataChangesIntegrationTest :
    FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var generator: FinanceDecisionGenerator
    @Autowired private lateinit var decisionController: VoucherValueDecisionController
    @Autowired private lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>

    val originalRange = dateRange(10, 20)

    val now = MockEvakaClock(2023, 1, 1, 0, 0)
    val admin = AuthenticatedUser.Employee(testDecisionMaker_2.id, setOf(UserRole.ADMIN))
    val placementId = PlacementId(UUID.randomUUID())
    lateinit var sentDecision: VoucherValueDecision

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insertGeneralTestFixtures()
            tx.insertTestPlacement(
                id = placementId,
                childId = testChild_1.id,
                unitId = testVoucherDaycare.id,
                startDate = originalRange.start,
                endDate = originalRange.end!!
            )
            tx.insertTestParentship(
                testAdult_1.id,
                testChild_1.id,
                startDate = originalRange.start.minusYears(1),
                endDate = originalRange.end!!.plusYears(1)
            )
        }
        generate()
        sendAllVoucherValueDecisions()
        val decisions = getAllVoucherValueDecisions()
        assertEquals(1, decisions.size)
        assertEquals(VoucherValueDecisionStatus.SENT, decisions.first().status)
        sentDecision = decisions.first()
    }

    @Test
    fun `no changes`() {
        generate()

        val feeDecisions = getAllVoucherValueDecisions()
        assertEquals(listOf(sentDecision), feeDecisions)
    }

    @Test
    fun `start date moves earlier`() {
        db.transaction { tx -> tx.updatePlacementStartAndEndDate(placementId, day(5), day(20)) }
        generate()

        assertDrafts(listOf(dateRange(5, 9) to false))

        sendAllVoucherValueDecisions()

        assertFinal(
            listOf(
                Triple(VoucherValueDecisionStatus.SENT, dateRange(5, 9), false),
                Triple(VoucherValueDecisionStatus.SENT, dateRange(10, 20), false),
            )
        )
    }

    @Test
    fun `start date moves later`() {
        db.transaction { tx -> tx.updatePlacementStartAndEndDate(placementId, day(15), day(20)) }
        generate()

        assertDrafts(
            listOf(
                dateRange(10, 14) to true,
                dateRange(15, 20) to false,
            )
        )

        sendAllVoucherValueDecisions()

        assertFinal(
            listOf(
                Triple(VoucherValueDecisionStatus.ANNULLED, dateRange(10, 20), false),
                Triple(VoucherValueDecisionStatus.SENT, dateRange(15, 20), false),
            )
        )
    }

    @Test
    fun `end date moves earlier`() {
        db.transaction { tx -> tx.updatePlacementStartAndEndDate(placementId, day(10), day(15)) }
        generate()

        assertDrafts(listOf(dateRange(16, 20) to true))

        sendAllVoucherValueDecisions()

        assertFinal(
            listOf(
                Triple(VoucherValueDecisionStatus.SENT, dateRange(10, 15), false),
            )
        )
    }

    @Test
    fun `end date moves later`() {
        db.transaction { tx -> tx.updatePlacementStartAndEndDate(placementId, day(10), day(25)) }
        generate()

        assertDrafts(listOf(dateRange(21, 25) to false))

        sendAllVoucherValueDecisions()

        assertFinal(
            listOf(
                Triple(VoucherValueDecisionStatus.SENT, dateRange(10, 20), false),
                Triple(VoucherValueDecisionStatus.SENT, dateRange(21, 25), false),
            )
        )
    }

    @Test
    fun `start date moves earlier and end date moves earlier`() {
        db.transaction { tx -> tx.updatePlacementStartAndEndDate(placementId, day(5), day(15)) }
        generate()

        assertDrafts(listOf(dateRange(5, 9) to false, dateRange(16, 20) to true))

        sendAllVoucherValueDecisions()

        assertFinal(
            listOf(
                Triple(VoucherValueDecisionStatus.SENT, dateRange(5, 9), false),
                Triple(VoucherValueDecisionStatus.SENT, dateRange(10, 15), false),
            )
        )
    }

    @Test
    fun `start date moves earlier and end date moves later`() {
        db.transaction { tx -> tx.updatePlacementStartAndEndDate(placementId, day(5), day(25)) }
        generate()

        assertDrafts(listOf(dateRange(5, 9) to false, dateRange(21, 25) to false))

        sendAllVoucherValueDecisions()

        assertFinal(
            listOf(
                Triple(VoucherValueDecisionStatus.SENT, dateRange(5, 9), false),
                Triple(VoucherValueDecisionStatus.SENT, dateRange(10, 20), false),
                Triple(VoucherValueDecisionStatus.SENT, dateRange(21, 25), false),
            )
        )
    }

    @Test
    fun `start date moves later and end date moves earlier`() {
        db.transaction { tx -> tx.updatePlacementStartAndEndDate(placementId, day(15), day(15)) }
        generate()

        assertDrafts(
            listOf(dateRange(10, 14) to true, dateRange(15, 15) to false, dateRange(16, 20) to true)
        )

        sendAllVoucherValueDecisions()

        assertFinal(
            listOf(
                Triple(VoucherValueDecisionStatus.ANNULLED, dateRange(10, 20), false),
                Triple(VoucherValueDecisionStatus.SENT, dateRange(15, 15), false)
            )
        )
    }

    @Test
    fun `start date moves later and end date moves later`() {
        db.transaction { tx -> tx.updatePlacementStartAndEndDate(placementId, day(15), day(25)) }
        generate()

        assertDrafts(listOf(dateRange(10, 14) to true, dateRange(15, 25) to false))

        sendAllVoucherValueDecisions()

        assertFinal(
            listOf(
                Triple(VoucherValueDecisionStatus.ANNULLED, dateRange(10, 20), false),
                Triple(VoucherValueDecisionStatus.SENT, dateRange(15, 25), false)
            )
        )
    }

    @Test
    fun `break in placement on days 14-15`() {
        db.transaction { tx ->
            tx.updatePlacementStartAndEndDate(placementId, day(10), day(13))
            tx.insertTestPlacement(
                id = PlacementId(UUID.randomUUID()),
                childId = testChild_1.id,
                unitId = testVoucherDaycare.id,
                startDate = day(16),
                endDate = originalRange.end!!
            )
        }

        generate()

        assertDrafts(
            listOf(
                dateRange(14, 15) to true,
                dateRange(16, 20) to false,
            )
        )

        sendAllVoucherValueDecisions()

        assertFinal(
            listOf(
                Triple(VoucherValueDecisionStatus.SENT, dateRange(10, 13), false),
                Triple(VoucherValueDecisionStatus.SENT, dateRange(16, 20), false),
            )
        )
    }

    @Test
    fun `placement is removed`() {
        db.transaction { tx -> tx.cancelPlacement(placementId) }
        generate()
        assertDrafts(listOf(dateRange(10, 20) to true))
        sendAllVoucherValueDecisions()
        assertFinal(listOf(Triple(VoucherValueDecisionStatus.ANNULLED, dateRange(10, 20), false)))
    }

    @Test
    fun `Incomplete income is equal to non-existing and does not cause new draft`() {
        db.transaction { tx ->
            tx.insertTestIncome(
                DevIncome(
                    personId = testAdult_1.id,
                    validFrom = originalRange.start,
                    validTo = originalRange.end,
                    data = emptyMap(),
                    effect = IncomeEffect.INCOMPLETE,
                    updatedBy = EvakaUserId(testDecisionMaker_2.id.raw)
                )
            )
        }
        generate()

        assertDrafts(emptyList())

        sendAllVoucherValueDecisions()

        val feeDecisions = getAllVoucherValueDecisions()
        assertEquals(listOf(sentDecision), feeDecisions)
    }

    private fun day(d: Int) = LocalDate.of(2022, 6, d)

    private fun dateRange(f: Int, t: Int) = DateRange(day(f), day(t))

    private fun assertDrafts(expectedDrafts: List<Pair<DateRange, Boolean>>) {
        val decisions = getAllVoucherValueDecisions()
        assertEquals(
            expectedDrafts.size,
            decisions.filter { it.status == VoucherValueDecisionStatus.DRAFT }.size
        )
        assertEquals(sentDecision, decisions.find { it.status == VoucherValueDecisionStatus.SENT })
        expectedDrafts.forEach { (range, empty) ->
            assertTrue {
                decisions.any {
                    it.status == VoucherValueDecisionStatus.DRAFT &&
                        it.validDuring == range &&
                        it.isEmpty() == empty
                }
            }
        }
    }

    private fun assertFinal(
        expectedFinalState: List<Triple<VoucherValueDecisionStatus, DateRange, Boolean>>
    ) {
        val sent = getAllVoucherValueDecisions()
        assertEquals(expectedFinalState.size, sent.size)
        expectedFinalState.forEach { (status, range, empty) ->
            assertTrue {
                sent.any {
                    it.status == status && it.validDuring == range && (it.isEmpty() == empty)
                }
            }
        }
    }

    private fun getAllVoucherValueDecisions(): List<VoucherValueDecision> {
        return db.read { tx -> tx.findValueDecisionsForChild(testChild_1.id) }
            .sortedBy { it.validFrom }
    }

    private fun generate() {
        db.transaction { tx ->
            generator.generateNewDecisionsForAdult(
                tx,
                now,
                testAdult_1.id,
                originalRange.start.minusYears(5)
            )
        }
    }

    private fun sendAllVoucherValueDecisions() {
        getAllVoucherValueDecisions()
            .filter { it.status == VoucherValueDecisionStatus.DRAFT }
            .map { it.id }
            .let { ids -> decisionController.sendDrafts(dbInstance(), admin, now, ids, null) }
        asyncJobRunner.runPendingJobsSync(now)
    }
}
