// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.feeThresholds
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.insertServiceNeedOptionVoucherValues
import fi.espoo.evaka.insertServiceNeedOptions
import fi.espoo.evaka.invoicing.data.feeDecisionQuery
import fi.espoo.evaka.invoicing.data.getFeeThresholds
import fi.espoo.evaka.invoicing.domain.FeeDecision
import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
import fi.espoo.evaka.invoicing.domain.VoucherValueDecision
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionStatus
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.placement.PlacementType.DAYCARE
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.dev.DevParentship
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testChild_3
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testVoucherDaycare
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class FinanceDecisionGeneratorIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var generator: FinanceDecisionGenerator

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insertGeneralTestFixtures()
            tx.insert(testDaycare)
            tx.insert(testVoucherDaycare)
            tx.insert(testAdult_1, DevPersonType.ADULT)
            listOf(testChild_1, testChild_2, testChild_3).forEach {
                tx.insert(it, DevPersonType.CHILD)
            }
            tx.insert(feeThresholds)
        }
    }

    @Test
    fun `getFeeThresholds works when from is same as validFrom`() {
        val from = LocalDate.of(2000, 1, 1)
        val result = db.read { it.getFeeThresholds(from) }

        assertEquals(1, result.size)
        result[0].let { feeThresholds ->
            assertEquals(from, feeThresholds.validDuring.start)
            assertEquals(null, feeThresholds.validDuring.end)
            assertEquals(BigDecimal("0.1070"), feeThresholds.incomeMultiplier2)
            assertEquals(BigDecimal("0.1070"), feeThresholds.incomeMultiplier(2))
        }
    }

    @Test
    fun `getFeeThresholds works as expected when from is before any fee thresholds configuration`() {
        val from = LocalDate.of(1990, 1, 1)
        val result = db.read { it.getFeeThresholds(from) }

        assertEquals(1, result.size)
        result[0].let { feeThresholds ->
            assertEquals(LocalDate.of(2000, 1, 1), feeThresholds.validDuring.start)
            assertEquals(null, feeThresholds.validDuring.end)
            assertEquals(BigDecimal("0.1070"), feeThresholds.incomeMultiplier2)
        }
    }

    @Test
    fun `family with children placed into voucher and municipal daycares get sibling discounts in fee and voucher value decisions`() {
        db.transaction {
            it.insertServiceNeedOptions()
            it.insertServiceNeedOptionVoucherValues()
        }

        val period = DateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31))
        insertFamilyRelations(
            testAdult_1.id,
            listOf(testChild_1.id, testChild_2.id, testChild_3.id),
            period
        )
        insertPlacement(testChild_1.id, period, DAYCARE, testDaycare.id)
        insertPlacement(testChild_2.id, period, DAYCARE, testVoucherDaycare.id)
        insertPlacement(testChild_3.id, period, DAYCARE, testDaycare.id)

        db.transaction { generator.generateNewDecisionsForAdult(it, testAdult_1.id) }

        val feeDecisions = getAllFeeDecisions()
        assertEquals(1, feeDecisions.size)
        feeDecisions.first().let { decision ->
            assertEquals(FeeDecisionStatus.DRAFT, decision.status)
            assertEquals(period.start, decision.validFrom)
            assertEquals(period.end, decision.validTo)
            assertEquals(43400, decision.totalFee)
            assertEquals(2, decision.children.size)
            decision.children.first().let { part ->
                assertEquals(testChild_3.id, part.child.id)
                assertEquals(testDaycare.id, part.placement.unitId)
                assertEquals(28900, part.fee)
            }
            decision.children.last().let { part ->
                assertEquals(testChild_1.id, part.child.id)
                assertEquals(testDaycare.id, part.placement.unitId)
                assertEquals(14500, part.fee)
            }
        }

        val voucherValueDecisions = getAllVoucherValueDecisions()
        assertEquals(1, voucherValueDecisions.size)
        voucherValueDecisions.first().let { decision ->
            assertEquals(VoucherValueDecisionStatus.DRAFT, decision.status)
            assertEquals(period.start, decision.validFrom)
            assertEquals(period.end, decision.validTo)
            assertEquals(testChild_2.id, decision.child.id)
            assertEquals(testVoucherDaycare.id, decision.placement?.unitId)
            assertEquals(5800, decision.coPayment)
            assertEquals(87000, decision.baseValue)
            assertEquals(BigDecimal("1.00"), decision.serviceNeed?.voucherValueCoefficient)
            assertEquals(87000, decision.voucherValue)
        }
    }

    private fun insertPlacement(
        childId: ChildId,
        period: DateRange,
        type: PlacementType,
        daycareId: DaycareId
    ): PlacementId {
        return db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    type = type,
                    childId = childId,
                    unitId = daycareId,
                    startDate = period.start,
                    endDate = period.end!!
                )
            )
        }
    }

    private fun insertFamilyRelations(
        headOfFamilyId: PersonId,
        childIds: List<ChildId>,
        period: DateRange
    ) {
        db.transaction { tx ->
            childIds.forEach { childId ->
                tx.insert(
                    DevParentship(
                        childId = childId,
                        headOfChildId = headOfFamilyId,
                        startDate = period.start,
                        endDate = period.end!!
                    )
                )
            }
        }
    }

    private fun getAllFeeDecisions(): List<FeeDecision> {
        return db.read { tx ->
                @Suppress("DEPRECATION")
                tx.createQuery(feeDecisionQuery()).mapTo<FeeDecision>().useIterable { rows ->
                    rows
                        .map { row ->
                            row.copy(
                                children = row.children.sortedByDescending { it.child.dateOfBirth }
                            )
                        }
                        .toList()
                }
            }
            .shuffled() // randomize order to expose assumptions
    }

    private fun getAllVoucherValueDecisions(): List<VoucherValueDecision> {
        return db.read { tx ->
                @Suppress("DEPRECATION")
                tx.createQuery("SELECT * FROM voucher_value_decision")
                    .toList<VoucherValueDecision>()
            }
            .shuffled() // randomize order to expose assumptions
    }
}
