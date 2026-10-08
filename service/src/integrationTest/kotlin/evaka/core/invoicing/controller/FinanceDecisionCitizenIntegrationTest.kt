// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.invoicing.controller

import evaka.core.FullApplicationTest
import evaka.core.application.ApplicationControllerCitizen
import evaka.core.application.FinanceDecisionChildInfo
import evaka.core.application.FinanceDecisionCitizenInfo
import evaka.core.application.LiableCitizenInfo
import evaka.core.insertServiceNeedOptions
import evaka.core.invoicing.createFeeDecisionChildFixture
import evaka.core.invoicing.data.markVoucherValueDecisionsSent
import evaka.core.invoicing.data.setFeeDecisionPartnerIsCodebtor
import evaka.core.invoicing.data.setFeeDecisionSent
import evaka.core.invoicing.data.setVoucherValueDecisionPartnerIsCodebtor
import evaka.core.invoicing.data.updateFeeDecisionDocumentKey
import evaka.core.invoicing.data.updateVoucherValueDecisionDocumentKey
import evaka.core.invoicing.data.upsertFeeDecisions
import evaka.core.invoicing.data.upsertValueDecisions
import evaka.core.invoicing.domain.ChildWithDateOfBirth
import evaka.core.invoicing.domain.DecisionIncome
import evaka.core.invoicing.domain.FeeAlterationWithEffect
import evaka.core.invoicing.domain.FeeDecision
import evaka.core.invoicing.domain.FeeDecisionChild
import evaka.core.invoicing.domain.FeeDecisionStatus
import evaka.core.invoicing.domain.FeeDecisionThresholds
import evaka.core.invoicing.domain.FeeDecisionType
import evaka.core.invoicing.domain.FinanceDecisionType
import evaka.core.invoicing.domain.VoucherValueDecision
import evaka.core.invoicing.domain.VoucherValueDecisionPlacement
import evaka.core.invoicing.domain.VoucherValueDecisionServiceNeed
import evaka.core.invoicing.domain.VoucherValueDecisionStatus
import evaka.core.invoicing.domain.VoucherValueDecisionType
import evaka.core.invoicing.testFeeThresholds
import evaka.core.placement.PlacementType
import evaka.core.shared.ChildId
import evaka.core.shared.DaycareId
import evaka.core.shared.FeeDecisionId
import evaka.core.shared.PersonId
import evaka.core.shared.VoucherValueDecisionId
import evaka.core.shared.auth.CitizenAuthLevel
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.Forbidden
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import evaka.core.shared.security.AccessControl
import evaka.core.shared.security.Action
import evaka.core.snDaycareFullDay35
import evaka.core.toFeeDecisionServiceNeed
import evaka.core.toValueDecisionServiceNeed
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class FinanceDecisionCitizenIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {

    @Autowired private lateinit var applicationControllerCitizen: ApplicationControllerCitizen
    @Autowired private lateinit var accessControl: AccessControl

    private val clock =
        MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2022, 10, 23), LocalTime.of(21, 0)))

    private val area = DevCareArea()
    private val daycare = DevDaycare(areaId = area.id)
    private val headOfFamily = DevPerson()
    private val partner = DevPerson()
    private val child = DevPerson(dateOfBirth = LocalDate.of(2020, 2, 2))

    private val testPeriod1 = FiniteDateRange(LocalDate.of(2018, 5, 1), LocalDate.of(2018, 5, 31))
    private val testPeriod2 =
        FiniteDateRange(testPeriod1.end.plusDays(1), testPeriod1.end.plusDays(1).plusMonths(1))
    private val fdId = FeeDecisionId(UUID.randomUUID())
    private val feeDecisionSentAt = HelsinkiDateTime.atStartOfDay(LocalDate.of(2018, 5, 1))
    private val vvdId = VoucherValueDecisionId(UUID.randomUUID())
    private val voucherValueSentAt = HelsinkiDateTime.atStartOfDay(LocalDate.of(2018, 6, 1))

    private val testFeeDecisions =
        listOf(
            createTestFeeDecision(
                id = fdId,
                decisionType = FeeDecisionType.NORMAL,
                headOfFamilyId = headOfFamily.id,
                partnerId = partner.id,
                period = testPeriod1,
                children =
                    listOf(
                        createFeeDecisionChildFixture(
                            childId = child.id,
                            dateOfBirth = child.dateOfBirth,
                            placementUnitId = daycare.id,
                            placementType = PlacementType.DAYCARE,
                            serviceNeed = snDaycareFullDay35.toFeeDecisionServiceNeed(),
                        )
                    ),
            )
        )

    private val feeDecisions = testFeeDecisions.map {
        FinanceDecisionCitizenInfo(
            id = it.id.raw,
            type = FinanceDecisionType.FEE_DECISION,
            validFrom = it.validFrom,
            validTo = it.validTo,
            sentAt = feeDecisionSentAt,
            coDebtors =
                listOf(
                    LiableCitizenInfo(partner.id, partner.firstName, partner.lastName),
                    LiableCitizenInfo(
                        headOfFamily.id,
                        headOfFamily.firstName,
                        headOfFamily.lastName,
                    ),
                ),
            decisionChildren = emptyList(),
        )
    }

    private val testVoucherValueDecisions =
        listOf(
            createTestVoucherValueDecision(
                id = vvdId,
                validFrom = testPeriod2.start,
                validTo = testPeriod2.end,
                headOfFamilyId = headOfFamily.id,
                childId = child.id,
                dateOfBirth = child.dateOfBirth,
                unitId = daycare.id,
                placementType = PlacementType.DAYCARE,
                serviceNeed = snDaycareFullDay35.toValueDecisionServiceNeed(),
            )
        )

    private val voucherValueDecisions = testVoucherValueDecisions.map {
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
                        headOfFamily.lastName,
                    )
                ),
            decisionChildren =
                listOf(FinanceDecisionChildInfo(child.id, child.firstName, child.lastName)),
        )
    }

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insertServiceNeedOptions()
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(headOfFamily, DevPersonType.ADULT)
            tx.insert(partner, DevPersonType.ADULT)
            tx.insert(child, DevPersonType.CHILD)
            tx.upsertFeeDecisions(testFeeDecisions)
            tx.setFeeDecisionSent(
                ids = listOf(fdId),
                clock = MockEvakaClock(now = feeDecisionSentAt),
            )
            tx.updateFeeDecisionDocumentKey(fdId, "test-fd-document-key")
            tx.upsertValueDecisions(testVoucherValueDecisions)
            tx.markVoucherValueDecisionsSent(ids = listOf(vvdId), now = voucherValueSentAt)
            tx.updateVoucherValueDecisionDocumentKey(vvdId, "test-vvd-document-key")
        }
    }

    @Test
    fun `finance decisions found for head of family`() {

        val financeDecisions =
            applicationControllerCitizen.getLiableCitizenFinanceDecisions(
                dbInstance(),
                headOfFamily.user(CitizenAuthLevel.STRONG),
                clock,
            )

        Assertions.assertThatIterable(financeDecisions)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(feeDecisions + voucherValueDecisions)
    }

    @Test
    fun `fee decision found for partner when codebtor status is not stored`() {
        // Decisions created before the codebtor status was stored have partner_is_codebtor NULL

        val financeDecisions =
            applicationControllerCitizen.getLiableCitizenFinanceDecisions(
                dbInstance(),
                partner.user(CitizenAuthLevel.STRONG),
                clock,
            )

        Assertions.assertThatIterable(financeDecisions)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(feeDecisions)
    }

    @Test
    fun `fee decision found for partner who is a codebtor`() {
        db.transaction { tx -> tx.setFeeDecisionPartnerIsCodebtor(fdId, true) }

        val financeDecisions =
            applicationControllerCitizen.getLiableCitizenFinanceDecisions(
                dbInstance(),
                partner.user(CitizenAuthLevel.STRONG),
                clock,
            )

        Assertions.assertThatIterable(financeDecisions)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(feeDecisions)
        assertTrue(canDownloadFeeDecision(partner, fdId))
    }

    @Test
    fun `fee decision not found for partner who is not a codebtor`() {
        db.transaction { tx -> tx.setFeeDecisionPartnerIsCodebtor(fdId, false) }

        val partnerDecisions =
            applicationControllerCitizen.getLiableCitizenFinanceDecisions(
                dbInstance(),
                partner.user(CitizenAuthLevel.STRONG),
                clock,
            )
        val headOfFamilyDecisions =
            applicationControllerCitizen.getLiableCitizenFinanceDecisions(
                dbInstance(),
                headOfFamily.user(CitizenAuthLevel.STRONG),
                clock,
            )

        Assertions.assertThat(partnerDecisions).isEmpty()
        Assertions.assertThat(headOfFamilyDecisions.single { it.id == fdId.raw }.coDebtors)
            .containsExactly(headOfFamilyInfo)
        assertFalse(canDownloadFeeDecision(partner, fdId))
        assertTrue(canDownloadFeeDecision(headOfFamily, fdId))
    }

    @Test
    fun `voucher value decision found for partner who is a codebtor`() {
        val decisionId = insertSentVoucherValueDecisionWithPartner(partnerIsCodebtor = true)

        val financeDecisions =
            applicationControllerCitizen.getLiableCitizenFinanceDecisions(
                dbInstance(),
                partner.user(CitizenAuthLevel.STRONG),
                clock,
            )

        Assertions.assertThat(financeDecisions.map { it.id }).contains(decisionId.raw)
        assertTrue(canDownloadVoucherValueDecision(partner, decisionId))
    }

    @Test
    fun `voucher value decision not found for partner who is not a codebtor`() {
        val decisionId = insertSentVoucherValueDecisionWithPartner(partnerIsCodebtor = false)

        val partnerDecisions =
            applicationControllerCitizen.getLiableCitizenFinanceDecisions(
                dbInstance(),
                partner.user(CitizenAuthLevel.STRONG),
                clock,
            )
        val headOfFamilyDecisions =
            applicationControllerCitizen.getLiableCitizenFinanceDecisions(
                dbInstance(),
                headOfFamily.user(CitizenAuthLevel.STRONG),
                clock,
            )

        Assertions.assertThat(partnerDecisions.map { it.id }).doesNotContain(decisionId.raw)
        Assertions.assertThat(headOfFamilyDecisions.single { it.id == decisionId.raw }.coDebtors)
            .containsExactly(headOfFamilyInfo)
        assertFalse(canDownloadVoucherValueDecision(partner, decisionId))
        assertTrue(canDownloadVoucherValueDecision(headOfFamily, decisionId))
    }

    @Test
    fun `no permission without strong auth`() {

        assertThrows<Forbidden> {
            applicationControllerCitizen.getLiableCitizenFinanceDecisions(
                dbInstance(),
                headOfFamily.user(CitizenAuthLevel.WEAK),
                clock,
            )
        }
    }

    private val headOfFamilyInfo =
        LiableCitizenInfo(headOfFamily.id, headOfFamily.firstName, headOfFamily.lastName)

    private fun insertSentVoucherValueDecisionWithPartner(
        partnerIsCodebtor: Boolean
    ): VoucherValueDecisionId {
        val decision =
            createTestVoucherValueDecision(
                id = VoucherValueDecisionId(UUID.randomUUID()),
                validFrom = testPeriod2.end.plusDays(1),
                validTo = testPeriod2.end.plusMonths(1),
                headOfFamilyId = headOfFamily.id,
                partnerId = partner.id,
                childId = child.id,
                dateOfBirth = child.dateOfBirth,
                unitId = daycare.id,
                placementType = PlacementType.DAYCARE,
                serviceNeed = snDaycareFullDay35.toValueDecisionServiceNeed(),
            )
        db.transaction { tx ->
            tx.upsertValueDecisions(listOf(decision))
            tx.markVoucherValueDecisionsSent(ids = listOf(decision.id), now = voucherValueSentAt)
            tx.updateVoucherValueDecisionDocumentKey(decision.id, "test-vvd-document-key")
            tx.setVoucherValueDecisionPartnerIsCodebtor(decision.id, partnerIsCodebtor)
        }
        return decision.id
    }

    private fun canDownloadFeeDecision(citizen: DevPerson, id: FeeDecisionId) = db.read { tx ->
        accessControl.hasPermissionFor(
            tx,
            citizen.user(CitizenAuthLevel.STRONG),
            clock,
            Action.Citizen.FeeDecision.DOWNLOAD,
            id,
        )
    }

    private fun canDownloadVoucherValueDecision(citizen: DevPerson, id: VoucherValueDecisionId) =
        db.read { tx ->
            accessControl.hasPermissionFor(
                tx,
                citizen.user(CitizenAuthLevel.STRONG),
                clock,
                Action.Citizen.VoucherValueDecision.DOWNLOAD,
                id,
            )
        }

    private fun createTestVoucherValueDecision(
        id: VoucherValueDecisionId,
        validFrom: LocalDate,
        validTo: LocalDate,
        headOfFamilyId: PersonId,
        partnerId: PersonId? = null,
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
        feeAlterations: List<FeeAlterationWithEffect> = listOf(),
        documentKey: String = "test-voucher-document-key",
    ) =
        VoucherValueDecision(
            id = id,
            status = VoucherValueDecisionStatus.DRAFT,
            decisionType = VoucherValueDecisionType.NORMAL,
            validFrom = validFrom,
            validTo = validTo,
            headOfFamilyId = headOfFamilyId,
            partnerId = partnerId,
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
            difference = emptySet(),
            documentKey = documentKey,
        )

    private fun createTestFeeDecision(
        id: FeeDecisionId,
        decisionType: FeeDecisionType,
        period: FiniteDateRange,
        headOfFamilyId: PersonId,
        children: List<FeeDecisionChild>,
        partnerId: PersonId? = null,
        feeThresholds: FeeDecisionThresholds =
            testFeeThresholds.getFeeDecisionThresholds(children.size + 1),
        headOfFamilyIncome: DecisionIncome? = null,
        partnerIncome: DecisionIncome? = null,
        familySize: Int = children.size + 1 + if (partnerId != null) 1 else 0,
        created: HelsinkiDateTime = HelsinkiDateTime.now(),
        documentKey: String = "test-fee-document-key",
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
            created = created,
            documentKey = documentKey,
        )
}
