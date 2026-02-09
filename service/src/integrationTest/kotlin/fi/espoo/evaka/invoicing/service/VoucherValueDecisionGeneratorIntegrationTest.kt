// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.assistanceneed.vouchercoefficient.AssistanceNeedVoucherCoefficientRequest
import fi.espoo.evaka.assistanceneed.vouchercoefficient.insertAssistanceNeedVoucherCoefficient
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.feeThresholds
import fi.espoo.evaka.insertServiceNeedOptionVoucherValues
import fi.espoo.evaka.insertServiceNeedOptions
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
import fi.espoo.evaka.serviceneed.ShiftCareType
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.ServiceNeedOptionId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevAssistanceFactor
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevFeeAlteration
import fi.espoo.evaka.shared.dev.DevIncome
import fi.espoo.evaka.shared.dev.DevParentship
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.DevServiceNeed
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertTestPartnership
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
import fi.espoo.evaka.toValueDecisionServiceNeed
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
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

    private val area = DevCareArea()
    private val voucherDaycare =
        DevDaycare(
            areaId = area.id,
            name = "Test Voucher Daycare",
            providerType = ProviderType.PRIVATE_SERVICE_VOUCHER,
        )
    private val voucherDaycare2 =
        DevDaycare(
            areaId = area.id,
            name = "Test Voucher Daycare 2",
            providerType = ProviderType.PRIVATE_SERVICE_VOUCHER,
        )
    private val employee = DevEmployee(roles = setOf(UserRole.ADMIN))
    private val decisionMaker = DevEmployee()
    // Needs SSN and address for sendVoucherValueDecisionDrafts PDF generation
    private val adult1 =
        DevPerson(
            ssn = "010180-1232",
            streetAddress = "Kamreerintie 2",
            postalCode = "02770",
            postOffice = "Espoo",
        )
    private val adult2 = DevPerson(dateOfBirth = LocalDate.of(1980, 2, 1))
    private val adult3 = DevPerson(dateOfBirth = LocalDate.of(1985, 6, 7))
    // dateOfBirth matters: 3rd birthday split at 2020-06-01
    private val child1 = DevPerson(dateOfBirth = LocalDate.of(2017, 6, 1))
    // Older than child1, used for sibling discount ordering
    private val child2 = DevPerson(dateOfBirth = LocalDate.of(2016, 3, 1))
    // Not born on 1st of month — tests rounding of 3rd birthday boundary
    private val child6 = DevPerson(dateOfBirth = LocalDate.of(2018, 11, 13))
    private val clock = MockEvakaClock(2021, 1, 1, 15, 0)

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(area)
            tx.insert(voucherDaycare)
            tx.insert(voucherDaycare2)
            tx.insert(employee)
            tx.insert(decisionMaker)
            listOf(adult1, adult2, adult3).forEach { tx.insert(it, DevPersonType.ADULT) }
            listOf(child1, child2, child6).forEach { tx.insert(it, DevPersonType.CHILD) }
            tx.insert(feeThresholds)
            tx.insertServiceNeedOptions()
            tx.insertServiceNeedOptionVoucherValues()
        }
    }

    @Test
    fun `voucher value decisions get correct base values`() {
        val period = FiniteDateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31))
        insertFamilyRelations(adult1.id, listOf(child1.id), period)
        insertPlacement(child1.id, period, PlacementType.DAYCARE, voucherDaycare.id)

        db.transaction { generator.generateNewDecisionsForAdult(it, adult1.id) }

        val voucherValueDecisions = getAllVoucherValueDecisions().sortedBy { it.validFrom }
        assertEquals(2, voucherValueDecisions.size)
        voucherValueDecisions.first().let { decision ->
            assertEquals(VoucherValueDecisionStatus.DRAFT, decision.status)
            assertEquals(period.start, decision.validFrom)
            assertEquals(child1.dateOfBirth.plusYears(3).minusDays(1), decision.validTo)
            assertEquals(child1.id, decision.child.id)
            assertEquals(134850, decision.baseValue)
            assertEquals(snDefaultDaycare.toValueDecisionServiceNeed(), decision.serviceNeed)
            assertEquals(134900, decision.voucherValue)
            assertEquals(28900, decision.finalCoPayment)
        }
        voucherValueDecisions.last().let { decision ->
            assertEquals(VoucherValueDecisionStatus.DRAFT, decision.status)
            assertEquals(child1.dateOfBirth.plusYears(3), decision.validFrom)
            assertEquals(period.end, decision.validTo)
            assertEquals(child1.id, decision.child.id)
            assertEquals(87000, decision.baseValue)
            assertEquals(snDefaultDaycare.toValueDecisionServiceNeed(), decision.serviceNeed)
            assertEquals(87000, decision.voucherValue)
            assertEquals(28900, decision.finalCoPayment)
        }
    }

    @Test
    fun `siblings in broken family`() {
        val dad = adult1
        val mom = adult2
        val youngerChild = child1
        val olderChild = child2
        val partnershipPeriod = DateRange(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 7, 31))
        insertPartnership(dad.id, mom.id, partnershipPeriod, clock.now())
        insertFamilyRelations(
            dad.id,
            listOf(youngerChild.id),
            FiniteDateRange(LocalDate.of(2021, 1, 1), youngerChild.dateOfBirth.plusYears(18)),
        )
        insertFamilyRelations(
            mom.id,
            listOf(olderChild.id),
            FiniteDateRange(LocalDate.of(2021, 1, 1), olderChild.dateOfBirth.plusYears(18)),
        )
        val placementPeriod = FiniteDateRange(LocalDate.of(2021, 3, 1), LocalDate.of(2021, 10, 31))
        insertPlacement(
            childId = olderChild.id,
            period = placementPeriod,
            type = PlacementType.DAYCARE,
            daycareId = voucherDaycare.id,
        )
        insertPlacement(
            childId = youngerChild.id,
            period = placementPeriod,
            type = PlacementType.DAYCARE,
            daycareId = voucherDaycare.id,
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
        val period = FiniteDateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31))
        insertFamilyRelations(adult1.id, listOf(child1.id), period)
        insertPlacement(child1.id, period, PlacementType.DAYCARE, voucherDaycare.id)
        insertFeeAlteration(child1.id, 50.0, period.asDateRange())

        db.transaction { generator.generateNewDecisionsForAdult(it, adult1.id) }

        val voucherValueDecisions = getAllVoucherValueDecisions().sortedBy { it.validFrom }
        assertEquals(2, voucherValueDecisions.size)
        voucherValueDecisions.first().let { decision ->
            assertEquals(VoucherValueDecisionStatus.DRAFT, decision.status)
            assertEquals(period.start, decision.validFrom)
            assertEquals(child1.dateOfBirth.plusYears(3).minusDays(1), decision.validTo)
            assertEquals(child1.id, decision.child.id)
            assertEquals(134850, decision.baseValue)
            assertEquals(snDefaultDaycare.toValueDecisionServiceNeed(), decision.serviceNeed)
            assertEquals(134900, decision.voucherValue)
            assertEquals(14450, decision.finalCoPayment)
        }
        voucherValueDecisions.last().let { decision ->
            assertEquals(VoucherValueDecisionStatus.DRAFT, decision.status)
            assertEquals(child1.dateOfBirth.plusYears(3), decision.validFrom)
            assertEquals(period.end, decision.validTo)
            assertEquals(child1.id, decision.child.id)
            assertEquals(87000, decision.baseValue)
            assertEquals(snDefaultDaycare.toValueDecisionServiceNeed(), decision.serviceNeed)
            assertEquals(87000, decision.voucherValue)
            assertEquals(14450, decision.finalCoPayment)
        }
    }

    @Test
    fun `voucher value decisions get correct base values for child not born on first of month`() {
        val period = FiniteDateRange(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31))

        val testChild = child6

        assertNotEquals(1, testChild.dateOfBirth.dayOfMonth)

        insertFamilyRelations(adult1.id, listOf(testChild.id), period)
        insertPlacement(testChild.id, period, PlacementType.DAYCARE, voucherDaycare.id)

        db.transaction { generator.generateNewDecisionsForAdult(it, adult1.id) }

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
        val period = FiniteDateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31))
        insertFamilyRelations(adult1.id, listOf(child2.id), period)
        val placementId =
            insertPlacement(child2.id, period, PlacementType.DAYCARE, voucherDaycare.id)
        val serviceNeedPeriod = period.copy(start = period.start.plusMonths(5))
        insertServiceNeed(placementId, serviceNeedPeriod, snDaycareFullDayPartWeek25.id)

        db.transaction { generator.generateNewDecisionsForAdult(it, adult1.id) }

        val voucherValueDecisions = getAllVoucherValueDecisions().sortedBy { it.validFrom }
        assertEquals(2, voucherValueDecisions.size)
        voucherValueDecisions.first().let { decision ->
            assertEquals(VoucherValueDecisionStatus.DRAFT, decision.status)
            assertEquals(period.start, decision.validFrom)
            assertEquals(serviceNeedPeriod.start.minusDays(1), decision.validTo)
            assertEquals(child2.id, decision.child.id)
            assertEquals(87000, decision.baseValue)
            assertEquals(snDefaultDaycare.toValueDecisionServiceNeed(), decision.serviceNeed)
            assertEquals(87000, decision.voucherValue)
        }
        voucherValueDecisions.last().let { decision ->
            assertEquals(VoucherValueDecisionStatus.DRAFT, decision.status)
            assertEquals(serviceNeedPeriod.start, decision.validFrom)
            assertEquals(period.end, decision.validTo)
            assertEquals(child2.id, decision.child.id)
            assertEquals(87000, decision.baseValue)
            assertEquals(
                snDaycareFullDayPartWeek25.toValueDecisionServiceNeed(),
                decision.serviceNeed,
            )
            assertEquals(52200, decision.voucherValue)
        }
    }

    @Test
    fun `voucher value decisions with five year olds daycare placement`() {
        val period = FiniteDateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31))
        insertFamilyRelations(adult1.id, listOf(child2.id), period)
        insertPlacement(child2.id, period, PlacementType.DAYCARE_FIVE_YEAR_OLDS, voucherDaycare.id)

        db.transaction { generator.generateNewDecisionsForAdult(it, adult1.id) }

        val voucherValueDecisions = getAllVoucherValueDecisions()
        assertEquals(1, voucherValueDecisions.size)
        voucherValueDecisions.first().let { decision ->
            assertEquals(VoucherValueDecisionStatus.DRAFT, decision.status)
            assertEquals(period.start, decision.validFrom)
            assertEquals(period.end, decision.validTo)
            assertEquals(child2.id, decision.child.id)
            assertEquals(87000, decision.baseValue)
            assertEquals(
                snDefaultFiveYearOldsDaycare.toValueDecisionServiceNeed(),
                decision.serviceNeed,
            )
            assertEquals(87000, decision.voucherValue)
        }
    }

    @Test
    fun `voucher value decisions with part day five year olds daycare placement`() {
        val period = FiniteDateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31))
        insertFamilyRelations(adult1.id, listOf(child2.id), period)
        insertPlacement(
            child2.id,
            period,
            PlacementType.DAYCARE_PART_TIME_FIVE_YEAR_OLDS,
            voucherDaycare.id,
        )

        db.transaction { generator.generateNewDecisionsForAdult(it, adult1.id) }

        val voucherValueDecisions = getAllVoucherValueDecisions()
        assertEquals(1, voucherValueDecisions.size)
        voucherValueDecisions.first().let { decision ->
            assertEquals(VoucherValueDecisionStatus.DRAFT, decision.status)
            assertEquals(period.start, decision.validFrom)
            assertEquals(period.end, decision.validTo)
            assertEquals(child2.id, decision.child.id)
            assertEquals(87000, decision.baseValue)
            assertEquals(
                snDefaultFiveYearOldsPartDayDaycare.toValueDecisionServiceNeed(),
                decision.serviceNeed,
            )
            assertEquals(52200, decision.voucherValue)
        }
    }

    @Test
    fun `voucher value decisions with preschool placement`() {
        val period = FiniteDateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31))
        insertFamilyRelations(adult1.id, listOf(child2.id), period)
        insertPlacement(child2.id, period, PlacementType.PRESCHOOL, voucherDaycare.id)

        db.transaction { generator.generateNewDecisionsForAdult(it, adult1.id) }

        val voucherValueDecisions = getAllVoucherValueDecisions()
        assertEquals(0, voucherValueDecisions.size)
    }

    @Test
    fun `voucher value decisions with preparatory placement`() {
        val period = FiniteDateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31))
        insertFamilyRelations(adult1.id, listOf(child2.id), period)
        insertPlacement(child2.id, period, PlacementType.PREPARATORY, voucherDaycare.id)

        db.transaction { generator.generateNewDecisionsForAdult(it, adult1.id) }

        val voucherValueDecisions = getAllVoucherValueDecisions()
        assertEquals(1, voucherValueDecisions.size)
        voucherValueDecisions.first().let { decision ->
            assertEquals(VoucherValueDecisionStatus.DRAFT, decision.status)
            assertEquals(period.start, decision.validFrom)
            assertEquals(period.end, decision.validTo)
            assertEquals(child2.id, decision.child.id)
            assertEquals(87000, decision.baseValue)
            assertEquals(snDefaultPreparatory.toValueDecisionServiceNeed(), decision.serviceNeed)
            assertEquals(43500, decision.voucherValue)
        }
    }

    @Test
    fun `voucher value decisions with five year old in daycare with part time hours per week`() {
        val period = FiniteDateRange(LocalDate.of(2021, 8, 1), LocalDate.of(2021, 12, 31))
        insertFamilyRelations(adult1.id, listOf(child2.id), period)
        val placementId =
            insertPlacement(child2.id, period, PlacementType.DAYCARE, voucherDaycare.id)
        insertServiceNeed(placementId, period, snDaycareFiveYearOldsFullDayPartWeek25.id)

        db.transaction { generator.generateNewDecisionsForAdult(it, adult1.id) }

        val voucherValueDecisions = getAllVoucherValueDecisions()
        assertEquals(1, voucherValueDecisions.size)
        voucherValueDecisions.first().let { decision ->
            assertEquals(VoucherValueDecisionStatus.DRAFT, decision.status)
            assertEquals(period.start, decision.validFrom)
            assertEquals(period.end, decision.validTo)
            assertEquals(child2.id, decision.child.id)
            assertEquals(87000, decision.baseValue)
            assertEquals(
                snDaycareFiveYearOldsFullDayPartWeek25.toValueDecisionServiceNeed(),
                decision.serviceNeed,
            )
            assertEquals(52200, decision.voucherValue)
        }
    }

    @Test
    fun `voucher value decisions for a family of two children placed in voucher units`() {
        val period = FiniteDateRange(LocalDate.of(2021, 8, 1), LocalDate.of(2021, 12, 31))
        insertFamilyRelations(adult1.id, listOf(child1.id, child2.id), period)
        insertPlacement(child1.id, period, PlacementType.DAYCARE, voucherDaycare.id)
        insertPlacement(
            child2.id,
            period.copy(start = period.start.plusMonths(1)),
            PlacementType.DAYCARE_FIVE_YEAR_OLDS,
            voucherDaycare.id,
        )

        db.transaction { generator.generateNewDecisionsForAdult(it, adult1.id) }

        val voucherValueDecisions = getAllVoucherValueDecisions().sortedBy { it.validFrom }
        assertEquals(2, voucherValueDecisions.size)
        voucherValueDecisions.first().let { decision ->
            assertEquals(VoucherValueDecisionStatus.DRAFT, decision.status)
            assertEquals(period.start, decision.validFrom)
            assertEquals(period.end, decision.validTo)
            assertEquals(child1.id, decision.child.id)
            assertEquals(87000, decision.voucherValue)
            assertEquals(28900, decision.coPayment)
        }
        voucherValueDecisions.last().let { decision ->
            assertEquals(VoucherValueDecisionStatus.DRAFT, decision.status)
            assertEquals(period.start.plusMonths(1), decision.validFrom)
            assertEquals(period.end, decision.validTo)
            assertEquals(child2.id, decision.child.id)
            assertEquals(87000, decision.voucherValue)
            assertEquals(28900, decision.baseCoPayment)
            // child2 is older than child1
            assertEquals(50, decision.siblingDiscount)
            assertEquals(PlacementType.DAYCARE_FIVE_YEAR_OLDS, decision.placement?.type)
            assertEquals(11600, decision.coPayment)
        }
    }

    @Test
    fun `twins are ordered consistently for sibling discount`() {
        // Younger per SSN
        val twin1 = DevPerson(dateOfBirth = LocalDate.of(2019, 1, 1), ssn = "010117A902X")

        // Older
        val twin2 = DevPerson(dateOfBirth = LocalDate.of(2019, 1, 1), ssn = "010117A901W")

        db.transaction { tx -> listOf(twin1, twin2).forEach { tx.insert(it, DevPersonType.CHILD) } }

        val placementPeriod = FiniteDateRange(LocalDate.of(2021, 8, 1), LocalDate.of(2021, 12, 31))
        insertPlacement(twin1.id, placementPeriod, PlacementType.DAYCARE, voucherDaycare.id)
        insertPlacement(twin2.id, placementPeriod, PlacementType.DAYCARE, voucherDaycare.id)
        insertFamilyRelations(adult1.id, listOf(twin1.id, twin2.id), placementPeriod)

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
        val period = FiniteDateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31))
        insertFamilyRelations(adult1.id, listOf(child2.id), period)
        insertPlacement(child2.id, period, PlacementType.DAYCARE, voucherDaycare.id)
        db.transaction { it.insert(DevAssistanceFactor(childId = child2.id, capacityFactor = 3.0)) }

        db.transaction { generator.generateNewDecisionsForAdult(it, adult1.id) }

        val voucherValueDecisions = getAllVoucherValueDecisions()
        assertEquals(1, voucherValueDecisions.size)
        voucherValueDecisions.first().let { decision ->
            assertEquals(VoucherValueDecisionStatus.DRAFT, decision.status)
            assertEquals(period.start, decision.validFrom)
            assertEquals(period.end, decision.validTo)
            assertEquals(child2.id, decision.child.id)
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
        val period = FiniteDateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31))
        insertFamilyRelations(adult1.id, listOf(child2.id), period)
        insertPlacement(child2.id, period, PlacementType.DAYCARE, voucherDaycare.id)
        insertAssistanceNeedCoefficient(child2.id, period, 3.55)

        db.transaction { generator.generateNewDecisionsForAdult(it, adult1.id) }

        val voucherValueDecisions = getAllVoucherValueDecisions()
        assertEquals(1, voucherValueDecisions.size)
        voucherValueDecisions.first().let { decision ->
            assertEquals(VoucherValueDecisionStatus.DRAFT, decision.status)
            assertEquals(period.start, decision.validFrom)
            assertEquals(period.end, decision.validTo)
            assertEquals(child2.id, decision.child.id)
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
        val firstPeriod = FiniteDateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 31))
        val secondPeriod = FiniteDateRange(LocalDate.of(2020, 2, 1), LocalDate.of(2020, 12, 31))
        val wholePeriod = firstPeriod.copy(end = secondPeriod.end)
        insertFamilyRelations(adult1.id, listOf(child2.id), wholePeriod)
        insertPlacement(child2.id, wholePeriod, PlacementType.DAYCARE, voucherDaycare.id)
        insertPartnership(adult1.id, adult2.id, firstPeriod.asDateRange(), clock.now())

        db.transaction { generator.generateNewDecisionsForAdult(it, adult2.id) }

        val voucherValueDecisions = getAllVoucherValueDecisions().sortedBy { it.validFrom }
        assertEquals(2, voucherValueDecisions.size)
        voucherValueDecisions.first().let { decision ->
            assertEquals(VoucherValueDecisionStatus.DRAFT, decision.status)
            assertEquals(firstPeriod, FiniteDateRange(decision.validFrom, decision.validTo))
            assertEquals(3, decision.familySize)
            assertEquals(adult1.id, decision.headOfFamilyId)
            assertEquals(adult2.id, decision.partnerId)
            assertEquals(child2.id, decision.child.id)
        }
        voucherValueDecisions.last().let { decision ->
            assertEquals(VoucherValueDecisionStatus.DRAFT, decision.status)
            assertEquals(secondPeriod, FiniteDateRange(decision.validFrom, decision.validTo))
            assertEquals(2, decision.familySize)
            assertEquals(adult1.id, decision.headOfFamilyId)
            assertEquals(null, decision.partnerId)
            assertEquals(child2.id, decision.child.id)
        }
    }

    @Test
    fun `voucher value decisions with child income`() {
        val period = FiniteDateRange(LocalDate.of(2021, 8, 1), LocalDate.of(2021, 12, 31))
        insertFamilyRelations(adult1.id, listOf(child1.id, child2.id), period)
        insertPlacement(child1.id, period, PlacementType.DAYCARE, voucherDaycare.id)
        insertPlacement(
            child2.id,
            period.copy(start = period.start.plusMonths(1)),
            PlacementType.DAYCARE_FIVE_YEAR_OLDS,
            voucherDaycare.id,
        )

        // Adult minimal income
        insertIncome(adult1.id, 310200, period.asDateRange())
        insertIncome(child1.id, 600000, period.asDateRange())

        db.transaction { generator.generateNewDecisionsForAdult(it, adult1.id) }

        val voucherValueDecisions = getAllVoucherValueDecisions().sortedBy { it.validFrom }
        assertEquals(2, voucherValueDecisions.size)
        voucherValueDecisions.first().let { decision ->
            assertEquals(VoucherValueDecisionStatus.DRAFT, decision.status)
            assertEquals(period.start, decision.validFrom)
            assertEquals(period.end, decision.validTo)
            assertEquals(child1.id, decision.child.id)
            assertEquals(87000, decision.voucherValue)
            assertEquals(28900, decision.coPayment)
        }
        voucherValueDecisions.last().let { decision ->
            assertEquals(VoucherValueDecisionStatus.DRAFT, decision.status)
            assertEquals(period.start.plusMonths(1), decision.validFrom)
            assertEquals(period.end, decision.validTo)
            assertEquals(child2.id, decision.child.id)
            assertEquals(87000, decision.voucherValue)
            assertEquals(4200, decision.baseCoPayment)
            // child2 is older than child1
            assertEquals(50, decision.siblingDiscount)
            assertEquals(PlacementType.DAYCARE_FIVE_YEAR_OLDS, decision.placement?.type)
            assertEquals(0, decision.coPayment)
        }
    }

    @Test
    fun `voucher value decision is generated for fridge family with two head of families with different children for placed child`() {
        val period = FiniteDateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31))

        insertFamilyRelations(adult1.id, listOf(child1.id), period)
        insertFamilyRelations(adult2.id, listOf(child2.id), period)
        insertPartnership(adult1.id, adult2.id, period.asDateRange(), clock.now())
        insertPlacement(child1.id, period, PlacementType.DAYCARE_FIVE_YEAR_OLDS, voucherDaycare.id)

        db.transaction { generator.generateNewDecisionsForChild(it, child1.id) }
        assertEquals(1, getAllVoucherValueDecisions().size)
    }

    @Test
    fun `voucher value decision is generated for fridge family with two head of families with different children for non-placed child`() {
        val period = FiniteDateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31))

        insertFamilyRelations(adult1.id, listOf(child1.id), period)
        insertFamilyRelations(adult2.id, listOf(child2.id), period)
        insertPartnership(adult1.id, adult2.id, period.asDateRange(), clock.now())
        insertPlacement(child1.id, period, PlacementType.DAYCARE_FIVE_YEAR_OLDS, voucherDaycare.id)

        db.transaction {
            generator.generateNewDecisionsForChild(it, child1.id)
            generator.generateNewDecisionsForChild(it, child2.id)
        }
        assertEquals(1, getAllVoucherValueDecisions().size)
    }

    @Test
    fun `head of family difference`() {
        val period = FiniteDateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31))
        val subPeriod1 = period.copy(end = LocalDate.of(2022, 6, 30))
        val subPeriod2 = period.copy(start = LocalDate.of(2022, 7, 1))
        insertFamilyRelations(adult1.id, listOf(child1.id), subPeriod1)
        insertFamilyRelations(adult2.id, listOf(child1.id), subPeriod2)
        insertPlacement(child1.id, period, PlacementType.DAYCARE, voucherDaycare.id)

        db.transaction { tx -> generator.generateNewDecisionsForChild(tx, child1.id) }

        assertThat(getAllVoucherValueDecisions())
            .extracting(
                { FiniteDateRange(it.validFrom, it.validTo) },
                { it.difference },
                { it.headOfFamilyId },
            )
            .containsExactlyInAnyOrder(
                Tuple(subPeriod1, emptySet<VoucherValueDecisionDifference>(), adult1.id),
                Tuple(subPeriod2, setOf(VoucherValueDecisionDifference.GUARDIANS), adult2.id),
            )
    }

    @Test
    fun `partner difference`() {
        val period = FiniteDateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31))
        val subPeriod1 = period.copy(end = LocalDate.of(2022, 6, 30))
        val subPeriod2 = period.copy(start = LocalDate.of(2022, 7, 1))
        val clock = MockEvakaClock(HelsinkiDateTime.of(period.start, LocalTime.MIN))
        insertFamilyRelations(adult1.id, listOf(child1.id), period)
        insertPlacement(child1.id, period, PlacementType.DAYCARE, voucherDaycare.id)
        insertPartnership(adult1.id, adult2.id, subPeriod1.asDateRange(), clock.now())
        insertPartnership(adult1.id, adult3.id, subPeriod2.asDateRange(), clock.now())

        db.transaction { tx -> generator.generateNewDecisionsForAdult(tx, adult1.id) }

        assertThat(getAllVoucherValueDecisions())
            .extracting(
                { FiniteDateRange(it.validFrom, it.validTo) },
                { it.difference },
                { it.partnerId },
            )
            .containsExactlyInAnyOrder(
                Tuple(subPeriod1, emptySet<VoucherValueDecisionDifference>(), adult2.id),
                Tuple(subPeriod2, setOf(VoucherValueDecisionDifference.GUARDIANS), adult3.id),
            )
    }

    @Test
    fun `head of family & partner switch difference`() {
        val period = FiniteDateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31))
        val subPeriod1 = period.copy(end = LocalDate.of(2022, 6, 30))
        val subPeriod2 = period.copy(start = LocalDate.of(2022, 7, 1))
        val clock = MockEvakaClock(HelsinkiDateTime.of(period.start, LocalTime.MIN))
        insertFamilyRelations(adult1.id, listOf(child1.id), subPeriod1)
        insertFamilyRelations(adult2.id, listOf(child1.id), subPeriod2)
        insertPlacement(child1.id, period, PlacementType.DAYCARE, voucherDaycare.id)
        insertPartnership(adult1.id, adult2.id, period.asDateRange(), clock.now())
        insertIncome(adult1.id, 10000, period.asDateRange())
        insertIncome(adult2.id, 20000, period.asDateRange())

        db.transaction { tx -> generator.generateNewDecisionsForChild(tx, child1.id) }

        val expectedHeadOfFamily = adult1
        assertThat(getAllVoucherValueDecisions())
            .extracting(
                { FiniteDateRange(it.validFrom, it.validTo) },
                { it.difference },
                { it.headOfFamilyId },
            )
            .containsExactly(
                Tuple(period, emptySet<VoucherValueDecisionDifference>(), expectedHeadOfFamily.id)
            )
    }

    @Test
    fun `head of family income difference`() {
        val period = FiniteDateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31))
        val subPeriod1 = period.copy(end = LocalDate.of(2022, 6, 30))
        val subPeriod2 = period.copy(start = LocalDate.of(2022, 7, 1))
        insertFamilyRelations(adult1.id, listOf(child1.id), period)
        insertPlacement(child1.id, period, PlacementType.DAYCARE, voucherDaycare.id)
        insertIncome(adult1.id, 10000, subPeriod2.asDateRange())

        db.transaction { tx -> generator.generateNewDecisionsForAdult(tx, adult1.id) }

        assertThat(getAllVoucherValueDecisions())
            .extracting({ FiniteDateRange(it.validFrom, it.validTo) }, { it.difference })
            .containsExactlyInAnyOrder(
                Tuple(subPeriod1, emptySet<VoucherValueDecisionDifference>()),
                Tuple(
                    subPeriod2,
                    setOf(
                        VoucherValueDecisionDifference.INCOME,
                        VoucherValueDecisionDifference.CO_PAYMENT,
                        VoucherValueDecisionDifference.FINAL_CO_PAYMENT,
                    ),
                ),
            )
    }

    @Test
    fun `partner income difference`() {
        val period = FiniteDateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31))
        val subPeriod1 = period.copy(end = LocalDate.of(2022, 6, 30))
        val subPeriod2 = period.copy(start = LocalDate.of(2022, 7, 1))
        val clock = MockEvakaClock(HelsinkiDateTime.of(period.start, LocalTime.MIN))
        insertFamilyRelations(adult1.id, listOf(child1.id), period)
        insertPlacement(child1.id, period, PlacementType.DAYCARE, voucherDaycare.id)
        insertPartnership(adult1.id, adult2.id, period.asDateRange(), clock.now())
        insertIncome(adult2.id, 10000, subPeriod2.asDateRange())

        db.transaction { tx -> generator.generateNewDecisionsForAdult(tx, adult1.id) }

        assertThat(getAllVoucherValueDecisions())
            .extracting({ FiniteDateRange(it.validFrom, it.validTo) }, { it.difference })
            .containsExactlyInAnyOrder(
                Tuple(subPeriod1, emptySet<VoucherValueDecisionDifference>()),
                Tuple(subPeriod2, setOf(VoucherValueDecisionDifference.INCOME)),
            )
    }

    @Test
    fun `partner income difference - decision is generated even if income changes to identical income`() {
        val period = FiniteDateRange(LocalDate.of(2025, 5, 1), LocalDate.of(2025, 12, 31))
        val incomePeriod1 = DateRange(period.start, null)
        val clock = MockEvakaClock(HelsinkiDateTime.of(period.start, LocalTime.of(0, 0)))
        insertFamilyRelations(adult1.id, listOf(child1.id), period)
        insertPlacement(child1.id, period, PlacementType.DAYCARE, voucherDaycare.id)
        insertIncome(adult1.id, 310200, incomePeriod1)

        db.transaction { generator.generateNewDecisionsForAdult(it, adult1.id) }

        val decisions = getAllVoucherValueDecisions()
        assertEquals(1, decisions.size)

        voucherValueDecisionController.sendVoucherValueDecisionDrafts(
            dbInstance(),
            employee.user,
            clock,
            listOf(decisions[0].id),
            null,
        )

        asyncJobRunner.runPendingJobsSync(clock)

        val incomePeriod2 = DateRange(period.start.plusMonths(3), null)
        db.transaction {
            it.execute {
                sql("UPDATE income SET valid_to = ${bind(incomePeriod2.start.minusDays(1))}")
            }
        }
        insertIncome(adult1.id, 310200, incomePeriod2)

        db.transaction { generator.generateNewDecisionsForAdult(it, adult1.id) }

        assertEquals(2, getAllVoucherValueDecisions().size)
    }

    @Test
    fun `partner income difference - if an income is missing id then old logic is used and a new decision is not generated if income changes to identical income`() {
        val period = FiniteDateRange(LocalDate.of(2025, 5, 1), LocalDate.of(2025, 12, 31))
        val incomePeriod1 = DateRange(period.start, null)
        val clock = MockEvakaClock(HelsinkiDateTime.of(period.start, LocalTime.of(0, 0)))
        insertFamilyRelations(adult1.id, listOf(child1.id), period)
        insertPlacement(child1.id, period, PlacementType.DAYCARE, voucherDaycare.id)
        insertIncome(adult1.id, 310200, incomePeriod1)

        db.transaction { generator.generateNewDecisionsForAdult(it, adult1.id) }

        val decisions = getAllVoucherValueDecisions()
        assertEquals(1, decisions.size)

        voucherValueDecisionController.sendVoucherValueDecisionDrafts(
            dbInstance(),
            employee.user,
            clock,
            listOf(decisions[0].id),
            null,
        )

        asyncJobRunner.runPendingJobsSync(clock)

        db.transaction { tx ->
            // old code did not store income id
            tx.execute {
                sql(
                    """
                UPDATE voucher_value_decision SET head_of_family_income = head_of_family_income - 'id'
            """
                )
            }
        }

        val incomePeriod2 = DateRange(period.start.plusMonths(3), null)
        db.transaction {
            it.execute {
                sql("UPDATE income SET valid_to = ${bind(incomePeriod2.start.minusDays(1))}")
            }
        }
        insertIncome(adult1.id, 310200, incomePeriod2)

        db.transaction { generator.generateNewDecisionsForAdult(it, adult1.id) }

        // No new DRAFT is generated because the incomes are identical
        assertEquals(1, getAllVoucherValueDecisions().size)
    }

    @Test
    fun `child income difference`() {
        val period = FiniteDateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31))
        val subPeriod1 = period.copy(end = LocalDate.of(2022, 6, 30))
        val subPeriod2 = period.copy(start = LocalDate.of(2022, 7, 1))
        insertFamilyRelations(adult1.id, listOf(child1.id), period)
        insertPlacement(child1.id, period, PlacementType.DAYCARE, voucherDaycare.id)
        insertIncome(child1.id, 10000, subPeriod2.asDateRange())

        db.transaction { tx -> generator.generateNewDecisionsForAdult(tx, adult1.id) }

        assertThat(getAllVoucherValueDecisions())
            .extracting({ FiniteDateRange(it.validFrom, it.validTo) }, { it.difference })
            .containsExactlyInAnyOrder(
                Tuple(subPeriod1, emptySet<VoucherValueDecisionDifference>()),
                Tuple(subPeriod2, setOf(VoucherValueDecisionDifference.INCOME)),
            )
    }

    @Test
    fun `family size difference`() {
        val period = FiniteDateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31))
        val subPeriod1 = period.copy(end = LocalDate.of(2022, 6, 30))
        val subPeriod2 = period.copy(start = LocalDate.of(2022, 7, 1))
        insertFamilyRelations(adult1.id, listOf(child1.id), period)
        insertPlacement(child1.id, period, PlacementType.DAYCARE, voucherDaycare.id)
        insertFamilyRelations(adult1.id, listOf(child2.id), subPeriod2)

        db.transaction { tx -> generator.generateNewDecisionsForAdult(tx, adult1.id) }

        assertThat(getAllVoucherValueDecisions())
            .extracting(
                { FiniteDateRange(it.validFrom, it.validTo) },
                { it.difference },
                { it.familySize },
            )
            .containsExactlyInAnyOrder(
                Tuple(subPeriod1, emptySet<VoucherValueDecisionDifference>(), 2),
                Tuple(
                    subPeriod2,
                    setOf(
                        VoucherValueDecisionDifference.FAMILY_SIZE,
                        VoucherValueDecisionDifference.FEE_THRESHOLDS,
                    ),
                    3,
                ),
            )
    }

    @Test
    fun `placement unit difference`() {
        val period = FiniteDateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31))
        val subPeriod1 = period.copy(end = LocalDate.of(2022, 6, 30))
        val subPeriod2 = period.copy(start = LocalDate.of(2022, 7, 1))
        insertFamilyRelations(adult1.id, listOf(child1.id), period)
        insertPlacement(child1.id, subPeriod1, PlacementType.DAYCARE, voucherDaycare.id)
        insertPlacement(child1.id, subPeriod2, PlacementType.DAYCARE, voucherDaycare2.id)

        db.transaction { tx -> generator.generateNewDecisionsForAdult(tx, adult1.id) }

        assertThat(getAllVoucherValueDecisions())
            .extracting(
                { FiniteDateRange(it.validFrom, it.validTo) },
                { it.difference },
                { it.placement?.unitId },
            )
            .containsExactlyInAnyOrder(
                Tuple(subPeriod1, emptySet<VoucherValueDecisionDifference>(), voucherDaycare.id),
                Tuple(
                    subPeriod2,
                    setOf(VoucherValueDecisionDifference.PLACEMENT),
                    voucherDaycare2.id,
                ),
            )
    }

    @Test
    fun `placement type difference`() {
        val period = FiniteDateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31))
        val subPeriod1 = period.copy(end = LocalDate.of(2022, 6, 30))
        val subPeriod2 = period.copy(start = LocalDate.of(2022, 7, 1))
        insertFamilyRelations(adult1.id, listOf(child1.id), period)
        insertPlacement(child1.id, subPeriod1, PlacementType.DAYCARE, voucherDaycare.id)
        insertPlacement(child1.id, subPeriod2, PlacementType.DAYCARE_PART_TIME, voucherDaycare.id)

        db.transaction { tx -> generator.generateNewDecisionsForAdult(tx, adult1.id) }

        assertThat(getAllVoucherValueDecisions())
            .extracting(
                { FiniteDateRange(it.validFrom, it.validTo) },
                { it.difference },
                { it.placement?.type },
            )
            .containsExactlyInAnyOrder(
                Tuple(
                    subPeriod1,
                    emptySet<VoucherValueDecisionDifference>(),
                    PlacementType.DAYCARE,
                ),
                Tuple(
                    subPeriod2,
                    setOf(
                        VoucherValueDecisionDifference.PLACEMENT,
                        VoucherValueDecisionDifference.SERVICE_NEED,
                        VoucherValueDecisionDifference.VOUCHER_VALUE,
                    ),
                    PlacementType.DAYCARE_PART_TIME,
                ),
            )
    }

    @Test
    fun `service need difference`() {
        val period = FiniteDateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31))
        val subPeriod1 = period.copy(end = LocalDate.of(2022, 6, 30))
        val subPeriod2 = period.copy(start = LocalDate.of(2022, 7, 1))
        insertFamilyRelations(adult1.id, listOf(child1.id), period)
        val placementId =
            insertPlacement(child1.id, period, PlacementType.DAYCARE, voucherDaycare.id)
        insertServiceNeed(placementId, subPeriod1, snDefaultDaycare.id)
        insertServiceNeed(placementId, subPeriod2, snDaycareFullDay35.id)

        db.transaction { tx -> generator.generateNewDecisionsForAdult(tx, adult1.id) }

        assertThat(getAllVoucherValueDecisions())
            .extracting(
                { FiniteDateRange(it.validFrom, it.validTo) },
                { it.difference },
                { it.serviceNeed?.voucherValueCoefficient },
            )
            .containsExactlyInAnyOrder(
                Tuple(
                    subPeriod1,
                    emptySet<VoucherValueDecisionDifference>(),
                    serviceNeedOptionVoucherValueCoefficients[snDefaultDaycare.id],
                ),
                Tuple(
                    subPeriod2,
                    setOf(VoucherValueDecisionDifference.SERVICE_NEED),
                    serviceNeedOptionVoucherValueCoefficients[snDaycareFullDay35.id],
                ),
            )
    }

    @Test
    fun `sibling discount difference`() {
        val period = FiniteDateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31))
        val subPeriod1 = period.copy(end = LocalDate.of(2022, 6, 30))
        val subPeriod2 = period.copy(start = LocalDate.of(2022, 7, 1))
        insertFamilyRelations(adult1.id, listOf(child2.id), period)
        insertPlacement(child2.id, period, PlacementType.DAYCARE, voucherDaycare.id)
        insertFamilyRelations(adult1.id, listOf(child1.id), period)
        insertPlacement(child1.id, subPeriod2, PlacementType.DAYCARE, voucherDaycare.id)

        db.transaction { tx -> generator.generateNewDecisionsForAdult(tx, adult1.id) }

        assertThat(getAllVoucherValueDecisions())
            .extracting(
                { it.child.dateOfBirth },
                { FiniteDateRange(it.validFrom, it.validTo) },
                { it.difference },
                { it.siblingDiscount },
            )
            .containsExactlyInAnyOrder(
                Tuple(
                    child2.dateOfBirth,
                    subPeriod1,
                    emptySet<VoucherValueDecisionDifference>(),
                    0,
                ),
                Tuple(
                    child2.dateOfBirth,
                    subPeriod2,
                    setOf(
                        VoucherValueDecisionDifference.SIBLING_DISCOUNT,
                        VoucherValueDecisionDifference.CO_PAYMENT,
                        VoucherValueDecisionDifference.FINAL_CO_PAYMENT,
                    ),
                    50,
                ),
                Tuple(child1.dateOfBirth, subPeriod2, emptySet<VoucherValueDecisionDifference>(), 0),
            )
    }

    @Test
    fun `fee alterations difference`() {
        val period = FiniteDateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31))
        val subPeriod1 = period.copy(end = LocalDate.of(2022, 6, 30))
        val subPeriod2 = period.copy(start = LocalDate.of(2022, 7, 1))
        insertFamilyRelations(adult1.id, listOf(child1.id), period)
        insertPlacement(child1.id, period, PlacementType.DAYCARE, voucherDaycare.id)
        insertFeeAlteration(child1.id, 50.0, subPeriod2.asDateRange())

        db.transaction { tx -> generator.generateNewDecisionsForAdult(tx, adult1.id) }

        assertThat(getAllVoucherValueDecisions())
            .extracting({ FiniteDateRange(it.validFrom, it.validTo) }, { it.difference })
            .containsExactlyInAnyOrder(
                Tuple(subPeriod1, emptySet<VoucherValueDecisionDifference>()),
                Tuple(
                    subPeriod2,
                    setOf(
                        VoucherValueDecisionDifference.FEE_ALTERATIONS,
                        VoucherValueDecisionDifference.FINAL_CO_PAYMENT,
                    ),
                ),
            )
    }

    @Test
    fun `base value difference`() {
        val period = FiniteDateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31))
        val subPeriod1 = period.copy(end = LocalDate.of(2020, 5, 31))
        val subPeriod2 = period.copy(start = LocalDate.of(2020, 6, 1)) // 3rd birthday
        insertFamilyRelations(adult1.id, listOf(child1.id), period)
        insertPlacement(child1.id, period, PlacementType.DAYCARE, voucherDaycare.id)

        db.transaction { tx -> generator.generateNewDecisionsForAdult(tx, adult1.id) }

        assertThat(getAllVoucherValueDecisions())
            .extracting({ FiniteDateRange(it.validFrom, it.validTo) }, { it.difference })
            .containsExactlyInAnyOrder(
                Tuple(subPeriod1, emptySet<VoucherValueDecisionDifference>()),
                Tuple(
                    subPeriod2,
                    setOf(
                        VoucherValueDecisionDifference.BASE_VALUE,
                        VoucherValueDecisionDifference.VOUCHER_VALUE,
                    ),
                ),
            )
    }

    @Test
    fun `fee thresholds difference`() {
        val period = FiniteDateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31))
        val subPeriod1 = period.copy(end = LocalDate.of(2022, 6, 30))
        val subPeriod2 = period.copy(start = LocalDate.of(2022, 7, 1))
        insertFamilyRelations(adult1.id, listOf(child1.id), period)
        insertPlacement(child1.id, period, PlacementType.DAYCARE, voucherDaycare.id)
        db.transaction { tx ->
            tx.createUpdate {
                    sql(
                        "UPDATE fee_thresholds SET valid_during = daterange(lower(valid_during), ${bind(subPeriod1.end)}, '[]')"
                    )
                }
                .updateExactlyOne()
            tx.insert(
                FeeThresholds(
                    validDuring = DateRange(subPeriod2.start, null),
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
                    temporaryFeeSiblingPartDay = 800,
                )
            )
            tx.insert(
                DevIncome(
                    personId = adult1.id,
                    validFrom = period.start,
                    data =
                        mapOf(
                            "MAIN_INCOME" to
                                IncomeValue(
                                    amount = 0,
                                    coefficient = IncomeCoefficient.MONTHLY_NO_HOLIDAY_BONUS,
                                    multiplier = 1,
                                    monthlyAmount = 0,
                                )
                        ),
                    effect = IncomeEffect.INCOME,
                    modifiedBy = decisionMaker.evakaUserId,
                )
            )
        }

        db.transaction { tx -> generator.generateNewDecisionsForAdult(tx, adult1.id) }

        assertThat(getAllVoucherValueDecisions())
            .extracting({ it.validFrom }, { it.validTo }, { it.difference })
            .containsExactlyInAnyOrder(
                Tuple(subPeriod1.start, subPeriod1.end, emptySet<VoucherValueDecisionDifference>()),
                Tuple(
                    subPeriod2.start,
                    subPeriod2.end,
                    setOf(VoucherValueDecisionDifference.FEE_THRESHOLDS),
                ),
            )
    }

    @Test
    fun `difference with overlapping drafts`() {
        val period = FiniteDateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31))
        val subPeriod1 = FiniteDateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 6, 30))
        val subPeriod2 = FiniteDateRange(LocalDate.of(2022, 7, 1), LocalDate.of(2022, 12, 31))
        val subPeriod3 = FiniteDateRange(LocalDate.of(2022, 4, 1), LocalDate.of(2022, 9, 1))
        insertFamilyRelations(adult1.id, listOf(child1.id), period)
        insertPlacement(child1.id, subPeriod1, PlacementType.DAYCARE, voucherDaycare.id)
        insertPlacement(child1.id, subPeriod2, PlacementType.DAYCARE, voucherDaycare2.id)
        insertIncome(child1.id, 10000, subPeriod3.asDateRange())

        db.transaction { tx -> generator.generateNewDecisionsForAdult(tx, adult1.id) }

        assertThat(getAllVoucherValueDecisions())
            .extracting({ it.validFrom }, { it.validTo }, { it.difference })
            .containsExactlyInAnyOrder(
                Tuple(
                    LocalDate.of(2022, 1, 1),
                    LocalDate.of(2022, 3, 31),
                    emptySet<VoucherValueDecisionDifference>(),
                ),
                Tuple(
                    LocalDate.of(2022, 4, 1),
                    LocalDate.of(2022, 6, 30),
                    setOf(VoucherValueDecisionDifference.INCOME),
                ),
                Tuple(
                    LocalDate.of(2022, 7, 1),
                    LocalDate.of(2022, 9, 1),
                    setOf(VoucherValueDecisionDifference.PLACEMENT),
                ),
                Tuple(
                    LocalDate.of(2022, 9, 2),
                    LocalDate.of(2022, 12, 31),
                    setOf(VoucherValueDecisionDifference.INCOME),
                ),
            )
    }

    @Test
    fun `difference between sent and draft`() {
        val period = FiniteDateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31))
        val subPeriod1 = period.copy(end = LocalDate.of(2022, 6, 30))
        val subPeriod2 = period.copy(start = LocalDate.of(2022, 7, 1))
        insertFamilyRelations(adult1.id, listOf(child1.id), period)
        insertPlacement(child1.id, subPeriod1, PlacementType.DAYCARE, voucherDaycare.id)
        db.transaction { tx ->
            generator.generateNewDecisionsForAdult(tx, adult1.id)
            tx.createUpdate { sql("UPDATE voucher_value_decision SET status = 'SENT'") }.execute()
        }
        insertPlacement(child1.id, subPeriod2, PlacementType.DAYCARE, voucherDaycare2.id)

        db.transaction { tx -> generator.generateNewDecisionsForAdult(tx, adult1.id) }

        assertThat(getAllVoucherValueDecisions())
            .extracting(
                { FiniteDateRange(it.validFrom, it.validTo) },
                { it.status },
                { it.difference },
            )
            .containsExactlyInAnyOrder(
                Tuple(
                    subPeriod1,
                    VoucherValueDecisionStatus.SENT,
                    emptySet<VoucherValueDecisionDifference>(),
                ),
                Tuple(
                    subPeriod2,
                    VoucherValueDecisionStatus.DRAFT,
                    setOf(VoucherValueDecisionDifference.PLACEMENT),
                ),
            )
    }

    @Test
    fun `difference when drafts replaces sent`() {
        val period = FiniteDateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31))
        val subPeriod1 = period.copy(end = LocalDate.of(2022, 6, 30))
        val subPeriod2 = period.copy(start = LocalDate.of(2022, 7, 1))
        insertFamilyRelations(adult1.id, listOf(child1.id), period)
        insertPlacement(child1.id, period, PlacementType.DAYCARE, voucherDaycare.id)
        db.transaction { tx ->
            generator.generateNewDecisionsForAdult(tx, adult1.id)
            tx.createUpdate { sql("UPDATE voucher_value_decision SET status = 'SENT'") }.execute()
            tx.createUpdate { sql("DELETE FROM placement") }.execute()
        }
        insertPlacement(child1.id, subPeriod1, PlacementType.DAYCARE, voucherDaycare2.id)
        insertPlacement(child1.id, subPeriod2, PlacementType.DAYCARE, voucherDaycare.id)

        db.transaction { tx -> generator.generateNewDecisionsForAdult(tx, adult1.id) }

        assertThat(getAllVoucherValueDecisions())
            .extracting(
                { FiniteDateRange(it.validFrom, it.validTo) },
                { it.status },
                { it.difference },
            )
            .containsExactlyInAnyOrder(
                Tuple(
                    period,
                    VoucherValueDecisionStatus.SENT,
                    emptySet<VoucherValueDecisionDifference>(),
                ),
                Tuple(
                    subPeriod1,
                    VoucherValueDecisionStatus.DRAFT,
                    setOf(VoucherValueDecisionDifference.PLACEMENT),
                ),
                Tuple(
                    subPeriod2,
                    VoucherValueDecisionStatus.DRAFT,
                    setOf(VoucherValueDecisionDifference.PLACEMENT),
                ),
            )
    }

    @Test
    fun `difference when gap between two drafts`() {
        val period = FiniteDateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31))
        val subPeriod1 = period.copy(end = LocalDate.of(2022, 6, 30))
        val subPeriod2 = period.copy(start = LocalDate.of(2022, 7, 2))
        insertFamilyRelations(adult1.id, listOf(child1.id), period)
        insertPlacement(child1.id, subPeriod1, PlacementType.DAYCARE, voucherDaycare.id)
        insertPlacement(child1.id, subPeriod2, PlacementType.DAYCARE, voucherDaycare2.id)

        db.transaction { tx -> generator.generateNewDecisionsForAdult(tx, adult1.id) }

        assertThat(getAllVoucherValueDecisions())
            .extracting({ FiniteDateRange(it.validFrom, it.validTo) }, { it.difference })
            .containsExactlyInAnyOrder(
                Tuple(subPeriod1, emptySet<VoucherValueDecisionDifference>()),
                Tuple(subPeriod2, emptySet<VoucherValueDecisionDifference>()),
            )
    }

    @Test
    fun `difference when gap between sent and draft`() {
        val period = FiniteDateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31))
        val subPeriod1 = period.copy(end = LocalDate.of(2022, 6, 30))
        val subPeriod2 = period.copy(start = LocalDate.of(2022, 7, 2))
        insertFamilyRelations(adult1.id, listOf(child1.id), period)
        insertPlacement(child1.id, subPeriod1, PlacementType.DAYCARE, voucherDaycare.id)
        db.transaction { tx ->
            generator.generateNewDecisionsForAdult(tx, adult1.id)
            tx.createUpdate { sql("UPDATE voucher_value_decision SET status = 'SENT'") }.execute()
        }
        insertPlacement(child1.id, subPeriod2, PlacementType.DAYCARE, voucherDaycare2.id)

        db.transaction { tx -> generator.generateNewDecisionsForAdult(tx, adult1.id) }

        assertThat(getAllVoucherValueDecisions())
            .extracting(
                { FiniteDateRange(it.validFrom, it.validTo) },
                { it.status },
                { it.difference },
            )
            .containsExactlyInAnyOrder(
                Tuple(
                    subPeriod1,
                    VoucherValueDecisionStatus.SENT,
                    emptySet<VoucherValueDecisionDifference>(),
                ),
                Tuple(
                    subPeriod2,
                    VoucherValueDecisionStatus.DRAFT,
                    emptySet<VoucherValueDecisionDifference>(),
                ),
            )
    }

    @Test
    fun `duplicate sent voucher value decision is not generated if there is a draft in the past`() {
        val testClock = MockEvakaClock(2023, 1, 1, 16, 30)
        val period = FiniteDateRange(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31))
        val subPeriod1 = period.copy(end = LocalDate.of(2022, 6, 30))
        val subPeriod2 = period.copy(start = LocalDate.of(2022, 7, 1))
        insertFamilyRelations(adult1.id, listOf(child1.id), period)
        insertPlacement(child1.id, subPeriod1, PlacementType.DAYCARE, voucherDaycare.id)
        insertPlacement(child1.id, subPeriod2, PlacementType.DAYCARE, voucherDaycare2.id)

        db.transaction { generator.generateNewDecisionsForAdult(it, adult1.id) }
        val decisions = getAllVoucherValueDecisions()
        assertEquals(2, decisions.size)
        assertEquals(2, decisions.filter { it.status == VoucherValueDecisionStatus.DRAFT }.size)

        val firstDecision = decisions.first { it.validTo == subPeriod2.end }

        voucherValueDecisionController.sendVoucherValueDecisionDrafts(
            dbInstance(),
            employee.user,
            testClock,
            listOf(firstDecision.id),
            null,
        )

        asyncJobRunner.runPendingJobsSync(testClock)

        getAllVoucherValueDecisions().let {
            assertEquals(2, it.size)
            assertEquals(1, it.filter { d -> d.status == VoucherValueDecisionStatus.SENT }.size)
            assertEquals(1, it.filter { d -> d.status == VoucherValueDecisionStatus.DRAFT }.size)
        }

        db.transaction { generator.generateNewDecisionsForAdult(it, adult1.id) }

        getAllVoucherValueDecisions().let {
            assertEquals(2, it.size)
            assertEquals(1, it.filter { d -> d.status == VoucherValueDecisionStatus.SENT }.size)
            assertEquals(1, it.filter { d -> d.status == VoucherValueDecisionStatus.DRAFT }.size)
        }
    }

    private fun insertPlacement(
        childId: ChildId,
        period: FiniteDateRange,
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
                    endDate = period.end,
                )
            )
        }
    }

    private fun insertFamilyRelations(
        headOfFamilyId: PersonId,
        childIds: List<ChildId>,
        period: FiniteDateRange,
    ) {
        db.transaction { tx ->
            childIds.forEach { childId ->
                tx.insert(
                    DevParentship(
                        childId = childId,
                        headOfChildId = headOfFamilyId,
                        startDate = period.start,
                        endDate = period.end,
                    )
                )
            }
        }
    }

    private fun insertPartnership(
        adultId1: PersonId,
        adultId2: PersonId,
        period: DateRange,
        createdAt: HelsinkiDateTime,
    ) {
        db.transaction { tx ->
            tx.insertTestPartnership(
                adultId1,
                adultId2,
                startDate = period.start,
                endDate = period.end,
                createdAt = createdAt,
            )
        }
    }

    private fun insertServiceNeed(
        placementId: PlacementId,
        period: FiniteDateRange,
        optionId: ServiceNeedOptionId,
    ) {
        db.transaction { tx ->
            tx.insert(
                DevServiceNeed(
                    placementId = placementId,
                    startDate = period.start,
                    endDate = period.end,
                    optionId = optionId,
                    shiftCare = ShiftCareType.NONE,
                    partWeek = false,
                    confirmedBy = decisionMaker.evakaUserId,
                    confirmedAt = HelsinkiDateTime.now(),
                )
            )
        }
    }

    private fun insertAssistanceNeedCoefficient(
        childId: ChildId,
        period: FiniteDateRange,
        coefficient: Double,
    ) {
        db.transaction { tx ->
            tx.insertAssistanceNeedVoucherCoefficient(
                employee.user,
                clock.now(),
                childId,
                AssistanceNeedVoucherCoefficientRequest(coefficient, period),
            )
        }
    }

    private fun getAllVoucherValueDecisions(): List<VoucherValueDecision> {
        return db.read { tx ->
                tx.createQuery { sql("SELECT * FROM voucher_value_decision") }
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
                                        ),
                                    ),
                                )
                        ),
                    modifiedBy = decisionMaker.evakaUserId,
                )
            )
        }
    }

    private fun insertFeeAlteration(personId: PersonId, amount: Double, period: DateRange) {
        db.transaction { tx ->
            tx.insert(
                DevFeeAlteration(
                    personId = personId,
                    type = FeeAlterationType.DISCOUNT,
                    amount = amount,
                    isAbsolute = false,
                    validFrom = period.start,
                    validTo = period.end,
                    modifiedBy = decisionMaker.evakaUserId,
                )
            )
        }
    }
}
