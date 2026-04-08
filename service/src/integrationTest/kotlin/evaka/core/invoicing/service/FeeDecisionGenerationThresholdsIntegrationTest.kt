// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.invoicing.service

import evaka.core.FullApplicationTest
import evaka.core.insertServiceNeedOptions
import evaka.core.invoicing.controller.FeeDecisionController
import evaka.core.invoicing.controller.updateFeeThresholdsValidity
import evaka.core.invoicing.data.feeDecisionQuery
import evaka.core.invoicing.domain.FeeDecision
import evaka.core.invoicing.domain.FeeDecisionStatus
import evaka.core.invoicing.testFeeThresholds
import evaka.core.shared.FeeThresholdsId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.auth.UserRole
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevParentship
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.DevPlacement
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.DateRange
import evaka.core.shared.domain.MockEvakaClock
import java.time.LocalDate
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class FeeDecisionGenerationThresholdsIntegrationTest :
    FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var generator: FinanceDecisionGenerator
    @Autowired private lateinit var feeDecisionController: FeeDecisionController
    @Autowired private lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>

    private val area = DevCareArea()
    private val daycare = DevDaycare(areaId = area.id)
    private val adult =
        DevPerson(
            ssn = "010180-1232",
            streetAddress = "Kamreerintie 2",
            postalCode = "02770",
            postOffice = "Espoo",
        )
    private val child = DevPerson(dateOfBirth = LocalDate.of(2020, 1, 1))
    private val employee = DevEmployee()

    val originalRange = dateRange(10, 20)

    val now = MockEvakaClock(2023, 1, 1, 0, 0)
    val admin = AuthenticatedUser.Employee(employee.id, setOf(UserRole.ADMIN))
    val originalFeeThresholdRange = DateRange(originalRange.start.minusYears(10), null)
    val newFeeThresholdRange = DateRange(day(10), null)
    lateinit var sentDecision: FeeDecision
    lateinit var feeThresholdId: FeeThresholdsId

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(adult, DevPersonType.RAW_ROW)
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(employee)
            tx.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycare.id,
                    startDate = originalRange.start,
                    endDate = originalRange.end!!,
                )
            )
            tx.insert(
                DevParentship(
                    childId = child.id,
                    headOfChildId = adult.id,
                    startDate = originalRange.start.minusYears(1),
                    endDate = originalRange.end!!.plusYears(1),
                )
            )
            feeThresholdId =
                tx.insert(testFeeThresholds.copy(validDuring = originalFeeThresholdRange))
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
                DateRange(originalFeeThresholdRange.start, day(9)),
            )
            tx.insert(
                testFeeThresholds.copy(
                    validDuring = newFeeThresholdRange,
                    maxFee = 2 * testFeeThresholds.maxFee,
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
        db.transaction { tx -> generator.generateNewDecisionsForAdult(tx, adult.id) }
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
