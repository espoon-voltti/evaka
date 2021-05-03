// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.occupancy

import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.DevAssistanceNeed
import fi.espoo.evaka.shared.dev.DevChild
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.insertTestApplication
import fi.espoo.evaka.shared.dev.insertTestAssistanceNeed
import fi.espoo.evaka.shared.dev.insertTestChild
import fi.espoo.evaka.shared.dev.insertTestPerson
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.dev.insertTestPlacementPlan
import fi.espoo.evaka.shared.dev.insertTestServiceNeed
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testDecisionMaker_1
import java.time.LocalDate
import java.util.UUID

fun Database.Transaction.createOccupancyTestFixture(
    unitId: UUID,
    period: FiniteDateRange,
    dateOfBirth: LocalDate,
    placementType: PlacementType,
    hours: Double? = null,
    assistanceExtra: Double? = null
) = createOccupancyTestFixture(
    UUID.randomUUID(),
    unitId,
    period,
    dateOfBirth,
    placementType,
    hours,
    assistanceExtra
)

fun Database.Transaction.createOccupancyTestFixture(
    childId: UUID,
    unitId: UUID,
    period: FiniteDateRange,
    dateOfBirth: LocalDate,
    placementType: PlacementType,
    hours: Double? = null,
    assistanceExtra: Double? = null,
    placementId: UUID = UUID.randomUUID()
) {
    insertTestPerson(DevPerson(id = childId, dateOfBirth = dateOfBirth))
    insertTestChild(DevChild(id = childId))
    insertTestPlacement(
        childId = childId,
        unitId = unitId,
        type = placementType,
        startDate = period.start,
        endDate = period.end,
        id = placementId
    )
    if (hours != null) {
        insertTestServiceNeed(
            childId = childId,
            hoursPerWeek = hours,
            startDate = period.start,
            endDate = period.end,
            updatedBy = testDecisionMaker_1.id
        )
    }
    if (assistanceExtra != null) {
        insertTestAssistanceNeed(
            DevAssistanceNeed(
                childId = childId,
                startDate = period.start,
                endDate = period.end,
                capacityFactor = assistanceExtra,
                updatedBy = testDecisionMaker_1.id
            )
        )
    }
}

fun Database.Transaction.createPlanOccupancyTestFixture(
    unitId: UUID,
    period: FiniteDateRange,
    dateOfBirth: LocalDate,
    placementType: PlacementType,
    assistanceExtra: Double? = null,
    preschoolDaycarePeriod: FiniteDateRange = period,
    deletedPlacementPlan: Boolean? = false
) = createPlanOccupancyTestFixture(
    UUID.randomUUID(),
    unitId,
    period,
    dateOfBirth,
    placementType,
    assistanceExtra,
    preschoolDaycarePeriod,
    deletedPlacementPlan
)

fun Database.Transaction.createPlanOccupancyTestFixture(
    childId: UUID,
    unitId: UUID,
    period: FiniteDateRange,
    dateOfBirth: LocalDate,
    placementType: PlacementType,
    assistanceExtra: Double? = null,
    preschoolDaycarePeriod: FiniteDateRange = period,
    deletedPlacementPlan: Boolean? = false
) {
    insertTestPerson(DevPerson(id = childId, dateOfBirth = dateOfBirth))
    insertTestChild(DevChild(id = childId))
    val applicationId = insertTestApplication(childId = childId, guardianId = testAdult_1.id)
    insertTestPlacementPlan(
        applicationId = applicationId,
        unitId = unitId,
        type = placementType,
        startDate = period.start,
        endDate = period.end,
        preschoolDaycareStartDate = preschoolDaycarePeriod.start,
        preschoolDaycareEndDate = preschoolDaycarePeriod.end,
        deleted = deletedPlacementPlan
    )
    if (assistanceExtra != null) {
        insertTestAssistanceNeed(
            DevAssistanceNeed(
                childId = childId,
                startDate = period.start,
                endDate = period.end,
                capacityFactor = assistanceExtra,
                updatedBy = testDecisionMaker_1.id
            )
        )
    }
}

fun Database.Transaction.createPreschoolPlanWithDistinctDaycareOccupancyTestFixture(
    unitId: UUID,
    period: FiniteDateRange,
    dateOfBirth: LocalDate,
    daycarePeriod: FiniteDateRange
) = createPlanOccupancyTestFixture(unitId, period, dateOfBirth, PlacementType.PRESCHOOL_DAYCARE, null, daycarePeriod)
