// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.assistance.DaycareAssistanceLevel
import fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionEmployee
import fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionStatus
import fi.espoo.evaka.assistanceneed.decision.ServiceOptions
import fi.espoo.evaka.assistanceneed.decision.StructuralMotivationOptions
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.insertAssistanceActionOptions
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevAssistanceAction
import fi.espoo.evaka.shared.dev.DevAssistanceNeedDecision
import fi.espoo.evaka.shared.dev.DevAssistanceNeedPreschoolDecision
import fi.espoo.evaka.shared.dev.DevAssistanceNeedVoucherCoefficient
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareAssistance
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.emptyAssistanceNeedPreschoolDecisionForm
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.domain.OfficialLanguage
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
                    daycareAssistanceNeedDecisionCount = 0,
                    preschoolAssistanceNeedDecisionCount = 0,
                    unitProviderType = ProviderType.MUNICIPAL,
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
                    daycareAssistanceNeedDecisionCount = 0,
                    preschoolAssistanceNeedDecisionCount = 0,
                    unitProviderType = ProviderType.MUNICIPAL,
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
                    daycareAssistanceNeedDecisionCount = 0,
                    preschoolAssistanceNeedDecisionCount = 0,
                    unitProviderType = ProviderType.MUNICIPAL,
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
                    daycareAssistanceNeedDecisionCount = 0,
                    preschoolAssistanceNeedDecisionCount = 0,
                    unitProviderType = ProviderType.MUNICIPAL,
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
                    daycareAssistanceNeedDecisionCount = 0,
                    preschoolAssistanceNeedDecisionCount = 0,
                    unitProviderType = ProviderType.MUNICIPAL,
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
                    daycareAssistanceNeedDecisionCount = 0,
                    preschoolAssistanceNeedDecisionCount = 0,
                    unitProviderType = ProviderType.MUNICIPAL,
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
                    daycareAssistanceNeedDecisionCount = 0,
                    preschoolAssistanceNeedDecisionCount = 0,
                    unitProviderType = ProviderType.MUNICIPAL,
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
                    daycareAssistanceNeedDecisionCount = 0,
                    preschoolAssistanceNeedDecisionCount = 0,
                    unitProviderType = ProviderType.MUNICIPAL,
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
                    daycareAssistanceNeedDecisionCount = 0,
                    preschoolAssistanceNeedDecisionCount = 0,
                    unitProviderType = ProviderType.MUNICIPAL,
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
                    daycareAssistanceNeedDecisionCount = 0,
                    preschoolAssistanceNeedDecisionCount = 0,
                    unitProviderType = ProviderType.MUNICIPAL,
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
                    daycareAssistanceNeedDecisionCount = 0,
                    preschoolAssistanceNeedDecisionCount = 0,
                    unitProviderType = ProviderType.MUNICIPAL,
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
                    daycareAssistanceNeedDecisionCount = 0,
                    preschoolAssistanceNeedDecisionCount = 0,
                    unitProviderType = ProviderType.MUNICIPAL,
                ),
            ),
            emptyChildReport.rows,
        )
    }

    @Test
    fun `assistance decision counting works`() {
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

        db.transaction { tx ->
            tx.insert(
                DevAssistanceNeedDecision(
                    decisionNumber = 10000,
                    childId = children[0].id,
                    validityPeriod = DateRange(date.minusYears(1), null),
                    status = AssistanceNeedDecisionStatus.ACCEPTED,
                    language = OfficialLanguage.FI,
                    decisionMade = date.minusYears(1),
                    sentForDecision = date.minusYears(1),
                    selectedUnit = null,
                    preparedBy1 =
                        AssistanceNeedDecisionEmployee(
                            employeeId = admin.id,
                            title = "title",
                            name = "admin",
                            phoneNumber = "111",
                        ),
                    preparedBy2 = null,
                    decisionMaker =
                        AssistanceNeedDecisionEmployee(
                            employeeId = admin.id,
                            title = "title",
                            name = "admin",
                            phoneNumber = "111",
                        ),
                    pedagogicalMotivation = null,
                    structuralMotivationOptions =
                        StructuralMotivationOptions(
                            smallerGroup = false,
                            specialGroup = false,
                            smallGroup = false,
                            groupAssistant = false,
                            childAssistant = false,
                            additionalStaff = false,
                        ),
                    structuralMotivationDescription = null,
                    careMotivation = null,
                    serviceOptions =
                        ServiceOptions(
                            consultationSpecialEd = false,
                            partTimeSpecialEd = false,
                            fullTimeSpecialEd = false,
                            interpretationAndAssistanceServices = false,
                            specialAides = false,
                        ),
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
                    endDateNotKnown = false,
                )
            )
            tx.insert(
                DevAssistanceNeedDecision(
                    decisionNumber = 10000,
                    childId = children[1].id,
                    validityPeriod = DateRange(date.minusYears(1), null),
                    status = AssistanceNeedDecisionStatus.ACCEPTED,
                    language = OfficialLanguage.FI,
                    decisionMade = date.minusYears(1),
                    sentForDecision = date.minusYears(1),
                    selectedUnit = null,
                    preparedBy1 =
                        AssistanceNeedDecisionEmployee(
                            employeeId = admin.id,
                            title = "title",
                            name = "admin",
                            phoneNumber = "111",
                        ),
                    preparedBy2 = null,
                    decisionMaker =
                        AssistanceNeedDecisionEmployee(
                            employeeId = admin.id,
                            title = "title",
                            name = "admin",
                            phoneNumber = "111",
                        ),
                    pedagogicalMotivation = null,
                    structuralMotivationOptions =
                        StructuralMotivationOptions(
                            smallerGroup = false,
                            specialGroup = false,
                            smallGroup = false,
                            groupAssistant = false,
                            childAssistant = false,
                            additionalStaff = false,
                        ),
                    structuralMotivationDescription = null,
                    careMotivation = null,
                    serviceOptions =
                        ServiceOptions(
                            consultationSpecialEd = false,
                            partTimeSpecialEd = false,
                            fullTimeSpecialEd = false,
                            interpretationAndAssistanceServices = false,
                            specialAides = false,
                        ),
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
                    endDateNotKnown = false,
                )
            )
            tx.insert(
                DevAssistanceNeedPreschoolDecision(
                    decisionNumber = 10001,
                    childId = children[1].id,
                    status = AssistanceNeedDecisionStatus.ACCEPTED,
                    decisionMade = date.minusYears(1),
                    sentForDecision = date.minusYears(1),
                    unreadGuardianIds = emptySet(),
                    annulmentReason = "",
                    form =
                        emptyAssistanceNeedPreschoolDecisionForm.copy(
                            validFrom = date.minusYears(1),
                            validTo = null,
                            selectedUnit = unit.id,
                            preparer1EmployeeId = admin.id,
                            decisionMakerEmployeeId = admin.id,
                        ),
                )
            )
        }

        val groupReport =
            controller.getAssistanceNeedsAndActionsReport(
                dbInstance(),
                admin,
                clock,
                date,
                DaycareAssistanceLevel.entries,
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
                    daycareAssistanceNeedDecisionCount = 2,
                    preschoolAssistanceNeedDecisionCount = 1,
                    unitProviderType = ProviderType.MUNICIPAL,
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
                    daycareAssistanceNeedDecisionCount = 0,
                    preschoolAssistanceNeedDecisionCount = 0,
                    unitProviderType = ProviderType.MUNICIPAL,
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
                    daycareAssistanceNeedDecisionCount = 0,
                    preschoolAssistanceNeedDecisionCount = 0,
                    unitProviderType = ProviderType.MUNICIPAL,
                ),
            ),
            groupReport.rows,
        )

        val childReport =
            controller.getAssistanceNeedsAndActionsReportByChild(
                dbInstance(),
                admin,
                clock,
                date,
                DaycareAssistanceLevel.entries,
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
                    daycareAssistanceNeedDecisionCount = 1,
                    preschoolAssistanceNeedDecisionCount = 0,
                    unitProviderType = ProviderType.MUNICIPAL,
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
                    daycareAssistanceNeedDecisionCount = 1,
                    preschoolAssistanceNeedDecisionCount = 1,
                    unitProviderType = ProviderType.MUNICIPAL,
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
                    daycareAssistanceNeedDecisionCount = 0,
                    preschoolAssistanceNeedDecisionCount = 0,
                    unitProviderType = ProviderType.MUNICIPAL,
                ),
            ),
            childReport.rows,
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
                daycareAssistanceNeedDecisionCount = 0,
                preschoolAssistanceNeedDecisionCount = 0,
                unitProviderType = ProviderType.MUNICIPAL,
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
                    daycareAssistanceNeedDecisionCount = 0,
                    preschoolAssistanceNeedDecisionCount = 0,
                    unitProviderType = ProviderType.MUNICIPAL,
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
                    daycareAssistanceNeedDecisionCount = 0,
                    preschoolAssistanceNeedDecisionCount = 0,
                    unitProviderType = ProviderType.MUNICIPAL,
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
                    daycareAssistanceNeedDecisionCount = 0,
                    preschoolAssistanceNeedDecisionCount = 0,
                    unitProviderType = ProviderType.MUNICIPAL,
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
                    daycareAssistanceNeedDecisionCount = 0,
                    preschoolAssistanceNeedDecisionCount = 0,
                    unitProviderType = ProviderType.MUNICIPAL,
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
                    daycareAssistanceNeedDecisionCount = 0,
                    preschoolAssistanceNeedDecisionCount = 0,
                    unitProviderType = ProviderType.MUNICIPAL,
                ),
            ),
            preschoolDaycareChildReport.rows,
        )

        assertEquals(0, daycareChildReport.rows.size)
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
