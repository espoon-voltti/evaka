// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.invoicing.domain.VoucherValueDecision
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionStatus
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.dev.insertTestNewServiceNeed
import fi.espoo.evaka.shared.dev.insertTestParentship
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.snDaycareFiveYearOldsFullDayPartWeek25
import fi.espoo.evaka.snDaycareFullDayPartWeek25
import fi.espoo.evaka.snDefaultDaycare
import fi.espoo.evaka.snDefaultFiveYearOldsDaycare
import fi.espoo.evaka.snDefaultFiveYearOldsPartDayDaycare
import fi.espoo.evaka.snDefaultPreparatory
import fi.espoo.evaka.snDefaultPreschool
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testDecisionMaker_1
import fi.espoo.evaka.testVoucherDaycare
import fi.espoo.evaka.toValueDecisionServiceNeed
import org.jdbi.v3.core.kotlin.mapTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class VoucherValueDecisionGeneratorIntegrationTest : FullApplicationTest() {
    @Autowired
    private lateinit var generator: FinanceDecisionGenerator

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
    fun `voucher value decisions get correct age coefficients`() {
        val period = DateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        insertPlacement(testChild_1.id, period, PlacementType.DAYCARE, testVoucherDaycare.id)

        db.transaction { generator.generateNewDecisionsForAdult(it, testAdult_1.id, period) }

        val voucherValueDecisions = getAllVoucherValueDecisions()
        assertEquals(2, voucherValueDecisions.size)
        voucherValueDecisions.first().let { decision ->
            assertEquals(VoucherValueDecisionStatus.DRAFT, decision.status)
            assertEquals(period.start, decision.validFrom)
            assertEquals(testChild_1.dateOfBirth.plusYears(3).minusDays(1), decision.validTo)
            assertEquals(testChild_1.id, decision.child.id)
            assertEquals(87000, decision.baseValue)
            assertEquals(BigDecimal("1.55"), decision.ageCoefficient)
            assertEquals(snDefaultDaycare.toValueDecisionServiceNeed(), decision.serviceNeed)
            assertEquals(134850, decision.voucherValue)
        }
        voucherValueDecisions.last().let { decision ->
            assertEquals(VoucherValueDecisionStatus.DRAFT, decision.status)
            assertEquals(testChild_1.dateOfBirth.plusYears(3), decision.validFrom)
            assertEquals(period.end, decision.validTo)
            assertEquals(testChild_1.id, decision.child.id)
            assertEquals(87000, decision.baseValue)
            assertEquals(BigDecimal("1.00"), decision.ageCoefficient)
            assertEquals(snDefaultDaycare.toValueDecisionServiceNeed(), decision.serviceNeed)
            assertEquals(87000, decision.voucherValue)
        }
    }

    @Test
    fun `voucher value decisions with part time service need`() {
        val period = DateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_2.id), period)
        val placementId = insertPlacement(testChild_2.id, period, PlacementType.DAYCARE, testVoucherDaycare.id)
        val serviceNeedPeriod = period.copy(start = period.start.plusMonths(5))
        insertServiceNeed(placementId, serviceNeedPeriod.asFiniteDateRange()!!, snDaycareFullDayPartWeek25.id)

        db.transaction { generator.generateNewDecisionsForAdult(it, testAdult_1.id, period) }

        val voucherValueDecisions = getAllVoucherValueDecisions()
        assertEquals(2, voucherValueDecisions.size)
        voucherValueDecisions.first().let { decision ->
            assertEquals(VoucherValueDecisionStatus.DRAFT, decision.status)
            assertEquals(period.start, decision.validFrom)
            assertEquals(serviceNeedPeriod.start.minusDays(1), decision.validTo)
            assertEquals(testChild_2.id, decision.child.id)
            assertEquals(87000, decision.baseValue)
            assertEquals(BigDecimal("1.00"), decision.ageCoefficient)
            assertEquals(snDefaultDaycare.toValueDecisionServiceNeed(), decision.serviceNeed)
            assertEquals(87000, decision.voucherValue)
        }
        voucherValueDecisions.last().let { decision ->
            assertEquals(VoucherValueDecisionStatus.DRAFT, decision.status)
            assertEquals(serviceNeedPeriod.start, decision.validFrom)
            assertEquals(period.end, decision.validTo)
            assertEquals(testChild_2.id, decision.child.id)
            assertEquals(87000, decision.baseValue)
            assertEquals(BigDecimal("1.00"), decision.ageCoefficient)
            assertEquals(snDaycareFullDayPartWeek25.toValueDecisionServiceNeed(), decision.serviceNeed)
            assertEquals(52200, decision.voucherValue)
        }
    }

    @Test
    fun `voucher value decisions with five year olds daycare placement`() {
        val period = DateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_2.id), period)
        insertPlacement(testChild_2.id, period, PlacementType.DAYCARE_FIVE_YEAR_OLDS, testVoucherDaycare.id)

        db.transaction { generator.generateNewDecisionsForAdult(it, testAdult_1.id, period) }

        val voucherValueDecisions = getAllVoucherValueDecisions()
        assertEquals(1, voucherValueDecisions.size)
        voucherValueDecisions.first().let { decision ->
            assertEquals(VoucherValueDecisionStatus.DRAFT, decision.status)
            assertEquals(period.start, decision.validFrom)
            assertEquals(period.end, decision.validTo)
            assertEquals(testChild_2.id, decision.child.id)
            assertEquals(87000, decision.baseValue)
            assertEquals(BigDecimal("1.00"), decision.ageCoefficient)
            assertEquals(snDefaultFiveYearOldsDaycare.toValueDecisionServiceNeed(), decision.serviceNeed)
            assertEquals(87000, decision.voucherValue)
        }
    }

    @Test
    fun `voucher value decisions with part day five year olds daycare placement`() {
        val period = DateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_2.id), period)
        insertPlacement(testChild_2.id, period, PlacementType.DAYCARE_PART_TIME_FIVE_YEAR_OLDS, testVoucherDaycare.id)

        db.transaction { generator.generateNewDecisionsForAdult(it, testAdult_1.id, period) }

        val voucherValueDecisions = getAllVoucherValueDecisions()
        assertEquals(1, voucherValueDecisions.size)
        voucherValueDecisions.first().let { decision ->
            assertEquals(VoucherValueDecisionStatus.DRAFT, decision.status)
            assertEquals(period.start, decision.validFrom)
            assertEquals(period.end, decision.validTo)
            assertEquals(testChild_2.id, decision.child.id)
            assertEquals(87000, decision.baseValue)
            assertEquals(BigDecimal("1.00"), decision.ageCoefficient)
            assertEquals(snDefaultFiveYearOldsPartDayDaycare.toValueDecisionServiceNeed(), decision.serviceNeed)
            assertEquals(52200, decision.voucherValue)
        }
    }

    @Test
    fun `voucher value decisions with preschool placement`() {
        val period = DateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_2.id), period)
        insertPlacement(testChild_2.id, period, PlacementType.PRESCHOOL, testVoucherDaycare.id)

        db.transaction { generator.generateNewDecisionsForAdult(it, testAdult_1.id, period) }

        val voucherValueDecisions = getAllVoucherValueDecisions()
        assertEquals(1, voucherValueDecisions.size)
        voucherValueDecisions.first().let { decision ->
            assertEquals(VoucherValueDecisionStatus.DRAFT, decision.status)
            assertEquals(period.start, decision.validFrom)
            assertEquals(period.end, decision.validTo)
            assertEquals(testChild_2.id, decision.child.id)
            assertEquals(87000, decision.baseValue)
            assertEquals(BigDecimal("1.00"), decision.ageCoefficient)
            assertEquals(snDefaultPreschool.toValueDecisionServiceNeed(), decision.serviceNeed)
            assertEquals(43500, decision.voucherValue)
        }
    }

    @Test
    fun `voucher value decisions with preparatory placement`() {
        val period = DateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_2.id), period)
        insertPlacement(testChild_2.id, period, PlacementType.PREPARATORY, testVoucherDaycare.id)

        db.transaction { generator.generateNewDecisionsForAdult(it, testAdult_1.id, period) }

        val voucherValueDecisions = getAllVoucherValueDecisions()
        assertEquals(1, voucherValueDecisions.size)
        voucherValueDecisions.first().let { decision ->
            assertEquals(VoucherValueDecisionStatus.DRAFT, decision.status)
            assertEquals(period.start, decision.validFrom)
            assertEquals(period.end, decision.validTo)
            assertEquals(testChild_2.id, decision.child.id)
            assertEquals(87000, decision.baseValue)
            assertEquals(BigDecimal("1.00"), decision.ageCoefficient)
            assertEquals(snDefaultPreparatory.toValueDecisionServiceNeed(), decision.serviceNeed)
            assertEquals(43500, decision.voucherValue)
        }
    }

    @Test
    fun `voucher value decisions with five year old in daycare with part time hours per week`() {
        val period = DateRange(LocalDate.of(2021, 8, 1), LocalDate.of(2021, 12, 31))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_2.id), period)
        val placementId = insertPlacement(testChild_2.id, period, PlacementType.DAYCARE, testVoucherDaycare.id)
        insertServiceNeed(placementId, period.asFiniteDateRange()!!, snDaycareFiveYearOldsFullDayPartWeek25.id)

        db.transaction { generator.generateNewDecisionsForAdult(it, testAdult_1.id, period) }

        val voucherValueDecisions = getAllVoucherValueDecisions()
        assertEquals(1, voucherValueDecisions.size)
        voucherValueDecisions.first().let { decision ->
            assertEquals(VoucherValueDecisionStatus.DRAFT, decision.status)
            assertEquals(period.start, decision.validFrom)
            assertEquals(period.end, decision.validTo)
            assertEquals(testChild_2.id, decision.child.id)
            assertEquals(87000, decision.baseValue)
            assertEquals(BigDecimal("1.00"), decision.ageCoefficient)
            assertEquals(snDaycareFiveYearOldsFullDayPartWeek25.toValueDecisionServiceNeed(), decision.serviceNeed)
            assertEquals(52200, decision.voucherValue)
        }
    }

    @Test
    fun `voucher value decisions for a family of two children placed in voucher units`() {
        val period = DateRange(LocalDate.of(2021, 8, 1), LocalDate.of(2021, 12, 31))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id, testChild_2.id), period)
        insertPlacement(testChild_1.id, period, PlacementType.DAYCARE, testVoucherDaycare.id)
        insertPlacement(
            testChild_2.id, period.copy(start = period.start.plusMonths(1)),
            PlacementType.DAYCARE_FIVE_YEAR_OLDS, testVoucherDaycare.id
        )

        db.transaction { generator.generateNewDecisionsForAdult(it, testAdult_1.id, period) }

        val voucherValueDecisions = getAllVoucherValueDecisions().sortedBy { it.validFrom }
        assertEquals(2, voucherValueDecisions.size)
        voucherValueDecisions.first().let { decision ->
            assertEquals(VoucherValueDecisionStatus.DRAFT, decision.status)
            assertEquals(period.start, decision.validFrom)
            assertEquals(period.end, decision.validTo)
            assertEquals(testChild_1.id, decision.child.id)
            assertEquals(87000, decision.voucherValue)
            assertEquals(28900, decision.coPayment)
        }
        voucherValueDecisions.last().let { decision ->
            assertEquals(VoucherValueDecisionStatus.DRAFT, decision.status)
            assertEquals(period.start.plusMonths(1), decision.validFrom)
            assertEquals(period.end, decision.validTo)
            assertEquals(testChild_2.id, decision.child.id)
            assertEquals(87000, decision.voucherValue)
            assertEquals(28900, decision.baseCoPayment)
            // testChild_2 is older than testChild_1
            assertEquals(50, decision.siblingDiscount)
            assertEquals(PlacementType.DAYCARE_FIVE_YEAR_OLDS, decision.placement.type)
            assertEquals(11600, decision.coPayment)
        }
    }

    private fun insertPlacement(childId: UUID, period: DateRange, type: PlacementType, daycareId: UUID): UUID {
        return db.transaction { tx ->
            tx.insertTestPlacement(
                childId = childId,
                unitId = daycareId,
                startDate = period.start,
                endDate = period.end!!,
                type = type
            )
        }
    }

    private fun insertFamilyRelations(headOfFamilyId: UUID, childIds: List<UUID>, period: DateRange) {
        db.transaction { tx ->
            childIds.forEach { childId ->
                tx.insertTestParentship(headOfFamilyId, childId, startDate = period.start, endDate = period.end!!)
            }
        }
    }

    private fun insertServiceNeed(
        placementId: UUID,
        period: FiniteDateRange,
        optionId: UUID
    ) {
        db.transaction { tx ->
            tx.insertTestNewServiceNeed(
                testDecisionMaker_1.id,
                placementId,
                period,
                optionId
            )
        }
    }

    private fun getAllVoucherValueDecisions(): List<VoucherValueDecision> {
        return db.read { tx ->
            tx.createQuery("SELECT * FROM voucher_value_decision")
                .mapTo<VoucherValueDecision>()
                .toList()
        }
    }
}
