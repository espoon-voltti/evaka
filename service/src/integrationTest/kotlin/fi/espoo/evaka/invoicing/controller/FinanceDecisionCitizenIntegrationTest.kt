// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.controller

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.application.ApplicationControllerCitizen
import fi.espoo.evaka.application.FinanceDecisionChildInfo
import fi.espoo.evaka.application.FinanceDecisionCitizenInfo
import fi.espoo.evaka.application.LiableCitizenInfo
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.invoicing.createFeeDecisionChildFixture
import fi.espoo.evaka.invoicing.data.markVoucherValueDecisionsSent
import fi.espoo.evaka.invoicing.data.setFeeDecisionSent
import fi.espoo.evaka.invoicing.data.upsertFeeDecisions
import fi.espoo.evaka.invoicing.data.upsertValueDecisions
import fi.espoo.evaka.invoicing.domain.ChildWithDateOfBirth
import fi.espoo.evaka.invoicing.domain.DecisionIncome
import fi.espoo.evaka.invoicing.domain.FeeAlterationWithEffect
import fi.espoo.evaka.invoicing.domain.FeeDecision
import fi.espoo.evaka.invoicing.domain.FeeDecisionChild
import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
import fi.espoo.evaka.invoicing.domain.FeeDecisionThresholds
import fi.espoo.evaka.invoicing.domain.FeeDecisionType
import fi.espoo.evaka.invoicing.domain.FinanceDecisionType
import fi.espoo.evaka.invoicing.domain.VoucherValueDecision
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionPlacement
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionServiceNeed
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionStatus
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionType
import fi.espoo.evaka.invoicing.testFeeThresholds
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.FeeDecisionId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.VoucherValueDecisionId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.security.upsertEmployeeUser
import fi.espoo.evaka.snDaycareFullDay35
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.toFeeDecisionServiceNeed
import fi.espoo.evaka.toValueDecisionServiceNeed
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertContains
import kotlin.test.assertEquals

class FinanceDecisionCitizenIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {

    @Autowired private lateinit var applicationControllerCitizen: ApplicationControllerCitizen

    private val clock =
        MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2022, 10, 23), LocalTime.of(21, 0)))

    private val testChild =
        DevPerson(
            id = ChildId(UUID.randomUUID()),
            dateOfBirth = LocalDate.of(2017, 6, 1),
            ssn = "010617A123U",
            firstName = "Ricky",
            lastName = "Doe",
            streetAddress = "Kamreerintie 2",
            postalCode = "02770",
            postOffice = "Espoo",
            restrictedDetailsEnabled = false
        )

    private lateinit var headOfFamily: DevPerson
    private lateinit var partner: DevPerson

    private lateinit var employeeId: EmployeeId
    private lateinit var employeeEvakaUserId: EvakaUserId
    private lateinit var hofAuthenticatedUser: AuthenticatedUser.Citizen
    private lateinit var partnerAuthenticatedUser: AuthenticatedUser.Citizen
    private lateinit var feeDecisions: List<FinanceDecisionCitizenInfo>
    private lateinit var voucherValueDecisions: List<FinanceDecisionCitizenInfo>

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insertGeneralTestFixtures()

            headOfFamily =
                DevPerson(id = PersonId(UUID.randomUUID()), firstName = "Hof", lastName = "Person")
            partner =
                DevPerson(
                    id = PersonId(UUID.randomUUID()),
                    firstName = "Partner",
                    lastName = "Person"
                )

            tx.insert(headOfFamily, DevPersonType.ADULT)

            tx.insert(partner, DevPersonType.ADULT)
            hofAuthenticatedUser =
                AuthenticatedUser.Citizen(headOfFamily.id, CitizenAuthLevel.STRONG)
            partnerAuthenticatedUser =
                AuthenticatedUser.Citizen(partner.id, CitizenAuthLevel.STRONG)

            employeeId = tx.insert(DevEmployee(roles = setOf(UserRole.SERVICE_WORKER)))
            tx.upsertEmployeeUser(employeeId)
            employeeEvakaUserId = EvakaUserId(employeeId.raw)

            val testPeriod1 = FiniteDateRange(LocalDate.of(2018, 5, 1), LocalDate.of(2018, 5, 31))
            val testPeriod2 =
                DateRange(testPeriod1.end.plusDays(1), testPeriod1.end.plusDays(1).plusMonths(1))

            val fdId = FeeDecisionId(UUID.randomUUID())
            val testFeeDecisions =
                listOf(
                    createTestFeeDecision(
                        id = fdId,
                        decisionType = FeeDecisionType.NORMAL,
                        headOfFamilyId = headOfFamily.id,
                        partnerId = partner.id,
                        period = DateRange(LocalDate.of(2018, 5, 1), LocalDate.of(2018, 5, 31)),
                        children =
                            listOf(
                                createFeeDecisionChildFixture(
                                    childId = testChild_1.id,
                                    dateOfBirth = testChild_1.dateOfBirth,
                                    placementUnitId = testDaycare.id,
                                    placementType = PlacementType.DAYCARE,
                                    serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                                ),
                                createFeeDecisionChildFixture(
                                    childId = testChild_2.id,
                                    dateOfBirth = testChild_2.dateOfBirth,
                                    placementUnitId = testDaycare.id,
                                    placementType = PlacementType.DAYCARE,
                                    serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                                    siblingDiscount = 50,
                                    fee = 14500
                                )
                            )
                    )
                )

            val feeDecisionSentAt = HelsinkiDateTime.atStartOfDay(LocalDate.of(2018, 5, 1))
            tx.upsertFeeDecisions(testFeeDecisions)
            tx.setFeeDecisionSent(
                ids = listOf(fdId),
                clock =
                    MockEvakaClock(now = feeDecisionSentAt)
            )

            feeDecisions =
                testFeeDecisions.map {
                    FinanceDecisionCitizenInfo(
                        id = it.id.raw,
                        type = FinanceDecisionType.FEE_DECISION,
                        validFrom = it.validFrom,
                        validTo = it.validTo,
                        sentAt = feeDecisionSentAt,
                        coDebtors =
                            listOf(
                                LiableCitizenInfo(
                                    headOfFamily.id,
                                    headOfFamily.firstName,
                                    headOfFamily.lastName
                                ),
                                LiableCitizenInfo(partner.id, partner.firstName, partner.lastName)
                            ),
                        decisionChildren = emptyList()
                    )
                }

            val vvdId = VoucherValueDecisionId(UUID.randomUUID())
            val testVoucherValueDecisions =
                listOf(
                    createTestVoucherValueDecision(
                        id = vvdId,
                        validFrom = testPeriod2.start,
                        validTo = testPeriod2.end,
                        headOfFamilyId = headOfFamily.id,
                        childId = testChild_1.id,
                        dateOfBirth = testChild_1.dateOfBirth,
                        unitId = testDaycare.id,
                        placementType = PlacementType.DAYCARE,
                        serviceNeed = snDaycareFullDay35.toValueDecisionServiceNeed()
                    )
                )
            val voucherValueSentAt = HelsinkiDateTime.atStartOfDay(LocalDate.of(2018, 6, 1))
            tx.upsertValueDecisions(testVoucherValueDecisions)
            tx.markVoucherValueDecisionsSent(
                ids = listOf(vvdId),
                now = voucherValueSentAt
            )

            voucherValueDecisions =
                testVoucherValueDecisions.map {
                    FinanceDecisionCitizenInfo(
                        id = it.id.raw,
                        type = FinanceDecisionType.VOUCHER_VALUE_DECISION,
                        validFrom = it.validFrom,
                        validTo = it.validTo,
                        sentAt = voucherValueSentAt,
                        coDebtors =
                            listOf(
                                LiableCitizenInfo(
                                    headOfFamily.id,
                                    headOfFamily.firstName,
                                    headOfFamily.lastName
                                )
                            ),
                        decisionChildren =
                            listOf(
                                FinanceDecisionChildInfo(
                                    testChild_1.id,
                                    testChild_1.firstName,
                                    testChild_1.lastName
                                )
                            )
                    )
                }
        }
    }

    @Test
    fun `finance decisions found for head of family`() {

        val financeDecisions =
            applicationControllerCitizen.getLiableCitizenFinanceDecisions(
                dbInstance(),
                hofAuthenticatedUser,
                clock
            )

        assertEquals(2, financeDecisions.size)
        feeDecisions.forEach {
            assertFinanceDecisionCitizenInfo()
        }
        voucherValueDecisions.forEach {
            assertContains(financeDecisions, it)
        }
    }

    @Test
    fun `fee decision found for partner`() {

        val financeDecisions =
            applicationControllerCitizen.getLiableCitizenFinanceDecisions(
                dbInstance(),
                partnerAuthenticatedUser,
                clock
            )

        assertEquals(1, financeDecisions.size)
    }


    fun assertFinanceDecisionCitizenInfo(expected: FinanceDecisionCitizenInfo, actual: FinanceDecisionCitizenInfo) {
        Assertions.assertThat(actual).usingRecursiveAssertion().ignoringFields("coDebtors", "decisionChildren").isEqualTo(expected)
        Assertions.assertThat(actual.coDebtors).containsAll(expected.coDebtors)
        Assertions.assertThat(actual.decisionChildren).containsAll(expected.decisionChildren)
    }

    private fun createTestVoucherValueDecision(
        id: VoucherValueDecisionId,
        validFrom: LocalDate,
        validTo: LocalDate?,
        headOfFamilyId: PersonId,
        childId: ChildId,
        dateOfBirth: LocalDate,
        unitId: DaycareId,
        familySize: Int = 2,
        placementType: PlacementType,
        serviceNeed: VoucherValueDecisionServiceNeed,
        baseValue: Int = 87000,
        assistanceNeedCoefficient: BigDecimal = BigDecimal("1.00"),
        value: Int = 87000,
        baseCoPayment: Int = 28900,
        siblingDiscount: Int = 0,
        coPayment: Int = 28900,
        feeAlterations: List<FeeAlterationWithEffect> = listOf()
    ) =
        VoucherValueDecision(
            id = id,
            status = VoucherValueDecisionStatus.DRAFT,
            decisionType = VoucherValueDecisionType.NORMAL,
            validFrom = validFrom,
            validTo = validTo,
            headOfFamilyId = headOfFamilyId,
            partnerId = null,
            headOfFamilyIncome = null,
            partnerIncome = null,
            childIncome = null,
            familySize = familySize,
            feeThresholds = testFeeThresholds.getFeeDecisionThresholds(familySize),
            child = ChildWithDateOfBirth(id = childId, dateOfBirth = dateOfBirth),
            placement = VoucherValueDecisionPlacement(unitId, placementType),
            serviceNeed = serviceNeed,
            baseValue = baseValue,
            assistanceNeedCoefficient = assistanceNeedCoefficient,
            voucherValue = value,
            baseCoPayment = baseCoPayment,
            siblingDiscount = siblingDiscount,
            coPayment = coPayment,
            feeAlterations = feeAlterations,
            finalCoPayment = coPayment + feeAlterations.sumOf { it.effect },
            difference = emptySet()
        )

    private fun createTestFeeDecision(
        id: FeeDecisionId,
        decisionType: FeeDecisionType,
        period: DateRange,
        headOfFamilyId: PersonId,
        children: List<FeeDecisionChild>,
        partnerId: PersonId? = null,
        feeThresholds: FeeDecisionThresholds =
            testFeeThresholds.getFeeDecisionThresholds(children.size + 1),
        headOfFamilyIncome: DecisionIncome? = null,
        partnerIncome: DecisionIncome? = null,
        familySize: Int = children.size + 1 + if (partnerId != null) 1 else 0,
        created: HelsinkiDateTime = HelsinkiDateTime.now()
    ) =
        FeeDecision(
            id = id,
            status = FeeDecisionStatus.DRAFT,
            decisionType = decisionType,
            validDuring = period,
            headOfFamilyId = headOfFamilyId,
            partnerId = partnerId,
            headOfFamilyIncome = headOfFamilyIncome,
            partnerIncome = partnerIncome,
            familySize = familySize,
            feeThresholds = feeThresholds,
            children = children,
            difference = emptySet(),
            created = created
        )
}
