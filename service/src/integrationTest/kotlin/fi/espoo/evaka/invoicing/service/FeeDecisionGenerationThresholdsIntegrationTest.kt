// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertServiceNeedOptions
import fi.espoo.evaka.invoicing.controller.FeeDecisionController
import fi.espoo.evaka.invoicing.controller.updateFeeThresholdsValidity
import fi.espoo.evaka.invoicing.data.feeDecisionQuery
import fi.espoo.evaka.invoicing.domain.FeeDecision
import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
import fi.espoo.evaka.invoicing.testFeeThresholds
import fi.espoo.evaka.shared.FeeThresholdsId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertTestParentship
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDecisionMaker_1
import java.time.LocalDate
import java.util.*
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class FeeDecisionGenerationThresholdsIntegrationTest :
    FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var generator: FinanceDecisionGenerator
    @Autowired private lateinit var feeDecisionController: FeeDecisionController
    @Autowired private lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>

    val originalRange = dateRange(10, 20)

    val now = MockEvakaClock(2023, 1, 1, 0, 0)
    val admin = AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.ADMIN))
    val placementId = PlacementId(UUID.randomUUID())
    val originalFeeThresholdRange = DateRange(originalRange.start.minusYears(10), null)
    val newFeeThresholdRange = DateRange(day(10), null)
    lateinit var sentDecision: FeeDecision
    lateinit var feeThresholdId: FeeThresholdsId

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(testAdult_1, DevPersonType.RAW_ROW)
            tx.insert(testChild_1, DevPersonType.CHILD)
            tx.insert(testArea)
            tx.insert(testDaycare)
            tx.insertTestPlacement(
                id = placementId,
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = originalRange.start,
                endDate = originalRange.end!!
            )
            tx.insertTestParentship(
                testAdult_1.id,
                testChild_1.id,
                startDate = originalRange.start.minusYears(1),
                endDate = originalRange.end!!.plusYears(1)
            )
            feeThresholdId =
                tx.insert(testFeeThresholds.copy(validDuring = originalFeeThresholdRange))
            tx.insert(testDecisionMaker_1)
            tx.insertServiceNeedOptions()
        }
        generate()
        sendAllFeeDecisions()
        val decisions = getAllFeeDecisions()
        assertEquals(1, decisions.size)
        assertEquals(FeeDecisionStatus.SENT, decisions.first().status)
        sentDecision = decisions.first()
    }

    @Test
    fun `fee threshold change generates new draft correctly`() {
        db.transaction { tx ->
            tx.updateFeeThresholdsValidity(
                feeThresholdId,
                DateRange(originalFeeThresholdRange.start, day(9))
            )
            tx.insert(
                testFeeThresholds.copy(
                    validDuring = newFeeThresholdRange,
                    maxFee = 2 * testFeeThresholds.maxFee
                )
            )
        }
        generate()
        val drafts = getAllFeeDecisions().filter { it.status == FeeDecisionStatus.DRAFT }
        assertEquals(1, drafts.size)
        with(drafts.first()) {
            assertEquals(day(10), validFrom)
            assertEquals(2 * testFeeThresholds.maxFee, this.feeThresholds.maxFee)
        }
    }

    private fun day(d: Int) = LocalDate.of(2022, 6, d)

    private fun dateRange(f: Int, t: Int) = DateRange(day(f), day(t))

    private fun getAllFeeDecisions(): List<FeeDecision> {
        return db.read { tx -> tx.createQuery(feeDecisionQuery()).toList<FeeDecision>() }
            .sortedBy { it.validFrom }
    }

    private fun generate() {
        db.transaction { tx -> generator.generateNewDecisionsForAdult(tx, testAdult_1.id) }
    }

    private fun sendAllFeeDecisions() {
        getAllFeeDecisions()
            .filter { it.status == FeeDecisionStatus.DRAFT }
            .map { it.id }
            .let { ids ->
                feeDecisionController.confirmFeeDecisionDrafts(dbInstance(), admin, now, ids, null)
            }
        asyncJobRunner.runPendingJobsSync(now)
    }
}
