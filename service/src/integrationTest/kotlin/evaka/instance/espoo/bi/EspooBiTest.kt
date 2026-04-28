// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.espoo.bi

import evaka.core.PureJdbiTest
import evaka.core.absence.AbsenceCategory
import evaka.core.application.ApplicationType
import evaka.core.application.persistence.daycare.Adult
import evaka.core.application.persistence.daycare.Apply
import evaka.core.application.persistence.daycare.CareDetails
import evaka.core.application.persistence.daycare.Child
import evaka.core.application.persistence.daycare.DaycareFormV0
import evaka.core.assistance.OtherAssistanceMeasureType
import evaka.core.assistanceneed.vouchercoefficient.AssistanceNeedVoucherCoefficientRequest
import evaka.core.assistanceneed.vouchercoefficient.insertAssistanceNeedVoucherCoefficient
import evaka.core.decision.DecisionStatus
import evaka.core.decision.DecisionType
import evaka.core.invoicing.data.upsertFeeDecisions
import evaka.core.invoicing.data.upsertValueDecisions
import evaka.core.invoicing.domain.ChildWithDateOfBirth
import evaka.core.invoicing.domain.FeeDecision
import evaka.core.invoicing.domain.FeeDecisionChild
import evaka.core.invoicing.domain.FeeDecisionDifference
import evaka.core.invoicing.domain.FeeDecisionPlacement
import evaka.core.invoicing.domain.FeeDecisionServiceNeed
import evaka.core.invoicing.domain.FeeDecisionStatus
import evaka.core.invoicing.domain.FeeDecisionType
import evaka.core.invoicing.domain.VoucherValueDecision
import evaka.core.invoicing.domain.VoucherValueDecisionDifference
import evaka.core.invoicing.domain.VoucherValueDecisionPlacement
import evaka.core.invoicing.domain.VoucherValueDecisionServiceNeed
import evaka.core.invoicing.domain.VoucherValueDecisionStatus
import evaka.core.invoicing.domain.VoucherValueDecisionType
import evaka.core.invoicing.testFeeThresholds
import evaka.core.placement.PlacementType
import evaka.core.serviceneed.ServiceNeedOption
import evaka.core.shared.ApplicationId
import evaka.core.shared.AreaId
import evaka.core.shared.ChildId
import evaka.core.shared.DaycareId
import evaka.core.shared.FeeDecisionId
import evaka.core.shared.GroupId
import evaka.core.shared.Id
import evaka.core.shared.PedagogicalDocumentId
import evaka.core.shared.PlacementId
import evaka.core.shared.ServiceNeedOptionId
import evaka.core.shared.VoucherValueDecisionId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.dev.DevAbsence
import evaka.core.shared.dev.DevAssistanceFactor
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevDaycareAssistance
import evaka.core.shared.dev.DevDaycareCaretaker
import evaka.core.shared.dev.DevDaycareGroup
import evaka.core.shared.dev.DevDaycareGroupPlacement
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevOtherAssistanceMeasure
import evaka.core.shared.dev.DevPedagogicalDocument
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.DevPlacement
import evaka.core.shared.dev.DevPreschoolAssistance
import evaka.core.shared.dev.DevServiceNeed
import evaka.core.shared.dev.TestDecision
import evaka.core.shared.dev.insert
import evaka.core.shared.dev.insertServiceNeedOption
import evaka.core.shared.dev.insertTestApplication
import evaka.core.shared.dev.insertTestDecision
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Month
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class EspooBiTest : PureJdbiTest(resetDbBeforeEach = true) {
    private val clock = MockEvakaClock(2024, 1, 2, 3, 4)

    @Test
    fun getAreas() {
        val id = db.transaction { it.insertTestArea() }
        assertSingleRowContainingId(EspooBi.getAreas, id)
    }

    @Test
    fun getUnits() {
        val id = db.transaction { it.insertTestDaycare() }
        assertSingleRowContainingId(EspooBi.getUnits, id)
    }

    @Test
    fun getGroups() {
        val id = db.transaction { it.insertTestGroup() }
        assertSingleRowContainingId(EspooBi.getGroups, id)
    }

    @Test
    fun getChildren() {
        val id = db.transaction { it.insertTestChild() }
        assertSingleRowContainingId(EspooBi.getChildren, id)
    }

    @Test
    fun getPlacements() {
        val id = db.transaction { it.insertTestPlacement() }
        assertSingleRowContainingId(EspooBi.getPlacements, id)
    }

    @Test
    fun getGroupPlacements() {
        val id = db.transaction {
            it.insertTestDaycare().let { daycare ->
                it.insert(
                    DevDaycareGroupPlacement(
                        daycarePlacementId = it.insertTestPlacement(daycare),
                        daycareGroupId = it.insertTestGroup(daycare),
                    )
                )
            }
        }
        assertSingleRowContainingId(EspooBi.getGroupPlacements, id)
    }

    @Test
    fun getAbsences() {
        val id = db.transaction {
            it.insert(
                DevAbsence(
                    childId = it.insertTestChild(),
                    date = LocalDate.of(2020, 1, 1),
                    absenceCategory = AbsenceCategory.BILLABLE,
                )
            )
        }
        assertSingleRowContainingId(EspooBi.getAbsences, id)
    }

    @Test
    fun getGroupCaretakerAllocations() {
        val id = db.transaction { it.insert(DevDaycareCaretaker(groupId = it.insertTestGroup())) }
        assertSingleRowContainingId(EspooBi.getGroupCaretakerAllocations, id)
    }

    @Test
    fun getApplications() {
        val id = db.transaction { it.insertTestApplicationWithForm() }
        assertSingleRowContainingId(EspooBi.getApplications, id)
    }

    @Test
    fun getDecisions() {
        val id = db.transaction {
            val daycare = it.insertTestDaycare()
            it.insertTestDecision(
                TestDecision(
                    applicationId = it.insertTestApplicationWithForm(daycare),
                    createdBy = AuthenticatedUser.SystemInternalUser.evakaUserId,
                    startDate = LocalDate.of(2019, 3, 1),
                    endDate = LocalDate.of(2019, 4, 1),
                    type = DecisionType.DAYCARE,
                    unitId = daycare,
                    status = DecisionStatus.ACCEPTED,
                )
            )
        }
        assertSingleRowContainingId(EspooBi.getDecisions, id)
    }

    @Test
    fun getServiceNeedOptions() {
        val id = db.transaction { it.insertTestServiceNeedOption() }
        assertSingleRowContainingId(EspooBi.getServiceNeedOptions, id)
    }

    @Test
    fun getServiceNeeds() {
        val id = db.transaction {
            val period = FiniteDateRange.ofMonth(2019, Month.JANUARY)
            it.insert(
                DevServiceNeed(
                    placementId = it.insertTestPlacement(),
                    startDate = period.start,
                    endDate = period.end,
                    optionId = it.insertTestServiceNeedOption(),
                    confirmedBy = AuthenticatedUser.SystemInternalUser.evakaUserId,
                    confirmedAt = HelsinkiDateTime.now(),
                )
            )
        }
        assertSingleRowContainingId(EspooBi.getServiceNeeds, id)
    }

    @Test
    fun getFeeDecisions() {
        val id = db.transaction { it.insertTestFeeDecision() }
        assertSingleRowContainingId(EspooBi.getFeeDecisions, id)
    }

    @Test
    fun getFeeDecisionChildren() {
        val feeDecisionId = db.transaction { it.insertTestFeeDecision() }
        // We intentionally test for *fee decision id*, not the child row id
        assertSingleRowContainingId(EspooBi.getFeeDecisionChildren, feeDecisionId)
    }

    @Test
    fun getVoucherValueDecisions() {
        val id = db.transaction { it.insertTestVoucherValueDecision() }
        assertSingleRowContainingId(EspooBi.getVoucherValueDecisions, id)
    }

    @Test
    fun getPedagogicalDocuments() {
        val id = db.transaction {
            it.insert(
                DevPedagogicalDocument(
                    id = PedagogicalDocumentId(UUID.randomUUID()),
                    childId = it.insertTestChild(),
                    description = "Test",
                )
            )
        }
        assertSingleRowContainingId(EspooBi.getPedagogicalDocuments, id)
    }

    @Test
    fun getAssistanceFactors() {
        val id = db.transaction { it.insert(DevAssistanceFactor(childId = it.insertTestChild())) }
        assertSingleRowContainingId(EspooBi.getAssistanceFactors, id)
    }

    @Test
    fun getDaycareAssistanceEntries() {
        val id = db.transaction { it.insert(DevDaycareAssistance(childId = it.insertTestChild())) }
        assertSingleRowContainingId(EspooBi.getDaycareAssistanceEntries, id)
    }

    @Test
    fun getPreschoolAssistanceEntries() {
        val id = db.transaction {
            it.insert(DevPreschoolAssistance(childId = it.insertTestChild()))
        }
        assertSingleRowContainingId(EspooBi.getPreschoolAssistanceEntries, id)
    }

    @Test
    fun getOtherAssistanceMeasureEntries() {
        val id = db.transaction {
            it.insert(
                DevOtherAssistanceMeasure(
                    childId = it.insertTestChild(),
                    type = OtherAssistanceMeasureType.TRANSPORT_BENEFIT,
                )
            )
        }
        assertSingleRowContainingId(EspooBi.getOtherAssistanceMeasureEntries, id)
    }

    @Test
    fun getAssistanceNeedVoucherCoefficients() {
        val id = db.transaction {
            val employee = DevEmployee()
            it.insert(employee)
            it.insertAssistanceNeedVoucherCoefficient(
                    user = employee.user,
                    now = clock.now(),
                    childId = it.insertTestChild(),
                    AssistanceNeedVoucherCoefficientRequest(
                        coefficient = 2.0,
                        validityPeriod = FiniteDateRange.ofMonth(2019, Month.JANUARY),
                    ),
                )
                .id
        }
        assertSingleRowContainingId(EspooBi.getAssistanceNeedVoucherCoefficients, id)
    }

    @Test
    fun getAssistanceNeedDaycareDecisions() {
        assertHeaderOnly(EspooBi.getAssistanceNeedDaycareDecisions)
    }

    @Test
    fun getAssistanceNeedPreschoolDecisions() {
        assertHeaderOnly(EspooBi.getAssistanceNeedPreschoolDecisions)
    }

    private fun assertSingleRowContainingId(query: CsvQuery, id: Id<*>) {
        val lines = db.read { tx ->
            query(tx) { records -> records.map { it.trim() }.filter { it.isNotEmpty() }.toList() }
        }

        assertTrue(lines.first().looksLikeHeaderRow())
        assertTrue(lines.drop(1).single().contains(id.toString()))
    }

    private fun assertHeaderOnly(query: CsvQuery) {
        val lines = db.read { tx ->
            query(tx) { records -> records.map { it.trim() }.filter { it.isNotEmpty() }.toList() }
        }

        assertTrue(lines.first().looksLikeHeaderRow())
        assertEquals(1, lines.size)
    }

    private fun String.looksLikeHeaderRow() = trim().contains(',')

    private fun Database.Transaction.insertTestArea(): AreaId = insert(DevCareArea())

    private fun Database.Transaction.insertTestDaycare(): DaycareId =
        insert(DevDaycare(areaId = insertTestArea()))

    private fun Database.Transaction.insertTestGroup(daycare: DaycareId? = null): GroupId =
        insert(DevDaycareGroup(daycareId = daycare ?: insertTestDaycare()))

    private fun Database.Transaction.insertTestChild(): ChildId =
        insert(DevPerson(), DevPersonType.CHILD)

    private fun Database.Transaction.insertTestPlacement(daycare: DaycareId? = null): PlacementId =
        insert(DevPlacement(childId = insertTestChild(), unitId = daycare ?: insertTestDaycare()))

    private fun Database.Transaction.insertTestApplicationWithForm(
        daycare: DaycareId? = null
    ): ApplicationId =
        insertTestApplication(
            type = ApplicationType.DAYCARE,
            childId = insertTestChild(),
            guardianId = insert(DevPerson(), DevPersonType.RAW_ROW),
            document =
                DaycareFormV0(
                    type = ApplicationType.DAYCARE,
                    connectedDaycare = false,
                    urgent = true,
                    careDetails = CareDetails(assistanceNeeded = true),
                    extendedCare = true,
                    child = Child(dateOfBirth = null),
                    guardian = Adult(),
                    apply = Apply(preferredUnits = listOf(daycare ?: insertTestDaycare())),
                    preferredStartDate = LocalDate.of(2019, 1, 1),
                ),
        )

    private fun Database.Transaction.insertTestServiceNeedOption(): ServiceNeedOptionId =
        ServiceNeedOptionId(UUID.randomUUID()).also {
            insertServiceNeedOption(
                ServiceNeedOption(
                    id = it,
                    nameFi = "Tarve",
                    nameSv = "",
                    nameEn = "",
                    validPlacementType = PlacementType.DAYCARE,
                    defaultOption = true,
                    feeCoefficient = BigDecimal.ONE,
                    occupancyCoefficient = BigDecimal.ONE,
                    occupancyCoefficientUnder3y = BigDecimal.ONE,
                    realizedOccupancyCoefficient = BigDecimal.ONE,
                    realizedOccupancyCoefficientUnder3y = BigDecimal.ONE,
                    daycareHoursPerWeek = 40,
                    contractDaysPerMonth = null,
                    daycareHoursPerMonth = null,
                    partDay = false,
                    partWeek = false,
                    feeDescriptionFi = "",
                    feeDescriptionSv = "",
                    voucherValueDescriptionFi = "",
                    voucherValueDescriptionSv = "",
                    validFrom = LocalDate.of(2000, 1, 1),
                    validTo = null,
                    showForCitizen = true,
                )
            )
        }

    private fun Database.Transaction.insertTestFeeDecision() =
        FeeDecisionId(UUID.randomUUID()).also { id ->
            val serviceNeedOption = insertTestServiceNeedOption()
            upsertFeeDecisions(
                listOf(
                    FeeDecision(
                        id = id,
                        children =
                            listOf(
                                FeeDecisionChild(
                                    child =
                                        ChildWithDateOfBirth(
                                            id = insertTestChild(),
                                            dateOfBirth = LocalDate.of(2020, 1, 1),
                                        ),
                                    placement =
                                        FeeDecisionPlacement(
                                            unitId = insertTestDaycare(),
                                            type = PlacementType.DAYCARE,
                                        ),
                                    serviceNeed =
                                        FeeDecisionServiceNeed(
                                            optionId = serviceNeedOption,
                                            feeCoefficient = BigDecimal.ONE,
                                            contractDaysPerMonth = null,
                                            descriptionFi = "",
                                            descriptionSv = "",
                                            missing = false,
                                        ),
                                    baseFee = 10_000,
                                    siblingDiscount = 0,
                                    fee = 10_000,
                                    finalFee = 10_000,
                                    feeAlterations = emptyList(),
                                    childIncome = null,
                                )
                            ),
                        headOfFamilyId = insert(DevPerson(), DevPersonType.RAW_ROW),
                        validDuring = FiniteDateRange.ofMonth(2019, Month.JANUARY),
                        status = FeeDecisionStatus.SENT,
                        decisionNumber = 999L,
                        decisionType = FeeDecisionType.NORMAL,
                        partnerId = null,
                        headOfFamilyIncome = null,
                        partnerIncome = null,
                        familySize = 1,
                        feeThresholds = testFeeThresholds.getFeeDecisionThresholds(1),
                        difference = setOf(FeeDecisionDifference.PLACEMENT),
                    )
                )
            )
        }

    private fun Database.Transaction.insertTestVoucherValueDecision() =
        VoucherValueDecisionId(UUID.randomUUID()).also { id ->
            upsertValueDecisions(
                listOf(
                    VoucherValueDecision(
                        id = id,
                        validFrom = LocalDate.of(2022, 1, 1),
                        validTo = LocalDate.of(2022, 2, 1),
                        headOfFamilyId = insert(DevPerson(), DevPersonType.RAW_ROW),
                        status = VoucherValueDecisionStatus.SENT,
                        decisionNumber = 999L,
                        decisionType = VoucherValueDecisionType.NORMAL,
                        partnerId = null,
                        headOfFamilyIncome = null,
                        partnerIncome = null,
                        childIncome = null,
                        familySize = 1,
                        feeThresholds = testFeeThresholds.getFeeDecisionThresholds(1),
                        child =
                            ChildWithDateOfBirth(
                                id = insertTestChild(),
                                dateOfBirth = LocalDate.of(2020, 1, 1),
                            ),
                        placement =
                            VoucherValueDecisionPlacement(
                                unitId = insertTestDaycare(),
                                type = PlacementType.DAYCARE,
                            ),
                        serviceNeed =
                            VoucherValueDecisionServiceNeed(
                                feeCoefficient = BigDecimal.ONE,
                                voucherValueCoefficient = BigDecimal.ONE,
                                feeDescriptionFi = "",
                                feeDescriptionSv = "",
                                voucherValueDescriptionFi = "",
                                voucherValueDescriptionSv = "",
                                missing = false,
                            ),
                        baseCoPayment = 1,
                        siblingDiscount = 0,
                        coPayment = 1,
                        feeAlterations = listOf(),
                        finalCoPayment = 1,
                        baseValue = 1,
                        assistanceNeedCoefficient = BigDecimal.ONE,
                        voucherValue = 1,
                        difference = setOf(VoucherValueDecisionDifference.PLACEMENT),
                    )
                )
            )
        }
}
