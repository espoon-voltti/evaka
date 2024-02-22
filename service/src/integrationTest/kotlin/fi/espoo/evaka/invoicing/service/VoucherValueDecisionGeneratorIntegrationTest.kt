// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.assistanceneed.vouchercoefficient.AssistanceNeedVoucherCoefficientRequest
import fi.espoo.evaka.assistanceneed.vouchercoefficient.insertAssistanceNeedVoucherCoefficient
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.invoicing.calculateMonthlyAmount
import fi.espoo.evaka.invoicing.controller.VoucherValueDecisionController
import fi.espoo.evaka.invoicing.domain.FeeAlterationType
import fi.espoo.evaka.invoicing.domain.FeeThresholds
import fi.espoo.evaka.invoicing.domain.IncomeCoefficient
import fi.espoo.evaka.invoicing.domain.IncomeEffect
import fi.espoo.evaka.invoicing.domain.IncomeValue
import fi.espoo.evaka.invoicing.domain.VoucherValueDecision
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionDifference
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionStatus
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.serviceNeedOptionVoucherValueCoefficients
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.FeeAlterationId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.ServiceNeedOptionId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevAssistanceFactor
import fi.espoo.evaka.shared.dev.DevFeeAlteration
import fi.espoo.evaka.shared.dev.DevIncome
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertTestParentship
import fi.espoo.evaka.shared.dev.insertTestPartnership
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.dev.insertTestServiceNeed
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.snDaycareFiveYearOldsFullDayPartWeek25
import fi.espoo.evaka.snDaycareFullDay35
import fi.espoo.evaka.snDaycareFullDayPartWeek25
import fi.espoo.evaka.snDefaultDaycare
import fi.espoo.evaka.snDefaultFiveYearOldsDaycare
import fi.espoo.evaka.snDefaultFiveYearOldsPartDayDaycare
import fi.espoo.evaka.snDefaultPreparatory
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testAdult_2
import fi.espoo.evaka.testAdult_3
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testChild_6
import fi.espoo.evaka.testDecisionMaker_1
import fi.espoo.evaka.testDecisionMaker_2
import fi.espoo.evaka.testVoucherDaycare
import fi.espoo.evaka.testVoucherDaycare2
import fi.espoo.evaka.toValueDecisionServiceNeed
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class VoucherValueDecisionGeneratorIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var generator: FinanceDecisionGenerator
    @Autowired private lateinit var voucherValueDecisionController: VoucherValueDecisionController
    @Autowired private lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>
    @Autowired
    private lateinit var coefficientMultiplierProvider: IncomeCoefficientMultiplierProvider

    val clock = MockEvakaClock(2021, 1, 1, 15, 0)

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx -> tx.insertGeneralTestFixtures() }
    }

    @Test
    fun `voucher value decisions get correct base values`() {
        val period = DateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        insertPlacement(testChild_1.id, period, PlacementType.DAYCARE, testVoucherDaycare.id)

        db.transaction { generator.generateNewDecisionsForAdult(it, testAdult_1.id) }

        val voucherValueDecisions = getAllVoucherValueDecisions().sortedBy { it.validFrom }
        assertEquals(2, voucherValueDecisions.size)
        voucherValueDecisions.first().let { decision ->
            assertEquals(VoucherValueDecisionStatus.DRAFT, decision.status)
            assertEquals(period.start, decision.validFrom)
            assertEquals(testChild_1.dateOfBirth.plusYears(3).minusDays(1), decision.validTo)
            assertEquals(testChild_1.id, decision.child.id)
            assertEquals(134850, decision.baseValue)
            assertEquals(snDefaultDaycare.toValueDecisionServiceNeed(), decision.serviceNeed)
            assertEquals(134900, decision.voucherValue)
            assertEquals(28900, decision.finalCoPayment)
        }
        voucherValueDecisions.last().let { decision ->
            assertEquals(VoucherValueDecisionStatus.DRAFT, decision.status)
            assertEquals(testChild_1.dateOfBirth.plusYears(3), decision.validFrom)
            assertEquals(period.end, decision.validTo)
            assertEquals(testChild_1.id, decision.child.id)
            assertEquals(87000, decision.baseValue)
            assertEquals(snDefaultDaycare.toValueDecisionServiceNeed(), decision.serviceNeed)
            assertEquals(87000, decision.voucherValue)
            assertEquals(28900, decision.finalCoPayment)
        }
    }

    @Test
    fun `siblings in broken family`() {
        val dad = testAdult_1
        val mom = testAdult_2
        val youngerChild = testChild_1
        val olderChild = testChild_2
        val partnershipPeriod = DateRange(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 7, 31))
        insertPartnership(dad.id, mom.id, partnershipPeriod, clock.now())
        insertFamilyRelations(
            dad.id,
            listOf(youngerChild.id),
            DateRange(LocalDate.of(2021, 1, 1), youngerChild.dateOfBirth.plusYears(18))
        )
        insertFamilyRelations(
            mom.id,
            listOf(olderChild.id),
            DateRange(LocalDate.of(2021, 1, 1), olderChild.dateOfBirth.plusYears(18))
        )
        val placementPeriod = DateRange(LocalDate.of(2021, 3, 1), LocalDate.of(2021, 10, 31))
        insertPlacement(
            childId = olderChild.id,
            period = placementPeriod,
            type = PlacementType.DAYCARE,
            daycareId = testVoucherDaycare.id
        )
        insertPlacement(
            childId = youngerChild.id,
            period = placementPeriod,
            type = PlacementType.DAYCARE,
            daycareId = testVoucherDaycare.id
        )

        db.transaction {
            generator.generateNewDecisionsForAdult(it, dad.id)
            generator.generateNewDecisionsForAdult(it, mom.id)
        }

        val voucherValueDecisions =
            getAllVoucherValueDecisions()
                .sortedWith(
                    compareBy<VoucherValueDecision> { it.validFrom }
                        .thenByDescending { it.child.dateOfBirth }
                )
        assertEquals(4, voucherValueDecisions.size)
        voucherValueDecisions[0].also { decision ->
            assertEquals(placementPeriod.intersection(partnershipPeriod), decision.validDuring)
            assertEquals(dad.id, decision.headOfFamilyId)
            assertEquals(mom.id, decision.partnerId)
            assertEquals(youngerChild.id, decision.child.id)
            assertEquals(4, decision.familySize)
        }
        voucherValueDecisions[1].also { decision ->
            assertEquals(placementPeriod.intersection(partnershipPeriod), decision.validDuring)
            assertEquals(dad.id, decision.headOfFamilyId)
            assertEquals(mom.id, decision.partnerId)
            assertEquals(olderChild.id, decision.child.id)
            assertEquals(4, decision.familySize)
        }
        val placementAfterSeparation =
            placementPeriod.copy(start = partnershipPeriod.end!!.plusDays(1))
        voucherValueDecisions[2].also { decision ->
            assertEquals(placementAfterSeparation, decision.validDuring)
            assertEquals(dad.id, decision.headOfFamilyId)
            assertEquals(null, decision.partnerId)
            assertEquals(youngerChild.id, decision.child.id)
            assertEquals(2, decision.familySize)
        }
        voucherValueDecisions[3].also { decision ->
            assertEquals(placementAfterSeparation, decision.validDuring)
            assertEquals(mom.id, decision.headOfFamilyId)
            assertEquals(null, decision.partnerId)
            assertEquals(olderChild.id, decision.child.id)
            assertEquals(2, decision.familySize)
        }
    }

    @Test
    fun `voucher value decisions works as expected with fee alterations`() {
        val period = DateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        insertPlacement(testChild_1.id, period, PlacementType.DAYCARE, testVoucherDaycare.id)
        insertFeeAlteration(testChild_1.id, 50.0, period)

        db.transaction { generator.generateNewDecisionsForAdult(it, testAdult_1.id) }

        val voucherValueDecisions = getAllVoucherValueDecisions().sortedBy { it.validFrom }
        assertEquals(2, voucherValueDecisions.size)
        voucherValueDecisions.first().let { decision ->
            assertEquals(VoucherValueDecisionStatus.DRAFT, decision.status)
            assertEquals(period.start, decision.validFrom)
            assertEquals(testChild_1.dateOfBirth.plusYears(3).minusDays(1), decision.validTo)
            assertEquals(testChild_1.id, decision.child.id)
            assertEquals(134850, decision.baseValue)
            assertEquals(snDefaultDaycare.toValueDecisionServiceNeed(), decision.serviceNeed)
            assertEquals(134900, decision.voucherValue)
            assertEquals(14450, decision.finalCoPayment)
        }
        voucherValueDecisions.last().let { decision ->
            assertEquals(VoucherValueDecisionStatus.DRAFT, decision.status)
            assertEquals(testChild_1.dateOfBirth.plusYears(3), decision.validFrom)
            assertEquals(period.end, decision.validTo)
            assertEquals(testChild_1.id, decision.child.id)
            assertEquals(87000, decision.baseValue)
            assertEquals(snDefaultDaycare.toValueDecisionServiceNeed(), decision.serviceNeed)
            assertEquals(87000, decision.voucherValue)
            assertEquals(14450, decision.finalCoPayment)
        }
    }

    @Test
    fun `voucher value decisions get correct base values for child not born on first of month`() {
        val period = DateRange(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31))

        val testChild = testChild_6

        assertNotEquals(1, testChild.dateOfBirth.dayOfMonth)

        insertFamilyRelations(testAdult_1.id, listOf(testChild.id), period)
        insertPlacement(testChild.id, period, PlacementType.DAYCARE, testVoucherDaycare.id)

        db.transaction { generator.generateNewDecisionsForAdult(it, testAdult_1.id) }

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
            assertEquals(134900, decision.voucherValue)
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
        val placementId =
            insertPlacement(testChild_2.id, period, PlacementType.DAYCARE, testVoucherDaycare.id)
        val serviceNeedPeriod = period.copy(start = period.start.plusMonths(5))
        insertServiceNeed(
            placementId,
            serviceNeedPeriod.asFiniteDateRange()!!,
            snDaycareFullDayPartWeek25.id
        )

        db.transaction { generator.generateNewDecisionsForAdult(it, testAdult_1.id) }

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
            assertEquals(
                snDaycareFullDayPartWeek25.toValueDecisionServiceNeed(),
                decision.serviceNeed
            )
            assertEquals(52200, decision.voucherValue)
        }
    }

    @Test
    fun `voucher value decisions with five year olds daycare placement`() {
        val period = DateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_2.id), period)
        insertPlacement(
            testChild_2.id,
            period,
            PlacementType.DAYCARE_FIVE_YEAR_OLDS,
            testVoucherDaycare.id
        )

        db.transaction { generator.generateNewDecisionsForAdult(it, testAdult_1.id) }

        val voucherValueDecisions = getAllVoucherValueDecisions()
        assertEquals(1, voucherValueDecisions.size)
        voucherValueDecisions.first().let { decision ->
            assertEquals(VoucherValueDecisionStatus.DRAFT, decision.status)
            assertEquals(period.start, decision.validFrom)
            assertEquals(period.end, decision.validTo)
            assertEquals(testChild_2.id, decision.child.id)
            assertEquals(87000, decision.baseValue)
            assertEquals(
                snDefaultFiveYearOldsDaycare.toValueDecisionServiceNeed(),
                decision.serviceNeed
            )
            assertEquals(87000, decision.voucherValue)
        }
    }

    @Test
    fun `voucher value decisions with part day five year olds daycare placement`() {
        val period = DateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_2.id), period)
        insertPlacement(
            testChild_2.id,
            period,
            PlacementType.DAYCARE_PART_TIME_FIVE_YEAR_OLDS,
            testVoucherDaycare.id
        )

        db.transaction { generator.generateNewDecisionsForAdult(it, testAdult_1.id) }

        val voucherValueDecisions = getAllVoucherValueDecisions()
        assertEquals(1, voucherValueDecisions.size)
        voucherValueDecisions.first().let { decision ->
            assertEquals(VoucherValueDecisionStatus.DRAFT, decision.status)
            assertEquals(period.start, decision.validFrom)
            assertEquals(period.end, decision.validTo)
            assertEquals(testChild_2.id, decision.child.id)
            assertEquals(87000, decision.baseValue)
            assertEquals(
                snDefaultFiveYearOldsPartDayDaycare.toValueDecisionServiceNeed(),
                decision.serviceNeed
            )
            assertEquals(52200, decision.voucherValue)
        }
    }

    @Test
    fun `voucher value decisions with preschool placement`() {
        val period = DateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_2.id), period)
        insertPlacement(testChild_2.id, period, PlacementType.PRESCHOOL, testVoucherDaycare.id)

        db.transaction { generator.generateNewDecisionsForAdult(it, testAdult_1.id) }

        val voucherValueDecisions = getAllVoucherValueDecisions()
        assertEquals(0, voucherValueDecisions.size)
    }

    @Test
    fun `voucher value decisions with preparatory placement`() {
        val period = DateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_2.id), period)
        insertPlacement(testChild_2.id, period, PlacementType.PREPARATORY, testVoucherDaycare.id)

        db.transaction { generator.generateNewDecisionsForAdult(it, testAdult_1.id) }

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
        val placementId =
            insertPlacement(testChild_2.id, period, PlacementType.DAYCARE, testVoucherDaycare.id)
        insertServiceNeed(
            placementId,
            period.asFiniteDateRange()!!,
            snDaycareFiveYearOldsFullDayPartWeek25.id
        )

        db.transaction { generator.generateNewDecisionsForAdult(it, testAdult_1.id) }

        val voucherValueDecisions = getAllVoucherValueDecisions()
        assertEquals(1, voucherValueDecisions.size)
        voucherValueDecisions.first().let { decision ->
            assertEquals(VoucherValueDecisionStatus.DRAFT, decision.status)
            assertEquals(period.start, decision.validFrom)
            assertEquals(period.end, decision.validTo)
            assertEquals(testChild_2.id, decision.child.id)
            assertEquals(87000, decision.baseValue)
            assertEquals(
                snDaycareFiveYearOldsFullDayPartWeek25.toValueDecisionServiceNeed(),
                decision.serviceNeed
            )
            assertEquals(52200, decision.voucherValue)
        }
    }

    @Test
    fun `voucher value decisions for a family of two children placed in voucher units`() {
        val period = DateRange(LocalDate.of(2021, 8, 1), LocalDate.of(2021, 12, 31))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id, testChild_2.id), period)
        insertPlacement(testChild_1.id, period, PlacementType.DAYCARE, testVoucherDaycare.id)
        insertPlacement(
            testChild_2.id,
            period.copy(start = period.start.plusMonths(1)),
            PlacementType.DAYCARE_FIVE_YEAR_OLDS,
            testVoucherDaycare.id
        )

        db.transaction { generator.generateNewDecisionsForAdult(it, testAdult_1.id) }

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
    fun `twins are ordered consistently for sibling discount`() {
        // Younger per SSN
        val twin1 =
            testChild_1.copy(
                id = ChildId(UUID.randomUUID()),
                dateOfBirth = LocalDate.of(2019, 1, 1),
                ssn = "010117A902X"
            )

        // Older
        val twin2 =
            testChild_2.copy(
                id = ChildId(UUID.randomUUID()),
                dateOfBirth = LocalDate.of(2019, 1, 1),
                ssn = "010117A901W"
            )

        db.transaction { tx -> listOf(twin1, twin2).forEach { tx.insert(it, DevPersonType.CHILD) } }

        val placementPeriod = DateRange(LocalDate.of(2021, 8, 1), LocalDate.of(2021, 12, 31))
        insertPlacement(twin1.id, placementPeriod, PlacementType.DAYCARE, testVoucherDaycare.id)
        insertPlacement(twin2.id, placementPeriod, PlacementType.DAYCARE, testVoucherDaycare.id)
        insertFamilyRelations(testAdult_1.id, listOf(twin1.id, twin2.id), placementPeriod)

        db.transaction {
            generator.generateNewDecisionsForChild(it, twin1.id)
            generator.generateNewDecisionsForChild(it, twin2.id)
        }

        val decisions = getAllVoucherValueDecisions()
        assertEquals(2, decisions.size)
        decisions
            .find { it.child.id == twin1.id }!!
            .let { decision -> assertEquals(0, decision.siblingDiscount) }
        decisions
            .find { it.child.id == twin2.id }!!
            .let { decision -> assertEquals(50, decision.siblingDiscount) }
    }

    @Test
    fun `assistance capacity factor does not affect voucher value decisions`() {
        val period = DateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_2.id), period)
        insertPlacement(testChild_2.id, period, PlacementType.DAYCARE, testVoucherDaycare.id)
        db.transaction {
            it.insert(DevAssistanceFactor(childId = testChild_2.id, capacityFactor = 3.0))
        }

        db.transaction { generator.generateNewDecisionsForAdult(it, testAdult_1.id) }

        val voucherValueDecisions = getAllVoucherValueDecisions()
        assertEquals(1, voucherValueDecisions.size)
        voucherValueDecisions.first().let { decision ->
            assertEquals(VoucherValueDecisionStatus.DRAFT, decision.status)
            assertEquals(period.start, decision.validFrom)
            assertEquals(period.end, decision.validTo)
            assertEquals(testChild_2.id, decision.child.id)
            assertEquals(87000, decision.baseValue)
            assertEquals(BigDecimal("1.00"), decision.assistanceNeedCoefficient)
            assertEquals(snDefaultDaycare.toValueDecisionServiceNeed(), decision.serviceNeed)
            assertEquals(snDefaultDaycare.toValueDecisionServiceNeed(), decision.serviceNeed)
            assertEquals(87000, decision.voucherValue)
            assertEquals(28900, decision.coPayment)
        }
    }

    @Test
    fun `voucher value decisions with assistance need voucher coefficient`() {
        val period = DateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_2.id), period)
        insertPlacement(testChild_2.id, period, PlacementType.DAYCARE, testVoucherDaycare.id)
        insertAssistanceNeedCoefficient(testChild_2.id, period.asFiniteDateRange()!!, 3.55)

        db.transaction { generator.generateNewDecisionsForAdult(it, testAdult_1.id) }

        val voucherValueDecisions = getAllVoucherValueDecisions()
        assertEquals(1, voucherValueDecisions.size)
        voucherValueDecisions.first().let { decision ->
            assertEquals(VoucherValueDecisionStatus.DRAFT, decision.status)
            assertEquals(period.start, decision.validFrom)
            assertEquals(period.end, decision.validTo)
            assertEquals(testChild_2.id, decision.child.id)
            assertEquals(87000, decision.baseValue)
            assertEquals(BigDecimal("3.55"), decision.assistanceNeedCoefficient)
            assertEquals(snDefaultDaycare.toValueDecisionServiceNeed(), decision.serviceNeed)
            assertEquals(snDefaultDaycare.toValueDecisionServiceNeed(), decision.serviceNeed)
            assertEquals(308900, decision.voucherValue)
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
        insertPartnership(testAdult_1.id, testAdult_2.id, firstPeriod, clock.now())

        db.transaction { generator.generateNewDecisionsForAdult(it, testAdult_2.id) }

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
            testChild_2.id,
            period.copy(start = period.start.plusMonths(1)),
            PlacementType.DAYCARE_FIVE_YEAR_OLDS,
            testVoucherDaycare.id
        )

        // Adult minimal income
        insertIncome(testAdult_1.id, 310200, period)
        insertIncome(testChild_1.id, 600000, period)

        db.transaction { generator.generateNewDecisionsForAdult(it, testAdult_1.id) }

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
        insertPartnership(testAdult_1.id, testAdult_2.id, period, clock.now())
        insertPlacement(
            testChild_1.id,
            period,
            PlacementType.DAYCARE_FIVE_YEAR_OLDS,
            testVoucherDaycare.id
        )

        db.transaction { generator.generateNewDecisionsForChild(it, testChild_1.id) }
        assertEquals(1, getAllVoucherValueDecisions().size)
    }

    @Test
    fun `voucher value decision is generated for fridge family with two head of families with different children for non-placed child`() {
        val period = DateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31))

        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        insertFamilyRelations(testAdult_2.id, listOf(testChild_2.id), period)
        insertPartnership(testAdult_1.id, testAdult_2.id, period, clock.now())
        insertPlacement(
            testChild_1.id,
            period,
            PlacementType.DAYCARE_FIVE_YEAR_OLDS,
            testVoucherDaycare.id
        )

        db.transaction {
            generator.generateNewDecisionsForChild(it, testChild_1.id)
            generator.generateNewDecisionsForChild(it, testChild_2.id)
        }
        assertEquals(1, getAllVoucherValueDecisions().size)
    }

    @Test
    fun `head of family difference`() {
        val period = DateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31))
        val subPeriod1 = period.copy(end = LocalDate.of(2022, 6, 30))
        val subPeriod2 = period.copy(start = LocalDate.of(2022, 7, 1))
        val clock = MockEvakaClock(HelsinkiDateTime.of(period.start, LocalTime.MIN))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), subPeriod1)
        insertFamilyRelations(testAdult_2.id, listOf(testChild_1.id), subPeriod2)
        insertPlacement(testChild_1.id, period, PlacementType.DAYCARE, testVoucherDaycare.id)

        db.transaction { tx -> generator.generateNewDecisionsForChild(tx, testChild_1.id) }

        assertThat(getAllVoucherValueDecisions())
            .extracting(
                { DateRange(it.validFrom, it.validTo) },
                { it.difference },
                { it.headOfFamilyId }
            )
            .containsExactlyInAnyOrder(
                Tuple(subPeriod1, emptySet<VoucherValueDecisionDifference>(), testAdult_1.id),
                Tuple(subPeriod2, setOf(VoucherValueDecisionDifference.GUARDIANS), testAdult_2.id)
            )
    }

    @Test
    fun `partner difference`() {
        val period = DateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31))
        val subPeriod1 = period.copy(end = LocalDate.of(2022, 6, 30))
        val subPeriod2 = period.copy(start = LocalDate.of(2022, 7, 1))
        val clock = MockEvakaClock(HelsinkiDateTime.of(period.start, LocalTime.MIN))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        insertPlacement(testChild_1.id, period, PlacementType.DAYCARE, testVoucherDaycare.id)
        insertPartnership(testAdult_1.id, testAdult_2.id, subPeriod1, clock.now())
        insertPartnership(testAdult_1.id, testAdult_3.id, subPeriod2, clock.now())

        db.transaction { tx -> generator.generateNewDecisionsForAdult(tx, testAdult_1.id) }

        assertThat(getAllVoucherValueDecisions())
            .extracting(
                { DateRange(it.validFrom, it.validTo) },
                { it.difference },
                { it.partnerId }
            )
            .containsExactlyInAnyOrder(
                Tuple(subPeriod1, emptySet<VoucherValueDecisionDifference>(), testAdult_2.id),
                Tuple(subPeriod2, setOf(VoucherValueDecisionDifference.GUARDIANS), testAdult_3.id)
            )
    }

    @Test
    fun `head of family & partner switch difference`() {
        val period = DateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31))
        val subPeriod1 = period.copy(end = LocalDate.of(2022, 6, 30))
        val subPeriod2 = period.copy(start = LocalDate.of(2022, 7, 1))
        val clock = MockEvakaClock(HelsinkiDateTime.of(period.start, LocalTime.MIN))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), subPeriod1)
        insertFamilyRelations(testAdult_2.id, listOf(testChild_1.id), subPeriod2)
        insertPlacement(testChild_1.id, period, PlacementType.DAYCARE, testVoucherDaycare.id)
        insertPartnership(testAdult_1.id, testAdult_2.id, period, clock.now())
        insertIncome(testAdult_1.id, 10000, period)
        insertIncome(testAdult_2.id, 20000, period)

        db.transaction { tx -> generator.generateNewDecisionsForChild(tx, testChild_1.id) }

        val expectedHeadOfFamily = testAdult_1
        assertThat(getAllVoucherValueDecisions())
            .extracting(
                { DateRange(it.validFrom, it.validTo) },
                { it.difference },
                { it.headOfFamilyId }
            )
            .containsExactly(
                Tuple(period, emptySet<VoucherValueDecisionDifference>(), expectedHeadOfFamily.id)
            )
    }

    @Test
    fun `head of family income difference`() {
        val period = DateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31))
        val subPeriod1 = period.copy(end = LocalDate.of(2022, 6, 30))
        val subPeriod2 = period.copy(start = LocalDate.of(2022, 7, 1))
        val clock = MockEvakaClock(HelsinkiDateTime.of(period.start, LocalTime.MIN))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        insertPlacement(testChild_1.id, period, PlacementType.DAYCARE, testVoucherDaycare.id)
        insertIncome(testAdult_1.id, 10000, subPeriod2)

        db.transaction { tx -> generator.generateNewDecisionsForAdult(tx, testAdult_1.id) }

        assertThat(getAllVoucherValueDecisions())
            .extracting({ DateRange(it.validFrom, it.validTo) }, { it.difference })
            .containsExactlyInAnyOrder(
                Tuple(subPeriod1, emptySet<VoucherValueDecisionDifference>()),
                Tuple(
                    subPeriod2,
                    setOf(
                        VoucherValueDecisionDifference.INCOME,
                        VoucherValueDecisionDifference.CO_PAYMENT,
                        VoucherValueDecisionDifference.FINAL_CO_PAYMENT
                    )
                )
            )
    }

    @Test
    fun `partner income difference`() {
        val period = DateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31))
        val subPeriod1 = period.copy(end = LocalDate.of(2022, 6, 30))
        val subPeriod2 = period.copy(start = LocalDate.of(2022, 7, 1))
        val clock = MockEvakaClock(HelsinkiDateTime.of(period.start, LocalTime.MIN))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        insertPlacement(testChild_1.id, period, PlacementType.DAYCARE, testVoucherDaycare.id)
        insertPartnership(testAdult_1.id, testAdult_2.id, period, clock.now())
        insertIncome(testAdult_2.id, 10000, subPeriod2)

        db.transaction { tx -> generator.generateNewDecisionsForAdult(tx, testAdult_1.id) }

        assertThat(getAllVoucherValueDecisions())
            .extracting({ DateRange(it.validFrom, it.validTo) }, { it.difference })
            .containsExactlyInAnyOrder(
                Tuple(subPeriod1, emptySet<VoucherValueDecisionDifference>()),
                Tuple(subPeriod2, setOf(VoucherValueDecisionDifference.INCOME))
            )
    }

    @Test
    fun `child income difference`() {
        val period = DateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31))
        val subPeriod1 = period.copy(end = LocalDate.of(2022, 6, 30))
        val subPeriod2 = period.copy(start = LocalDate.of(2022, 7, 1))
        val clock = MockEvakaClock(HelsinkiDateTime.of(period.start, LocalTime.MIN))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        insertPlacement(testChild_1.id, period, PlacementType.DAYCARE, testVoucherDaycare.id)
        insertIncome(testChild_1.id, 10000, subPeriod2)

        db.transaction { tx -> generator.generateNewDecisionsForAdult(tx, testAdult_1.id) }

        assertThat(getAllVoucherValueDecisions())
            .extracting({ DateRange(it.validFrom, it.validTo) }, { it.difference })
            .containsExactlyInAnyOrder(
                Tuple(subPeriod1, emptySet<VoucherValueDecisionDifference>()),
                Tuple(subPeriod2, setOf(VoucherValueDecisionDifference.INCOME))
            )
    }

    @Test
    fun `family size difference`() {
        val period = DateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31))
        val subPeriod1 = period.copy(end = LocalDate.of(2022, 6, 30))
        val subPeriod2 = period.copy(start = LocalDate.of(2022, 7, 1))
        val clock = MockEvakaClock(HelsinkiDateTime.of(period.start, LocalTime.MIN))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        insertPlacement(testChild_1.id, period, PlacementType.DAYCARE, testVoucherDaycare.id)
        insertFamilyRelations(testAdult_1.id, listOf(testChild_2.id), subPeriod2)

        db.transaction { tx -> generator.generateNewDecisionsForAdult(tx, testAdult_1.id) }

        assertThat(getAllVoucherValueDecisions())
            .extracting(
                { DateRange(it.validFrom, it.validTo) },
                { it.difference },
                { it.familySize }
            )
            .containsExactlyInAnyOrder(
                Tuple(subPeriod1, emptySet<VoucherValueDecisionDifference>(), 2),
                Tuple(
                    subPeriod2,
                    setOf(
                        VoucherValueDecisionDifference.FAMILY_SIZE,
                        VoucherValueDecisionDifference.FEE_THRESHOLDS
                    ),
                    3
                )
            )
    }

    @Test
    fun `placement unit difference`() {
        val period = DateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31))
        val subPeriod1 = period.copy(end = LocalDate.of(2022, 6, 30))
        val subPeriod2 = period.copy(start = LocalDate.of(2022, 7, 1))
        val clock = MockEvakaClock(HelsinkiDateTime.of(period.start, LocalTime.MIN))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        insertPlacement(testChild_1.id, subPeriod1, PlacementType.DAYCARE, testVoucherDaycare.id)
        insertPlacement(testChild_1.id, subPeriod2, PlacementType.DAYCARE, testVoucherDaycare2.id)

        db.transaction { tx -> generator.generateNewDecisionsForAdult(tx, testAdult_1.id) }

        assertThat(getAllVoucherValueDecisions())
            .extracting(
                { DateRange(it.validFrom, it.validTo) },
                { it.difference },
                { it.placement?.unitId }
            )
            .containsExactlyInAnyOrder(
                Tuple(
                    subPeriod1,
                    emptySet<VoucherValueDecisionDifference>(),
                    testVoucherDaycare.id
                ),
                Tuple(
                    subPeriod2,
                    setOf(VoucherValueDecisionDifference.PLACEMENT),
                    testVoucherDaycare2.id
                )
            )
    }

    @Test
    fun `placement type difference`() {
        val period = DateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31))
        val subPeriod1 = period.copy(end = LocalDate.of(2022, 6, 30))
        val subPeriod2 = period.copy(start = LocalDate.of(2022, 7, 1))
        val clock = MockEvakaClock(HelsinkiDateTime.of(period.start, LocalTime.MIN))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        insertPlacement(testChild_1.id, subPeriod1, PlacementType.DAYCARE, testVoucherDaycare.id)
        insertPlacement(
            testChild_1.id,
            subPeriod2,
            PlacementType.DAYCARE_PART_TIME,
            testVoucherDaycare.id
        )

        db.transaction { tx -> generator.generateNewDecisionsForAdult(tx, testAdult_1.id) }

        assertThat(getAllVoucherValueDecisions())
            .extracting(
                { DateRange(it.validFrom, it.validTo) },
                { it.difference },
                { it.placement?.type }
            )
            .containsExactlyInAnyOrder(
                Tuple(
                    subPeriod1,
                    emptySet<VoucherValueDecisionDifference>(),
                    PlacementType.DAYCARE
                ),
                Tuple(
                    subPeriod2,
                    setOf(
                        VoucherValueDecisionDifference.PLACEMENT,
                        VoucherValueDecisionDifference.SERVICE_NEED,
                        VoucherValueDecisionDifference.VOUCHER_VALUE
                    ),
                    PlacementType.DAYCARE_PART_TIME
                )
            )
    }

    @Test
    fun `service need difference`() {
        val period = FiniteDateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31))
        val subPeriod1 = period.copy(end = LocalDate.of(2022, 6, 30))
        val subPeriod2 = period.copy(start = LocalDate.of(2022, 7, 1))
        val clock = MockEvakaClock(HelsinkiDateTime.of(period.start, LocalTime.MIN))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period.asDateRange())
        val placementId =
            insertPlacement(
                testChild_1.id,
                period.asDateRange(),
                PlacementType.DAYCARE,
                testVoucherDaycare.id
            )
        insertServiceNeed(placementId, subPeriod1, snDefaultDaycare.id)
        insertServiceNeed(placementId, subPeriod2, snDaycareFullDay35.id)

        db.transaction { tx -> generator.generateNewDecisionsForAdult(tx, testAdult_1.id) }

        assertThat(getAllVoucherValueDecisions())
            .extracting(
                { DateRange(it.validFrom, it.validTo) },
                { it.difference },
                { it.serviceNeed?.voucherValueCoefficient }
            )
            .containsExactlyInAnyOrder(
                Tuple(
                    subPeriod1.asDateRange(),
                    emptySet<VoucherValueDecisionDifference>(),
                    serviceNeedOptionVoucherValueCoefficients[snDefaultDaycare.id]
                ),
                Tuple(
                    subPeriod2.asDateRange(),
                    setOf(VoucherValueDecisionDifference.SERVICE_NEED),
                    serviceNeedOptionVoucherValueCoefficients[snDaycareFullDay35.id]
                )
            )
    }

    @Test
    fun `sibling discount difference`() {
        val period = DateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31))
        val subPeriod1 = period.copy(end = LocalDate.of(2022, 6, 30))
        val subPeriod2 = period.copy(start = LocalDate.of(2022, 7, 1))
        val clock = MockEvakaClock(HelsinkiDateTime.of(period.start, LocalTime.MIN))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_2.id), period)
        insertPlacement(testChild_2.id, period, PlacementType.DAYCARE, testVoucherDaycare.id)
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        insertPlacement(testChild_1.id, subPeriod2, PlacementType.DAYCARE, testVoucherDaycare.id)

        db.transaction { tx -> generator.generateNewDecisionsForAdult(tx, testAdult_1.id) }

        assertThat(getAllVoucherValueDecisions())
            .extracting(
                { it.child.dateOfBirth },
                { DateRange(it.validFrom, it.validTo) },
                { it.difference },
                { it.siblingDiscount }
            )
            .containsExactlyInAnyOrder(
                Tuple(
                    testChild_2.dateOfBirth,
                    subPeriod1,
                    emptySet<VoucherValueDecisionDifference>(),
                    0
                ),
                Tuple(
                    testChild_2.dateOfBirth,
                    subPeriod2,
                    setOf(
                        VoucherValueDecisionDifference.SIBLING_DISCOUNT,
                        VoucherValueDecisionDifference.CO_PAYMENT,
                        VoucherValueDecisionDifference.FINAL_CO_PAYMENT
                    ),
                    50
                ),
                Tuple(
                    testChild_1.dateOfBirth,
                    subPeriod2,
                    emptySet<VoucherValueDecisionDifference>(),
                    0
                )
            )
    }

    @Test
    fun `fee alterations difference`() {
        val period = DateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31))
        val subPeriod1 = period.copy(end = LocalDate.of(2022, 6, 30))
        val subPeriod2 = period.copy(start = LocalDate.of(2022, 7, 1))
        val clock = MockEvakaClock(HelsinkiDateTime.of(period.start, LocalTime.MIN))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        insertPlacement(testChild_1.id, period, PlacementType.DAYCARE, testVoucherDaycare.id)
        insertFeeAlteration(testChild_1.id, 50.0, subPeriod2)

        db.transaction { tx -> generator.generateNewDecisionsForAdult(tx, testAdult_1.id) }

        assertThat(getAllVoucherValueDecisions())
            .extracting({ DateRange(it.validFrom, it.validTo) }, { it.difference })
            .containsExactlyInAnyOrder(
                Tuple(subPeriod1, emptySet<VoucherValueDecisionDifference>()),
                Tuple(
                    subPeriod2,
                    setOf(
                        VoucherValueDecisionDifference.FEE_ALTERATIONS,
                        VoucherValueDecisionDifference.FINAL_CO_PAYMENT
                    )
                )
            )
    }

    @Test
    fun `base value difference`() {
        val period = DateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31))
        val subPeriod1 = period.copy(end = LocalDate.of(2020, 5, 31))
        val subPeriod2 = period.copy(start = LocalDate.of(2020, 6, 1)) // 3rd birthday
        val clock = MockEvakaClock(HelsinkiDateTime.of(period.start, LocalTime.MIN))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        insertPlacement(testChild_1.id, period, PlacementType.DAYCARE, testVoucherDaycare.id)

        db.transaction { tx -> generator.generateNewDecisionsForAdult(tx, testAdult_1.id) }

        assertThat(getAllVoucherValueDecisions())
            .extracting({ DateRange(it.validFrom, it.validTo) }, { it.difference })
            .containsExactlyInAnyOrder(
                Tuple(subPeriod1, emptySet<VoucherValueDecisionDifference>()),
                Tuple(
                    subPeriod2,
                    setOf(
                        VoucherValueDecisionDifference.BASE_VALUE,
                        VoucherValueDecisionDifference.VOUCHER_VALUE
                    )
                )
            )
    }

    @Test
    fun `fee thresholds difference`() {
        val period = DateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31))
        val subPeriod1 = period.copy(end = LocalDate.of(2022, 6, 30))
        val subPeriod2 = period.copy(start = LocalDate.of(2022, 7, 1))
        val clock = MockEvakaClock(HelsinkiDateTime.of(period.start, LocalTime.MIN))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        insertPlacement(testChild_1.id, period, PlacementType.DAYCARE, testVoucherDaycare.id)
        db.transaction { tx ->
            @Suppress("DEPRECATION")
            tx.createUpdate(
                    "UPDATE fee_thresholds SET valid_during = daterange(lower(valid_during), :endDate, '[]')"
                )
                .bind("endDate", subPeriod1.end)
                .updateExactlyOne()
            tx.insert(
                FeeThresholds(
                    validDuring = subPeriod2.copy(end = null),
                    minIncomeThreshold2 = 213600,
                    minIncomeThreshold3 = 275600,
                    minIncomeThreshold4 = 312900,
                    minIncomeThreshold5 = 350200,
                    minIncomeThreshold6 = 387400,
                    maxIncomeThreshold2 = 482300,
                    maxIncomeThreshold3 = 544300,
                    maxIncomeThreshold4 = 581600,
                    maxIncomeThreshold5 = 618900,
                    maxIncomeThreshold6 = 656100,
                    incomeMultiplier2 = BigDecimal("0.1070"),
                    incomeMultiplier3 = BigDecimal("0.1070"),
                    incomeMultiplier4 = BigDecimal("0.1070"),
                    incomeMultiplier5 = BigDecimal("0.1070"),
                    incomeMultiplier6 = BigDecimal("0.1070"),
                    incomeThresholdIncrease6Plus = 14200,
                    siblingDiscount2 = BigDecimal("0.5"),
                    siblingDiscount2Plus = BigDecimal("0.8"),
                    maxFee = 28900,
                    minFee = 2700,
                    temporaryFee = 2900,
                    temporaryFeePartDay = 1500,
                    temporaryFeeSibling = 1500,
                    temporaryFeeSiblingPartDay = 800
                )
            )
            tx.insert(
                DevIncome(
                    personId = testAdult_1.id,
                    validFrom = period.start,
                    data =
                        mapOf(
                            "MAIN_INCOME" to
                                IncomeValue(
                                    amount = 0,
                                    coefficient = IncomeCoefficient.MONTHLY_NO_HOLIDAY_BONUS,
                                    multiplier = 1,
                                    monthlyAmount = 0
                                )
                        ),
                    effect = IncomeEffect.INCOME,
                    updatedBy = EvakaUserId(testDecisionMaker_1.id.raw)
                )
            )
        }

        db.transaction { tx -> generator.generateNewDecisionsForAdult(tx, testAdult_1.id) }

        assertThat(getAllVoucherValueDecisions())
            .extracting({ it.validFrom }, { it.validTo }, { it.difference })
            .containsExactlyInAnyOrder(
                Tuple(subPeriod1.start, subPeriod1.end, emptySet<VoucherValueDecisionDifference>()),
                Tuple(
                    subPeriod2.start,
                    subPeriod2.end,
                    setOf(VoucherValueDecisionDifference.FEE_THRESHOLDS)
                )
            )
    }

    @Test
    fun `difference with overlapping drafts`() {
        val period = DateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31))
        val subPeriod1 = DateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 6, 30))
        val subPeriod2 = DateRange(LocalDate.of(2022, 7, 1), LocalDate.of(2022, 12, 31))
        val subPeriod3 = DateRange(LocalDate.of(2022, 4, 1), LocalDate.of(2022, 9, 1))
        val clock = MockEvakaClock(HelsinkiDateTime.of(period.start, LocalTime.MIN))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        insertPlacement(testChild_1.id, subPeriod1, PlacementType.DAYCARE, testVoucherDaycare.id)
        insertPlacement(testChild_1.id, subPeriod2, PlacementType.DAYCARE, testVoucherDaycare2.id)
        insertIncome(testChild_1.id, 10000, subPeriod3)

        db.transaction { tx -> generator.generateNewDecisionsForAdult(tx, testAdult_1.id) }

        assertThat(getAllVoucherValueDecisions())
            .extracting({ it.validFrom }, { it.validTo }, { it.difference })
            .containsExactlyInAnyOrder(
                Tuple(
                    LocalDate.of(2022, 1, 1),
                    LocalDate.of(2022, 3, 31),
                    emptySet<VoucherValueDecisionDifference>()
                ),
                Tuple(
                    LocalDate.of(2022, 4, 1),
                    LocalDate.of(2022, 6, 30),
                    setOf(VoucherValueDecisionDifference.INCOME)
                ),
                Tuple(
                    LocalDate.of(2022, 7, 1),
                    LocalDate.of(2022, 9, 1),
                    setOf(VoucherValueDecisionDifference.PLACEMENT)
                ),
                Tuple(
                    LocalDate.of(2022, 9, 2),
                    LocalDate.of(2022, 12, 31),
                    setOf(VoucherValueDecisionDifference.INCOME)
                )
            )
    }

    @Test
    fun `difference between sent and draft`() {
        val period = DateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31))
        val subPeriod1 = period.copy(end = LocalDate.of(2022, 6, 30))
        val subPeriod2 = period.copy(start = LocalDate.of(2022, 7, 1))
        val clock = MockEvakaClock(HelsinkiDateTime.of(period.start, LocalTime.MIN))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        insertPlacement(testChild_1.id, subPeriod1, PlacementType.DAYCARE, testVoucherDaycare.id)
        db.transaction { tx ->
            generator.generateNewDecisionsForAdult(tx, testAdult_1.id)
            @Suppress("DEPRECATION")
            tx.createUpdate("UPDATE voucher_value_decision SET status = 'SENT'").execute()
        }
        insertPlacement(testChild_1.id, subPeriod2, PlacementType.DAYCARE, testVoucherDaycare2.id)

        db.transaction { tx -> generator.generateNewDecisionsForAdult(tx, testAdult_1.id) }

        assertThat(getAllVoucherValueDecisions())
            .extracting({ DateRange(it.validFrom, it.validTo) }, { it.status }, { it.difference })
            .containsExactlyInAnyOrder(
                Tuple(
                    subPeriod1,
                    VoucherValueDecisionStatus.SENT,
                    emptySet<VoucherValueDecisionDifference>()
                ),
                Tuple(
                    subPeriod2,
                    VoucherValueDecisionStatus.DRAFT,
                    setOf(VoucherValueDecisionDifference.PLACEMENT)
                )
            )
    }

    @Test
    fun `difference when drafts replaces sent`() {
        val period = DateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31))
        val subPeriod1 = period.copy(end = LocalDate.of(2022, 6, 30))
        val subPeriod2 = period.copy(start = LocalDate.of(2022, 7, 1))
        val clock = MockEvakaClock(HelsinkiDateTime.of(period.start, LocalTime.MIN))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        insertPlacement(testChild_1.id, period, PlacementType.DAYCARE, testVoucherDaycare.id)
        db.transaction { tx ->
            generator.generateNewDecisionsForAdult(tx, testAdult_1.id)
            @Suppress("DEPRECATION")
            tx.createUpdate("UPDATE voucher_value_decision SET status = 'SENT'").execute()
            @Suppress("DEPRECATION") tx.createUpdate("DELETE FROM placement").execute()
        }
        insertPlacement(testChild_1.id, subPeriod1, PlacementType.DAYCARE, testVoucherDaycare2.id)
        insertPlacement(testChild_1.id, subPeriod2, PlacementType.DAYCARE, testVoucherDaycare.id)

        db.transaction { tx -> generator.generateNewDecisionsForAdult(tx, testAdult_1.id) }

        assertThat(getAllVoucherValueDecisions())
            .extracting({ DateRange(it.validFrom, it.validTo) }, { it.status }, { it.difference })
            .containsExactlyInAnyOrder(
                Tuple(
                    period,
                    VoucherValueDecisionStatus.SENT,
                    emptySet<VoucherValueDecisionDifference>()
                ),
                Tuple(
                    subPeriod1,
                    VoucherValueDecisionStatus.DRAFT,
                    setOf(VoucherValueDecisionDifference.PLACEMENT)
                ),
                Tuple(
                    subPeriod2,
                    VoucherValueDecisionStatus.DRAFT,
                    setOf(VoucherValueDecisionDifference.PLACEMENT)
                )
            )
    }

    @Test
    fun `difference when gap between two drafts`() {
        val period = DateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31))
        val subPeriod1 = period.copy(end = LocalDate.of(2022, 6, 30))
        val subPeriod2 = period.copy(start = LocalDate.of(2022, 7, 2))
        val clock = MockEvakaClock(HelsinkiDateTime.of(period.start, LocalTime.MIN))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        insertPlacement(testChild_1.id, subPeriod1, PlacementType.DAYCARE, testVoucherDaycare.id)
        insertPlacement(testChild_1.id, subPeriod2, PlacementType.DAYCARE, testVoucherDaycare2.id)

        db.transaction { tx -> generator.generateNewDecisionsForAdult(tx, testAdult_1.id) }

        assertThat(getAllVoucherValueDecisions())
            .extracting({ DateRange(it.validFrom, it.validTo) }, { it.difference })
            .containsExactlyInAnyOrder(
                Tuple(subPeriod1, emptySet<VoucherValueDecisionDifference>()),
                Tuple(subPeriod2, emptySet<VoucherValueDecisionDifference>())
            )
    }

    @Test
    fun `difference when gap between sent and draft`() {
        val period = DateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31))
        val subPeriod1 = period.copy(end = LocalDate.of(2022, 6, 30))
        val subPeriod2 = period.copy(start = LocalDate.of(2022, 7, 2))
        val clock = MockEvakaClock(HelsinkiDateTime.of(period.start, LocalTime.MIN))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        insertPlacement(testChild_1.id, subPeriod1, PlacementType.DAYCARE, testVoucherDaycare.id)
        db.transaction { tx ->
            generator.generateNewDecisionsForAdult(tx, testAdult_1.id)
            @Suppress("DEPRECATION")
            tx.createUpdate("UPDATE voucher_value_decision SET status = 'SENT'").execute()
        }
        insertPlacement(testChild_1.id, subPeriod2, PlacementType.DAYCARE, testVoucherDaycare2.id)

        db.transaction { tx -> generator.generateNewDecisionsForAdult(tx, testAdult_1.id) }

        assertThat(getAllVoucherValueDecisions())
            .extracting({ DateRange(it.validFrom, it.validTo) }, { it.status }, { it.difference })
            .containsExactlyInAnyOrder(
                Tuple(
                    subPeriod1,
                    VoucherValueDecisionStatus.SENT,
                    emptySet<VoucherValueDecisionDifference>()
                ),
                Tuple(
                    subPeriod2,
                    VoucherValueDecisionStatus.DRAFT,
                    emptySet<VoucherValueDecisionDifference>()
                )
            )
    }

    @Test
    fun `duplicate sent voucher value decision is not generated if there is a draft in the past`() {
        val testClock = MockEvakaClock(2023, 1, 1, 16, 30)
        val period = DateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31))
        val subPeriod1 = period.copy(end = LocalDate.of(2022, 6, 30))
        val subPeriod2 = period.copy(start = LocalDate.of(2022, 7, 1))
        insertFamilyRelations(testAdult_1.id, listOf(testChild_1.id), period)
        insertPlacement(testChild_1.id, subPeriod1, PlacementType.DAYCARE, testVoucherDaycare.id)
        insertPlacement(testChild_1.id, subPeriod2, PlacementType.DAYCARE, testVoucherDaycare2.id)

        db.transaction { generator.generateNewDecisionsForAdult(it, testAdult_1.id) }
        val decisions = getAllVoucherValueDecisions()
        assertEquals(2, decisions.size)
        assertEquals(2, decisions.filter { it.status == VoucherValueDecisionStatus.DRAFT }.size)

        val firstDecision = decisions.first { it.validTo == subPeriod2.end }

        voucherValueDecisionController.sendVoucherValueDecisionDrafts(
            dbInstance(),
            AuthenticatedUser.Employee(testDecisionMaker_2.id, setOf(UserRole.ADMIN)),
            testClock,
            listOf(firstDecision.id),
            null
        )

        asyncJobRunner.runPendingJobsSync(testClock)

        getAllVoucherValueDecisions().let {
            assertEquals(2, it.size)
            assertEquals(1, it.filter { it.status == VoucherValueDecisionStatus.SENT }.size)
            assertEquals(1, it.filter { it.status == VoucherValueDecisionStatus.DRAFT }.size)
        }

        db.transaction { generator.generateNewDecisionsForAdult(it, testAdult_1.id) }

        getAllVoucherValueDecisions().let {
            assertEquals(2, it.size)
            assertEquals(1, it.filter { it.status == VoucherValueDecisionStatus.SENT }.size)
            assertEquals(1, it.filter { it.status == VoucherValueDecisionStatus.DRAFT }.size)
        }
    }

    private fun insertPlacement(
        childId: ChildId,
        period: DateRange,
        type: PlacementType,
        daycareId: DaycareId
    ): PlacementId {
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

    private fun insertFamilyRelations(
        headOfFamilyId: PersonId,
        childIds: List<ChildId>,
        period: DateRange
    ) {
        db.transaction { tx ->
            childIds.forEach { childId ->
                tx.insertTestParentship(
                    headOfFamilyId,
                    childId,
                    startDate = period.start,
                    endDate = period.end!!
                )
            }
        }
    }

    private fun insertPartnership(
        adultId1: PersonId,
        adultId2: PersonId,
        period: DateRange,
        createdAt: HelsinkiDateTime
    ) {
        db.transaction { tx ->
            tx.insertTestPartnership(
                adultId1,
                adultId2,
                startDate = period.start,
                endDate = period.end,
                createdAt = createdAt
            )
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

    private fun insertAssistanceNeedCoefficient(
        childId: ChildId,
        period: FiniteDateRange,
        coefficient: Double
    ) {
        db.transaction { tx ->
            tx.insertAssistanceNeedVoucherCoefficient(
                childId,
                AssistanceNeedVoucherCoefficientRequest(coefficient, period)
            )
        }
    }

    private fun getAllVoucherValueDecisions(): List<VoucherValueDecision> {
        return db.read { tx ->
                @Suppress("DEPRECATION")
                tx.createQuery("SELECT * FROM voucher_value_decision")
                    .toList<VoucherValueDecision>()
            }
            .shuffled() // randomize order to expose assumptions
    }

    private fun insertIncome(adultId: PersonId, amount: Int, period: DateRange) {
        db.transaction { tx ->
            tx.insert(
                DevIncome(
                    personId = adultId,
                    validFrom = period.start,
                    validTo = period.end,
                    effect = IncomeEffect.INCOME,
                    data =
                        mapOf(
                            "MAIN_INCOME" to
                                IncomeValue(
                                    amount,
                                    IncomeCoefficient.MONTHLY_NO_HOLIDAY_BONUS,
                                    1,
                                    calculateMonthlyAmount(
                                        amount,
                                        coefficientMultiplierProvider.multiplier(
                                            IncomeCoefficient.MONTHLY_NO_HOLIDAY_BONUS
                                        )
                                    )
                                )
                        ),
                    updatedBy = EvakaUserId(testDecisionMaker_1.id.raw)
                )
            )
        }
    }

    private fun insertFeeAlteration(personId: PersonId, amount: Double, period: DateRange) {
        db.transaction { tx ->
            tx.insert(
                DevFeeAlteration(
                    id = FeeAlterationId(UUID.randomUUID()),
                    personId = personId,
                    type = FeeAlterationType.DISCOUNT,
                    amount = amount,
                    isAbsolute = false,
                    validFrom = period.start,
                    validTo = period.end,
                    updatedBy = EvakaUserId(testDecisionMaker_1.id.raw)
                )
            )
        }
    }
}
