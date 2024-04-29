// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.espoo.bi

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.absence.AbsenceCategory
import fi.espoo.evaka.application.ApplicationType
import fi.espoo.evaka.application.persistence.daycare.Adult
import fi.espoo.evaka.application.persistence.daycare.Apply
import fi.espoo.evaka.application.persistence.daycare.CareDetails
import fi.espoo.evaka.application.persistence.daycare.Child
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.assistance.OtherAssistanceMeasureType
import fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionStatus
import fi.espoo.evaka.assistanceneed.decision.ServiceOptions
import fi.espoo.evaka.assistanceneed.decision.StructuralMotivationOptions
import fi.espoo.evaka.assistanceneed.preschooldecision.AssistanceNeedPreschoolDecisionForm
import fi.espoo.evaka.assistanceneed.preschooldecision.AssistanceNeedPreschoolDecisionType
import fi.espoo.evaka.assistanceneed.vouchercoefficient.AssistanceNeedVoucherCoefficientRequest
import fi.espoo.evaka.assistanceneed.vouchercoefficient.insertAssistanceNeedVoucherCoefficient
import fi.espoo.evaka.decision.DecisionStatus
import fi.espoo.evaka.decision.DecisionType
import fi.espoo.evaka.invoicing.data.upsertFeeDecisions
import fi.espoo.evaka.invoicing.data.upsertValueDecisions
import fi.espoo.evaka.invoicing.domain.ChildWithDateOfBirth
import fi.espoo.evaka.invoicing.domain.FeeDecision
import fi.espoo.evaka.invoicing.domain.FeeDecisionChild
import fi.espoo.evaka.invoicing.domain.FeeDecisionDifference
import fi.espoo.evaka.invoicing.domain.FeeDecisionPlacement
import fi.espoo.evaka.invoicing.domain.FeeDecisionServiceNeed
import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
import fi.espoo.evaka.invoicing.domain.FeeDecisionType
import fi.espoo.evaka.invoicing.domain.VoucherValueDecision
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionDifference
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionPlacement
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionServiceNeed
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionStatus
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionType
import fi.espoo.evaka.invoicing.testFeeThresholds
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.serviceneed.ServiceNeedOption
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.AreaId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.FeeDecisionId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.Id
import fi.espoo.evaka.shared.PedagogicalDocumentId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.ServiceNeedOptionId
import fi.espoo.evaka.shared.VoucherValueDecisionId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.DevAbsence
import fi.espoo.evaka.shared.dev.DevAssistanceFactor
import fi.espoo.evaka.shared.dev.DevAssistanceNeedDecision
import fi.espoo.evaka.shared.dev.DevAssistanceNeedPreschoolDecision
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareAssistance
import fi.espoo.evaka.shared.dev.DevDaycareCaretaker
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevOtherAssistanceMeasure
import fi.espoo.evaka.shared.dev.DevPedagogicalDocument
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.DevPreschoolAssistance
import fi.espoo.evaka.shared.dev.TestDecision
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertServiceNeedOption
import fi.espoo.evaka.shared.dev.insertTestApplication
import fi.espoo.evaka.shared.dev.insertTestApplicationForm
import fi.espoo.evaka.shared.dev.insertTestAssistanceNeedDecision
import fi.espoo.evaka.shared.dev.insertTestAssistanceNeedPreschoolDecision
import fi.espoo.evaka.shared.dev.insertTestDecision
import fi.espoo.evaka.shared.dev.insertTestServiceNeed
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.OfficialLanguage
import fi.espoo.evaka.vasu.CurriculumType
import fi.espoo.evaka.vasu.getDefaultVasuContent
import fi.espoo.evaka.vasu.getVasuTemplate
import fi.espoo.evaka.vasu.insertVasuDocument
import fi.espoo.evaka.vasu.insertVasuTemplate
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.time.Month
import java.util.UUID
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class EspooBiTest : PureJdbiTest(resetDbBeforeEach = true) {
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
        val id =
            db.transaction {
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
        val id =
            db.transaction {
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
        val id =
            db.transaction {
                it.insert(
                    DevDaycareCaretaker(
                        groupId = it.insertTestGroup(),
                    )
                )
            }
        assertSingleRowContainingId(EspooBi.getGroupCaretakerAllocations, id)
    }

    @Test
    fun getApplications() {
        val id = db.transaction { it.insertTestApplicationWithForm() }
        assertSingleRowContainingId(EspooBi.getApplications, id)
    }

    @Test
    fun getDecisions() {
        val id =
            db.transaction {
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
        val id =
            db.transaction {
                it.insertTestServiceNeed(
                    confirmedBy = AuthenticatedUser.SystemInternalUser.evakaUserId,
                    placementId = it.insertTestPlacement(),
                    period = FiniteDateRange.ofMonth(2019, Month.JANUARY),
                    optionId = it.insertTestServiceNeedOption(),
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
    fun getCurriculumTemplates() {
        val id = db.transaction { it.insertTestCurriculumTemplate() }
        assertSingleRowContainingId(EspooBi.getCurriculumTemplates, id)
    }

    @Test
    fun getCurriculumDocuments() {
        val id =
            db.transaction {
                it.insertTestCurriculumTemplate().let { templateId ->
                    val template = it.getVasuTemplate(templateId)!!
                    it.insertVasuDocument(
                        HelsinkiDateTime.of(
                            LocalDate.of(2022, 1, 1),
                            LocalTime.of(12, 0),
                        ),
                        childId = it.insertTestChild(),
                        template = template
                    )
                }
            }
        assertSingleRowContainingId(EspooBi.getCurriculumDocuments, id)
    }

    @Test
    fun getPedagogicalDocuments() {
        val id =
            db.transaction {
                it.insert(
                    DevPedagogicalDocument(
                        id = PedagogicalDocumentId(UUID.randomUUID()),
                        childId = it.insertTestChild(),
                        description = "Test"
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
        val id =
            db.transaction { it.insert(DevPreschoolAssistance(childId = it.insertTestChild())) }
        assertSingleRowContainingId(EspooBi.getPreschoolAssistanceEntries, id)
    }

    @Test
    fun getOtherAssistanceMeasureEntries() {
        val id =
            db.transaction {
                it.insert(
                    DevOtherAssistanceMeasure(
                        childId = it.insertTestChild(),
                        type = OtherAssistanceMeasureType.TRANSPORT_BENEFIT
                    )
                )
            }
        assertSingleRowContainingId(EspooBi.getOtherAssistanceMeasureEntries, id)
    }

    @Test
    fun getAssistanceNeedVoucherCoefficients() {
        val id =
            db.transaction {
                it.insertAssistanceNeedVoucherCoefficient(
                        childId = it.insertTestChild(),
                        AssistanceNeedVoucherCoefficientRequest(
                            coefficient = 2.0,
                            validityPeriod = FiniteDateRange.ofMonth(2019, Month.JANUARY)
                        )
                    )
                    .id
            }
        assertSingleRowContainingId(EspooBi.getAssistanceNeedVoucherCoefficients, id)
    }

    @Test
    fun getAssistanceNeedDaycareDecisions() {
        val id =
            db.transaction {
                val child = it.insertTestChild()
                it.insertTestAssistanceNeedDecision(
                    childId = child,
                    DevAssistanceNeedDecision(
                        decisionNumber = 999,
                        childId = child,
                        validityPeriod = DateRange.ofMonth(2019, Month.JANUARY),
                        status = AssistanceNeedDecisionStatus.ACCEPTED,
                        language = OfficialLanguage.FI,
                        decisionMade = null,
                        sentForDecision = null,
                        selectedUnit = null,
                        preparedBy1 = null,
                        preparedBy2 = null,
                        decisionMaker = null,
                        pedagogicalMotivation = null,
                        structuralMotivationOptions =
                            StructuralMotivationOptions(false, false, false, false, false, false),
                        structuralMotivationDescription = null,
                        careMotivation = null,
                        serviceOptions = ServiceOptions(false, false, false, false, false),
                        servicesMotivation = null,
                        expertResponsibilities = null,
                        guardiansHeardOn = null,
                        guardianInfo = emptySet(),
                        viewOfGuardians = null,
                        otherRepresentativeHeard = false,
                        otherRepresentativeDetails = null,
                        assistanceLevels = emptySet(),
                        motivationForDecision = null,
                        unreadGuardianIds = null,
                        annulmentReason = "",
                    )
                )
            }
        assertSingleRowContainingId(EspooBi.getAssistanceNeedDaycareDecisions, id)
    }

    @Test
    fun getAssistanceNeedPreschoolDecisions() {
        val id =
            db.transaction {
                val child = it.insertTestChild()
                it.insertTestAssistanceNeedPreschoolDecision(
                    DevAssistanceNeedPreschoolDecision(
                        decisionNumber = 999,
                        childId = child,
                        form =
                            AssistanceNeedPreschoolDecisionForm(
                                language = OfficialLanguage.FI,
                                type = AssistanceNeedPreschoolDecisionType.NEW,
                                validFrom = LocalDate.of(2019, 1, 1),
                                validTo = null,
                                extendedCompulsoryEducation = false,
                                extendedCompulsoryEducationInfo = "",
                                grantedAssistanceService = false,
                                grantedInterpretationService = false,
                                grantedAssistiveDevices = false,
                                grantedServicesBasis = "",
                                selectedUnit = it.insertTestDaycare(),
                                primaryGroup = "",
                                decisionBasis = "",
                                basisDocumentPedagogicalReport = false,
                                basisDocumentPsychologistStatement = false,
                                basisDocumentSocialReport = false,
                                basisDocumentDoctorStatement = false,
                                basisDocumentPedagogicalReportDate = null,
                                basisDocumentPsychologistStatementDate = null,
                                basisDocumentSocialReportDate = null,
                                basisDocumentDoctorStatementDate = null,
                                basisDocumentOtherOrMissing = false,
                                basisDocumentOtherOrMissingInfo = "",
                                basisDocumentsInfo = "",
                                guardiansHeardOn = null,
                                guardianInfo = emptySet(),
                                otherRepresentativeHeard = false,
                                otherRepresentativeDetails = "",
                                viewOfGuardians = "",
                                preparer1EmployeeId = it.insert(DevEmployee()),
                                preparer1Title = "",
                                preparer1PhoneNumber = "",
                                preparer2EmployeeId = null,
                                preparer2Title = "",
                                preparer2PhoneNumber = "",
                                decisionMakerEmployeeId = it.insert(DevEmployee()),
                                decisionMakerTitle = "",
                            ),
                        status = AssistanceNeedDecisionStatus.ACCEPTED,
                        annulmentReason = "",
                        sentForDecision = null,
                        decisionMade = LocalDate.of(2019, 5, 1),
                        unreadGuardianIds = emptySet(),
                    )
                )
            }
        assertSingleRowContainingId(EspooBi.getAssistanceNeedPreschoolDecisions, id)
    }

    private fun assertSingleRowContainingId(query: CsvQuery, id: Id<*>) {
        val lines =
            db.read { tx ->
                query(tx) { records ->
                    records.map { it.trim() }.filter { it.isNotEmpty() }.toList()
                }
            }

        assertTrue(lines.first().looksLikeHeaderRow())
        assertTrue(lines.drop(1).single().contains(id.toString()))
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
                guardianId = insert(DevPerson(), DevPersonType.RAW_ROW)
            )
            .also {
                insertTestApplicationForm(
                    it,
                    DaycareFormV0(
                        type = ApplicationType.DAYCARE,
                        connectedDaycare = false,
                        urgent = true,
                        careDetails =
                            CareDetails(
                                assistanceNeeded = true,
                            ),
                        extendedCare = true,
                        child = Child(dateOfBirth = null),
                        guardian = Adult(),
                        apply = Apply(preferredUnits = listOf(daycare ?: insertTestDaycare())),
                        preferredStartDate = LocalDate.of(2019, 1, 1)
                    )
                )
            }

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
                    validTo = null
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
                        validDuring = DateRange.ofMonth(2019, Month.JANUARY),
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
                        difference = setOf(VoucherValueDecisionDifference.PLACEMENT)
                    )
                )
            )
        }

    private fun Database.Transaction.insertTestCurriculumTemplate() =
        insertVasuTemplate(
            "Template",
            valid = FiniteDateRange.ofMonth(2022, Month.JANUARY),
            type = CurriculumType.DAYCARE,
            language = OfficialLanguage.FI,
            content = getDefaultVasuContent(OfficialLanguage.FI)
        )
}
