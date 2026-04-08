// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.invoicing.service

import evaka.core.FullApplicationTest
import evaka.core.daycare.domain.ProviderType
import evaka.core.feeThresholds
import evaka.core.insertServiceNeedOptionVoucherValues
import evaka.core.insertServiceNeedOptions
import evaka.core.invoicing.data.feeDecisionQuery
import evaka.core.invoicing.data.getFeeThresholds
import evaka.core.invoicing.domain.FeeDecision
import evaka.core.invoicing.domain.FeeDecisionStatus
import evaka.core.invoicing.domain.VoucherValueDecision
import evaka.core.invoicing.domain.VoucherValueDecisionStatus
import evaka.core.placement.PlacementType
import evaka.core.placement.PlacementType.DAYCARE
import evaka.core.shared.ChildId
import evaka.core.shared.DaycareId
import evaka.core.shared.PersonId
import evaka.core.shared.PlacementId
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevParentship
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.DevPlacement
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.DateRange
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class FinanceDecisionGeneratorIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var generator: FinanceDecisionGenerator

    private val area = DevCareArea()
    private val daycare = DevDaycare(areaId = area.id)
    private val voucherDaycare =
        DevDaycare(
            areaId = area.id,
            name = "Test Voucher Daycare",
            providerType = ProviderType.PRIVATE_SERVICE_VOUCHER,
        )
    private val adult = DevPerson()
    private val child1 = DevPerson(dateOfBirth = LocalDate.of(2017, 6, 1))
    private val child2 = DevPerson(dateOfBirth = LocalDate.of(2016, 3, 1))
    private val child3 = DevPerson(dateOfBirth = LocalDate.of(2018, 9, 1))

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(voucherDaycare)
            tx.insert(adult, DevPersonType.ADULT)
            listOf(child1, child2, child3).forEach { tx.insert(it, DevPersonType.CHILD) }
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
        insertFamilyRelations(adult.id, listOf(child1.id, child2.id, child3.id), period)
        insertPlacement(child1.id, period, DAYCARE, daycare.id)
        insertPlacement(child2.id, period, DAYCARE, voucherDaycare.id)
        insertPlacement(child3.id, period, DAYCARE, daycare.id)

        db.transaction { generator.generateNewDecisionsForAdult(it, adult.id) }

        val feeDecisions = getAllFeeDecisions()
        assertEquals(1, feeDecisions.size)
        feeDecisions.first().let { decision ->
            assertEquals(FeeDecisionStatus.DRAFT, decision.status)
            assertEquals(period.start, decision.validFrom)
            assertEquals(period.end, decision.validTo)
            assertEquals(43400, decision.totalFee)
            assertEquals(2, decision.children.size)
            decision.children.first().let { part ->
                assertEquals(child3.id, part.child.id)
                assertEquals(daycare.id, part.placement.unitId)
                assertEquals(28900, part.fee)
            }
            decision.children.last().let { part ->
                assertEquals(child1.id, part.child.id)
                assertEquals(daycare.id, part.placement.unitId)
                assertEquals(14500, part.fee)
            }
        }

        val voucherValueDecisions = getAllVoucherValueDecisions()
        assertEquals(1, voucherValueDecisions.size)
        voucherValueDecisions.first().let { decision ->
            assertEquals(VoucherValueDecisionStatus.DRAFT, decision.status)
            assertEquals(period.start, decision.validFrom)
            assertEquals(period.end, decision.validTo)
            assertEquals(child2.id, decision.child.id)
            assertEquals(voucherDaycare.id, decision.placement?.unitId)
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
        daycareId: DaycareId,
    ): PlacementId {
        return db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    type = type,
                    childId = childId,
                    unitId = daycareId,
                    startDate = period.start,
                    endDate = period.end!!,
                )
            )
        }
    }

    private fun insertFamilyRelations(
        headOfFamilyId: PersonId,
        childIds: List<ChildId>,
        period: DateRange,
    ) {
        db.transaction { tx ->
            childIds.forEach { childId ->
                tx.insert(
                    DevParentship(
                        childId = childId,
                        headOfChildId = headOfFamilyId,
                        startDate = period.start,
                        endDate = period.end!!,
                    )
                )
            }
        }
    }

    private fun getAllFeeDecisions(): List<FeeDecision> {
        return db.read { tx ->
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
                tx.createQuery { sql("SELECT * FROM voucher_value_decision") }
                    .toList<VoucherValueDecision>()
            }
            .shuffled() // randomize order to expose assumptions
    }
}
