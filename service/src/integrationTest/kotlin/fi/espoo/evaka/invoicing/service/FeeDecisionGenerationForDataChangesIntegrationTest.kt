// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.feeThresholds
import fi.espoo.evaka.insertServiceNeedOptions
import fi.espoo.evaka.invoicing.controller.FeeDecisionController
import fi.espoo.evaka.invoicing.data.feeDecisionQuery
import fi.espoo.evaka.invoicing.domain.FeeDecision
import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
import fi.espoo.evaka.invoicing.domain.IncomeEffect
import fi.espoo.evaka.pis.deleteParentship
import fi.espoo.evaka.placement.cancelPlacement
import fi.espoo.evaka.placement.updatePlacementStartAndEndDate
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevIncome
import fi.espoo.evaka.shared.dev.DevParentship
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.MockEvakaClock
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class FeeDecisionGenerationForDataChangesIntegrationTest :
    FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var generator: FinanceDecisionGenerator
    @Autowired private lateinit var feeDecisionController: FeeDecisionController
    @Autowired private lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>

    private val area = DevCareArea()
    private val daycare = DevDaycare(areaId = area.id)
    private val employee = DevEmployee()
    private val adult =
        DevPerson(
            ssn = "010180-1232",
            streetAddress = "Kamreerintie 2",
            postalCode = "02770",
            postOffice = "Espoo",
        )
    private val child1 = DevPerson(dateOfBirth = LocalDate.of(2017, 6, 1))
    private val child2 = DevPerson(dateOfBirth = LocalDate.of(2016, 3, 1))

    val originalRange = dateRange(10, 20)

    val now = MockEvakaClock(2023, 1, 1, 0, 0)
    val admin = AuthenticatedUser.Employee(employee.id, setOf(UserRole.ADMIN))
    private val placement =
        DevPlacement(
            childId = child1.id,
            unitId = daycare.id,
            startDate = originalRange.start,
            endDate = originalRange.end,
        )
    lateinit var sentDecision: FeeDecision

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(employee)
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(adult, DevPersonType.ADULT)
            tx.insert(child1, DevPersonType.CHILD)
            tx.insert(child2, DevPersonType.CHILD)
            tx.insert(feeThresholds)
            tx.insertServiceNeedOptions()
            tx.insert(placement)
            tx.insert(
                DevParentship(
                    childId = child1.id,
                    headOfChildId = adult.id,
                    startDate = originalRange.start.minusYears(1),
                    endDate = originalRange.end.plusYears(1),
                )
            )
        }
        generate()
        sendAllFeeDecisions()
        val decisions = getAllFeeDecisions()
        assertEquals(1, decisions.size)
        assertEquals(FeeDecisionStatus.SENT, decisions.first().status)
        sentDecision = decisions.first()
    }

    @Test
    fun `no changes`() {
        generate()

        val feeDecisions = getAllFeeDecisions()
        assertEquals(listOf(sentDecision), feeDecisions)
    }

    @Test
    fun `start date moves earlier`() {
        db.transaction { tx ->
            tx.updatePlacementStartAndEndDate(
                placement.id,
                day(5),
                day(20),
                now.now(),
                employee.evakaUserId,
            )
        }
        generate()

        assertDrafts(listOf(dateRange(5, 9) to 1))

        sendAllFeeDecisions()

        assertFinal(
            listOf(
                Triple(FeeDecisionStatus.SENT, dateRange(5, 9), 1),
                Triple(FeeDecisionStatus.SENT, dateRange(10, 20), 1),
            )
        )
    }

    @Test
    fun `start date moves later`() {
        db.transaction { tx ->
            tx.updatePlacementStartAndEndDate(
                placement.id,
                day(15),
                day(20),
                now.now(),
                employee.evakaUserId,
            )
        }
        generate()

        assertDrafts(listOf(dateRange(10, 14) to 0, dateRange(15, 20) to 1))

        sendAllFeeDecisions()

        assertFinal(
            listOf(
                Triple(FeeDecisionStatus.ANNULLED, dateRange(10, 20), 1),
                Triple(FeeDecisionStatus.SENT, dateRange(15, 20), 1),
            )
        )
    }

    @Test
    fun `end date moves earlier`() {
        db.transaction { tx ->
            tx.updatePlacementStartAndEndDate(
                placement.id,
                day(10),
                day(15),
                now.now(),
                employee.evakaUserId,
            )
        }
        generate()

        assertDrafts(listOf(dateRange(16, 20) to 0))

        sendAllFeeDecisions()

        assertFinal(listOf(Triple(FeeDecisionStatus.SENT, dateRange(10, 15), 1)))
    }

    @Test
    fun `end date moves later`() {
        db.transaction { tx ->
            tx.updatePlacementStartAndEndDate(
                placement.id,
                day(10),
                day(25),
                now.now(),
                employee.evakaUserId,
            )
        }
        generate()

        assertDrafts(listOf(dateRange(21, 25) to 1))

        sendAllFeeDecisions()

        assertFinal(
            listOf(
                Triple(FeeDecisionStatus.SENT, dateRange(10, 20), 1),
                Triple(FeeDecisionStatus.SENT, dateRange(21, 25), 1),
            )
        )
    }

    @Test
    fun `start date moves earlier and end date moves earlier`() {
        db.transaction { tx ->
            tx.updatePlacementStartAndEndDate(
                placement.id,
                day(5),
                day(15),
                now.now(),
                employee.evakaUserId,
            )
        }
        generate()

        assertDrafts(listOf(dateRange(5, 9) to 1, dateRange(16, 20) to 0))

        sendAllFeeDecisions()

        assertFinal(
            listOf(
                Triple(FeeDecisionStatus.SENT, dateRange(5, 9), 1),
                Triple(FeeDecisionStatus.SENT, dateRange(10, 15), 1),
            )
        )
    }

    @Test
    fun `start date moves earlier and end date moves later`() {
        db.transaction { tx ->
            tx.updatePlacementStartAndEndDate(
                placement.id,
                day(5),
                day(25),
                now.now(),
                employee.evakaUserId,
            )
        }
        generate()

        assertDrafts(listOf(dateRange(5, 9) to 1, dateRange(21, 25) to 1))

        sendAllFeeDecisions()

        assertFinal(
            listOf(
                Triple(FeeDecisionStatus.SENT, dateRange(5, 9), 1),
                Triple(FeeDecisionStatus.SENT, dateRange(10, 20), 1),
                Triple(FeeDecisionStatus.SENT, dateRange(21, 25), 1),
            )
        )
    }

    @Test
    fun `start date moves later and end date moves earlier`() {
        db.transaction { tx ->
            tx.updatePlacementStartAndEndDate(
                placement.id,
                day(15),
                day(15),
                now.now(),
                employee.evakaUserId,
            )
        }
        generate()

        assertDrafts(listOf(dateRange(10, 14) to 0, dateRange(15, 15) to 1, dateRange(16, 20) to 0))

        sendAllFeeDecisions()

        assertFinal(
            listOf(
                Triple(FeeDecisionStatus.ANNULLED, dateRange(10, 20), 1),
                Triple(FeeDecisionStatus.SENT, dateRange(15, 15), 1),
            )
        )
    }

    @Test
    fun `start date moves later and end date moves later`() {
        db.transaction { tx ->
            tx.updatePlacementStartAndEndDate(
                placement.id,
                day(15),
                day(25),
                now.now(),
                employee.evakaUserId,
            )
        }
        generate()

        assertDrafts(listOf(dateRange(10, 14) to 0, dateRange(15, 25) to 1))

        sendAllFeeDecisions()

        assertFinal(
            listOf(
                Triple(FeeDecisionStatus.ANNULLED, dateRange(10, 20), 1),
                Triple(FeeDecisionStatus.SENT, dateRange(15, 25), 1),
            )
        )
    }

    @Test
    fun `break in placement on days 14-15`() {
        db.transaction { tx ->
            tx.updatePlacementStartAndEndDate(
                placement.id,
                day(10),
                day(13),
                now.now(),
                employee.evakaUserId,
            )
            tx.insert(
                DevPlacement(
                    childId = child1.id,
                    unitId = daycare.id,
                    startDate = day(16),
                    endDate = originalRange.end,
                )
            )
        }

        generate()

        assertDrafts(listOf(dateRange(14, 15) to 0, dateRange(16, 20) to 1))

        sendAllFeeDecisions()

        assertFinal(
            listOf(
                Triple(FeeDecisionStatus.SENT, dateRange(10, 13), 1),
                Triple(FeeDecisionStatus.SENT, dateRange(16, 20), 1),
            )
        )
    }

    @Test
    fun `placement is removed`() {
        db.transaction { tx ->
            tx.cancelPlacement(
                now.now(),
                AuthenticatedUser.SystemInternalUser.evakaUserId,
                placement.id,
            )
        }
        generate()
        assertDrafts(listOf(dateRange(10, 20) to 0))
        sendAllFeeDecisions()
        assertFinal(listOf(Triple(FeeDecisionStatus.ANNULLED, dateRange(10, 20), 1)))
    }

    @Test
    fun `end date moves later then draft is ignored`() {
        db.transaction { tx ->
            tx.updatePlacementStartAndEndDate(
                placement.id,
                day(10),
                day(25),
                now.now(),
                employee.evakaUserId,
            )
        }
        generate()
        assertDrafts(listOf(dateRange(21, 25) to 1))

        ignoreDrafts()
        assertDrafts(emptyList())

        // regenerating does not bring back ignored drafts
        generate()
        assertDrafts(emptyList())

        // non identical drafts are not ignored
        val secondParentshipId =
            db.transaction { tx ->
                tx.insert(
                    DevParentship(
                        childId = child2.id,
                        headOfChildId = adult.id,
                        startDate = originalRange.start.minusYears(1),
                        endDate = originalRange.end.plusYears(1),
                    )
                )
            }
        generate()
        assertDrafts(listOf(dateRange(10, 25) to 1))

        // going back to ignored state keeps the draft ignored
        db.transaction { tx -> tx.deleteParentship(secondParentshipId) }
        generate()
        assertDrafts(emptyList())

        unignoreIgnoredDrafts()
        assertDrafts(listOf(dateRange(21, 25) to 1))
    }

    @Test
    fun `Incomplete income is equal to non-existing and does not cause new draft`() {
        db.transaction { tx ->
            tx.insert(
                DevIncome(
                    personId = adult.id,
                    validFrom = originalRange.start,
                    validTo = originalRange.end,
                    data = emptyMap(),
                    effect = IncomeEffect.INCOMPLETE,
                    modifiedBy = employee.evakaUserId,
                )
            )
        }
        generate()

        assertDrafts(emptyList())

        sendAllFeeDecisions()

        val feeDecisions = getAllFeeDecisions()
        assertEquals(listOf(sentDecision), feeDecisions)
    }

    private fun day(d: Int) = LocalDate.of(2022, 6, d)

    private fun dateRange(f: Int, t: Int) = FiniteDateRange(day(f), day(t))

    private fun assertDrafts(expectedDrafts: List<Pair<FiniteDateRange, Int>>) {
        val decisions = getAllFeeDecisions()
        assertEquals(
            expectedDrafts.size,
            decisions.filter { it.status == FeeDecisionStatus.DRAFT }.size,
        )
        assertEquals(sentDecision, decisions.find { it.status == FeeDecisionStatus.SENT })
        expectedDrafts.forEach { (range, children) ->
            assertTrue {
                decisions.any {
                    it.status == FeeDecisionStatus.DRAFT &&
                        it.validDuring == range &&
                        it.children.size == children
                }
            }
        }
    }

    private fun assertFinal(
        expectedFinalState: List<Triple<FeeDecisionStatus, FiniteDateRange, Int>>
    ) {
        val sent = getAllFeeDecisions()
        assertEquals(expectedFinalState.size, sent.size)
        expectedFinalState.forEach { (status, range, children) ->
            assertTrue {
                sent.any {
                    it.status == status && it.validDuring == range && it.children.size == children
                }
            }
        }
    }

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

    private fun ignoreDrafts() {
        getAllFeeDecisions()
            .filter { it.status == FeeDecisionStatus.DRAFT }
            .map { it.id }
            .let { ids ->
                feeDecisionController.ignoreFeeDecisionDrafts(dbInstance(), admin, now, ids)
            }
        asyncJobRunner.runPendingJobsSync(now)
    }

    private fun unignoreIgnoredDrafts() {
        getAllFeeDecisions()
            .filter { it.status == FeeDecisionStatus.IGNORED }
            .map { it.id }
            .let { ids ->
                feeDecisionController.unignoreFeeDecisionDrafts(dbInstance(), admin, now, ids)
            }
        asyncJobRunner.runPendingJobsSync(now)
    }
}
