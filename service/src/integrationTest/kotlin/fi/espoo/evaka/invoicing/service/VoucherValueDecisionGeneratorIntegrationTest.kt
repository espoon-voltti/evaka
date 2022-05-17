// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.assistanceneed.AssistanceNeedRequest
import fi.espoo.evaka.assistanceneed.insertAssistanceNeed
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.invoicing.domain.IncomeCoefficient
import fi.espoo.evaka.invoicing.domain.IncomeEffect
import fi.espoo.evaka.invoicing.domain.IncomeValue
import fi.espoo.evaka.invoicing.domain.VoucherValueDecision
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionStatus
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.ServiceNeedOptionId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.dev.DevIncome
import fi.espoo.evaka.shared.dev.insertTestIncome
import fi.espoo.evaka.shared.dev.insertTestParentship
import fi.espoo.evaka.shared.dev.insertTestPartnership
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.dev.insertTestServiceNeed
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.snDaycareFiveYearOldsFullDayPartWeek25
import fi.espoo.evaka.snDaycareFullDayPartWeek25
import fi.espoo.evaka.snDefaultDaycare
import fi.espoo.evaka.snDefaultFiveYearOldsDaycare
import fi.espoo.evaka.snDefaultFiveYearOldsPartDayDaycare
import fi.espoo.evaka.snDefaultPreparatory
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testAdult_2
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testChild_6
import fi.espoo.evaka.testDecisionMaker_1
import fi.espoo.evaka.testVoucherDaycare
import fi.espoo.evaka.toValueDecisionServiceNeed
import org.jdbi.v3.core.kotlin.mapTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class VoucherValueDecisionGeneratorIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired
    private lateinit var generator: FinanceDecisionGenerator

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insertGeneralTestFixtures()
        }
    }

    @Test
    fun `voucher value decisions get correct base values`() {
        val period = DateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        insertPlacement(testChild_1.id, period, PlacementType.DAYCARE, testVoucherDaycare.id)

        db.transaction { generator.generateNewDecisionsForAdult(it, testAdult_1.id, period.start) }

        val voucherValueDecisions = getAllVoucherValueDecisions().sortedBy { it.validFrom }
        assertEquals(2, voucherValueDecisions.size)
        voucherValueDecisions.first().let { decision ->
            assertEquals(VoucherValueDecisionStatus.DRAFT, decision.status)
            assertEquals(period.start, decision.validFrom)
            assertEquals(testChild_1.dateOfBirth.plusYears(3).minusDays(1), decision.validTo)
            assertEquals(testChild_1.id, decision.child.id)
            assertEquals(134850, decision.baseValue)
            assertEquals(snDefaultDaycare.toValueDecisionServiceNeed(), decision.serviceNeed)
            assertEquals(134850, decision.voucherValue)
        }
        voucherValueDecisions.last().let { decision ->
            assertEquals(VoucherValueDecisionStatus.DRAFT, decision.status)
            assertEquals(testChild_1.dateOfBirth.plusYears(3), decision.validFrom)
            assertEquals(period.end, decision.validTo)
            assertEquals(testChild_1.id, decision.child.id)
            assertEquals(87000, decision.baseValue)
            assertEquals(snDefaultDaycare.toValueDecisionServiceNeed(), decision.serviceNeed)
            assertEquals(87000, decision.voucherValue)
        }
    }

    @Test
    fun `voucher value decisions get correct base values for child not born on first of month`() {
        val period = DateRange(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31))

        val testChild = testChild_6

        assertNotEquals(1, testChild.dateOfBirth.dayOfMonth)

        insertFamilyRelations(testAdult_1.id, listOf(testChild.id), period)
        insertPlacement(testChild.id, period, PlacementType.DAYCARE, testVoucherDaycare.id)

        db.transaction { generator.generateNewDecisionsForAdult(it, testAdult_1.id, period.start) }

        val voucherValueDecisions = getAllVoucherValueDecisions().sortedBy { it.validFrom }
        assertEquals(2, voucherValueDecisions.size)
        val assumedPeriodStart = testChild.dateOfBirth.plusYears(3).plusMonths(1).withDayOfMonth(1)
        voucherValueDecisions.first().let { decision ->
            assertEquals(VoucherValueDecisionStatus.DRAFT, decision.status)
            assertEquals(period.start, decision.validFrom)
            assertEquals(assumedPeriodStart.minusDays(1), decision.validTo)
            assertEquals(testChild.id, decision.child.id)
            assertEquals(134850, decision.baseValue)
            assertEquals(snDefaultDaycare.toValueDecisionServiceNeed(), decision.serviceNeed)
            assertEquals(134850, decision.voucherValue)
        }
        voucherValueDecisions.last().let { decision ->
            assertEquals(VoucherValueDecisionStatus.DRAFT, decision.status)
            assertEquals(assumedPeriodStart, decision.validFrom)
            assertEquals(period.end, decision.validTo)
            assertEquals(testChild.id, decision.child.id)
            assertEquals(87000, decision.baseValue)
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

        db.transaction { generator.generateNewDecisionsForAdult(it, testAdult_1.id, period.start) }

        val voucherValueDecisions = getAllVoucherValueDecisions().sortedBy { it.validFrom }
        assertEquals(2, voucherValueDecisions.size)
        voucherValueDecisions.first().let { decision ->
            assertEquals(VoucherValueDecisionStatus.DRAFT, decision.status)
            assertEquals(period.start, decision.validFrom)
            assertEquals(serviceNeedPeriod.start.minusDays(1), decision.validTo)
            assertEquals(testChild_2.id, decision.child.id)
            assertEquals(87000, decision.baseValue)
            assertEquals(snDefaultDaycare.toValueDecisionServiceNeed(), decision.serviceNeed)
            assertEquals(87000, decision.voucherValue)
        }
        voucherValueDecisions.last().let { decision ->
            assertEquals(VoucherValueDecisionStatus.DRAFT, decision.status)
            assertEquals(serviceNeedPeriod.start, decision.validFrom)
            assertEquals(period.end, decision.validTo)
            assertEquals(testChild_2.id, decision.child.id)
            assertEquals(87000, decision.baseValue)
            assertEquals(snDaycareFullDayPartWeek25.toValueDecisionServiceNeed(), decision.serviceNeed)
            assertEquals(52200, decision.voucherValue)
        }
    }

    @Test
    fun `voucher value decisions with five year olds daycare placement`() {
        val period = DateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_2.id), period)
        insertPlacement(testChild_2.id, period, PlacementType.DAYCARE_FIVE_YEAR_OLDS, testVoucherDaycare.id)

        db.transaction { generator.generateNewDecisionsForAdult(it, testAdult_1.id, period.start) }

        val voucherValueDecisions = getAllVoucherValueDecisions()
        assertEquals(1, voucherValueDecisions.size)
        voucherValueDecisions.first().let { decision ->
            assertEquals(VoucherValueDecisionStatus.DRAFT, decision.status)
            assertEquals(period.start, decision.validFrom)
            assertEquals(period.end, decision.validTo)
            assertEquals(testChild_2.id, decision.child.id)
            assertEquals(87000, decision.baseValue)
            assertEquals(snDefaultFiveYearOldsDaycare.toValueDecisionServiceNeed(), decision.serviceNeed)
            assertEquals(87000, decision.voucherValue)
        }
    }

    @Test
    fun `voucher value decisions with part day five year olds daycare placement`() {
        val period = DateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_2.id), period)
        insertPlacement(testChild_2.id, period, PlacementType.DAYCARE_PART_TIME_FIVE_YEAR_OLDS, testVoucherDaycare.id)

        db.transaction { generator.generateNewDecisionsForAdult(it, testAdult_1.id, period.start) }

        val voucherValueDecisions = getAllVoucherValueDecisions()
        assertEquals(1, voucherValueDecisions.size)
        voucherValueDecisions.first().let { decision ->
            assertEquals(VoucherValueDecisionStatus.DRAFT, decision.status)
            assertEquals(period.start, decision.validFrom)
            assertEquals(period.end, decision.validTo)
            assertEquals(testChild_2.id, decision.child.id)
            assertEquals(87000, decision.baseValue)
            assertEquals(snDefaultFiveYearOldsPartDayDaycare.toValueDecisionServiceNeed(), decision.serviceNeed)
            assertEquals(52200, decision.voucherValue)
        }
    }

    @Test
    fun `voucher value decisions with preschool placement`() {
        val period = DateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_2.id), period)
        insertPlacement(testChild_2.id, period, PlacementType.PRESCHOOL, testVoucherDaycare.id)

        db.transaction { generator.generateNewDecisionsForAdult(it, testAdult_1.id, period.start) }

        val voucherValueDecisions = getAllVoucherValueDecisions()
        assertEquals(0, voucherValueDecisions.size)
    }

    @Test
    fun `voucher value decisions with preparatory placement`() {
        val period = DateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_2.id), period)
        insertPlacement(testChild_2.id, period, PlacementType.PREPARATORY, testVoucherDaycare.id)

        db.transaction { generator.generateNewDecisionsForAdult(it, testAdult_1.id, period.start) }

        val voucherValueDecisions = getAllVoucherValueDecisions()
        assertEquals(1, voucherValueDecisions.size)
        voucherValueDecisions.first().let { decision ->
            assertEquals(VoucherValueDecisionStatus.DRAFT, decision.status)
            assertEquals(period.start, decision.validFrom)
            assertEquals(period.end, decision.validTo)
            assertEquals(testChild_2.id, decision.child.id)
            assertEquals(87000, decision.baseValue)
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

        db.transaction { generator.generateNewDecisionsForAdult(it, testAdult_1.id, period.start) }

        val voucherValueDecisions = getAllVoucherValueDecisions()
        assertEquals(1, voucherValueDecisions.size)
        voucherValueDecisions.first().let { decision ->
            assertEquals(VoucherValueDecisionStatus.DRAFT, decision.status)
            assertEquals(period.start, decision.validFrom)
            assertEquals(period.end, decision.validTo)
            assertEquals(testChild_2.id, decision.child.id)
            assertEquals(87000, decision.baseValue)
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

        db.transaction { generator.generateNewDecisionsForAdult(it, testAdult_1.id, period.start) }

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
            assertEquals(PlacementType.DAYCARE_FIVE_YEAR_OLDS, decision.placement?.type)
            assertEquals(11600, decision.coPayment)
        }
    }

    @Test
    fun `voucher value decisions with assistance capacity factor`() {
        val period = DateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_2.id), period)
        insertPlacement(testChild_2.id, period, PlacementType.DAYCARE, testVoucherDaycare.id)
        insertAssistanceNeed(testChild_2.id, period.asFiniteDateRange()!!, 3.0)

        db.transaction { generator.generateNewDecisionsForAdult(it, testAdult_1.id, period.start) }

        val voucherValueDecisions = getAllVoucherValueDecisions()
        assertEquals(1, voucherValueDecisions.size)
        voucherValueDecisions.first().let { decision ->
            assertEquals(VoucherValueDecisionStatus.DRAFT, decision.status)
            assertEquals(period.start, decision.validFrom)
            assertEquals(period.end, decision.validTo)
            assertEquals(testChild_2.id, decision.child.id)
            assertEquals(87000, decision.baseValue)
            assertEquals(BigDecimal("3.00"), decision.capacityFactor)
            assertEquals(snDefaultDaycare.toValueDecisionServiceNeed(), decision.serviceNeed)
            assertEquals(snDefaultDaycare.toValueDecisionServiceNeed(), decision.serviceNeed)
            assertEquals(261000, decision.voucherValue)
            assertEquals(28900, decision.coPayment)
        }
    }

    @Test
    fun `voucher value decisions with changing partners`() {
        val firstPeriod = DateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 31))
        val secondPeriod = DateRange(LocalDate.of(2020, 2, 1), LocalDate.of(2020, 12, 31))
        val wholePeriod = firstPeriod.copy(end = secondPeriod.end)
        insertFamilyRelations(testAdult_1.id, listOf(testChild_2.id), wholePeriod)
        insertPlacement(testChild_2.id, wholePeriod, PlacementType.DAYCARE, testVoucherDaycare.id)
        insertPartnership(testAdult_1.id, testAdult_2.id, firstPeriod)

        db.transaction { generator.generateNewDecisionsForAdult(it, testAdult_2.id, firstPeriod.start) }

        val voucherValueDecisions = getAllVoucherValueDecisions().sortedBy { it.validFrom }
        assertEquals(2, voucherValueDecisions.size)
        voucherValueDecisions.first().let { decision ->
            assertEquals(VoucherValueDecisionStatus.DRAFT, decision.status)
            assertEquals(firstPeriod, DateRange(decision.validFrom, decision.validTo))
            assertEquals(3, decision.familySize)
            assertEquals(testAdult_1.id, decision.headOfFamilyId)
            assertEquals(testAdult_2.id, decision.partnerId)
            assertEquals(testChild_2.id, decision.child.id)
        }
        voucherValueDecisions.last().let { decision ->
            assertEquals(VoucherValueDecisionStatus.DRAFT, decision.status)
            assertEquals(secondPeriod, DateRange(decision.validFrom, decision.validTo))
            assertEquals(2, decision.familySize)
            assertEquals(testAdult_1.id, decision.headOfFamilyId)
            assertEquals(null, decision.partnerId)
            assertEquals(testChild_2.id, decision.child.id)
        }
    }

    @Test
    fun `voucher value decisions with child income`() {
        val period = DateRange(LocalDate.of(2021, 8, 1), LocalDate.of(2021, 12, 31))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id, testChild_2.id), period)
        insertPlacement(testChild_1.id, period, PlacementType.DAYCARE, testVoucherDaycare.id)
        insertPlacement(
            testChild_2.id, period.copy(start = period.start.plusMonths(1)),
            PlacementType.DAYCARE_FIVE_YEAR_OLDS, testVoucherDaycare.id
        )

        // Adult minimal income
        insertIncome(testAdult_1.id, 310200, period)
        insertIncome(testChild_1.id, 600000, period)

        db.transaction { generator.generateNewDecisionsForAdult(it, testAdult_1.id, period.start) }

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
            assertEquals(4200, decision.baseCoPayment)
            // testChild_2 is older than testChild_1
            assertEquals(50, decision.siblingDiscount)
            assertEquals(PlacementType.DAYCARE_FIVE_YEAR_OLDS, decision.placement?.type)
            assertEquals(0, decision.coPayment)
        }
    }

    @Test
    fun `voucher value decision is generated for fridge family with two head of families with different children for placed child`() {
        val period = DateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31))

        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        insertFamilyRelations(testAdult_2.id, listOf(testChild_2.id), period)
        insertPartnership(testAdult_1.id, testAdult_2.id, period)
        insertPlacement(testChild_1.id, period, PlacementType.DAYCARE_FIVE_YEAR_OLDS, testVoucherDaycare.id)

        db.transaction { generator.generateNewDecisionsForChild(it, testChild_1.id, period.start) }
        assertEquals(1, getAllVoucherValueDecisions().size)
    }

    @Test
    fun `voucher value decision is generated for fridge family with two head of families with different children for non-placed child`() {
        val period = DateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31))

        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        insertFamilyRelations(testAdult_2.id, listOf(testChild_2.id), period)
        insertPartnership(testAdult_1.id, testAdult_2.id, period)
        insertPlacement(testChild_1.id, period, PlacementType.DAYCARE_FIVE_YEAR_OLDS, testVoucherDaycare.id)

        db.transaction { generator.generateNewDecisionsForChild(it, testChild_2.id, period.start) }
        assertEquals(1, getAllVoucherValueDecisions().size)
    }

    private fun insertPlacement(childId: ChildId, period: DateRange, type: PlacementType, daycareId: DaycareId): PlacementId {
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

    private fun insertFamilyRelations(headOfFamilyId: PersonId, childIds: List<ChildId>, period: DateRange) {
        db.transaction { tx ->
            childIds.forEach { childId ->
                tx.insertTestParentship(headOfFamilyId, childId, startDate = period.start, endDate = period.end!!)
            }
        }
    }

    private fun insertPartnership(adultId1: PersonId, adultId2: PersonId, period: DateRange) {
        db.transaction { tx ->
            tx.insertTestPartnership(adultId1, adultId2, startDate = period.start, endDate = period.end!!)
        }
    }

    private fun insertServiceNeed(
        placementId: PlacementId,
        period: FiniteDateRange,
        optionId: ServiceNeedOptionId
    ) {
        db.transaction { tx ->
            tx.insertTestServiceNeed(
                EvakaUserId(testDecisionMaker_1.id.raw),
                placementId,
                period,
                optionId
            )
        }
    }

    private fun insertAssistanceNeed(
        childId: ChildId,
        period: FiniteDateRange,
        capacityFactor: Double
    ) {
        db.transaction { tx ->
            tx.insertAssistanceNeed(
                AuthenticatedUser.Employee(testDecisionMaker_1.id, roles = setOf()),
                childId,
                AssistanceNeedRequest(period.start, period.end, capacityFactor)
            )
        }
    }

    private fun getAllVoucherValueDecisions(): List<VoucherValueDecision> {
        return db.read { tx ->
            tx.createQuery("SELECT * FROM voucher_value_decision")
                .mapTo<VoucherValueDecision>()
                .toList()
        }.shuffled() // randomize order to expose assumptions
    }

    private fun insertIncome(adultId: PersonId, amount: Int, period: DateRange) {
        db.transaction { tx ->
            tx.insertTestIncome(
                DevIncome(
                    personId = adultId,
                    validFrom = period.start,
                    validTo = period.end,
                    effect = IncomeEffect.INCOME,
                    data = mapOf("MAIN_INCOME" to IncomeValue(amount, IncomeCoefficient.MONTHLY_NO_HOLIDAY_BONUS, 1)),
                    updatedBy = EvakaUserId(testDecisionMaker_1.id.raw)
                )
            )
        }
    }
}
