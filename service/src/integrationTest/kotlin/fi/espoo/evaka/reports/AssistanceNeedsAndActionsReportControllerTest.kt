// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.assistance.DaycareAssistanceLevel
import fi.espoo.evaka.insertAssistanceActionOptions
import fi.espoo.evaka.shared.FeatureConfig
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.config.testFeatureConfig
import fi.espoo.evaka.shared.dev.DevAssistanceAction
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
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.security.actionrule.AccessControlFilter
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class AssistanceNeedsAndActionsReportControllerTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var controller: AssistanceNeedsAndActionsReportController

    val featureConfig: FeatureConfig = testFeatureConfig
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

        val unitId =
            db.transaction { tx ->
                val areaId = tx.insert(DevCareArea())
                tx.insert(DevDaycare(areaId = areaId))
            }
        val group1Id =
            db.transaction { tx ->
                tx.insert(DevDaycareGroup(daycareId = unitId, name = "Group 1", startDate = date))
            }
        val group2Id =
            db.transaction { tx ->
                tx.insert(DevDaycareGroup(daycareId = unitId, name = "Group 2", startDate = date))
            }
        val group3Id =
            db.transaction { tx ->
                tx.insert(DevDaycareGroup(daycareId = unitId, name = "Group 3", startDate = date))
            }

        val child1Id =
            db.transaction { tx ->
                val childId =
                    tx.insert(
                        DevPerson(firstName = "Test 1", dateOfBirth = date.minusYears(3)),
                        DevPersonType.CHILD
                    )
                val placementId =
                    tx.insert(
                        DevPlacement(
                            childId = childId,
                            unitId = unitId,
                            startDate = date,
                            endDate = date
                        )
                    )
                tx.insert(
                    DevDaycareGroupPlacement(
                        daycarePlacementId = placementId,
                        daycareGroupId = group1Id,
                        startDate = date,
                        endDate = date
                    )
                )
                tx.insert(
                    DevDaycareAssistance(
                        childId = childId,
                        validDuring = FiniteDateRange(date, date),
                        level = DaycareAssistanceLevel.GENERAL_SUPPORT
                    )
                )
                tx.insert(
                    DevAssistanceAction(
                        childId = childId,
                        startDate = date,
                        endDate = date,
                        actions = setOf("ASSISTANCE_SERVICE_CHILD")
                    )
                )
                tx.insert(
                    DevAssistanceNeedVoucherCoefficient(
                        childId = childId,
                        validityPeriod = FiniteDateRange(date, date),
                        coefficient = BigDecimal(1.50)
                    )
                )
                childId
            }
        val child2Id =
            db.transaction { tx ->
                val childId =
                    tx.insert(
                        DevPerson(firstName = "Test 2", dateOfBirth = date.minusYears(3)),
                        DevPersonType.CHILD
                    )
                val placementId =
                    tx.insert(
                        DevPlacement(
                            childId = childId,
                            unitId = unitId,
                            startDate = date,
                            endDate = date
                        )
                    )
                tx.insert(
                    DevDaycareGroupPlacement(
                        daycarePlacementId = placementId,
                        daycareGroupId = group1Id,
                        startDate = date,
                        endDate = date
                    )
                )
                tx.insert(
                    DevDaycareAssistance(
                        childId = childId,
                        validDuring = FiniteDateRange(date, date),
                        level = DaycareAssistanceLevel.GENERAL_SUPPORT
                    )
                )
                tx.insert(
                    DevAssistanceAction(
                        childId = childId,
                        startDate = date,
                        endDate = date,
                        otherAction = "other action test"
                    )
                )
                childId
            }
        val child3Id =
            db.transaction { tx ->
                val childId =
                    tx.insert(
                        DevPerson(firstName = "Test 3", dateOfBirth = date.minusYears(3)),
                        DevPersonType.CHILD
                    )
                val placementId =
                    tx.insert(
                        DevPlacement(
                            childId = childId,
                            unitId = unitId,
                            startDate = date,
                            endDate = date
                        )
                    )
                tx.insert(
                    DevDaycareGroupPlacement(
                        daycarePlacementId = placementId,
                        daycareGroupId = group2Id,
                        startDate = date,
                        endDate = date
                    )
                )
                tx.insert(
                    DevDaycareAssistance(
                        childId = childId,
                        validDuring = FiniteDateRange(date, date),
                        level = DaycareAssistanceLevel.GENERAL_SUPPORT
                    )
                )
                tx.insert(
                    DevAssistanceAction(
                        childId = childId,
                        startDate = date,
                        endDate = date,
                        actions = setOf("ASSISTANCE_SERVICE_CHILD")
                    )
                )
                childId
            }

        val groupReport =
            controller.getAssistanceNeedsAndActionsReport(dbInstance(), admin, clock, date)
        assertEquals(
            listOf(
                AssistanceNeedsAndActionsReportController.AssistanceNeedsAndActionsReportRow(
                    careAreaName = "Test Care Area",
                    unitId = unitId,
                    unitName = "Test Daycare",
                    groupId = group1Id,
                    groupName = "Group 1",
                    actionCounts = mapOf("ASSISTANCE_SERVICE_CHILD" to 1),
                    otherActionCount = 1,
                    noActionCount = 0,
                    daycareAssistanceCounts = mapOf(DaycareAssistanceLevel.GENERAL_SUPPORT to 2),
                    preschoolAssistanceCounts = emptyMap(),
                    otherAssistanceMeasureCounts = emptyMap(),
                    assistanceNeedVoucherCoefficientCount = 1
                ),
                AssistanceNeedsAndActionsReportController.AssistanceNeedsAndActionsReportRow(
                    careAreaName = "Test Care Area",
                    unitId = unitId,
                    unitName = "Test Daycare",
                    groupId = group2Id,
                    groupName = "Group 2",
                    actionCounts = mapOf("ASSISTANCE_SERVICE_CHILD" to 1),
                    otherActionCount = 0,
                    noActionCount = 0,
                    daycareAssistanceCounts = mapOf(DaycareAssistanceLevel.GENERAL_SUPPORT to 1),
                    preschoolAssistanceCounts = emptyMap(),
                    otherAssistanceMeasureCounts = emptyMap(),
                    assistanceNeedVoucherCoefficientCount = 0
                ),
                AssistanceNeedsAndActionsReportController.AssistanceNeedsAndActionsReportRow(
                    careAreaName = "Test Care Area",
                    unitId = unitId,
                    unitName = "Test Daycare",
                    groupId = group3Id,
                    groupName = "Group 3",
                    actionCounts = emptyMap(),
                    otherActionCount = 0,
                    noActionCount = 0,
                    daycareAssistanceCounts = emptyMap(),
                    preschoolAssistanceCounts = emptyMap(),
                    otherAssistanceMeasureCounts = emptyMap(),
                    assistanceNeedVoucherCoefficientCount = 0
                )
            ),
            groupReport.rows
        )

        val childReport =
            db.transaction { tx ->
                controller.getAssistanceNeedsAndActionsReportByChild(
                    tx,
                    date,
                    AccessControlFilter.PermitAll,
                    true
                )
            }
        assertEquals(
            listOf(
                AssistanceNeedsAndActionsReportController.AssistanceNeedsAndActionsReportRowByChild(
                    careAreaName = "Test Care Area",
                    unitId = unitId,
                    unitName = "Test Daycare",
                    groupId = group1Id,
                    groupName = "Group 1",
                    childId = child1Id,
                    childLastName = "Person",
                    childFirstName = "Test 1",
                    childAge = 3,
                    actions = setOf("ASSISTANCE_SERVICE_CHILD"),
                    otherAction = "",
                    daycareAssistanceCounts = mapOf(DaycareAssistanceLevel.GENERAL_SUPPORT to 1),
                    preschoolAssistanceCounts = emptyMap(),
                    otherAssistanceMeasureCounts = emptyMap(),
                    assistanceNeedVoucherCoefficient = BigDecimal("1.50")
                ),
                AssistanceNeedsAndActionsReportController.AssistanceNeedsAndActionsReportRowByChild(
                    careAreaName = "Test Care Area",
                    unitId = unitId,
                    unitName = "Test Daycare",
                    groupId = group1Id,
                    groupName = "Group 1",
                    childId = child2Id,
                    childLastName = "Person",
                    childFirstName = "Test 2",
                    childAge = 3,
                    actions = emptySet(),
                    otherAction = "other action test",
                    daycareAssistanceCounts = mapOf(DaycareAssistanceLevel.GENERAL_SUPPORT to 1),
                    preschoolAssistanceCounts = emptyMap(),
                    otherAssistanceMeasureCounts = emptyMap(),
                    assistanceNeedVoucherCoefficient = BigDecimal("1.0")
                ),
                AssistanceNeedsAndActionsReportController.AssistanceNeedsAndActionsReportRowByChild(
                    careAreaName = "Test Care Area",
                    unitId = unitId,
                    unitName = "Test Daycare",
                    groupId = group2Id,
                    groupName = "Group 2",
                    childId = child3Id,
                    childLastName = "Person",
                    childFirstName = "Test 3",
                    childAge = 3,
                    actions = setOf("ASSISTANCE_SERVICE_CHILD"),
                    otherAction = "",
                    daycareAssistanceCounts = mapOf(DaycareAssistanceLevel.GENERAL_SUPPORT to 1),
                    preschoolAssistanceCounts = emptyMap(),
                    otherAssistanceMeasureCounts = emptyMap(),
                    assistanceNeedVoucherCoefficient = BigDecimal("1.0")
                )
            ),
            childReport.rows
        )
    }
}
