// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.assistance.DaycareAssistanceLevel
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.document.ChildDocumentType
import fi.espoo.evaka.document.DocumentTemplateContent
import fi.espoo.evaka.document.Question
import fi.espoo.evaka.document.Section
import fi.espoo.evaka.document.childdocument.AnsweredQuestion
import fi.espoo.evaka.document.childdocument.ChildDocumentDecisionStatus
import fi.espoo.evaka.document.childdocument.DocumentContent
import fi.espoo.evaka.document.childdocument.DocumentStatus
import fi.espoo.evaka.insertAssistanceActionOptions
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.DocumentTemplateId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevAssistanceAction
import fi.espoo.evaka.shared.dev.DevAssistanceNeedVoucherCoefficient
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevChildDocument
import fi.espoo.evaka.shared.dev.DevChildDocumentDecision
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareAssistance
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.DevDocumentTemplate
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class AssistanceNeedsAndActionsReportControllerTest :
    FullApplicationTest(resetDbBeforeEach = true) {

    @Autowired private lateinit var controller: AssistanceNeedsAndActionsReportController

    private lateinit var admin: AuthenticatedUser.Employee

    @BeforeEach
    fun setup() {
        admin =
            db.transaction { tx ->
                val employeeId = tx.insert(DevEmployee(roles = setOf(UserRole.ADMIN)))
                AuthenticatedUser.Employee(employeeId, setOf(UserRole.ADMIN))
            }
        db.transaction { tx -> tx.insertAssistanceActionOptions() }
    }

    @Test
    fun `daycare assistance works`() {
        val date = LocalDate.of(2023, 10, 5)
        val clock = MockEvakaClock(HelsinkiDateTime.of(date, LocalTime.of(14, 15)))
        val area = DevCareArea()
        val unit = DevDaycare(areaId = area.id)
        db.transaction { tx ->
            tx.insert(area)
            tx.insert(unit)
        }

        val groups =
            listOf(
                DevDaycareGroup(daycareId = unit.id, name = "Group 1", startDate = date),
                DevDaycareGroup(daycareId = unit.id, name = "Group 2", startDate = date),
                DevDaycareGroup(daycareId = unit.id, name = "Group 3", startDate = date),
            )

        db.transaction { tx -> groups.forEach { group -> tx.insert(group) } }

        val children = insertAssistanceData(unit = unit, groups = groups, date = date)
        val groupReport =
            controller.getAssistanceNeedsAndActionsReport(
                dbInstance(),
                admin,
                clock,
                date,
                DaycareAssistanceLevel.entries,
                emptyList(),
                emptyList(),
            )
        assertEquals(
            listOf(
                AssistanceNeedsAndActionsReportController.AssistanceNeedsAndActionsReportRow(
                    careAreaName = "Test Care Area",
                    unitId = unit.id,
                    unitName = unit.name,
                    groupId = groups[0].id,
                    groupName = groups[0].name,
                    actionCounts = mapOf("ASSISTANCE_SERVICE_CHILD" to 1),
                    otherActionCount = 1,
                    noActionCount = 0,
                    daycareAssistanceCounts = mapOf(DaycareAssistanceLevel.GENERAL_SUPPORT to 2),
                    preschoolAssistanceCounts = emptyMap(),
                    otherAssistanceMeasureCounts = emptyMap(),
                    assistanceNeedVoucherCoefficientCount = 1,
                    unitProviderType = ProviderType.MUNICIPAL,
                    documentDecisionCounts = emptyMap(),
                ),
                AssistanceNeedsAndActionsReportController.AssistanceNeedsAndActionsReportRow(
                    careAreaName = "Test Care Area",
                    unitId = unit.id,
                    unitName = unit.name,
                    groupId = groups[1].id,
                    groupName = groups[1].name,
                    actionCounts = mapOf("ASSISTANCE_SERVICE_CHILD" to 1),
                    otherActionCount = 0,
                    noActionCount = 0,
                    daycareAssistanceCounts = mapOf(DaycareAssistanceLevel.GENERAL_SUPPORT to 1),
                    preschoolAssistanceCounts = emptyMap(),
                    otherAssistanceMeasureCounts = emptyMap(),
                    assistanceNeedVoucherCoefficientCount = 0,
                    unitProviderType = ProviderType.MUNICIPAL,
                    documentDecisionCounts = emptyMap(),
                ),
                AssistanceNeedsAndActionsReportController.AssistanceNeedsAndActionsReportRow(
                    careAreaName = "Test Care Area",
                    unitId = unit.id,
                    unitName = unit.name,
                    groupId = groups[2].id,
                    groupName = groups[2].name,
                    actionCounts = emptyMap(),
                    otherActionCount = 0,
                    noActionCount = 0,
                    daycareAssistanceCounts = emptyMap(),
                    preschoolAssistanceCounts = emptyMap(),
                    otherAssistanceMeasureCounts = emptyMap(),
                    assistanceNeedVoucherCoefficientCount = 0,
                    unitProviderType = ProviderType.MUNICIPAL,
                    documentDecisionCounts = emptyMap(),
                ),
            ),
            groupReport.rows,
        )

        val emptyGroupReport =
            controller.getAssistanceNeedsAndActionsReport(
                dbInstance(),
                admin,
                clock,
                date,
                emptyList(),
                emptyList(),
                emptyList(),
            )
        assertEquals(
            listOf(
                AssistanceNeedsAndActionsReportController.AssistanceNeedsAndActionsReportRow(
                    careAreaName = "Test Care Area",
                    unitId = unit.id,
                    unitName = unit.name,
                    groupId = groups[0].id,
                    groupName = groups[0].name,
                    actionCounts = emptyMap(),
                    otherActionCount = 0,
                    noActionCount = 0,
                    daycareAssistanceCounts = mapOf(DaycareAssistanceLevel.GENERAL_SUPPORT to 2),
                    preschoolAssistanceCounts = emptyMap(),
                    otherAssistanceMeasureCounts = emptyMap(),
                    assistanceNeedVoucherCoefficientCount = 1,
                    unitProviderType = ProviderType.MUNICIPAL,
                    documentDecisionCounts = emptyMap(),
                ),
                AssistanceNeedsAndActionsReportController.AssistanceNeedsAndActionsReportRow(
                    careAreaName = "Test Care Area",
                    unitId = unit.id,
                    unitName = unit.name,
                    groupId = groups[1].id,
                    groupName = groups[1].name,
                    actionCounts = emptyMap(),
                    otherActionCount = 0,
                    noActionCount = 0,
                    daycareAssistanceCounts = mapOf(DaycareAssistanceLevel.GENERAL_SUPPORT to 1),
                    preschoolAssistanceCounts = emptyMap(),
                    otherAssistanceMeasureCounts = emptyMap(),
                    assistanceNeedVoucherCoefficientCount = 0,
                    unitProviderType = ProviderType.MUNICIPAL,
                    documentDecisionCounts = emptyMap(),
                ),
                AssistanceNeedsAndActionsReportController.AssistanceNeedsAndActionsReportRow(
                    careAreaName = "Test Care Area",
                    unitId = unit.id,
                    unitName = unit.name,
                    groupId = groups[2].id,
                    groupName = groups[2].name,
                    actionCounts = emptyMap(),
                    otherActionCount = 0,
                    noActionCount = 0,
                    daycareAssistanceCounts = emptyMap(),
                    preschoolAssistanceCounts = emptyMap(),
                    otherAssistanceMeasureCounts = emptyMap(),
                    assistanceNeedVoucherCoefficientCount = 0,
                    unitProviderType = ProviderType.MUNICIPAL,
                    documentDecisionCounts = emptyMap(),
                ),
            ),
            emptyGroupReport.rows,
        )

        val childReport =
            controller.getAssistanceNeedsAndActionsReportByChild(
                dbInstance(),
                admin,
                clock,
                date,
                DaycareAssistanceLevel.entries,
                emptyList(),
                emptyList(),
            )
        assertEquals(
            listOf(
                AssistanceNeedsAndActionsReportController.AssistanceNeedsAndActionsReportRowByChild(
                    careAreaName = "Test Care Area",
                    unitId = unit.id,
                    unitName = unit.name,
                    groupId = groups[0].id,
                    groupName = groups[0].name,
                    childId = children[0].id,
                    childLastName = children[0].lastName,
                    childFirstName = children[0].firstName,
                    childAge = 3,
                    actions = setOf("ASSISTANCE_SERVICE_CHILD"),
                    otherAction = "",
                    daycareAssistanceCounts = mapOf(DaycareAssistanceLevel.GENERAL_SUPPORT to 1),
                    preschoolAssistanceCounts = emptyMap(),
                    otherAssistanceMeasureCounts = emptyMap(),
                    assistanceNeedVoucherCoefficient = BigDecimal("1.50"),
                    unitProviderType = ProviderType.MUNICIPAL,
                    documentDecisionCounts = emptyMap(),
                ),
                AssistanceNeedsAndActionsReportController.AssistanceNeedsAndActionsReportRowByChild(
                    careAreaName = "Test Care Area",
                    unitId = unit.id,
                    unitName = unit.name,
                    groupId = groups[0].id,
                    groupName = groups[0].name,
                    childId = children[1].id,
                    childLastName = children[1].lastName,
                    childFirstName = children[1].firstName,
                    childAge = 3,
                    actions = emptySet(),
                    otherAction = "other action test",
                    daycareAssistanceCounts = mapOf(DaycareAssistanceLevel.GENERAL_SUPPORT to 1),
                    preschoolAssistanceCounts = emptyMap(),
                    otherAssistanceMeasureCounts = emptyMap(),
                    assistanceNeedVoucherCoefficient = BigDecimal("1.0"),
                    unitProviderType = ProviderType.MUNICIPAL,
                    documentDecisionCounts = emptyMap(),
                ),
                AssistanceNeedsAndActionsReportController.AssistanceNeedsAndActionsReportRowByChild(
                    careAreaName = "Test Care Area",
                    unitId = unit.id,
                    unitName = unit.name,
                    groupId = groups[1].id,
                    groupName = groups[1].name,
                    childId = children[2].id,
                    childLastName = children[2].lastName,
                    childFirstName = children[2].firstName,
                    childAge = 3,
                    actions = setOf("ASSISTANCE_SERVICE_CHILD"),
                    otherAction = "",
                    daycareAssistanceCounts = mapOf(DaycareAssistanceLevel.GENERAL_SUPPORT to 1),
                    preschoolAssistanceCounts = emptyMap(),
                    otherAssistanceMeasureCounts = emptyMap(),
                    assistanceNeedVoucherCoefficient = BigDecimal("1.0"),
                    unitProviderType = ProviderType.MUNICIPAL,
                    documentDecisionCounts = emptyMap(),
                ),
            ),
            childReport.rows,
        )

        val emptyChildReport =
            controller.getAssistanceNeedsAndActionsReportByChild(
                dbInstance(),
                admin,
                clock,
                date,
                emptyList(),
                emptyList(),
                emptyList(),
            )
        assertEquals(
            listOf(
                AssistanceNeedsAndActionsReportController.AssistanceNeedsAndActionsReportRowByChild(
                    careAreaName = "Test Care Area",
                    unitId = unit.id,
                    unitName = unit.name,
                    groupId = groups[0].id,
                    groupName = groups[0].name,
                    childId = children[0].id,
                    childLastName = children[0].lastName,
                    childFirstName = children[0].firstName,
                    childAge = 3,
                    actions = emptySet(),
                    otherAction = "",
                    daycareAssistanceCounts = mapOf(DaycareAssistanceLevel.GENERAL_SUPPORT to 1),
                    preschoolAssistanceCounts = emptyMap(),
                    otherAssistanceMeasureCounts = emptyMap(),
                    assistanceNeedVoucherCoefficient = BigDecimal("1.50"),
                    unitProviderType = ProviderType.MUNICIPAL,
                    documentDecisionCounts = emptyMap(),
                ),
                AssistanceNeedsAndActionsReportController.AssistanceNeedsAndActionsReportRowByChild(
                    careAreaName = "Test Care Area",
                    unitId = unit.id,
                    unitName = unit.name,
                    groupId = groups[0].id,
                    groupName = groups[0].name,
                    childId = children[1].id,
                    childLastName = children[1].lastName,
                    childFirstName = children[1].firstName,
                    childAge = 3,
                    actions = emptySet(),
                    otherAction = "",
                    daycareAssistanceCounts = mapOf(DaycareAssistanceLevel.GENERAL_SUPPORT to 1),
                    preschoolAssistanceCounts = emptyMap(),
                    otherAssistanceMeasureCounts = emptyMap(),
                    assistanceNeedVoucherCoefficient = BigDecimal("1.0"),
                    unitProviderType = ProviderType.MUNICIPAL,
                    documentDecisionCounts = emptyMap(),
                ),
                AssistanceNeedsAndActionsReportController.AssistanceNeedsAndActionsReportRowByChild(
                    careAreaName = "Test Care Area",
                    unitId = unit.id,
                    unitName = unit.name,
                    groupId = groups[1].id,
                    groupName = groups[1].name,
                    childId = children[2].id,
                    childLastName = children[2].lastName,
                    childFirstName = children[2].firstName,
                    childAge = 3,
                    actions = emptySet(),
                    otherAction = "",
                    daycareAssistanceCounts = mapOf(DaycareAssistanceLevel.GENERAL_SUPPORT to 1),
                    preschoolAssistanceCounts = emptyMap(),
                    otherAssistanceMeasureCounts = emptyMap(),
                    assistanceNeedVoucherCoefficient = BigDecimal("1.0"),
                    unitProviderType = ProviderType.MUNICIPAL,
                    documentDecisionCounts = emptyMap(),
                ),
            ),
            emptyChildReport.rows,
        )
    }

    @Test
    fun `placement type filtering works`() {
        val date = LocalDate.of(2023, 10, 5)
        val clock = MockEvakaClock(HelsinkiDateTime.of(date, LocalTime.of(14, 15)))
        val area = DevCareArea()
        val unit = DevDaycare(areaId = area.id)
        db.transaction { tx ->
            tx.insert(area)
            tx.insert(unit)
        }

        val groups =
            listOf(
                DevDaycareGroup(daycareId = unit.id, name = "Group 1", startDate = date),
                DevDaycareGroup(daycareId = unit.id, name = "Group 2", startDate = date),
                DevDaycareGroup(daycareId = unit.id, name = "Group 3", startDate = date),
            )

        db.transaction { tx -> groups.forEach { group -> tx.insert(group) } }
        val children =
            insertAssistanceData(
                unit = unit,
                groups = groups,
                date = date,
                placementType = PlacementType.PRESCHOOL_DAYCARE,
            )

        val daycareReport =
            controller.getAssistanceNeedsAndActionsReport(
                dbInstance(),
                admin,
                clock,
                date,
                DaycareAssistanceLevel.entries,
                placementTypes = listOf(PlacementType.DAYCARE),
            )

        val preschoolDaycareReport =
            controller.getAssistanceNeedsAndActionsReport(
                dbInstance(),
                admin,
                clock,
                date,
                DaycareAssistanceLevel.entries,
                placementTypes = listOf(PlacementType.PRESCHOOL_DAYCARE),
            )

        fun getEmptyGroupExpectation(group: DevDaycareGroup) =
            AssistanceNeedsAndActionsReportController.AssistanceNeedsAndActionsReportRow(
                careAreaName = "Test Care Area",
                unitId = unit.id,
                unitName = unit.name,
                groupId = group.id,
                groupName = group.name,
                actionCounts = emptyMap(),
                otherActionCount = 0,
                noActionCount = 0,
                daycareAssistanceCounts = emptyMap(),
                preschoolAssistanceCounts = emptyMap(),
                otherAssistanceMeasureCounts = emptyMap(),
                assistanceNeedVoucherCoefficientCount = 0,
                unitProviderType = ProviderType.MUNICIPAL,
                documentDecisionCounts = emptyMap(),
            )

        assertEquals(
            listOf(
                AssistanceNeedsAndActionsReportController.AssistanceNeedsAndActionsReportRow(
                    careAreaName = "Test Care Area",
                    unitId = unit.id,
                    unitName = unit.name,
                    groupId = groups[0].id,
                    groupName = groups[0].name,
                    actionCounts = mapOf("ASSISTANCE_SERVICE_CHILD" to 1),
                    otherActionCount = 1,
                    noActionCount = 0,
                    daycareAssistanceCounts = mapOf(DaycareAssistanceLevel.GENERAL_SUPPORT to 2),
                    preschoolAssistanceCounts = emptyMap(),
                    otherAssistanceMeasureCounts = emptyMap(),
                    assistanceNeedVoucherCoefficientCount = 1,
                    unitProviderType = ProviderType.MUNICIPAL,
                    documentDecisionCounts = emptyMap(),
                ),
                AssistanceNeedsAndActionsReportController.AssistanceNeedsAndActionsReportRow(
                    careAreaName = "Test Care Area",
                    unitId = unit.id,
                    unitName = unit.name,
                    groupId = groups[1].id,
                    groupName = groups[1].name,
                    actionCounts = mapOf("ASSISTANCE_SERVICE_CHILD" to 1),
                    otherActionCount = 0,
                    noActionCount = 0,
                    daycareAssistanceCounts = mapOf(DaycareAssistanceLevel.GENERAL_SUPPORT to 1),
                    preschoolAssistanceCounts = emptyMap(),
                    otherAssistanceMeasureCounts = emptyMap(),
                    assistanceNeedVoucherCoefficientCount = 0,
                    unitProviderType = ProviderType.MUNICIPAL,
                    documentDecisionCounts = emptyMap(),
                ),
                getEmptyGroupExpectation(groups[2]),
            ),
            preschoolDaycareReport.rows,
        )

        assertEquals(groups.map { getEmptyGroupExpectation(it) }, daycareReport.rows)

        val daycareChildReport =
            controller.getAssistanceNeedsAndActionsReportByChild(
                dbInstance(),
                admin,
                clock,
                date,
                DaycareAssistanceLevel.entries,
                placementTypes = listOf(PlacementType.DAYCARE),
            )
        val preschoolDaycareChildReport =
            controller.getAssistanceNeedsAndActionsReportByChild(
                dbInstance(),
                admin,
                clock,
                date,
                DaycareAssistanceLevel.entries,
                placementTypes = listOf(PlacementType.DAYCARE, PlacementType.PRESCHOOL_DAYCARE),
            )

        assertEquals(
            listOf(
                AssistanceNeedsAndActionsReportController.AssistanceNeedsAndActionsReportRowByChild(
                    careAreaName = "Test Care Area",
                    unitId = unit.id,
                    unitName = unit.name,
                    groupId = groups[0].id,
                    groupName = groups[0].name,
                    childId = children[0].id,
                    childLastName = children[0].lastName,
                    childFirstName = children[0].firstName,
                    childAge = 3,
                    actions = setOf("ASSISTANCE_SERVICE_CHILD"),
                    otherAction = "",
                    daycareAssistanceCounts = mapOf(DaycareAssistanceLevel.GENERAL_SUPPORT to 1),
                    preschoolAssistanceCounts = emptyMap(),
                    otherAssistanceMeasureCounts = emptyMap(),
                    assistanceNeedVoucherCoefficient = BigDecimal("1.50"),
                    unitProviderType = ProviderType.MUNICIPAL,
                    documentDecisionCounts = emptyMap(),
                ),
                AssistanceNeedsAndActionsReportController.AssistanceNeedsAndActionsReportRowByChild(
                    careAreaName = "Test Care Area",
                    unitId = unit.id,
                    unitName = unit.name,
                    groupId = groups[0].id,
                    groupName = groups[0].name,
                    childId = children[1].id,
                    childLastName = children[1].lastName,
                    childFirstName = children[1].firstName,
                    childAge = 3,
                    actions = emptySet(),
                    otherAction = "other action test",
                    daycareAssistanceCounts = mapOf(DaycareAssistanceLevel.GENERAL_SUPPORT to 1),
                    preschoolAssistanceCounts = emptyMap(),
                    otherAssistanceMeasureCounts = emptyMap(),
                    assistanceNeedVoucherCoefficient = BigDecimal("1.0"),
                    unitProviderType = ProviderType.MUNICIPAL,
                    documentDecisionCounts = emptyMap(),
                ),
                AssistanceNeedsAndActionsReportController.AssistanceNeedsAndActionsReportRowByChild(
                    careAreaName = "Test Care Area",
                    unitId = unit.id,
                    unitName = unit.name,
                    groupId = groups[1].id,
                    groupName = groups[1].name,
                    childId = children[2].id,
                    childLastName = children[2].lastName,
                    childFirstName = children[2].firstName,
                    childAge = 3,
                    actions = setOf("ASSISTANCE_SERVICE_CHILD"),
                    otherAction = "",
                    daycareAssistanceCounts = mapOf(DaycareAssistanceLevel.GENERAL_SUPPORT to 1),
                    preschoolAssistanceCounts = emptyMap(),
                    otherAssistanceMeasureCounts = emptyMap(),
                    assistanceNeedVoucherCoefficient = BigDecimal("1.0"),
                    unitProviderType = ProviderType.MUNICIPAL,
                    documentDecisionCounts = emptyMap(),
                ),
            ),
            preschoolDaycareChildReport.rows,
        )

        assertEquals(0, daycareChildReport.rows.size)
    }

    // Helper functions for document decision tests
    private data class DocumentDecisionTestSetup(
        val date: LocalDate,
        val clock: MockEvakaClock,
        val area: DevCareArea,
        val unit: DevDaycare,
        val groups: List<DevDaycareGroup>,
        val decisionMaker: DevEmployee,
    )

    private fun setupDocumentDecisionTest(): DocumentDecisionTestSetup {
        val date = LocalDate.of(2023, 10, 5)
        val clock = MockEvakaClock(HelsinkiDateTime.of(date, LocalTime.of(14, 15)))
        val area = DevCareArea()
        val unit = DevDaycare(areaId = area.id)
        val decisionMaker = DevEmployee(roles = setOf(UserRole.DIRECTOR))

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(unit)
            tx.insert(decisionMaker)
        }

        val groups =
            listOf(
                DevDaycareGroup(daycareId = unit.id, name = "Group 1", startDate = date),
                DevDaycareGroup(daycareId = unit.id, name = "Group 2", startDate = date),
            )

        db.transaction { tx -> groups.forEach { group -> tx.insert(group) } }

        return DocumentDecisionTestSetup(date, clock, area, unit, groups, decisionMaker)
    }

    private fun createDecisionTemplate(
        name: String,
        type: ChildDocumentType,
        date: LocalDate,
        validity: DateRange = DateRange(date.minusYears(1), null),
    ): DevDocumentTemplate {
        return DevDocumentTemplate(
            name = name,
            type = type,
            validity = validity,
            content =
                DocumentTemplateContent(
                    sections =
                        listOf(
                            Section(
                                id = "s1",
                                label = "Section 1",
                                questions =
                                    listOf(
                                        Question.CheckboxQuestion(id = "q1", label = "Question 1")
                                    ),
                            )
                        )
                ),
            endDecisionWhenUnitChanges = true,
        )
    }

    private fun createChildWithPlacement(
        name: String,
        groupId: GroupId,
        unitId: DaycareId,
        date: LocalDate,
    ): DevPerson {
        return db.transaction { tx ->
            val child = DevPerson(firstName = name, dateOfBirth = date.minusYears(4))
            tx.insert(child, DevPersonType.CHILD)
            val placementId =
                tx.insert(
                    DevPlacement(
                        childId = child.id,
                        unitId = unitId,
                        startDate = date.minusMonths(2),
                        endDate = date.plusMonths(2),
                        type = PlacementType.DAYCARE,
                    )
                )
            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placementId,
                    daycareGroupId = groupId,
                    startDate = date.minusMonths(2),
                    endDate = date.plusMonths(2),
                )
            )
            child
        }
    }

    private fun createChildDocumentWithDecision(
        childId: ChildId,
        templateId: DocumentTemplateId,
        decisionMaker: DevEmployee,
        status: ChildDocumentDecisionStatus,
        validity: DateRange?,
        unitId: DaycareId?,
        clock: MockEvakaClock,
    ) {
        db.transaction { tx ->
            tx.insert(
                DevChildDocument(
                    status = DocumentStatus.COMPLETED,
                    childId = childId,
                    templateId = templateId,
                    decisionMaker = decisionMaker.id,
                    content =
                        DocumentContent(
                            answers =
                                listOf(
                                    AnsweredQuestion.CheckboxAnswer(
                                        questionId = "q1",
                                        answer = true,
                                    )
                                )
                        ),
                    publishedContent =
                        DocumentContent(
                            answers =
                                listOf(
                                    AnsweredQuestion.CheckboxAnswer(
                                        questionId = "q1",
                                        answer = true,
                                    )
                                )
                        ),
                    publishedAt = clock.now(),
                    publishedBy = decisionMaker.evakaUserId,
                    modifiedAt = clock.now(),
                    modifiedBy = decisionMaker.evakaUserId,
                    contentLockedAt = clock.now(),
                    contentLockedBy = decisionMaker.id,
                    decision =
                        DevChildDocumentDecision(
                            createdBy = decisionMaker.id,
                            modifiedBy = decisionMaker.id,
                            status = status,
                            validity = validity,
                            daycareId = unitId,
                        ),
                )
            )
        }
    }

    @Test
    fun `document decision counts aggregates by template name`() {
        val setup = setupDocumentDecisionTest()

        // Create two templates with the same name but different types
        val template1 =
            createDecisionTemplate(
                "Special Support Decision",
                ChildDocumentType.OTHER_DECISION,
                setup.date,
                validity = DateRange(setup.date.minusMonths(6), setup.date.minusMonths(1)),
            )
        val template2 =
            createDecisionTemplate(
                "Special Support Decision",
                ChildDocumentType.OTHER_DECISION,
                setup.date,
                validity = DateRange(setup.date.minusMonths(2), setup.date.plusMonths(6)),
            )

        db.transaction { tx ->
            tx.insert(template1)
            tx.insert(template2)
        }

        // Create two children, each with a decision using different templates
        val child1 =
            createChildWithPlacement("Child 1", setup.groups[0].id, setup.unit.id, setup.date)
        val child2 =
            createChildWithPlacement("Child 2", setup.groups[0].id, setup.unit.id, setup.date)

        createChildDocumentWithDecision(
            child1.id,
            template1.id,
            setup.decisionMaker,
            ChildDocumentDecisionStatus.ACCEPTED,
            DateRange(setup.date.minusMonths(1), setup.date.plusMonths(1)),
            setup.unit.id,
            setup.clock,
        )

        createChildDocumentWithDecision(
            child2.id,
            template2.id,
            setup.decisionMaker,
            ChildDocumentDecisionStatus.ACCEPTED,
            DateRange(setup.date.minusMonths(1), setup.date.plusMonths(1)),
            setup.unit.id,
            setup.clock,
        )

        // Test group report - should aggregate both templates under the same name
        val groupReport =
            controller.getAssistanceNeedsAndActionsReport(
                dbInstance(),
                admin,
                setup.clock,
                setup.date,
                emptyList(),
                emptyList(),
                emptyList(),
                emptyList(),
                includeDecisions = true,
            )

        assertEquals(
            mapOf("Special Support Decision" to 2),
            groupReport.rows.find { it.groupId == setup.groups[0].id }?.documentDecisionCounts,
        )

        // Test child report - each child should have their own count
        val childReport =
            controller.getAssistanceNeedsAndActionsReportByChild(
                dbInstance(),
                admin,
                setup.clock,
                setup.date,
                emptyList(),
                emptyList(),
                emptyList(),
                emptyList(),
                includeDecisions = true,
            )

        assertEquals(
            mapOf("Special Support Decision" to 1),
            childReport.rows.find { it.childId == child1.id }?.documentDecisionCounts,
        )
        assertEquals(
            mapOf("Special Support Decision" to 1),
            childReport.rows.find { it.childId == child2.id }?.documentDecisionCounts,
        )
    }

    @Test
    fun `document decision counts excludes rejected decisions`() {
        val setup = setupDocumentDecisionTest()

        val template =
            createDecisionTemplate("Support Decision", ChildDocumentType.OTHER_DECISION, setup.date)
        db.transaction { tx -> tx.insert(template) }

        val child =
            createChildWithPlacement("Child 1", setup.groups[0].id, setup.unit.id, setup.date)

        // Create one accepted and one rejected decision
        createChildDocumentWithDecision(
            child.id,
            template.id,
            setup.decisionMaker,
            ChildDocumentDecisionStatus.ACCEPTED,
            DateRange(setup.date.minusMonths(1), setup.date.plusMonths(1)),
            setup.unit.id,
            setup.clock,
        )

        createChildDocumentWithDecision(
            child.id,
            template.id,
            setup.decisionMaker,
            ChildDocumentDecisionStatus.REJECTED,
            null, // REJECTED decisions must have null validity
            null,
            setup.clock,
        )

        val groupReport =
            controller.getAssistanceNeedsAndActionsReport(
                dbInstance(),
                admin,
                setup.clock,
                setup.date,
                emptyList(),
                emptyList(),
                emptyList(),
                emptyList(),
                includeDecisions = true,
            )

        // Should only count the accepted decision
        assertEquals(
            mapOf("Support Decision" to 1),
            groupReport.rows.find { it.groupId == setup.groups[0].id }?.documentDecisionCounts,
        )
    }

    @Test
    fun `document decision counts excludes expired decisions`() {
        val setup = setupDocumentDecisionTest()

        val template =
            createDecisionTemplate("Support Decision", ChildDocumentType.OTHER_DECISION, setup.date)
        db.transaction { tx -> tx.insert(template) }

        val child =
            createChildWithPlacement("Child 1", setup.groups[0].id, setup.unit.id, setup.date)

        // Create one current and one expired decision
        createChildDocumentWithDecision(
            child.id,
            template.id,
            setup.decisionMaker,
            ChildDocumentDecisionStatus.ACCEPTED,
            DateRange(setup.date.minusMonths(1), setup.date.plusMonths(1)),
            setup.unit.id,
            setup.clock,
        )

        createChildDocumentWithDecision(
            child.id,
            template.id,
            setup.decisionMaker,
            ChildDocumentDecisionStatus.ACCEPTED,
            DateRange(setup.date.minusYears(1), setup.date.minusMonths(1)),
            setup.unit.id,
            setup.clock,
        )

        val groupReport =
            controller.getAssistanceNeedsAndActionsReport(
                dbInstance(),
                admin,
                setup.clock,
                setup.date,
                emptyList(),
                emptyList(),
                emptyList(),
                emptyList(),
                includeDecisions = true,
            )

        // Should only count the current decision
        assertEquals(
            mapOf("Support Decision" to 1),
            groupReport.rows.find { it.groupId == setup.groups[0].id }?.documentDecisionCounts,
        )
    }

    @Test
    fun `includeDecisions flag controls document decision counting`() {
        val setup = setupDocumentDecisionTest()

        val template =
            createDecisionTemplate("Support Decision", ChildDocumentType.OTHER_DECISION, setup.date)
        db.transaction { tx -> tx.insert(template) }

        val child =
            createChildWithPlacement("Child 1", setup.groups[0].id, setup.unit.id, setup.date)

        createChildDocumentWithDecision(
            child.id,
            template.id,
            setup.decisionMaker,
            ChildDocumentDecisionStatus.ACCEPTED,
            DateRange(setup.date.minusMonths(1), setup.date.plusMonths(1)),
            setup.unit.id,
            setup.clock,
        )

        // With includeDecisions = true
        val reportWithDecisions =
            controller.getAssistanceNeedsAndActionsReport(
                dbInstance(),
                admin,
                setup.clock,
                setup.date,
                emptyList(),
                emptyList(),
                emptyList(),
                emptyList(),
                includeDecisions = true,
            )

        assertEquals(
            mapOf("Support Decision" to 1),
            reportWithDecisions.rows
                .find { it.groupId == setup.groups[0].id }
                ?.documentDecisionCounts,
        )

        // With includeDecisions = false
        val reportWithoutDecisions =
            controller.getAssistanceNeedsAndActionsReport(
                dbInstance(),
                admin,
                setup.clock,
                setup.date,
                emptyList(),
                emptyList(),
                emptyList(),
                emptyList(),
                includeDecisions = false,
            )

        assertEquals(
            emptyMap(),
            reportWithoutDecisions.rows
                .find { it.groupId == setup.groups[0].id }
                ?.documentDecisionCounts,
        )
    }

    @Test
    fun `document decision counts include decisions from expired templates if decision is still valid`() {
        val setup = setupDocumentDecisionTest()

        // Create a template that expired 1 month ago (but was valid when decision was made)
        val expiredTemplate =
            createDecisionTemplate(
                name = "Legacy Support Decision",
                type = ChildDocumentType.OTHER_DECISION,
                date = setup.date,
                validity = DateRange(setup.date.minusMonths(6), setup.date.minusMonths(1)),
            )

        db.transaction { tx -> tx.insert(expiredTemplate) }

        val child =
            createChildWithPlacement("Child 1", setup.groups[0].id, setup.unit.id, setup.date)

        // Create a decision that was made 3 months ago (when template was still valid),
        // and the decision's validity extends 2 months into the future
        createChildDocumentWithDecision(
            child.id,
            expiredTemplate.id,
            setup.decisionMaker,
            ChildDocumentDecisionStatus.ACCEPTED,
            DateRange(
                setup.date.minusMonths(3),
                setup.date.plusMonths(2),
            ), // Decision is currently valid
            setup.unit.id,
            setup.clock,
        )

        val groupReport =
            controller.getAssistanceNeedsAndActionsReport(
                dbInstance(),
                admin,
                setup.clock,
                setup.date,
                emptyList(),
                emptyList(),
                emptyList(),
                emptyList(),
                includeDecisions = true,
            )

        // Should count the decision even though template has expired,
        // because the decision itself is still valid
        assertEquals(
            mapOf("Legacy Support Decision" to 1),
            groupReport.rows.find { it.groupId == setup.groups[0].id }?.documentDecisionCounts,
        )

        // Verify in child report as well
        val childReport =
            controller.getAssistanceNeedsAndActionsReportByChild(
                dbInstance(),
                admin,
                setup.clock,
                setup.date,
                emptyList(),
                emptyList(),
                emptyList(),
                emptyList(),
                includeDecisions = true,
            )

        assertEquals(
            mapOf("Legacy Support Decision" to 1),
            childReport.rows.find { it.childId == child.id }?.documentDecisionCounts,
        )
    }

    private fun insertAssistanceData(
        unit: DevDaycare,
        date: LocalDate,
        groups: List<DevDaycareGroup>,
        placementType: PlacementType = PlacementType.DAYCARE,
    ): List<DevPerson> {
        val child1 =
            db.transaction { tx ->
                val child = DevPerson(firstName = "Test 1", dateOfBirth = date.minusYears(3))
                tx.insert(child, DevPersonType.CHILD)
                val placementId =
                    tx.insert(
                        DevPlacement(
                            childId = child.id,
                            unitId = unit.id,
                            startDate = date,
                            endDate = date,
                            type = placementType,
                        )
                    )
                tx.insert(
                    DevDaycareGroupPlacement(
                        daycarePlacementId = placementId,
                        daycareGroupId = groups[0].id,
                        startDate = date,
                        endDate = date,
                    )
                )
                tx.insert(
                    DevDaycareAssistance(
                        childId = child.id,
                        validDuring = FiniteDateRange(date, date),
                        level = DaycareAssistanceLevel.GENERAL_SUPPORT,
                    )
                )
                tx.insert(
                    DevAssistanceAction(
                        childId = child.id,
                        startDate = date,
                        endDate = date,
                        actions = setOf("ASSISTANCE_SERVICE_CHILD"),
                    )
                )
                tx.insert(
                    DevAssistanceNeedVoucherCoefficient(
                        childId = child.id,
                        validityPeriod = FiniteDateRange(date, date),
                        coefficient = BigDecimal(1.50),
                    )
                )
                child
            }
        val child2 =
            db.transaction { tx ->
                val child = DevPerson(firstName = "Test 2", dateOfBirth = date.minusYears(3))
                tx.insert(child, DevPersonType.CHILD)
                val placementId =
                    tx.insert(
                        DevPlacement(
                            childId = child.id,
                            unitId = unit.id,
                            startDate = date,
                            endDate = date,
                            type = placementType,
                        )
                    )
                tx.insert(
                    DevDaycareGroupPlacement(
                        daycarePlacementId = placementId,
                        daycareGroupId = groups[0].id,
                        startDate = date,
                        endDate = date,
                    )
                )
                tx.insert(
                    DevDaycareAssistance(
                        childId = child.id,
                        validDuring = FiniteDateRange(date, date),
                        level = DaycareAssistanceLevel.GENERAL_SUPPORT,
                    )
                )
                tx.insert(
                    DevAssistanceAction(
                        childId = child.id,
                        startDate = date,
                        endDate = date,
                        otherAction = "other action test",
                    )
                )
                child
            }
        val child3 =
            db.transaction { tx ->
                val child = DevPerson(firstName = "Test 3", dateOfBirth = date.minusYears(3))
                tx.insert(child, DevPersonType.CHILD)
                val placementId =
                    tx.insert(
                        DevPlacement(
                            childId = child.id,
                            unitId = unit.id,
                            startDate = date,
                            endDate = date,
                            type = placementType,
                        )
                    )
                tx.insert(
                    DevDaycareGroupPlacement(
                        daycarePlacementId = placementId,
                        daycareGroupId = groups[1].id,
                        startDate = date,
                        endDate = date,
                    )
                )
                tx.insert(
                    DevDaycareAssistance(
                        childId = child.id,
                        validDuring = FiniteDateRange(date, date),
                        level = DaycareAssistanceLevel.GENERAL_SUPPORT,
                    )
                )
                tx.insert(
                    DevAssistanceAction(
                        childId = child.id,
                        startDate = date,
                        endDate = date,
                        actions = setOf("ASSISTANCE_SERVICE_CHILD"),
                    )
                )
                child
            }
        return listOf(child1, child2, child3)
    }
}
