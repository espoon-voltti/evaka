// SPDX-FileCopyrightText: 2021-2022 City of Tampere
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.tampere.invoice.config

import evaka.core.invoicing.data.invoiceDetailedQuery
import evaka.core.invoicing.data.upsertFeeDecisions
import evaka.core.invoicing.domain.ChildWithDateOfBirth
import evaka.core.invoicing.domain.DecisionIncome
import evaka.core.invoicing.domain.FeeAlterationWithEffect
import evaka.core.invoicing.domain.FeeDecision
import evaka.core.invoicing.domain.FeeDecisionChild
import evaka.core.invoicing.domain.FeeDecisionDifference
import evaka.core.invoicing.domain.FeeDecisionPlacement
import evaka.core.invoicing.domain.FeeDecisionServiceNeed
import evaka.core.invoicing.domain.FeeDecisionStatus
import evaka.core.invoicing.domain.FeeDecisionThresholds
import evaka.core.invoicing.domain.FeeDecisionType
import evaka.core.invoicing.domain.FeeThresholds
import evaka.core.invoicing.domain.InvoiceDetailed
import evaka.core.invoicing.service.InvoiceGenerator
import evaka.core.placement.PlacementType
import evaka.core.serviceneed.ServiceNeedOption
import evaka.core.serviceneed.findServiceNeedOptionById
import evaka.core.shared.AreaId
import evaka.core.shared.ChildId
import evaka.core.shared.DaycareId
import evaka.core.shared.EvakaUserId
import evaka.core.shared.FeeDecisionId
import evaka.core.shared.ParentshipId
import evaka.core.shared.PersonId
import evaka.core.shared.ServiceNeedOptionId
import evaka.core.shared.db.Database
import evaka.core.shared.db.Predicate
import evaka.core.shared.dev.DevChild
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevParentship
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.DevPlacement
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.DateRange
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.security.PilotFeature
import evaka.instance.tampere.AbstractTampereIntegrationTest
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class InvoiceConfigurationIT : AbstractTampereIntegrationTest() {

    @Autowired private lateinit var generator: InvoiceGenerator

    private final val questionnaireId = UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")
    private final val evakaUserId = EvakaUserId(UUID.randomUUID())
    private final val placementPeriod =
        FiniteDateRange(LocalDate.of(2021, 8, 31), LocalDate.of(2022, 8, 31))

    @BeforeEach
    fun insertBaseData() {
        val serviceNeedOption =
            db.transaction { tx ->
                tx.createUpdate {
                        sql(
                            """
                INSERT INTO holiday_period_questionnaire(id, type, absence_type, requires_strong_auth, active, title, description, description_link, condition_continuous_placement, period_options, period_option_label)
                VALUES (:questionnaireId, 'FIXED_PERIOD', 'FREE_ABSENCE', false, daterange('2022-04-13', '2022-04-29'), '{"fi": "title"}', '{"fi": "description"}', '{"fi": "link"}', daterange('2021-08-31', '2022-06-30'), array[daterange('2022-06-06', '2022-07-31', '[]'), daterange('2022-06-13', '2022-08-07', '[]'), daterange('2022-06-20', '2022-08-14', '[]'), daterange('2022-06-27', '2022-08-21', '[]'), daterange('2022-07-04', '2022-08-28', '[]')], '{"fi": "period option label"}')
                """
                        )
                    }
                    .bind("questionnaireId", questionnaireId)
                    .execute()
                tx.insert(
                    FeeThresholds(
                        validDuring = DateRange(LocalDate.of(2000, 1, 1), null),
                        minIncomeThreshold2 = 210200,
                        minIncomeThreshold3 = 271300,
                        minIncomeThreshold4 = 308000,
                        minIncomeThreshold5 = 344700,
                        minIncomeThreshold6 = 381300,
                        maxIncomeThreshold2 = 479900,
                        maxIncomeThreshold3 = 541000,
                        maxIncomeThreshold4 = 577700,
                        maxIncomeThreshold5 = 614400,
                        maxIncomeThreshold6 = 651000,
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
                tx.insert(testDaycare)
                tx.insert(testChild, DevPersonType.CHILD)
                tx.insert(DevChild(testChild.id))
                tx.insert(testAdult, DevPersonType.ADULT)
                tx.insert(testParentship)
                tx.createUpdate {
                        sql(
                            "INSERT INTO evaka_user (id, type, name) VALUES (:id, 'UNKNOWN', 'integration-test')"
                        )
                    }
                    .bind("id", evakaUserId)
                    .execute()
                tx.findServiceNeedOptionById(
                    ServiceNeedOptionId(UUID.fromString("86ef70a0-bf85-11eb-91e6-1fb57a101161"))
                )!!
            }
        val decisions =
            listOf(
                createFeeDecisionFixture(
                    FeeDecisionStatus.SENT,
                    FeeDecisionType.NORMAL,
                    placementPeriod,
                    testAdult.id,
                    listOf(
                        createFeeDecisionChildFixture(
                            childId = testChild.id,
                            dateOfBirth = testChild.dateOfBirth,
                            placementUnitId = testDaycare.id,
                            placementType = PlacementType.DAYCARE,
                            serviceNeed = serviceNeedOption.toFeeDecisionServiceNeed(),
                            baseFee = 28900,
                            fee = 28900,
                            feeAlterations = listOf(),
                        )
                    ),
                )
            )
        insertDecisionsAndPlacements(decisions)
    }

    @Test
    fun getAllInvoices() {
        val result = db.read { getAllInvoices(it) }
        Assertions.assertEquals(0, result.size)
    }

    @Test
    fun test8WeeksReserved() {
        db.transaction { tx ->
            tx.createUpdate {
                    sql(
                        """
                INSERT INTO absence(child_id, date, absence_type, modified_at, modified_by, category, questionnaire_id)
                VALUES (:childId, generate_series('2022-06-06', '2022-07-31', interval '1 day')::date, 'FREE_ABSENCE', now(), :evakaUserId, 'BILLABLE', :questionnaireId)
                """
                    )
                }
                .bind("childId", testChild.id)
                .bind("evakaUserId", evakaUserId)
                .bind("questionnaireId", questionnaireId)
                .execute()
        }

        val june = YearMonth.of(2022, 6)
        db.transaction { generator.generateAllDraftInvoices(it, june) }
        val result = db.read { getAllInvoices(it) }
        Assertions.assertEquals(0, result.size)
    }

    @Test
    fun test7WeeksReserved() {
        db.transaction { tx ->
            tx.createUpdate {
                    sql(
                        """
                INSERT INTO absence(child_id, date, absence_type, modified_at, modified_by, category, questionnaire_id)
                VALUES (:childId, generate_series('2022-06-13', '2022-07-31', interval '1 day')::date, 'FREE_ABSENCE', now(), :evakaUserId, 'BILLABLE', :questionnaireId)
                """
                    )
                }
                .bind("childId", testChild.id)
                .bind("evakaUserId", evakaUserId)
                .bind("questionnaireId", questionnaireId)
                .execute()
        }

        val june = YearMonth.of(2022, 6)
        db.transaction { generator.generateAllDraftInvoices(it, june) }
        val result = db.read { getAllInvoices(it) }
        Assertions.assertEquals(1, result.size)
    }

    private final val testChild =
        DevPerson(
            id = ChildId(UUID.randomUUID()),
            dateOfBirth = LocalDate.of(2017, 6, 1),
            ssn = "010617A123U",
            firstName = "Ricky",
            lastName = "Doe",
            streetAddress = "Kamreerintie 2",
            postalCode = "02770",
            postOffice = "Espoo",
            restrictedDetailsEnabled = false,
        )

    private final val testAdult =
        DevPerson(
            id = PersonId(UUID.randomUUID()),
            dateOfBirth = LocalDate.of(1980, 1, 1),
            ssn = "010180-1232",
            firstName = "John",
            lastName = "Doe",
            streetAddress = "Kamreerintie 2",
            postalCode = "02770",
            postOffice = "Espoo",
            restrictedDetailsEnabled = false,
        )

    private final val testParentship =
        DevParentship(
            id = ParentshipId(UUID.randomUUID()),
            headOfChildId = testAdult.id,
            childId = testChild.id,
            startDate = testChild.dateOfBirth,
            endDate = testChild.dateOfBirth.plusYears(18).minusDays(1),
        )

    private final val defaultMunicipalOrganizerOid = "1.2.246.562.10.888888888888"

    private final val testDaycare =
        DevDaycare(
            id = DaycareId(UUID.randomUUID()),
            name = "Test Daycare",
            areaId = AreaId(UUID.fromString("6529e31e-9777-11eb-ba88-33a923255570")), // Etelä
            ophOrganizerOid = defaultMunicipalOrganizerOid,
            enabledPilotFeatures =
                setOf(
                    PilotFeature.MESSAGING,
                    PilotFeature.MOBILE,
                    PilotFeature.RESERVATIONS,
                    PilotFeature.PLACEMENT_TERMINATION,
                ),
        )

    private val getAllInvoices: (Database.Read) -> List<InvoiceDetailed> = { r ->
        r.createQuery { invoiceDetailedQuery(Predicate.alwaysTrue()) }
            .toList<InvoiceDetailed>()
            .shuffled() // randomize order to expose assumptions
    }

    private final val testFeeThresholds =
        FeeThresholds(
            validDuring = DateRange(LocalDate.of(2000, 1, 1), null),
            maxFee = 28900,
            minFee = 2700,
            minIncomeThreshold2 = 210200,
            minIncomeThreshold3 = 271300,
            minIncomeThreshold4 = 308000,
            minIncomeThreshold5 = 344700,
            minIncomeThreshold6 = 381300,
            maxIncomeThreshold2 = 479900,
            maxIncomeThreshold3 = 541000,
            maxIncomeThreshold4 = 577700,
            maxIncomeThreshold5 = 614400,
            maxIncomeThreshold6 = 651000,
            incomeMultiplier2 = BigDecimal("0.1070"),
            incomeMultiplier3 = BigDecimal("0.1070"),
            incomeMultiplier4 = BigDecimal("0.1070"),
            incomeMultiplier5 = BigDecimal("0.1070"),
            incomeMultiplier6 = BigDecimal("0.1070"),
            incomeThresholdIncrease6Plus = 14200,
            siblingDiscount2 = BigDecimal("0.5000"),
            siblingDiscount2Plus = BigDecimal("0.8000"),
            temporaryFee = 2900,
            temporaryFeePartDay = 1500,
            temporaryFeeSibling = 1500,
            temporaryFeeSiblingPartDay = 800,
        )

    private fun createFeeDecisionFixture(
        status: FeeDecisionStatus,
        decisionType: FeeDecisionType,
        period: FiniteDateRange,
        headOfFamilyId: PersonId,
        children: List<FeeDecisionChild>,
        partnerId: PersonId? = null,
        feeThresholds: FeeDecisionThresholds =
            testFeeThresholds.getFeeDecisionThresholds(children.size + 1),
        difference: Set<FeeDecisionDifference> = emptySet(),
        headOfFamilyIncome: DecisionIncome? = null,
    ) =
        FeeDecision(
            id = FeeDecisionId(UUID.randomUUID()),
            status = status,
            decisionType = decisionType,
            validDuring = period,
            headOfFamilyId = headOfFamilyId,
            partnerId = partnerId,
            headOfFamilyIncome = headOfFamilyIncome,
            partnerIncome = null,
            familySize = children.size + 1,
            feeThresholds = feeThresholds,
            difference = difference,
            children = children,
        )

    private fun ServiceNeedOption.toFeeDecisionServiceNeed() =
        FeeDecisionServiceNeed(
            optionId = this.id,
            feeCoefficient = this.feeCoefficient,
            contractDaysPerMonth = this.contractDaysPerMonth,
            descriptionFi = this.feeDescriptionFi,
            descriptionSv = this.feeDescriptionSv,
            missing = this.defaultOption,
        )

    private fun createFeeDecisionChildFixture(
        childId: ChildId,
        dateOfBirth: LocalDate,
        placementUnitId: DaycareId,
        placementType: PlacementType,
        serviceNeed: FeeDecisionServiceNeed,
        baseFee: Int = 28900,
        siblingDiscount: Int = 0,
        fee: Int = 28900,
        feeAlterations: List<FeeAlterationWithEffect> = listOf(),
    ) =
        FeeDecisionChild(
            child = ChildWithDateOfBirth(id = childId, dateOfBirth = dateOfBirth),
            placement = FeeDecisionPlacement(placementUnitId, placementType),
            serviceNeed = serviceNeed,
            baseFee = baseFee,
            siblingDiscount = siblingDiscount,
            fee = fee,
            feeAlterations = feeAlterations,
            finalFee = fee + feeAlterations.sumOf { it.effect },
            childIncome = null,
        )

    private fun insertDecisionsAndPlacements(feeDecisions: List<FeeDecision>) =
        db.transaction { tx ->
            tx.upsertFeeDecisions(feeDecisions)
            feeDecisions.forEach { decision ->
                decision.children.forEach { part ->
                    tx.insert(
                        DevPlacement(
                            childId = part.child.id,
                            unitId = part.placement.unitId,
                            startDate = decision.validFrom,
                            endDate = decision.validTo,
                            type = part.placement.type,
                        )
                    )
                }
            }
        }
}
