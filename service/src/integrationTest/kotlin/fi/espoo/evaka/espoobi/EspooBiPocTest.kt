// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.espoobi

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.application.ApplicationType
import fi.espoo.evaka.application.persistence.daycare.Adult
import fi.espoo.evaka.application.persistence.daycare.Apply
import fi.espoo.evaka.application.persistence.daycare.CareDetails
import fi.espoo.evaka.application.persistence.daycare.Child
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.daycare.service.AbsenceCategory
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
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevChild
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareCaretaker
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.DevPedagogicalDocument
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.TestDecision
import fi.espoo.evaka.shared.dev.insertPedagogicalDocument
import fi.espoo.evaka.shared.dev.insertServiceNeedOption
import fi.espoo.evaka.shared.dev.insertTestAbsence
import fi.espoo.evaka.shared.dev.insertTestApplication
import fi.espoo.evaka.shared.dev.insertTestApplicationForm
import fi.espoo.evaka.shared.dev.insertTestCareArea
import fi.espoo.evaka.shared.dev.insertTestChild
import fi.espoo.evaka.shared.dev.insertTestDaycare
import fi.espoo.evaka.shared.dev.insertTestDaycareCaretaker
import fi.espoo.evaka.shared.dev.insertTestDaycareGroup
import fi.espoo.evaka.shared.dev.insertTestDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.insertTestDecision
import fi.espoo.evaka.shared.dev.insertTestPerson
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.dev.insertTestServiceNeed
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.vasu.CurriculumType
import fi.espoo.evaka.vasu.VasuLanguage
import fi.espoo.evaka.vasu.getDefaultVasuContent
import fi.espoo.evaka.vasu.getVasuTemplate
import fi.espoo.evaka.vasu.insertVasuDocument
import fi.espoo.evaka.vasu.insertVasuTemplate
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Month
import java.util.UUID
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class EspooBiPocTest : PureJdbiTest(resetDbBeforeEach = true) {
    private val user = AuthenticatedUser.Integration

    @Test
    fun getAreas() {
        val id = db.transaction { it.insertTestArea() }
        assertSingleRowContainingId(EspooBiPoc.getAreas, id)
    }

    @Test
    fun getUnits() {
        val id = db.transaction { it.insertTestDaycare() }
        assertSingleRowContainingId(EspooBiPoc.getUnits, id)
    }

    @Test
    fun getGroups() {
        val id = db.transaction { it.insertTestGroup() }
        assertSingleRowContainingId(EspooBiPoc.getGroups, id)
    }

    @Test
    fun getChildren() {
        val id = db.transaction { it.insertTestChild() }
        assertSingleRowContainingId(EspooBiPoc.getChildren, id)
    }

    @Test
    fun getPlacements() {
        val id = db.transaction { it.insertTestPlacement() }
        assertSingleRowContainingId(EspooBiPoc.getPlacements, id)
    }

    @Test
    fun getGroupPlacements() {
        val id =
            db.transaction {
                it.insertTestDaycare().let { daycare ->
                    it.insertTestDaycareGroupPlacement(
                        DevDaycareGroupPlacement(
                            daycarePlacementId = it.insertTestPlacement(daycare),
                            daycareGroupId = it.insertTestGroup(daycare),
                        )
                    )
                }
            }
        assertSingleRowContainingId(EspooBiPoc.getGroupPlacements, id)
    }

    @Test
    fun getAbsences() {
        val id =
            db.transaction {
                it.insertTestAbsence(
                    DevAbsence(
                        childId = it.insertTestChild(),
                        date = LocalDate.of(2020, 1, 1),
                        absenceCategory = AbsenceCategory.BILLABLE,
                    )
                )
            }
        assertSingleRowContainingId(EspooBiPoc.getAbsences, id)
    }

    @Test
    fun getGroupCaretakerAllocations() {
        val id =
            db.transaction {
                it.insertTestDaycareCaretaker(
                    DevDaycareCaretaker(
                        groupId = it.insertTestGroup(),
                    )
                )
            }
        assertSingleRowContainingId(EspooBiPoc.getGroupCaretakerAllocations, id)
    }

    @Test
    fun getApplications() {
        val id = db.transaction { it.insertTestApplicationWithForm() }
        assertSingleRowContainingId(EspooBiPoc.getApplications, id)
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
        assertSingleRowContainingId(EspooBiPoc.getDecisions, id)
    }

    @Test
    fun getServiceNeedOptions() {
        val id = db.transaction { it.insertTestServiceNeedOption() }
        assertSingleRowContainingId(EspooBiPoc.getServiceNeedOptions, id)
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
        assertSingleRowContainingId(EspooBiPoc.getServiceNeeds, id)
    }

    @Test
    fun getFeeDecisions() {
        val id = db.transaction { it.insertTestFeeDecision() }
        assertSingleRowContainingId(EspooBiPoc.getFeeDecisions, id)
    }

    @Test
    fun getFeeDecisionChildren() {
        val feeDecisionId = db.transaction { it.insertTestFeeDecision() }
        // We intentionally test for *fee decision id*, not the child row id
        assertSingleRowContainingId(EspooBiPoc.getFeeDecisionChildren, feeDecisionId)
    }

    @Test
    fun getVoucherValueDecisions() {
        val id = db.transaction { it.insertTestVoucherValueDecision() }
        assertSingleRowContainingId(EspooBiPoc.getVoucherValueDecisions, id)
    }

    @Test
    fun getCurriculumTemplates() {
        val id = db.transaction { it.insertTestCurriculumTemplate() }
        assertSingleRowContainingId(EspooBiPoc.getCurriculumTemplates, id)
    }

    @Test
    fun getCurriculumDocuments() {
        val id =
            db.transaction {
                it.insertTestCurriculumTemplate().let { templateId ->
                    val template = it.getVasuTemplate(templateId)!!
                    it.insertVasuDocument(
                        MockEvakaClock(2022, 1, 1, 12, 0),
                        childId = it.insertTestChild(),
                        template = template
                    )
                }
            }
        assertSingleRowContainingId(EspooBiPoc.getCurriculumDocuments, id)
    }

    @Test
    fun getPedagogicalDocuments() {
        val id =
            db.transaction {
                it.insertPedagogicalDocument(
                    DevPedagogicalDocument(
                        id = PedagogicalDocumentId(UUID.randomUUID()),
                        childId = it.insertTestChild(),
                        description = "Test"
                    )
                )
            }
        assertSingleRowContainingId(EspooBiPoc.getPedagogicalDocuments, id)
    }

    private fun assertSingleRowContainingId(route: StreamingCsvRoute, id: Id<*>) {
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val messageConverters = emptyList<HttpMessageConverter<*>>()
        route(dbInstance(), user).writeTo(request, response) { messageConverters }

        val content = response.contentAsString
        val lines = content.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }
        assertTrue(lines.first().looksLikeHeaderRow())
        assertTrue(lines.drop(1).single().contains(id.toString()))
    }

    private fun String.looksLikeHeaderRow() = trim().contains(',')

    private fun Database.Transaction.insertTestArea(): AreaId = insertTestCareArea(DevCareArea())
    private fun Database.Transaction.insertTestDaycare(): DaycareId =
        insertTestDaycare(DevDaycare(areaId = insertTestArea()))
    private fun Database.Transaction.insertTestGroup(daycare: DaycareId? = null): GroupId =
        insertTestDaycareGroup(DevDaycareGroup(daycareId = daycare ?: insertTestDaycare()))
    private fun Database.Transaction.insertTestChild(): ChildId =
        insertTestPerson(DevPerson()).also { insertTestChild(DevChild(it)) }
    private fun Database.Transaction.insertTestPlacement(daycare: DaycareId? = null): PlacementId =
        insertTestPlacement(
            DevPlacement(childId = insertTestChild(), unitId = daycare ?: insertTestDaycare())
        )

    private fun Database.Transaction.insertTestApplicationWithForm(
        daycare: DaycareId? = null
    ): ApplicationId =
        insertTestApplication(
                type = ApplicationType.DAYCARE,
                childId = insertTestChild(),
                guardianId = insertTestPerson(DevPerson())
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
                    partDay = false,
                    partWeek = false,
                    feeDescriptionFi = "",
                    feeDescriptionSv = "",
                    voucherValueDescriptionFi = "",
                    voucherValueDescriptionSv = "",
                    active = true,
                )
            )
        }

    private fun Database.Transaction.insertTestFeeDecision() =
        FeeDecisionId(UUID.randomUUID()).also { id ->
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
                        headOfFamilyId = insertTestPerson(DevPerson()),
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
                        headOfFamilyId = insertTestPerson(DevPerson()),
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
            language = VasuLanguage.FI,
            content = getDefaultVasuContent(VasuLanguage.FI)
        )
}
